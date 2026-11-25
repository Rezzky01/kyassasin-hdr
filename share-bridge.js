import { Share } from "@capacitor/share";
import { Filesystem, Directory } from "@capacitor/filesystem";

window.KYASSASINShare = {
  async shareFile(file, kind) {
    const bytes = new Uint8Array(await file.arrayBuffer());
    let binary = "";
    const chunkSize = 0x8000;
    for (let i = 0; i < bytes.length; i += chunkSize) {
      binary += String.fromCharCode(...bytes.subarray(i, i + chunkSize));
    }
    const safeName = file.name.replace(/[^a-zA-Z0-9._-]/g, "_");
    const saved = await Filesystem.writeFile({
      path: "kyassasin-hdr/" + safeName,
      data: btoa(binary),
      directory: Directory.Cache,
      recursive: true
    });
    return Share.share({
      title: "KYASSASIN HDR - " + kind,
      text: "Hasil recovery KYASSASIN HDR",
      files: [saved.uri],
      dialogTitle: "Bagikan hasil ke"
    });
  }
};
