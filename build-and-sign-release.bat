@echo off
echo ========================================
echo   True Caller - Build and Sign Release APK
echo ========================================
echo.

:: Chuyển đến thư mục chứa script
cd /d "%~dp0"

:: Kiểm tra keystore
if not exist "truecaller.keystore" (
    echo WARNING: truecaller.keystore not found!
    echo.
    echo Creating keystore...
    call create-keystore.bat
    if %ERRORLEVEL% NEQ 0 (
        echo.
        echo ERROR: Failed to create keystore!
        pause
        exit /b 1
    )
    echo.
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

:: Sign APK
echo [2/2] Signing APK...
call sign-apk.bat
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Signing failed!
    pause
    exit /b 1
)

echo.
echo ========================================
echo Done! Release APK is ready.
echo ========================================
echo.
pause

