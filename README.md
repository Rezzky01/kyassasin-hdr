# DONGHUA COMIC ID

Aplikasi Android untuk membuka katalog dan episode Anichin langsung di dalam aplikasi.

- Sumber: https://anichin.moe/
- Android application ID: id.kyassasin.donghuacomicid
- Minimum Android: 6.0 (API 23)
- Compile SDK: 35
- Android Gradle Plugin: 8.7.3
- Build Gradle: 8.9
- Java: 17

## Fitur aplikasi
- Situs Anichin dibuka langsung di WebView aplikasi.
- JavaScript, DOM storage, cookie pihak ketiga, dan media HTML5.
- Mode fullscreen untuk pemutar video.
- Tombol kembali mengikuti riwayat halaman.
- Pemilih file untuk input dari halaman web.
- Unduhan web diteruskan ke Download Manager Android.
- Indikator progres dan pesan gangguan koneksi.

## Build
Di lingkungan yang memiliki JDK 17, Android SDK Platform 35, dan Gradle 8.9:
`gradle --no-daemon assembleDebug`

APK debug: `app/build/outputs/apk/debug/app-debug.apk`

GitHub Actions membangun APK setiap ada push dan mengunggahnya sebagai artifact bernama **DonghuaComicID-debug-apk**.

## Batasan sumber video
Aplikasi ini menampilkan situs Anichin di dalam WebView. Daftar donghua dan episode berasal dari situs tersebut, bukan database lokal buatan aplikasi. Pemutaran tergantung pada player dan penyedia video yang dipakai Anichin. Tidak ada bypass DRM, login, pembatasan wilayah, atau akses berbayar. Karena situs dan player pihak ketiga dapat berubah atau membatasi WebView, semua episode tidak dapat dijamin bisa diputar.

## Status
Source dan workflow sudah disiapkan. Status APK harus ditentukan dari hasil GitHub Actions; belum boleh dianggap berhasil sebelum workflow selesai dan APK artifact terverifikasi.
