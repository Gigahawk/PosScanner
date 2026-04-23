#!/usr/bin/env bash
set -euo pipefail

OUTDIR=".dist"

rm -rf .dist
mkdir -p "$OUTDIR"

echo "===Building unsigned APK==="
rm -f result
nix build .#apk

unsigned_name=$(ls result | grep "_release_unsigned.apk")
signed_name="${unsigned_name/_release_unsigned/_release_signed}"

echo "===Copying unsigned release $unsigned_name to $OUTDIR==="

cp "result/$unsigned_name" "$OUTDIR"
chmod 755 "$OUTDIR/$unsigned_name"

echo "===Signing APK==="

apksigner sign "$@" --out "$OUTDIR/$signed_name" "$OUTDIR/$unsigned_name"

echo "===Verifying signed APK==="
apksigner verify \
  -v \
  --print-certs \
  --print-certs-pem \
  "$OUTDIR/$signed_name"

echo "===Signed APK is at $OUTDIR/$signed_name==="
