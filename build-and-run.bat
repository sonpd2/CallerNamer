@echo off
echo ========================================
echo   CallerNamer - Build and Run Script
echo ========================================
echo.

:: Chuyển đến thư mục chứa script
cd /d "%~dp0"

:: Kiểm tra file gradlew.bat có tồn tại không
if not exist "gradlew.bat" (
    echo ERROR: gradlew.bat not found!
    echo.
    echo Please ensure you are running this script from the project root directory.
    echo Current directory: %CD%
    echo.
    pause
    exit /b 1
)

:: Kiểm tra thiết bị đã kết nối chưa
echo [1/4] Checking connected devices...
adb devices
echo.

:: Build và cài đặt
echo [2/4] Building and installing application...
call gradlew.bat clean installDebug
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Build failed!
    pause
    exit /b 1
)
echo.

:: Chạy ứng dụng
echo [3/4] Starting application...
adb shell am start -n com.sonpd2.incomingcall/.MainActivity
if %ERRORLEVEL% NEQ 0 (
    echo WARNING: Could not start application. It may already be running.
)
echo.

:: Thông báo hoàn tất
echo [4/4] Done!
echo.
echo Application installed and launched successfully!
echo.
echo To view logs, run:
echo   adb logcat ^| findstr "com.sonpd2.incomingcall"
echo.
pause

