# Builds build\TiviHome.apk with a JDK 17 and Android SDK (platform 34, build-tools 34.0.0), no Gradle, offline.
# Expects <BuildTools>\jdk\<jdk folder> and <BuildTools>\android-sdk; default <BuildTools> is ..\buildtools.
# Usage: powershell -ExecutionPolicy Bypass -File build.ps1 [-VersionCode 2] [-VersionName 1.1] [-BuildTools D:\tools]
param(
    [int]$VersionCode = 1,
    [string]$VersionName = "1.0",
    [string]$BuildTools = ''
)
if (-not $BuildTools) { $BuildTools = Join-Path (Split-Path $PSScriptRoot -Parent) 'buildtools' }
$ErrorActionPreference = 'Stop'

$root = $PSScriptRoot
$tools = $BuildTools
$env:JAVA_HOME = (Get-ChildItem (Join-Path $tools 'jdk') -Directory | Select-Object -First 1).FullName
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$sdk = Join-Path $tools 'android-sdk'
$bt = Join-Path $sdk 'build-tools\34.0.0'
$androidJar = Join-Path $sdk 'platforms\android-34\android.jar'
$out = Join-Path $root 'build'

function Step($name, [scriptblock]$cmd) {
    Write-Host "== $name"
    & $cmd
    if ($LASTEXITCODE -ne 0) { throw "$name failed (exit $LASTEXITCODE)" }
}

if (Test-Path $out) { Remove-Item -Recurse -Force $out }
New-Item -ItemType Directory -Force "$out\gen", "$out\classes", "$out\test", "$out\dex" | Out-Null

Step 'Unit tests (lunar calendar, Vietnamese tones, Telex)' {
    $src = "$root\app\java\com\tivibox\home"
    & javac -encoding UTF-8 -d "$out\test" "$src\LunarCalendar.java" "$src\VietTone.java" "$src\Telex.java" `
        "$root\test\LunarCalendarTest.java" "$root\test\VietToneTest.java" "$root\test\TelexTest.java"
    if ($LASTEXITCODE -eq 0) { & java -cp "$out\test" LunarCalendarTest }
    if ($LASTEXITCODE -eq 0) { & java -cp "$out\test" VietToneTest }
    if ($LASTEXITCODE -eq 0) { & java -cp "$out\test" TelexTest }
}

Step 'aapt2 compile' { & "$bt\aapt2.exe" compile --dir "$root\app\res" -o "$out\res.zip" }
Step 'aapt2 link' {
    & "$bt\aapt2.exe" link -I $androidJar --manifest "$root\app\AndroidManifest.xml" --java "$out\gen" `
        --min-sdk-version 26 --target-sdk-version 34 --version-code $VersionCode --version-name $VersionName `
        -o "$out\base.apk" "$out\res.zip"
}

$sources = @(Get-ChildItem "$root\app\java", "$out\gen" -Recurse -Filter *.java | ForEach-Object { $_.FullName })
Step 'javac' { & javac -encoding UTF-8 -source 11 -target 11 -Xlint:-options -cp $androidJar -d "$out\classes" $sources }
Step 'jar' { & jar cf "$out\classes.jar" -C "$out\classes" . }
Step 'd8' { & "$bt\d8.bat" --release --min-api 26 --lib $androidJar --output "$out\dex" "$out\classes.jar" }

Write-Host '== package'
Copy-Item "$out\base.apk" "$out\unaligned.apk"
Add-Type -AssemblyName System.IO.Compression, System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::Open("$out\unaligned.apk", 'Update')
try {
    [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, "$out\dex\classes.dex", 'classes.dex') | Out-Null
} finally {
    $zip.Dispose()
}
Step 'zipalign' { & "$bt\zipalign.exe" -p -f 4 "$out\unaligned.apk" "$out\aligned.apk" }

# A fixed key per checkout, so later builds install over earlier ones.
$keystore = Join-Path $root 'debug.keystore'
if (-not (Test-Path $keystore)) {
    Step 'keytool' {
        & keytool -genkeypair -keystore $keystore -storepass android -keypass android -alias tivihome `
            -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Tivi Home' -noprompt
    }
}
Step 'apksigner' {
    & "$bt\apksigner.bat" sign --ks $keystore --ks-pass pass:android --key-pass pass:android `
        --out "$out\TiviHome.apk" "$out\aligned.apk"
}
$apk = Get-Item "$out\TiviHome.apk"
Write-Host ("Built {0} ({1:N0} bytes)" -f $apk.FullName, $apk.Length)
