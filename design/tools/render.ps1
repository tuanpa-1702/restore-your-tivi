# Renders every reference page to screenshots\*.png at 1280x720 with headless Chrome (or Edge).
# Usage: powershell -ExecutionPolicy Bypass -File tools\render.ps1
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$browser = @(
    "$env:ProgramFiles\Google\Chrome\Application\chrome.exe",
    "${env:ProgramFiles(x86)}\Microsoft\Edge\Application\msedge.exe",
    "$env:ProgramFiles\Microsoft\Edge\Application\msedge.exe"
) | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $browser) { throw 'Chrome or Edge not found' }

$out = Join-Path $root 'screenshots'
New-Item -ItemType Directory -Force $out | Out-Null
$pages = [ordered]@{
    '01-home'               = 'home.html'
    '02-apps'               = 'apps.html'
    '03-settings'           = 'settings.html'
    '04-child-lock-pick'    = 'child-lock.html?page=pick'
    '05-child-lock-active'  = 'child-lock.html?page=active'
    '06-child-lock-pin'     = 'child-lock.html?page=pin'
    '07-child-lock-overlay' = 'child-lock.html?page=overlay'
}
foreach ($name in $pages.Keys) {
    $url = 'file:///' + (Join-Path $root "reference\$($pages[$name])").Replace('\', '/')
    $png = Join-Path $out "$name.png"
    Start-Process -FilePath $browser -NoNewWindow -Wait -ArgumentList @(
        '--headless=new', '--disable-gpu', '--hide-scrollbars', '--window-size=1280,720',
        '--virtual-time-budget=4000', "--screenshot=$png", $url) 2>$null
    Write-Host "rendered $name.png"
}
