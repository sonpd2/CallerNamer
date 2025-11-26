@echo off
echo ========================================
echo   CallerNamer - Build Debug APK
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

:: Build APK debug
echo Building debug APK...
call gradlew.bat assembleDebug

if %ERRORLEVEL% EQU 0 (
    echo.
    echo SUCCESS: APK built successfully!
    echo Location: app\build\outputs\apk\debug\app-debug.apk
) else (
    echo.
    echo ERROR: Build failed!
)

echo.
pause

