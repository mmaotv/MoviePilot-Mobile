#!/bin/bash

# MoviePilot Mobile APK 构建脚本
# 用法: ./scripts/build-apk.sh [release|debug]

set -e

BUILD_TYPE=${1:-release}
echo "Building MoviePilot Mobile APK ($BUILD_TYPE)..."

# 检查依赖
echo "Checking dependencies..."
if ! command -v node &> /dev/null; then
    echo "Error: Node.js is not installed"
    exit 1
fi

if ! command -v yarn &> /dev/null; then
    echo "Error: Yarn is not installed"
    exit 1
fi

# 安装依赖
echo "Installing dependencies..."
yarn install --frozen-lockfile

# 构建 Web 资源
echo "Building web assets..."
yarn build

# 同步 Capacitor
echo "Syncing Capacitor..."
npx cap sync android

# 构建 APK
echo "Building Android APK..."
cd android

if [ "$BUILD_TYPE" = "release" ]; then
    ./gradlew assembleRelease
    APK_PATH="app/build/outputs/apk/release/app-release.apk"
else
    ./gradlew assembleDebug
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
fi

cd ..

# 复制 APK 到根目录
if [ -f "android/$APK_PATH" ]; then
    cp "android/$APK_PATH" "MoviePilot-v$(node -p "require('./package.json').version").apk"
    echo "Build successful!"
    echo "APK location: android/$APK_PATH"
    echo "Copied to: MoviePilot-v$(node -p "require('./package.json').version").apk"
else
    echo "Error: APK not found at android/$APK_PATH"
    exit 1
fi
