# Xora Live — Android wrapper project

This Android Studio project packages the existing Xora Live web app locally inside an Android WebView.

- `app/src/main/assets/index.html`: original splash + Supabase email/password login/signup page.
- `app/src/main/assets/home.html`: latest RealtimeKit main screen from the existing real-login bundle.
- Package ID: `com.xoralive.app`
- Camera and microphone runtime permissions are wired to WebView permission requests.
- WebViewAssetLoader serves local pages from an HTTPS-style origin so browser APIs that require a secure context can work.

## Build

Open this folder in Android Studio (Hedgehog or newer), allow Gradle sync, then run **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

A GitHub Actions workflow is included at `.github/workflows/build-apk.yml`. Once this project is pushed to a GitHub repository, it builds a debug APK and uploads it as the `XoraLive-debug-apk` workflow artifact.

This source package is **not itself an APK**. The APK has not been built or device-tested in this environment because Android SDK/Gradle are not installed here.
