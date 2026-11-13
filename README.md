# Overworld

A game-themed navigation app for Android (with Android Auto support). "Overworld" is a placeholder name.

**Stack ($0):** Kotlin + Jetpack Compose, MapLibre Native, OpenFreeMap tiles (OpenStreetMap data),
Geoapify for search + routing (free tier), Android for Cars App Library for the car screen,
GitHub Actions for builds.

## What works in this first version
- Onboarding: promise, then pick your world (4 map skins). No sign-up.
- Themed 3D map from real OpenStreetMap data (skins = colour sets in `Skins.kt`).
- Search, route planning, turn instructions, arrival, and a **Demo drive** (simulated movement, so you can test without driving).
- Location asked only when you tap the locate button.
- Android Auto: service + navigation template + drawable surface. **The car map is a placeholder drawing for now**; rendering the real MapLibre map to the car surface is the next milestone.

## Build the APK on GitHub (no local Android setup)
1. Get a free key at https://www.geoapify.com (no card).
2. Repo -> Settings -> Secrets and variables -> Actions -> New repository secret: `GEOAPIFY_KEY`.
3. Actions tab -> **Build APK** -> Run workflow (it also runs on every push).
4. Download the `overworld-debug-apk` artifact and install the APK on your phone.

Without a key the map still loads, but search and routing show a message.
The key is compiled into the APK, so restrict it in the Geoapify dashboard before sharing builds.

## Test on a car screen (no car needed)
1. Install the debug APK. In the Android Auto app, enable Developer settings (tap Version 10 times)
   and turn on **Unknown sources**.
2. Overflow menu -> **Start head unit server**.
3. On a computer: `adb forward tcp:5277 tcp:5277`, then run the **Desktop Head Unit** (`desktop-head-unit`
   from the Android SDK's `extras/google/auto`).
4. Open Overworld from the DHU launcher.
Then test on a real car or an aftermarket Android Auto unit.

## Swapping providers
Search and routing sit behind `GeoProvider` (`GeoProvider.kt`). To move to self-hosted Photon + Valhalla,
add a second implementation and change one line in `MapScreen.kt`.

## Before a Play Store release
- Replace the package name, name, and icon (all placeholders).
- Replace `ALLOW_ALL_HOSTS_VALIDATOR` in `OverworldCarService.kt` with Google's Android Auto host allow-list.
- Apply for the navigation app category, add a privacy policy, sign a release build, run the required closed test.
- Keep the OpenStreetMap / OpenFreeMap attribution (MapLibre shows it by default).
