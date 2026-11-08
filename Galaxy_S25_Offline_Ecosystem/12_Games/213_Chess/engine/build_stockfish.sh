#!/usr/bin/env bash
# Stockfish'i Android için derler ve doğrular (CI'da çalışır; internet yalnızca derleme sırasında gerekir).
# Çıktı: <out>/jniLibs/{arm64-v8a,x86_64}/libstockfish.so, SHA256SUMS, BUILDINFO.txt, kaynak paketi (GPL-3 karşılık gelen kaynak).
# Kullanım: build_stockfish.sh <çıktı_klasörü>   (ANDROID_SDK_ROOT tanımlı olmalı)
set -euo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
OUT=$(mkdir -p "${1:?çıktı klasörü}" && cd "$1" && pwd)
# shellcheck disable=SC1091
source "$HERE/stockfish.env"
WORK=$(mktemp -d)
log() { echo "== $*"; }

log "Stockfish $SF_TAG kaynağı alınıyor"
git clone -q --depth 1 --branch "$SF_TAG" https://github.com/official-stockfish/Stockfish "$WORK/sf"
got=$(git -C "$WORK/sf" rev-parse HEAD)
[ "$got" = "$SF_COMMIT" ] || { echo "Commit uyuşmuyor: $got != $SF_COMMIT"; exit 1; }
grep -q "\"$SF_NET\"" "$WORK/sf/src/evaluate.h" || { echo "Ağ adı uyuşmuyor"; exit 1; }

log "NNUE ağı indiriliyor ve SHA-256 önekiyle doğrulanıyor"
make -C "$WORK/sf/src" net
net_sha=$(sha256sum "$WORK/sf/src/$SF_NET" | cut -d' ' -f1)
[ "${net_sha:0:12}" = "$(echo "$SF_NET" | sed 's/^nn-\([0-9a-f]*\)\.nnue$/\1/')" ] || { echo "Ağ özeti uyuşmuyor"; exit 1; }

NDK_BIN="$ANDROID_SDK_ROOT/ndk/$NDK_VERSION/toolchains/llvm/prebuilt/linux-x86_64/bin"
[ -x "$NDK_BIN/aarch64-linux-android29-clang++" ] || { echo "NDK $NDK_VERSION bulunamadı: $NDK_BIN"; exit 1; }

build() { # $1=ARCH $2=COMP $3=hedef dosya
  make -C "$WORK/sf/src" clean >/dev/null
  if [ "$2" = ndk ]; then
    PATH="$NDK_BIN:$PATH" LDFLAGS="-static -Wno-unused-command-line-argument" make -C "$WORK/sf/src" -j"$(nproc)" ARCH="$1" COMP=ndk build >/dev/null
  else
    make -C "$WORK/sf/src" -j"$(nproc)" ARCH="$1" COMP="$2" build >/dev/null
  fi
  cp "$WORK/sf/src/stockfish" "$3"
}

log "Sunucu (Linux x86-64) derlemesi: perft ve bench referansı için"
build x86-64-sse41-popcnt gcc "$WORK/stockfish-host"
log "Android arm64 (armv8 + armv8-dotprod, çalışma anında seçim) derlemesi"
mkdir -p "$OUT/jniLibs/arm64-v8a" "$OUT/jniLibs/x86_64"
build arm64-universal ndk "$OUT/jniLibs/arm64-v8a/libstockfish.so"
log "Android x86_64 (yalnızca debug/emülatör) derlemesi"
build x86-64-sse41-popcnt ndk "$OUT/jniLibs/x86_64/libstockfish.so"

sig() { # $1=çalıştırma öneki $2=ikili dosya
  local s; s=$($1 "$2" bench 2>&1 >/dev/null | awk '/Nodes searched  :/ {print $4}')
  echo "${s:-YOK}"
}
log "Bench imzaları (beklenen $SF_BENCH)"
s_host=$(sig "" "$WORK/stockfish-host")
s_arm=$(sig "qemu-aarch64-static" "$OUT/jniLibs/arm64-v8a/libstockfish.so")
s_x86=$(sig "" "$OUT/jniLibs/x86_64/libstockfish.so" || true)
echo "host=$s_host arm64(qemu)=$s_arm android-x86_64=$s_x86"
[ "$s_host" = "$SF_BENCH" ] || { echo "Sunucu bench imzası uyuşmuyor"; exit 1; }
[ "$s_arm" = "$SF_BENCH" ] || { echo "arm64 bench imzası uyuşmuyor"; exit 1; }
[ "$s_x86" = "$SF_BENCH" ] || { echo "Android x86_64 bench imzası uyuşmuyor"; exit 1; }

log "Stockfish'in kendi perft testleri (sunucu ikilisiyle)"
( cd "$WORK" && cp stockfish-host "$WORK/sf/src/stockfish" && cd "$WORK/sf/src" && bash ../tests/perft.sh )

log "Ek perft doğrulaması: zor durumlar seti (kendi test listemiz)"
fail=0
while read -r depth expected fen; do
  [ -z "$depth" ] && continue
  got=$(printf 'position fen %s\ngo perft %s\nquit\n' "$fen" "$depth" | "$WORK/stockfish-host" | awk '/Nodes searched:/ {print $3}')
  st=OK; [ "$got" = "$expected" ] || { st=UYUŞMADI; fail=1; }
  echo "perft d$depth $expected -> $got $st  $fen"
done < "$HERE/perft_tricky.txt"
[ $fail = 0 ] || { echo "Ek perft setinde uyuşmazlık var"; exit 1; }

log "Paketleme"
( cd "$OUT/jniLibs" && sha256sum arm64-v8a/libstockfish.so x86_64/libstockfish.so > "$OUT/SHA256SUMS" )
{
  echo "Stockfish $SF_TAG ($SF_COMMIT)"
  echo "NNUE: $SF_NET sha256=$net_sha (ikili dosyaya gömülü)"
  echo "NDK: $NDK_VERSION, statik bağlantı"
  echo "arm64-v8a: ARCH=arm64-universal (armv8 + armv8-dotprod, çalışma anında seçim)"
  echo "x86_64: ARCH=x86-64-sse41-popcnt (yalnızca debug/emülatör)"
  echo "Bench imzası: host=$s_host arm64(qemu)=$s_arm x86_64=$s_x86 beklenen=$SF_BENCH"
  echo "Boyut: arm64=$(stat -c %s "$OUT/jniLibs/arm64-v8a/libstockfish.so") B, x86_64=$(stat -c %s "$OUT/jniLibs/x86_64/libstockfish.so") B"
} > "$OUT/BUILDINFO.txt"
# GPL-3 "karşılık gelen kaynak": Stockfish kaynağı (aynı commit), ağ dosyası ve bu derleme betikleri
mkdir -p "$WORK/src-bundle"
git -C "$WORK/sf" archive --format=tar --prefix="stockfish-$SF_TAG/" HEAD | tar -x -C "$WORK/src-bundle"
cp "$WORK/sf/src/$SF_NET" "$WORK/src-bundle/stockfish-$SF_TAG/src/"
mkdir -p "$WORK/src-bundle/build-scripts" && cp "$HERE"/stockfish.env "$HERE"/build_stockfish.sh "$HERE"/perft_tricky.txt "$WORK/src-bundle/build-scripts/"
tar -czf "$OUT/stockfish-$SF_TAG-corresponding-source.tar.gz" -C "$WORK/src-bundle" .
cat "$OUT/BUILDINFO.txt"
