# Royal Treasure ASMR — Android prototype

A native Android game prototype built with Kotlin and Jetpack Compose. The core loop is a
jewellery-placement puzzle: drag a royal accessory from the tray and drop it near its matching
position on the princess portrait.

## Implemented in this source ZIP

- Portrait Android activity (min Android 7.0 / API 24).
- One playable Rajasthani royal portrait puzzle with five transparent PNG jewellery pieces.
- Drag-and-drop placement with snap tolerance and correct/incorrect feedback.
- Progress counter, reference-image toggle, hint, reset, pause and sound toggle.
- Developer/test overlay showing target anchors and snap areas.
- Completion dialog and basic tone feedback.
- GitHub Actions workflow that builds a **debug APK** and uploads it as an artifact.

## Not implemented yet

- Multiple fully-authored levels or a level-select/map screen.
- Persistent save/resume across app restarts.
- Real ASMR audio/music, haptics, rewarded ads, banner ads or an in-app No Ads purchase.
- Production signing, Play Store listing, analytics, privacy policy or device QA.

The promotional screenshots supplied with the request are visual references. The playable portrait
and transparent jewellery assets bundled in `app/src/main/res/drawable/` are the assets currently
used by the app. Do not assume the reference screenshots themselves are playable game content.

## Build a test APK using GitHub Actions

Requirements: a GitHub account and an empty GitHub repository.

1. Extract this ZIP.
2. Create a new GitHub repository, for example `RoyalTreasureASMR`.
3. In Termux, enter the extracted project folder and run the commands in `TERMUX_GUIDE_HI.txt`.
4. Open the repository on GitHub → **Actions** → **Build Android APK** → **Run workflow**.
5. When the workflow succeeds, open that run and download the artifact
   `Royal-Treasure-ASMR-Debug-APK`.
6. Extract the artifact ZIP and install `app-debug.apk` on an Android test phone. You may need to
   allow installation from that file manager/browser.

## Build configuration

- JDK 17
- Gradle 8.9 (provided by the GitHub workflow)
- Android Gradle Plugin 8.7.3
- Kotlin 2.0.21
- compileSdk 35 / targetSdk 35 / minSdk 24

## Important

This is source code, not a prebuilt or verified APK. This environment does not have the Android SDK,
Gradle or an Android device, so an APK build and real-device test could not be run here. The GitHub
Actions build must complete successfully before treating the APK as installable. A debug APK is for
testing only; publishing requires a properly signed release build and Play App Signing.


## v3.0 gameplay update

This source update adds eight accessory drag-and-drop targets, a 180-second timer, five lives, hint/reset/pause/reference controls, a 10-round progression loop, and local unlocked-round saving. The rounds currently reuse the bundled princess artwork and target layout; later art/content updates are needed for truly distinct stages. See `BUILD_STATUS.txt` and `QA_CHECKLIST_HI.md` before testing.
