# Perfect Dark Android Port (with Netplay)

This repository contains an Android port of the [Perfect Dark decompilation](https://github.com/n64decomp/perfect_dark), including experimental **Multiplayer Netplay** support (powered by ENet).

See [docs/netplay.md](docs/netplay.md) for more details on how the netplay system works.

To run the port, you must already have a Perfect Dark ROM, specifically one of the following:
* `ntsc-final`/`US V1.1`/`US Rev 1` (md5 `e03b088b6ac9e0080440efed07c1e40f`).  
  **This is the recommended version to use**.  
  Called `NTSC version 8.7 final` on the boot screen.
* `ntsc-1.0`/`US V1.0` (md5 `7f4171b0c8d17815be37913f535e4e93`).  
  Technically supported, but not recommended.  
  Called `NTSC version 8.7 final` on the boot screen as well.
* `jpn-final` (md5 `538d2b75945eae069b29c46193e74790`).  
  Technically supported, but requires a separate custom-built executable.  
  Called `JPN version 8.9 final` on the boot screen.
* `pal-final` (md5 `d9b5cd305d228424891ce38e71bc9213`).  
  Technically supported, but requires a separate custom-built executable.  
  Called `PAL 8.7 final` on the boot screen.

## Status

The game is in a mostly functional state, with singleplayer, split-screen multiplayer, and online/LAN netplay modes working.  
There are minor graphics- and gameplay-related issues, and possibly occasional crashes.

**The following extra features are implemented:**
* Multiplayer Netplay (Host/Join rooms, LAN and Online via ENet UDP);
* On-screen touch controls with customizable layout;
* Dual analog and external gamepad support;
* Widescreen resolution support;
* Configurable field of view;
* 60 FPS support, including fixes for framerate-related issues;
* External MP3 voice dubbing and streaming mixer;
* Basic mod support;
* Emulated Transfer Pak functionality (`pd.gbc`).

**Supported Android architectures:**
* arm64-v8a
* armeabi-v7a
* x86_64
* x86

## Download

Latest automatic builds are available via GitHub Actions. Check the Actions tab for the latest APK releases.

## Running

You must already have a Perfect Dark ROM to run the game, as specified above.

1. Install the APK on your Android device
2. Launch the app and grant storage permissions when prompted
3. Select your Perfect Dark ROM file when asked
4. The game will start automatically

If you want to use a PAL or JPN ROM instead, select the appropriate ROM file when prompted.

Optionally, you can also put your Perfect Dark for GameBoy Color ROM named `pd.gbc` in the app's data directory if you want to emulate having the Nintendo 64's Transfer Pak and unlock some cheats automatically.

A GPU supporting OpenGL ES 3.0 or above is required to run the port.

## Controls

The Android port includes touch controls and supports external controllers.

1964GEPD-style and Xbox-style bindings are implemented for external controllers.

N64 pad buttons X and Y (or `X_BUTTON`, `Y_BUTTON` in the code) refer to the reserved buttons `0x40` and `0x80`, which are also leveraged by 1964GEPD.

Default control scheme for external controllers is as follows:

| Action           | Xbox pad                 | N64 pad                   |
| -                | -                        | -                         |
| Fire / Accept    | RT                       | Z Trigger                 |
| Aim mode         | LT                       | R Trigger                 |
| Use / Cancel     | N/A                      | B                         |
| Use / Accept     | A                        | A                         |
| Crouch cycle     | L3                       | `0x80000000` (Extra)      |
| Half-Crouch      | N/A                      | `0x40000000` (Extra)      |
| Full-Crouch      | N/A                      | `0x20000000` (Extra)      |
| Reload           | X                        | X `(0x40)`                |
| Previous weapon  | B                        | D-Left                    |
| Next weapon      | Y                        | Y `(0x80)`                |
| Radial menu      | LB                       | D-Down                    |
| Alt fire mode    | RB                       | L Trigger                 |
| Alt-fire oneshot | `A + RT` or  `RB + RT`   | `A + Z`     or `L + Z`    |
| Quick-detonate   | `A + B`  or  `A + X`     | `A + D-Left`or `A + X`    |

## Building

### Android

1. Install Android Studio and set up the Android SDK
2. Get the source code:  
   `git clone --recursive https://github.com/WINDROID-EMU/PERFECT-DARK-PORT-ANDROID.git && cd perfect_dark`
3. Open the `android` directory in Android Studio
4. Build the APK using Android Studio's build system or Gradle:
   ```bash
   cd android
   ./gradlew assembleRelease
   ```
5. The resulting APK will be at `android/app/build/outputs/apk/release/app-release.apk`
