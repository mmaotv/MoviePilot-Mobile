# MoviePilot Mobile APK 构建脚本 (PowerShell)
# 用法: .\scripts\build-apk.ps1 [release|debug]

param(
    [string]$BuildType = "release"
)

$ErrorActionPreference = "Stop"

Write-Host "Building MoviePilot Mobile APK ($BuildType)..." -ForegroundColor Green

# 检查依赖
Write-Host "Checking dependencies..." -ForegroundColor Yellow
if (!(Get-Command node -ErrorAction SilentlyContinue)) {
    Write-Error "Error: Node.js is not installed"
    exit 1
}

# 安装依赖
Write-Host "Installing dependencies..." -ForegroundColor Yellow
yarn install --frozen-lockfile

# 构建 Web 资源
Write-Host "Building web assets..." -ForegroundColor Yellow
yarn build

# 同步 Capacitor
Write-Host "Syncing Capacitor..." -ForegroundColor Yellow
npx cap sync android

# 构建 APK
Write-Host "Building Android APK..." -ForegroundColor Yellow
Set-Location android

if ($BuildType -eq "release") {
    .\gradlew.bat assembleRelease
    $apkPath = "app\build\outputs\apk\release\app-release.apk"
} else {
    .\gradlew.bat assembleDebug
    $apkPath = "app\build\outputs\apk\debug\app-debug.apk"
}

Set-Location ..

# 复制 APK 到根目录
$fullApkPath = Join-Path "android" $apkPath
if (Test-Path $fullApkPath) {
    $version = (Get-Content package.json | ConvertFrom-Json).version
    $destPath = "MoviePilot-v$version.apk"
    Copy-Item $fullApkPath $destPath -Force
    Write-Host "Build successful!" -ForegroundColor Green
    Write-Host "APK location: $fullApkPath"
    Write-Host "Copied to: $destPath"
} else {
    Write-Error "Error: APK not found at $fullApkPath"
    exit 1
}
