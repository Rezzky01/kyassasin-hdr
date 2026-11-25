# KYASSASIN HDR

Android/Web app for recovering locally available WhatsApp status media and applying light visual enhancement.

## Firebase setup required before first use

1. Open the Firebase project configured in `index.html`.
2. In **Authentication → Sign-in method**, enable **Email/Password** and **Phone**.
3. In **Authentication → Settings → Authorized domains**, add the GitHub Pages host for this repository and `localhost` for the Capacitor Android WebView.
4. Create/enable **Cloud Firestore**.
5. Publish the rules in `firestore.rules` to the project's Firestore database.
6. The administrator is restricted by the UID configured as `ADMIN_UID` in `index.html`. The admin must sign in using that exact Firebase Auth account.

Email accounts use email + password and email verification. Phone accounts use SMS OTP. Firebase Phone Auth does not provide a secure phone-number-plus-password credential by itself; phone sign-in is OTP-based.

## APK build

The GitHub Actions workflow **Build KYASSASIN HDR APK** builds a debug APK. Open the repository's **Actions** tab, select the workflow, and download the `kyassasin-hdr-debug-apk` artifact after a successful run.

## Important limitations

- Recovery can only read media that WhatsApp or Android still makes available on the device; it cannot retrieve deleted media from WhatsApp servers.
- Image/video enhancement applies visual filters and sharpening. It cannot recreate detail that was never captured.
- Sharing opens Android's share chooser; the user must select WhatsApp and confirm publishing the Status.
- Enable and test Firebase Auth providers and Firestore rules before public use. A successful APK build alone does not configure the Firebase backend.
