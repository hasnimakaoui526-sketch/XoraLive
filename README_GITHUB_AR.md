Xora Live - Android project

1) Upload the CONTENTS of this folder to the root of the private GitHub repository:
   hasnimakaoui526-sketch/XoraLive
   Keep the .github folder (it contains the APK build workflow).
2) Wait for the GitHub Actions workflow named "Build Xora Live APK" to finish.
3) Open Actions > Build Xora Live APK > latest successful run > Artifacts > XoraLive-debug-apk.
4) Download the artifact ZIP, extract it, and install app-debug.apk on Android.

This is a source project package, not a prebuilt APK. The workflow builds a debug APK; it does not sign a Play Store release APK.
