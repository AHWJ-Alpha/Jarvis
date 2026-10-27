param(
    [string]$BuildRoot = "D:\Jarvis\.build\embedded-runtime",
    [string]$OpenClawInstall = "D:\Jarvis\.build\openclaw-node",
    [string]$AndroidRuntime = "D:\Jarvis\.build\android-runtime",
    [string]$JarvisPlugin = "D:\Jarvis\tools\jarvis-plugin"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# Always stage from a clean directory. Robocopy does not replace a stale file
# with a directory (or the reverse), which can leave pnpm package links such as
# `tslog` as shell-script files after a previous Windows staging run.
if (Test-Path $BuildRoot) {
    Remove-Item $BuildRoot -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $BuildRoot, "$BuildRoot\lib", "$BuildRoot\bin", "$BuildRoot\etc", "$BuildRoot\tmp", "$BuildRoot\home" | Out-Null
Copy-Item "$AndroidRuntime\node-data\data\data\com.termux\files\usr\bin\node" "$BuildRoot\node" -Force
Copy-Item "$AndroidRuntime\bootstrap\lib\*" "$BuildRoot\lib" -Force
Copy-Item "$AndroidRuntime\bootstrap\etc\*" "$BuildRoot\etc" -Recurse -Force
$dependencyLib = "$AndroidRuntime\runtime-root\data\data\com.termux\files\usr\lib"
if (Test-Path $dependencyLib) {
    Copy-Item "$dependencyLib\*.so*" "$BuildRoot\lib" -Force -ErrorAction SilentlyContinue
}

# Windows staging loses Termux symlink entries. Recreate the SONAME aliases
# that Node and its Android native dependencies use at runtime.
$aliases = @{
    "libz.so.1" = "libz.so.1.3.2"
    "libicudata.so.78" = "libicudata.so.78.3"
    "libicui18n.so.78" = "libicui18n.so.78.3"
    "libicuio.so.78" = "libicuio.so.78.3"
    "libicutest.so.78" = "libicutest.so.78.3"
    "libicutu.so.78" = "libicutu.so.78.3"
    "libicuuc.so.78" = "libicuuc.so.78.3"
    "libsqlite3.so.0" = "libsqlite3.so.3.53.4"
    "libsqlite3.so" = "libsqlite3.so.3.53.4"
}
foreach ($alias in $aliases.GetEnumerator()) {
    $source = Join-Path "$BuildRoot\lib" $alias.Value
    if (Test-Path $source) { Copy-Item $source (Join-Path "$BuildRoot\lib" $alias.Key) -Force }
}

robocopy "$OpenClawInstall\node_modules" "$BuildRoot\node_modules" /E /COPY:DAT /R:1 /W:1 /NFL /NDL /NJH /NJS | Out-Null
robocopy "$OpenClawInstall\node_modules\openclaw" "$BuildRoot" /E /COPY:DAT /R:1 /W:1 /NFL /NDL /NJH /NJS | Out-Null
Remove-Item "$BuildRoot\node_modules\openclaw" -Recurse -Force -ErrorAction SilentlyContinue
robocopy $JarvisPlugin "$BuildRoot\jarvis-plugin" /E /COPY:DAT /R:1 /W:1 /NFL /NDL /NJH /NJS | Out-Null
Copy-Item "$PSScriptRoot\android-compat.mjs" "$BuildRoot\android-compat.mjs" -Force
$compatDist = Join-Path $PSScriptRoot "..\app\src\main\assets\openclaw\overrides\node_modules\@openclaw\fs-safe\dist"
$stagedDist = Join-Path $BuildRoot "node_modules\@openclaw\fs-safe\dist"
Copy-Item (Join-Path $compatDist "publish-copy-stage.js") $stagedDist -Force
Copy-Item (Join-Path $compatDist "pinned-write-staged.js") $stagedDist -Force
New-Item -ItemType Directory -Force "$BuildRoot\openclaw-home" | Out-Null
@{
    gateway = @{ mode = "local"; bind = "loopback" }
    plugins = @{
        load = @{ paths = @("./jarvis-plugin") }
        entries = @{ "jarvis-device-bridge" = @{ enabled = $true } }
    }
} | ConvertTo-Json -Depth 8 | Set-Content "$BuildRoot\openclaw-home\openclaw.json"

@{
    runtime = "openclaw"
    version = "2026.9.7"
    commit = "c074824a27c96d3983043f9eeb33823cd1772d8c"
    architecture = "android-arm64"
    node = "24.18.0-termux"
    source = "Termux official aarch64 package"
    nativeDependencies = "unverified on device"
    embeddedCandidate = $true
} | ConvertTo-Json | Set-Content "$BuildRoot\runtime.json"

$sum = (Get-ChildItem $BuildRoot -Recurse -File | Measure-Object Length -Sum).Sum
Write-Output "staged bytes=$sum MB=$([math]::Round($sum / 1MB, 1))"

$archive = "$BuildRoot.zip"
tar -a -c -f $archive -C $BuildRoot .
Write-Output "archive=$archive bytes=$((Get-Item $archive).Length)"
