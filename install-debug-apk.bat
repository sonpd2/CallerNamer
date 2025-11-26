@echo off
echo ========================================
echo   True Caller - Install Debug APK
echo ========================================
echo.

:: Chuyển đến thư mục chứa script
cd /d "%~dp0"

:: Kiểm tra thiết bị đã kết nối chưa
echo [1/3] Checking connected devices...
adb devices
echo.

:: Uninstall old app if exists
echo [2/3] Uninstalling old app (if exists)...
adb uninstall com.zobaer53.incomingcall >nul 2>&1
adb uninstall com.sonpd2.incomingcall >nul 2>&1
echo.

:: Build debug APK
echo [3/3] Building and installing debug APK...
call gradlew.bat installDebug
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Build/Install failed!
    pause
    exit /b 1
)
echo.
echo Application installed successfully!
echo.
pause

