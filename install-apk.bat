@echo off
echo ========================================
echo   CallerNamer - Install APK
echo ========================================
echo.

:: Chuyển đến thư mục chứa script
cd /d "%~dp0"

:: Kiểm tra file APK tồn tại
if not exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo ERROR: APK not found!
    echo Please build the project first using build-debug.bat
    echo.
    pause
    exit /b 1
)

:: Kiểm tra thiết bị
echo Checking connected devices...
adb devices
echo.

:: Cài đặt APK
echo Installing APK...
adb install -r app\build\outputs\apk\debug\app-debug.apk

if %ERRORLEVEL% EQU 0 (
    echo.
    echo SUCCESS: APK installed successfully!
    echo.
    echo To launch the app, run:
    echo   adb shell am start -n com.zobaer53.incomingcall/.MainActivity
) else (
    echo.
    echo ERROR: Installation failed!
)

echo.
pause

