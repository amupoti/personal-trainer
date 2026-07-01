#!/bin/sh

set -eu

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

is_jdk_17() {
    [ -x "$1/bin/java" ] &&
        "$1/bin/java" -version 2>&1 | head -n 1 | grep -Eq 'version "17(\.|")'
}

if [ -z "${JAVA_HOME:-}" ] || ! is_jdk_17 "$JAVA_HOME"; then
    JAVA_HOME=
    for candidate in \
        /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
        /usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
    do
        if is_jdk_17 "$candidate"; then
            JAVA_HOME=$candidate
            break
        fi
    done
fi

if [ -z "${JAVA_HOME:-}" ] && [ -x /usr/libexec/java_home ]; then
    JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null || true)
fi

if [ -z "${JAVA_HOME:-}" ] || ! is_jdk_17 "$JAVA_HOME"; then
    echo "JDK 17 was not found. Install it with: brew install openjdk@17" >&2
    exit 1
fi

if [ -z "${ANDROID_HOME:-}" ] || [ ! -d "$ANDROID_HOME/platforms/android-36" ]; then
    ANDROID_HOME=
    for candidate in \
        "$HOME/Library/Android/sdk" \
        /opt/homebrew/share/android-commandlinetools \
        /usr/local/share/android-commandlinetools \
        "$HOME/Android/Sdk"
    do
        if [ -d "$candidate/platforms/android-36" ]; then
            ANDROID_HOME=$candidate
            break
        fi
    done
fi

if [ -z "${ANDROID_HOME:-}" ] || [ ! -d "$ANDROID_HOME/platforms/android-36" ]; then
    echo "Android SDK Platform 36 was not found. Install it with sdkmanager." >&2
    exit 1
fi

export JAVA_HOME
export ANDROID_HOME
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$project_dir/.gradle}"

exec "$project_dir/gradlew" test assembleDebug "$@"
