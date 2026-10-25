# Re-enables the "group 1" system packages that were disabled (pm disable-user) as unused on the TV box.
# Usage: powershell -ExecutionPolicy Bypass -File undo-debloat-group1.ps1 -Tv 192.168.1.50
param(
    [Parameter(Mandatory = $true)][string]$Tv,
    [string]$BuildTools = ''
)
if (-not $BuildTools) { $BuildTools = Join-Path (Split-Path $PSScriptRoot -Parent) 'buildtools' }
$adb = Join-Path $BuildTools 'android-sdk\platform-tools\adb.exe'
if (-not (Test-Path $adb)) { $adb = 'adb' } # fall back to adb on PATH
$serial = if ($Tv -match ':') { $Tv } else { "${Tv}:5555" }

$packages = @(
    'com.google.android.tvrecommendations',
    'com.google.android.feedback',
    'com.android.adservices.api',
    'com.android.ext.adservices.api',
    'com.android.ondevicepersonalization.services',
    'com.android.federatedcompute.services',
    'com.android.healthconnect.controller',
    'com.android.health.connect.backuprestore',
    'com.android.nearby.halfsheet',
    'com.android.hotspot2.osulogin',
    'com.android.dynsystem',
    'com.google.android.syncadapters.calendar'
)
& $adb connect $serial
foreach ($p in $packages) {
    # One pm call per shell command: chained pm commands can stop silently after the first.
    & $adb -s $serial shell pm enable $p
}
