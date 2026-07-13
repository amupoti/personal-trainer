# Personal Trainer

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

## Publish a signed APK

The release workflow publishes an installable APK to GitHub Releases whenever a
tag beginning with `v` is pushed. The repository must be public for anyone to
download the APK without a GitHub account.

Create the signing key once and keep both the keystore and its passwords backed
up. Losing it prevents future APKs from updating an existing installation.

```shell
keytool -genkeypair \
  -keystore release.jks \
  -alias personal-trainer \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

Create a public GitHub repository, push this project, then configure these
Actions secrets in the repository:

- `RELEASE_KEYSTORE_BASE64`: output of `base64 -i release.jks | tr -d '\n'`
- `RELEASE_STORE_PASSWORD`: keystore password
- `RELEASE_KEY_ALIAS`: `personal-trainer`
- `RELEASE_KEY_PASSWORD`: key password

Publish a version:

```shell
git tag v1.0.0
git push origin v1.0.0
```

Before each release, update `README.md` and `docs/backlog.md`.

The app checks this repository's latest release from **Settings**. When a newer
version is available, select **Download and install**, allow **Install unknown
apps** for Personal Trainer when prompted, and confirm the Android installation.
Future versions signed with the same key can be installed over the existing app
without losing its data.
