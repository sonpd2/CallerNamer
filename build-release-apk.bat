@echo off
echo ========================================
echo   True Caller - Build Release APK
echo ========================================
echo.

:: Chuyển đến thư mục chứa script
cd /d "%~dp0"

:: Kiểm tra file gradlew.bat có tồn tại không
if not exist "gradlew.bat" (
    echo ERROR: gradlew.bat not found!
    pause
    exit /b 1
)

:: Build release APK
echo [1/2] Building release APK...
call gradlew.bat assembleRelease
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Build failed!
    pause
    exit /b 1
)
echo.

:: Tìm file APK
set APK_PATH=app\build\outputs\apk\release\app-release-unsigned.apk
if not exist "%APK_PATH%" (
    echo ERROR: APK not found at %APK_PATH%
    pause
    exit /b 1
)

echo [2/2] Release APK built successfully!
echo.
echo APK Location: %CD%\%APK_PATH%
echo.
echo NOTE: This is an UNSIGNED APK. To install it:
echo   1. Use debug APK instead (already signed): gradlew.bat assembleDebug
echo   2. Or sign this APK with your keystore
echo.
pause

