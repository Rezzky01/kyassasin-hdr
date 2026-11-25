import * as ort from "onnxruntime-web";

const MODEL_URL = "https://huggingface.co/thisismert/real-esrgan-onnx/resolve/main/onnx/realesr-general-x4v3.onnx";
const DB_NAME = "kyassasin-ai-models";
const STORE = "models";
const MODEL_KEY = "realesr-general-x4v3";
const TILE = 128;
const OVERLAP = 12;
let sessionPromise;

function openDb() {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, 1);
    req.onupgradeneeded = () => req.result.createObjectStore(STORE);
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}
async function readCachedModel() {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, "readonly");
    const req = tx.objectStore(STORE).get(MODEL_KEY);
    req.onsuccess = () => { db.close(); resolve(req.result || null); };
    req.onerror = () => { db.close(); reject(req.error); };
  });
}
async function cacheModel(buffer) {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, "readwrite");
    tx.objectStore(STORE).put(buffer, MODEL_KEY);
    tx.oncomplete = () => { db.close(); resolve(); };
    tx.onerror = () => { db.close(); reject(tx.error); };
  });
}
async function loadModel(onProgress) {
  let buffer = await readCachedModel();
  if (buffer) {
    onProgress?.("Model AI sudah tersimpan di HP. Menyiapkan mesin AI...");
    return buffer;
  }
  onProgress?.("Mengunduh model AI pertama kali (sekitar 5 MB)...");
  const response = await fetch(MODEL_URL);
  if (!response.ok) throw new Error("Model AI gagal diunduh. Periksa koneksi internet.");
  const total = Number(response.headers.get("content-length")) || 0;
  if (response.body?.getReader) {
    const reader = response.body.getReader();
    const chunks = [];
    let received = 0;
    while (true) {
      const {done, value} = await reader.read();
      if (done) break;
      chunks.push(value);
      received += value.length;
      if (total) onProgress?.("Mengunduh model AI... " + Math.round(received / total * 100) + "%");
    }
    const bytes = new Uint8Array(received);
    let offset = 0;
    for (const chunk of chunks) { bytes.set(chunk, offset); offset += chunk.length; }
    buffer = bytes.buffer;
  } else {
    buffer = await response.arrayBuffer();
  }
  await cacheModel(buffer);
  onProgress?.("Model AI tersimpan. Menyiapkan mesin...");
  return buffer;
}
async function getSession(onProgress) {
  if (!sessionPromise) {
    sessionPromise = (async () => {
      ort.env.wasm.wasmPaths = new URL("./", import.meta.url).href;
      ort.env.wasm.numThreads = 1;
      ort.env.wasm.proxy = false;
      const model = await loadModel(onProgress);
      return ort.InferenceSession.create(model, {
        executionProviders: ["wasm"],
        graphOptimizationLevel: "all"
      });
    })().catch(error => { sessionPromise = null; throw error; });
  }
  return sessionPromise;
}

function makeInput(source, sourceWidth, sourceHeight, left, top) {
  const input = new Float32Array(3 * TILE * TILE);
  for (let y = 0; y < TILE; y++) {
    const sy = Math.min(sourceHeight - 1, top + y);
    for (let x = 0; x < TILE; x++) {
      const sx = Math.min(sourceWidth - 1, left + x);
      const p = (sy * sourceWidth + sx) * 4;
      const i = y * TILE + x;
      input[i] = source.data[p] / 255;
      input[TILE * TILE + i] = source.data[p + 1] / 255;
      input[2 * TILE * TILE + i] = source.data[p + 2] / 255;
    }
  }
  return input;
}

export async function enhanceImageFile(file, onProgress = () => {}) {
  if (!file?.type?.startsWith("image/")) throw new Error("Pilih file foto yang valid.");
  const session = await getSession(onProgress);
  onProgress("Membaca foto...");
  const bitmap = await createImageBitmap(file);
  // Keep the original input dimensions; no 768px downscale.
  const width = bitmap.width;
  const height = bitmap.height;
  if (!width || !height) { bitmap.close?.(); throw new Error("Resolusi foto tidak valid."); }
  if (width * height * 16 > 1_500_000_000) { bitmap.close?.(); throw new Error("Foto terlalu besar untuk memori perangkat pada keluaran AI 4×. Coba foto dengan resolusi lebih rendah."); }
  const inputCanvas = document.createElement("canvas");
  inputCanvas.width = width;
  inputCanvas.height = height;
  const inputCtx = inputCanvas.getContext("2d", {willReadFrequently: true});
  inputCtx.drawImage(bitmap, 0, 0, width, height);
  bitmap.close?.();
  const source = inputCtx.getImageData(0, 0, width, height);

  const outputCanvas = document.createElement("canvas");
  outputCanvas.width = width * 4;
  outputCanvas.height = height * 4;
  const outputCtx = outputCanvas.getContext("2d");
  const step = TILE - OVERLAP * 2;
  const totalTiles = Math.ceil(width / step) * Math.ceil(height / step);
  let completed = 0;

  for (let top = 0; top < height; top += step) {
    for (let left = 0; left < width; left += step) {
      const input = new ort.Tensor("float32", makeInput(source, width, height, left, top), [1, 3, TILE, TILE]);
      const result = await session.run({[session.inputNames[0]]: input});
      const tensor = result[session.outputNames[0]];
      const dims = tensor.dims;
      if (!dims || dims.length !== 4 || dims[0] !== 1 || dims[1] !== 3) throw new Error("Format keluaran AI tidak dikenali: " + JSON.stringify(dims));
      const outHeight = dims[2], outWidth = dims[3], plane = outWidth * outHeight;
      if (outWidth !== TILE * 4 || outHeight !== TILE * 4) throw new Error("Ukuran hasil AI tidak sesuai: " + outWidth + "x" + outHeight);
      const rgba = new Uint8ClampedArray(plane * 4);
      for (let i = 0; i < plane; i++) {
        rgba[i*4] = Math.max(0,Math.min(255,Math.round(tensor.data[i]*255)));
        rgba[i*4+1] = Math.max(0,Math.min(255,Math.round(tensor.data[plane+i]*255)));
        rgba[i*4+2] = Math.max(0,Math.min(255,Math.round(tensor.data[plane*2+i]*255)));
        rgba[i*4+3] = 255;
      }
      const tileImage = new ImageData(rgba,outWidth,outHeight);
      const tileCanvas = document.createElement("canvas");
      tileCanvas.width=outWidth; tileCanvas.height=outHeight;
      const tileCtx=tileCanvas.getContext("2d"); tileCtx.putImageData(tileImage,0,0);
      const validW=Math.min(TILE,width-left), validH=Math.min(TILE,height-top);
      const cropLeft=left===0?0:OVERLAP*4, cropTop=top===0?0:OVERLAP*4;
      const cropRight=left+TILE>=width?validW*4:(TILE-OVERLAP)*4;
      const cropBottom=top+TILE>=height?validH*4:(TILE-OVERLAP)*4;
      const cropWidth=Math.max(1,cropRight-cropLeft), cropHeight=Math.max(1,cropBottom-cropTop);
      outputCtx.drawImage(tileCanvas,cropLeft,cropTop,cropWidth,cropHeight,
        (left+(left===0?0:OVERLAP))*4,(top+(top===0?0:OVERLAP))*4,cropWidth,cropHeight);
      completed++;
      onProgress("AI sedang memulihkan detail foto... " + Math.round(completed / totalTiles * 100) + "%");
      await new Promise(resolve => setTimeout(resolve, 0));
    }
  }
  const blob = await new Promise(resolve => outputCanvas.toBlob(resolve, "image/png"));
  if (!blob) throw new Error("Gagal membuat hasil foto AI.");
  return new File([blob], "KYASSASIN-AI-ULTRA-HD.png", {type: "image/png", lastModified: Date.now()});
}
