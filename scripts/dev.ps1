# LoopHabit local dev: build, ensure emulator, install, launch.
# Usage: npm run dev   OR   .\scripts\dev.ps1

param(
    [switch]$BuildOnly,
    [switch]$InstallOnly
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

$Jbr = "C:\Program Files\Android\Android Studio\jbr"
$Sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME }
       elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT }
       else { "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk" }

if (-not (Test-Path "$Jbr\bin\java.exe")) {
    Write-Error "Android Studio JBR not found at $Jbr. Install Android Studio or set JAVA_HOME to JDK 17+."
}
if (-not (Test-Path $Sdk)) {
    Write-Error "Android SDK not found at $Sdk. Set ANDROID_HOME or install the SDK."
}

$env:JAVA_HOME = $Jbr
$env:ANDROID_HOME = $Sdk
$env:ANDROID_SDK_ROOT = $Sdk
$Adb = Join-Path $Sdk "platform-tools\adb.exe"
$Emulator = Join-Path $Sdk "emulator\emulator.exe"
$Package = "com.example.loophabit"
$Activity = "com.example.loophabit.MainActivity"
$AvdName = "loop36"

function Get-DeviceId {
    $lines = & $Adb devices 2>$null | Select-Object -Skip 1 | Where-Object { $_ -match "\tdevice$" }
    if ($lines) {
        return ($lines[0] -split "\s+")[0]
    }
    return $null
}

function Wait-ForDevice {
    param([int]$TimeoutSec = 180)
    Write-Host "Waiting for device/emulator (up to ${TimeoutSec}s)..."
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $id = Get-DeviceId
        if ($id) {
            # Boot completed?
            $boot = & $Adb -s $id shell getprop sys.boot_completed 2>$null
            if (($boot | Out-String).Trim() -eq "1") {
                Write-Host "Device ready: $id"
                return $id
            }
        }
        Start-Sleep -Seconds 3
    }
    Write-Error "No booted Android device within ${TimeoutSec}s. Start an emulator or plug in a phone with USB debugging."
}

function Ensure-Emulator {
    $id = Get-DeviceId
    if ($id) { return }

    if (-not (Test-Path $Emulator)) {
        Write-Error "No device attached and emulator not found at $Emulator."
    }

    $avds = & $Emulator -list-avds 2>$null
    $target = if ($avds -contains $AvdName) { $AvdName } elseif ($avds) { $avds[0] } else { $null }
    if (-not $target) {
        Write-Error "No Android Virtual Devices found. Create one in Android Studio (Device Manager)."
    }

    Write-Host "Starting emulator: $target"
    # Software GPU is more reliable when hardware acceleration misbehaves.
    Start-Process -FilePath $Emulator -ArgumentList @(
        "-avd", $target,
        "-netdelay", "none",
        "-netspeed", "full",
        "-gpu", "swiftshader_indirect",
        "-no-audio"
    ) | Out-Null
}

Write-Host "==> Building debug APK"
& "$Root\gradlew.bat" assembleDebug
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if ($BuildOnly) {
    Write-Host "APK: $Root\app\build\outputs\apk\debug\LoopHabit-debug.apk"
    exit 0
}

Ensure-Emulator
$device = Wait-ForDevice

Write-Host "==> Installing on $device"
& $Adb -s $device install -r "$Root\app\build\outputs\apk\debug\LoopHabit-debug.apk"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if ($InstallOnly) { exit 0 }

Write-Host "==> Launching $Package"
& $Adb -s $device shell am start -n "$Package/$Activity"
Write-Host "Done. LoopHabit should be on the emulator/device."
