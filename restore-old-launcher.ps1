# Undo of "make Tivi Home the default": re-enables CleanYourTV (com.gathin.tvhome) and makes it Home again.
# Tivi Home stays installed. Usage: powershell -ExecutionPolicy Bypass -File restore-old-launcher.ps1 -Tv 192.168.1.50
param(
    [Parameter(Mandatory = $true)][string]$Tv,
    [string]$BuildTools = ''
)
if (-not $BuildTools) { $BuildTools = Join-Path (Split-Path $PSScriptRoot -Parent) 'buildtools' }
$ErrorActionPreference = 'Stop'

$adb = Join-Path $BuildTools 'android-sdk\platform-tools\adb.exe'
if (-not (Test-Path $adb)) { $adb = 'adb' } # fall back to adb on PATH
$serial = if ($Tv -match ':') { $Tv } else { "${Tv}:5555" }

& $adb connect $serial
& $adb -s $serial shell pm enable com.gathin.tvhome
& $adb -s $serial shell cmd package set-home-activity com.gathin.tvhome/.MainActivity
& $adb -s $serial shell input keyevent HOME
Start-Sleep 3
& $adb -s $serial shell "dumpsys activity activities | grep -m1 mFocusedApp"
