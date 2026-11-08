#!/usr/bin/env bash
# APK güvenlik denetimi: izinler (uygulama başına izin listesi), INTERNET, yasaklı SDK izleri, sürüm bilgileri.
# Kullanım: tools/audit_apks.sh <çıktı_klasörü>
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
OUT=${1:-$ROOT/build/ci}
mkdir -p "$OUT/release" "$OUT/debug"
AAPT2=$(ls "$ANDROID_HOME"/build-tools/*/aapt2 | sort -V | tail -n 1)
REPORT="$OUT/audit.md"
FORBIDDEN='L(com/google/firebase|com/google/android/gms|com/google/android/ads|com/facebook/ads|com/appsflyer|com/adjust/sdk|io/sentry|com/crashlytics|com/onesignal|com/mixpanel|com/amplitude|com/applovin|com/unity3d/ads|com/ironsource|com/flurry|com/bugsnag|com/instabug)/'
fail=0
{
  echo "# APK denetim raporu"
  echo
  echo "| Uygulama | Paket | Sürüm | min/target SDK | APK boyutu | İzinler | INTERNET | Yasaklı SDK | Sonuç |"
  echo "|---|---|---|---|---|---|---|---|---|"
} > "$REPORT"

for allow in "$ROOT"/[0-9][0-9]_*/*/permissions.allow; do
  dir=$(dirname "$allow")
  module=$(basename "$dir")
  apk=$(ls "$dir"/build/outputs/apk/release/*.apk 2>/dev/null | head -n 1 || true)
  [ -z "$apk" ] && continue
  badging=$("$AAPT2" dump badging "$apk")
  pkg=$(echo "$badging" | sed -n "s/^package: name='\([^']*\)'.*/\1/p")
  vname=$(echo "$badging" | sed -n "s/.*versionName='\([^']*\)'.*/\1/p" | head -n 1)
  vcode=$(echo "$badging" | sed -n "s/.*versionCode='\([^']*\)'.*/\1/p" | head -n 1)
  minsdk=$(echo "$badging" | sed -n "s/^minSdkVersion:'\([0-9]*\)'.*/\1/p")
  target=$(echo "$badging" | sed -n "s/^targetSdkVersion:'\([0-9]*\)'.*/\1/p")
  label=$(echo "$badging" | sed -n "s/^application-label:'\(.*\)'$/\1/p" | head -n 1)
  perms=$("$AAPT2" dump permissions "$apk" | sed -n "s/^uses-permission: name='\([^']*\)'.*/\1/p" | sort -u)
  unexpected=$(comm -23 <(printf '%s\n' "$perms" | sed '/^$/d') <(sort -u "$allow" | sed '/^$/d') || true)
  internet="Yok"; echo "$perms" | grep -qx "android.permission.INTERNET" && internet="VAR"
  sdk=$(unzip -p "$apk" 'classes*.dex' | strings -n 6 | grep -Eo "$FORBIDDEN" | sort -u | tr '\n' ' ' || true)
  size=$(stat -c %s "$apk")
  status="GEÇTİ"
  if [ -n "$unexpected" ] || [ "$internet" = "VAR" ] || [ -n "$sdk" ]; then status="KALDI"; fail=1; fi
  permtxt=$(printf '%s' "$perms" | sed 's/android.permission.//' | tr '\n' ',' | sed 's/,$//; s/,/, /g')
  [ -z "$permtxt" ] && permtxt="yok"
  [ -n "$unexpected" ] && permtxt="$permtxt (BEKLENMEYEN: $(echo $unexpected))"
  echo "| $label | \`$pkg\` | $vname ($vcode) | $minsdk / $target | $(awk "BEGIN{printf \"%.2f MB\", $size/1048576}") | $permtxt | $internet | ${sdk:-yok} | $status |" >> "$REPORT"
  name="${pkg##*.}-$vname"
  cp "$apk" "$OUT/release/$pkg-$vname.apk"
  ( cd "$OUT/release" && sha256sum "$pkg-$vname.apk" >> SHA256SUMS.txt )
  dbg=$(ls "$dir"/build/outputs/apk/debug/*.apk 2>/dev/null | head -n 1 || true)
  tst=$(ls "$dir"/build/outputs/apk/androidTest/debug/*.apk 2>/dev/null | head -n 1 || true)
  [ -n "$dbg" ] && cp "$dbg" "$OUT/debug/$pkg.apk"
  [ -n "$tst" ] && cp "$tst" "$OUT/debug/$pkg.test.apk"
done
echo >> "$REPORT"
cat "$REPORT"
exit $fail
