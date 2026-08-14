# Perfect Dark Android Port

This repository contains an Android port of the [Perfect Dark decompilation](https://github.com/n64decomp/perfect_dark).

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

The game is in a mostly functional state, with both singleplayer and split-screen multiplayer modes fully working.  
There are minor graphics- and gameplay-related issues, and possibly occasional crashes.

**The following extra features are implemented:**
* mouselook;
* dual analog controller support;
* widescreen resolution support;
* configurable field of view;
* 60 FPS support, including fixes for some framerate-related issues;
* fixes for a couple original bugs and crashes;
* basic mod support, currently enough to load a few custom levels;
* slightly expanded memory heap size;
* experimental high framerate support (up to 240 FPS):
  * enable `Uncap Tickrate` in `Extended Video Options` to activate;
  * in practice the game will have issues running faster than ~165 FPS, so use VSync or `Video.FramerateLimit` to cap it.
* emulate the Transfer Pak functionality the game has on the Nintendo 64 to unlock some cheats automatically.

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

Support for one controller, two-stick configurations are enabled for 1.2.

Default control scheme for external controllers is as follows:

|| Action           | Xbox pad                 | N64 pad                   |
|| -                | -                        | -                         |
|| Fire / Accept    | RT                       | Z Trigger                 |
|| Aim mode         | LT                       | R Trigger                 |
|| Use / Cancel     | N/A                      | B                         |
|| Use / Accept     | A                        | A                         |
|| Crouch cycle     | L3                       | `0x80000000` (Extra)      |
|| Half-Crouch      | N/A                      | `0x40000000` (Extra)      |
|| Full-Crouch      | N/A                      | `0x20000000` (Extra)      |
|| Reload           | X                        | X `(0x40)`                |
|| Previous weapon  | B                        | D-Left                    |
|| Next weapon      | Y                        | Y `(0x80)`                |
|| Radial menu      | LB                       | D-Down                    |
|| Alt fire mode    | RB                       | L Trigger                 |
|| Alt-fire oneshot | `A + RT` or  `RB + RT`   | `A + Z`     or `L + Z`    |
|| Quick-detonate   | `A + B`  or  `A + X`     | `A + D-Left`or `A + X`    |

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

### Notes

The Android port uses the native CMake build system integrated with Gradle. The CMakeLists.txt in the root directory is used by the Android build system.

You will need to provide a `jpn-final` or `pal-final` ROM to run builds for those regions.

## Credits

* the original [decompilation project](https://github.com/n64decomp/perfect_dark) authors;
* Ryan Dwyer for the above, additional help, and `pd-extract`;
* doomhack for the only other publicly available [PD porting effort](https://github.com/doomhack/perfect_dark) I could find;
* [sm64-port](https://github.com/sm64-port/sm64-port) authors for the audio mixer and some other changes;
* [Ship of Harkinian team](https://github.com/Kenix3/libultraship/tree/main/src/graphic/Fast3D), Emill and MaikelChan for the libultraship version of fast3d that this port uses;
* lieff for [minimp3](https://github.com/lieff/minimp3);
* Mouse Injector and 1964GEPD authors for some of the 60FPS- and mouselook-related fixes;
* Raf for the 64-bit port;
* NicNamSam for the icon;
* everyone who has submitted pull requests and issues to this repository and tested the port;
* probably more I'm forgetting.
