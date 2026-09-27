#!/usr/bin/env bash
set -euo pipefail
SDK="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"
export PATH="$SDK/cmdline-tools/latest/bin:$SDK/platform-tools:$PATH"
export ANDROID_USER_HOME="${RUNNER_TEMP:-/tmp}/pocket-android"
export ANDROID_EMULATOR_HOME="$ANDROID_USER_HOME"
export ANDROID_AVD_HOME="$ANDROID_USER_HOME/avd"
mkdir -p "$ANDROID_AVD_HOME"
APK="app/build/outputs/apk/debug/app-debug.apk"
"$SDK/build-tools/35.0.0/apksigner" verify --verbose "$APK"
sdkmanager 'system-images;android-35;google_apis;x86_64' 'emulator'
echo no | avdmanager create avd --force --name pocket-test --path "$ANDROID_AVD_HOME/pocket-test.avd" --package 'system-images;android-35;google_apis;x86_64'
sudo chmod a+rw /dev/kvm
"$SDK/emulator/emulator" -avd pocket-test -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -no-snapshot > /tmp/pocket-emulator.log 2>&1 &
EMULATOR_PID=$!
trap 'cat /tmp/pocket-emulator.log; kill "$EMULATOR_PID" || true' EXIT
timeout 90 adb wait-for-device
READY=false
for attempt in $(seq 1 120); do
  if [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = 1 ]; then READY=true; break; fi
  sleep 2
done
[ "$READY" = true ]
adb shell input keyevent 82
adb install "$APK"
adb shell am start -W -n co.pocket.companion.preview/co.pocket.companion.MainActivity
sleep 3
adb shell uiautomator dump /sdcard/pocket-ui.xml
adb pull /sdcard/pocket-ui.xml /tmp/pocket-ui.xml
grep -q 'Create Pocket account' /tmp/pocket-ui.xml
adb install -r "$APK"
echo 'APK signature, clean install, launch and same-key reinstall passed on Android 35.'
