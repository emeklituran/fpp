#!/usr/bin/env bash
# Emülatörde: her release APK kurulur, soğuk başlatılır, çökme denetlenir, ekran görüntüsü alınır;
# ardından debug APK + test APK ile cihaz üstü (instrumentation) testler çalıştırılır.
# Kullanım: tools/device_tests.sh <ci_klasörü> [parça_no] [parça_sayısı]
set -uo pipefail
CI=${1:?ci klasörü}
SHARD=${2:-0}
SHARDS=${3:-1}
REPORT="$CI/device-$SHARD.md"
mkdir -p "$CI/screenshots"
AAPT2=$(ls "$ANDROID_HOME"/build-tools/*/aapt2 | sort -V | tail -n 1)
fail=0

# Emülatör bağlantısı koparsa yeniden bağlanmayı dener (CI ortamındaki geçici sorunlara karşı).
ensure_device() {
  for _ in $(seq 1 12); do
    if [ "$(adb get-state 2>/dev/null)" = "device" ] && [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; then
      return 0
    fi
    adb reconnect offline >/dev/null 2>&1 || true
    sleep 5
  done
  echo "Cihaz yanıt vermiyor"; return 1
}
ensure_device
adb shell settings put global window_animation_scale 0 || true
adb shell settings put global transition_animation_scale 0 || true
adb shell settings put global animator_duration_scale 0 || true
{
  echo "# Cihaz test raporu"
  echo
  echo "Parça $((SHARD + 1))/$SHARDS · Emülatör: Android $(adb shell getprop ro.build.version.release | tr -d '\r') (API $(adb shell getprop ro.build.version.sdk | tr -d '\r')), $(adb shell wm size | tr -d '\r')"
  echo
  echo "| Paket | Kurulum | Açılış (soğuk) | Çökme | Yeniden açılış | Cihaz testleri |"
  echo "|---|---|---|---|---|---|"
} > "$REPORT"

lost=0
idx=0
for apk in "$CI"/release/*.apk; do
  idx=$((idx + 1))
  [ $(( (idx - 1) % SHARDS )) -ne "$SHARD" ] && continue
  pkg=$("$AAPT2" dump badging "$apk" | sed -n "s/^package: name='\([^']*\)'.*/\1/p")
  install="TAMAM"; launch="-"; crash="yok"; relaunch="-"; tests="-"
  if [ "$lost" = 1 ] || ! ensure_device; then
    lost=1; fail=1
    echo "| \`$pkg\` | TEST EDİLEMEDİ (emülatör bağlantısı koptu) | - | - | - | - |" >> "$REPORT"
    continue
  fi
  adb uninstall "$pkg" >/dev/null 2>&1 || true
  if ! adb install "$apk" >/dev/null 2>&1; then
    ensure_device || true
    adb install "$apk" >/dev/null 2>&1 || { install="BAŞARISIZ"; fail=1; }
  fi
  if [ "$install" = "TAMAM" ]; then
    activity=$(adb shell cmd package resolve-activity --brief -c android.intent.category.LAUNCHER "$pkg" | tail -n 1 | tr -d '\r')
    adb logcat -c || true
    t=$(adb shell am start -W -n "$activity" | sed -n 's/^TotalTime: //p' | tr -d '\r')
    launch="${t:-?} ms"
    sleep 4
    adb exec-out screencap -p > "$CI/screenshots/$pkg.png" || true
    if adb logcat -d -b crash | grep -q "$pkg"; then crash="VAR"; fail=1; adb logcat -d -b crash | tail -n 40; fi
    [ -z "$(adb shell pidof "$pkg" | tr -d '\r')" ] && { crash="süreç kapandı"; fail=1; }
    # Uygulamayı tamamen kapatıp yeniden aç (veri kalıcılığı ve soğuk başlangıç).
    adb shell am force-stop "$pkg"
    t2=$(adb shell am start -W -n "$activity" | sed -n 's/^TotalTime: //p' | tr -d '\r')
    sleep 2
    [ -n "$(adb shell pidof "$pkg" | tr -d '\r')" ] && relaunch="TAMAM (${t2:-?} ms)" || { relaunch="BAŞARISIZ"; fail=1; }
    adb shell am force-stop "$pkg"
  fi
  # Cihaz üstü testler (varsa)
  if [ -f "$CI/debug/$pkg.test.apk" ]; then
    ensure_device || true
    adb uninstall "$pkg" >/dev/null 2>&1 || true
    adb install "$CI/debug/$pkg.apk" >/dev/null && adb install "$CI/debug/$pkg.test.apk" >/dev/null
    out=$(timeout 240 adb shell am instrument -w "$pkg.test/androidx.test.runner.AndroidJUnitRunner" | tr -d '\r')
    echo "----- $pkg instrumentation -----"; echo "$out" | tail -n 30
    if echo "$out" | grep -q "^OK ("; then tests="GEÇTİ $(echo "$out" | grep -o 'OK ([0-9]* tests\?)')"
    else tests="KALDI"; fail=1; fi
    adb uninstall "$pkg.test" >/dev/null 2>&1 || true
  fi
  echo "| \`$pkg\` | $install | $launch | $crash | $relaunch | $tests |" >> "$REPORT"
done
echo >> "$REPORT"
cat "$REPORT"
exit $fail
