const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { timingSafeEqual } = require("node:crypto");

initializeApp();

const ADMIN_UID = "fxzhHNEoDmOBQfoLxGoI0HPDR4q1";
const ADMIN_BOOTSTRAP_SECRET = defineSecret("ADMIN_BOOTSTRAP_SECRET");

exports.adminBootstrap = onCall(
  {
    region: "asia-southeast1",
    secrets: [ADMIN_BOOTSTRAP_SECRET],
    maxInstances: 2,
  },
  async (request) => {
    const supplied = request.data && request.data.secret;
    const expected = ADMIN_BOOTSTRAP_SECRET.value();

    if (typeof supplied !== "string" || !expected || supplied.length < 32) {
      throw new HttpsError("unauthenticated", "Kode aktivasi tidak valid.");
    }

    const suppliedBuffer = Buffer.from(supplied, "utf8");
    const expectedBuffer = Buffer.from(expected, "utf8");
    if (
      suppliedBuffer.length !== expectedBuffer.length ||
      !timingSafeEqual(suppliedBuffer, expectedBuffer)
    ) {
      throw new HttpsError("unauthenticated", "Kode aktivasi tidak valid.");
    }

    const adminUser = await getAuth().getUser(ADMIN_UID);
    if (adminUser.disabled) {
      throw new HttpsError("permission-denied", "Akun administrator dinonaktifkan.");
    }

    const customToken = await getAuth().createCustomToken(ADMIN_UID, {
      adminBootstrap: true,
    });
    return { token: customToken };
  }
);
