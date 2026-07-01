# Daily Movement

An offline Android app for tracking completion of daily back exercises, stretches,
walks, and runs.

## Requirements

- JDK 17
- Android SDK Platform 36 and Build Tools 36

The build script detects standard Homebrew, Android Studio, and Linux SDK
locations automatically.

## Build an installable APK

```shell
./build-apk.sh
```

If your tools are installed elsewhere, set `JAVA_HOME` and `ANDROID_HOME`
before running the script. Alternatively, create `local.properties`:

```properties
sdk.dir=/path/to/Android/sdk
```

The APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install it on a connected phone with USB debugging enabled:

```shell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
