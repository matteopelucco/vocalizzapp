#!/usr/bin/env bash
# Scarica le librerie native precompilate di sherpa-onnx (solo arm64-v8a) in app/src/main/jniLibs.
# Equivalente di setup-sherpa.ps1 per macOS / Linux / Git Bash.
# Uso: ./scripts/setup-sherpa.sh
set -euo pipefail

VERSION="1.13.8"
URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/v${VERSION}/sherpa-onnx-v${VERSION}-android.tar.bz2"

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEST="${ROOT}/app/src/main/jniLibs/arm64-v8a"
TMP="$(mktemp -d)"
trap 'rm -rf "${TMP}"' EXIT

mkdir -p "${DEST}"
echo "Scarico ${URL}"
curl -L --fail -sS -o "${TMP}/android.tar.bz2" "${URL}"
mkdir -p "${TMP}/extract"
tar -xjf "${TMP}/android.tar.bz2" -C "${TMP}/extract"
for lib in libonnxruntime.so libsherpa-onnx-jni.so; do
    cp "${TMP}/extract/jniLibs/arm64-v8a/${lib}" "${DEST}/"
done
echo "Librerie copiate in ${DEST}"
