# Installs build\TiviHome.apk on the TV over network ADB and opens it. Does NOT change the default Home.
# Usage: powershell -ExecutionPolicy Bypass -File install.ps1 -Tv 192.168.1.50
param(
    [Parameter(Mandatory = $true)][string]$Tv,
    [string]$BuildTools = ''
)
if (-not $BuildTools) { $BuildTools = Join-Path (Split-Path $PSScriptRoot -Parent) 'buildtools' }
$ErrorActionPreference = 'Stop'

$root = $PSScriptRoot
$adb = Join-Path $BuildTools 'android-sdk\platform-tools\adb.exe'
if (-not (Test-Path $adb)) { $adb = 'adb' } # fall back to adb on PATH
$apk = Join-Path $root 'build\TiviHome.apk'
if (-not (Test-Path $apk)) { throw "Run build.ps1 first ($apk not found)" }
$serial = if ($Tv -match ':') { $Tv } else { "${Tv}:5555" }

& $adb connect $serial
Write-Host 'If the TV shows "Allow USB debugging?", accept it, then wait a moment.'
& $adb -s $serial wait-for-device
# Push and pm install instead of `adb install`, which can hang on Android 14 waiting for an on-screen prompt.
& $adb -s $serial push $apk /data/local/tmp/TiviHome.apk
if ($LASTEXITCODE -ne 0) { throw 'push failed' }
& $adb -s $serial shell pm install -r /data/local/tmp/TiviHome.apk
if ($LASTEXITCODE -ne 0) { throw 'pm install failed' }
& $adb -s $serial shell rm /data/local/tmp/TiviHome.apk
# Child lock covers other apps; write-settings makes the grant survive a reboot or a power cut.
& $adb -s $serial shell appops set com.tivibox.home SYSTEM_ALERT_WINDOW allow
& $adb -s $serial shell appops set --uid com.tivibox.home SYSTEM_ALERT_WINDOW allow
& $adb -s $serial shell appops write-settings
& $adb -s $serial shell am start -n com.tivibox.home/.MainActivity
Write-Host 'Installed and opened Tivi Home. The stock launcher is unchanged; press Home to go back to it.'
