# Moonlight Android

## Moonlight 터치 (unofficial personal fork)

This public fork modifies official `moonlight-stream/moonlight-android` commit
`b48494cb96bff23d8886c4775cc4f39a1075495d`. Modified on 2026-11-10 by Donnowhoo.
The `touch` branch adds an F3 touch shortcut bar and optional touch-only work gestures.
Korean realtime IME composition (F4) and F1/F2 drag-lock/acceleration are deferred.
See [F3-LOCAL-REVIEW.md](F3-LOCAL-REVIEW.md) for behavior, settings, thresholds and verification limits.
It is not the official Moonlight application. Existing copyright notices and
`LICENSE.txt` are retained. No code from other unofficial forks has been copied.

The test APK is named **Moonlight 터치** and uses
`com.donnowhoo.moonlight.touch.debug`, so it installs beside the official app.
It requires separate pairing. See [TOUCH-ANDROID.md](TOUCH-ANDROID.md) for build,
signing responsibilities, verification, and source correspondence.
The upstream download links and build instructions below describe the official
project, not this fork's test APK. Do not install Android build tools on the live
Sunshine host for this project; our build runs only on GitHub-hosted runners.

[![AppVeyor Build Status](https://ci.appveyor.com/api/projects/status/232a8tadrrn8jv0k/branch/master?svg=true)](https://ci.appveyor.com/project/cgutman/moonlight-android/branch/master)
[![Translation Status](https://hosted.weblate.org/widgets/moonlight/-/moonlight-android/svg-badge.svg)](https://hosted.weblate.org/projects/moonlight/moonlight-android/)

[Moonlight for Android](https://moonlight-stream.org) is an open source client for NVIDIA GameStream and [Sunshine](https://github.com/LizardByte/Sunshine).

Moonlight for Android will allow you to stream your full collection of games from your Windows PC to your Android device,
whether in your own home or over the internet.

Moonlight also has a [PC client](https://github.com/moonlight-stream/moonlight-qt) and [iOS/tvOS client](https://github.com/moonlight-stream/moonlight-ios).

You can follow development on our [Discord server](https://moonlight-stream.org/discord) and help translate Moonlight into your language on [Weblate](https://hosted.weblate.org/projects/moonlight/moonlight-android/).

## Downloads
* [Google Play Store](https://play.google.com/store/apps/details?id=com.limelight)
* [Amazon App Store](https://www.amazon.com/gp/product/B00JK4MFN2)
* [F-Droid](https://f-droid.org/packages/com.limelight)
* [APK](https://github.com/moonlight-stream/moonlight-android/releases)

## Building
* Install Android Studio and the Android NDK
* Run ‘git submodule update --init --recursive’ from within moonlight-android/
* In moonlight-android/, create a file called ‘local.properties’. Add an ‘ndk.dir=’ property to the local.properties file and set it equal to your NDK directory.
* Build the APK using Android Studio or gradle

## Authors

* [Cameron Gutman](https://github.com/cgutman)  
* [Diego Waxemberg](https://github.com/dwaxemberg)  
* [Aaron Neyer](https://github.com/Aaronneyer)  
* [Andrew Hennessy](https://github.com/yetanothername)

Moonlight is the work of students at [Case Western](http://case.edu) and was
started as a project at [MHacks](http://mhacks.org).
