param([string]$BuildRoot = "D:\Jarvis\.build")

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$android = Join-Path $BuildRoot "android-runtime"
$openclaw = Join-Path $BuildRoot "openclaw-node"
New-Item -ItemType Directory -Force $android | Out-Null

function Fetch([string]$Url, [string]$Path, [string]$Sha256) {
    if (!(Test-Path $Path)) { Invoke-WebRequest -UseBasicParsing $Url -OutFile $Path }
    $actual = (Get-FileHash $Path -Algorithm SHA256).Hash.ToUpperInvariant()
    if ($actual -ne $Sha256.ToUpperInvariant()) { throw "SHA-256 mismatch for ${Path}: $actual" }
}

Fetch "https://packages.termux.dev/apt/termux-main/pool/main/n/nodejs-lts/nodejs-lts_24.18.0-1_aarch64.deb" `
    (Join-Path $android "nodejs-lts.deb") "490F4D08C45B25A7EA7DB6EE466EBB3EE61F07083260B85332704ED018F59A87"
Fetch "https://github.com/termux/termux-packages/releases/download/bootstrap-2026.09.27-r1%2Bapt.android-7/bootstrap-aarch64.zip" `
    (Join-Path $android "bootstrap-aarch64.zip") "9DDC32921187C85B04556BF56C6CCE94E00B813ECD9299959A2D9B7C33386994"
Fetch "https://packages.termux.dev/apt/termux-main/pool/main/libc/libc%2B%2B/libc%2B%2B_30_aarch64.deb" `
    (Join-Path $android "libc++.deb") "53D0B84A7BA7459024257CB94D5B136FE13EF858567F65A8064B35950799F2CA"
Fetch "https://packages.termux.dev/apt/termux-main/pool/main/c/c-ares/c-ares_1.34.8_aarch64.deb" `
    (Join-Path $android "c-ares.deb") "7681FC23E822D7988BA8B2ADF3468F93AE68F724DDA365CFF1385096A9FA87E6"
Fetch "https://packages.termux.dev/apt/termux-main/pool/main/libi/libicu/libicu_78.3_aarch64.deb" `
    (Join-Path $android "libicu.deb") "F536403F65A08FE0DF6E7304184E902D54DEF77D5C3BD5EDFD9109D57601D276"
Fetch "https://packages.termux.dev/apt/termux-main/pool/main/libs/libsqlite/libsqlite_3.53.4_aarch64.deb" `
    (Join-Path $android "libsqlite.deb") "0E909CE0D50FE123305446CD22E0C5EDF535D40344B9B065FBDCDEE52F53198D"

if (!(Test-Path (Join-Path $android "bootstrap\lib"))) {
    Expand-Archive (Join-Path $android "bootstrap-aarch64.zip") (Join-Path $android "bootstrap") -Force
}
if (!(Test-Path (Join-Path $android "node-data\data\data\com.termux\files\usr\bin\node"))) {
    $nodePkg = Join-Path $android "nodepkg"
    $nodeData = Join-Path $android "node-data"
    New-Item -ItemType Directory -Force $nodePkg, $nodeData | Out-Null
    tar -xf (Join-Path $android "nodejs-lts.deb") -C $nodePkg
    tar -xf (Join-Path $nodePkg "data.tar.xz") -C $nodeData
}
if (!(Test-Path (Join-Path $android "runtime-root"))) {
    $runtimeRoot = Join-Path $android "runtime-root"
    New-Item -ItemType Directory -Force $runtimeRoot | Out-Null
    Get-ChildItem $android -Filter "*.deb" | ForEach-Object {
        $part = Join-Path $android ("part-" + $_.BaseName)
        New-Item -ItemType Directory -Force $part | Out-Null
        tar -xf $_.FullName -C $part
        tar -xf (Join-Path $part "data.tar.xz") -C $runtimeRoot
    }
}

if (!(Test-Path (Join-Path $openclaw "node_modules\openclaw\openclaw.mjs"))) {
    New-Item -ItemType Directory -Force $openclaw | Out-Null
    npm install --prefix $openclaw --ignore-scripts --omit=dev --omit=optional --no-audit --no-fund openclaw@2026.9.7
}

& "$PSScriptRoot\build-embedded-runtime.ps1" -BuildRoot (Join-Path $BuildRoot "embedded-runtime") -OpenClawInstall $openclaw -AndroidRuntime $android
