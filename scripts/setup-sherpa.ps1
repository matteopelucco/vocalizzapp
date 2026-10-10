# Scarica le librerie native precompilate di sherpa-onnx (solo arm64-v8a) in app/src/main/jniLibs.
# Uso: .\scripts\setup-sherpa.ps1
$ErrorActionPreference = 'Stop'
$version = '1.13.8'
$url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/v$version/sherpa-onnx-v$version-android.tar.bz2"

$root = Split-Path -Parent $PSScriptRoot
$dest = Join-Path $root 'app\src\main\jniLibs\arm64-v8a'
$tmp = Join-Path ([IO.Path]::GetTempPath()) "sherpa-onnx-$version"
$extract = Join-Path $tmp 'extract'

New-Item -ItemType Directory -Force $tmp, $extract, $dest | Out-Null
$archive = Join-Path $tmp 'android.tar.bz2'
if (-not (Test-Path $archive)) {
    Write-Host "Scarico $url"
    Invoke-WebRequest -Uri $url -OutFile $archive
}
tar -xjf $archive -C $extract
foreach ($lib in 'libonnxruntime.so', 'libsherpa-onnx-jni.so') {
    Copy-Item (Join-Path $extract "jniLibs\arm64-v8a\$lib") $dest -Force
}
Write-Host "Librerie copiate in $dest"
