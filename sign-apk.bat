@echo off
echo ========================================
echo   True Caller - Sign Release APK
echo ========================================
echo.

:: Chuyển đến thư mục chứa script
cd /d "%~dp0"

:: Kiểm tra keystore
if not exist "truecaller.keystore" (
    echo ERROR: truecaller.keystore not found!
    echo.
    echo Please run create-keystore.bat first to create a keystore.
    echo.
    pause
    exit /b 1
)

:: Kiểm tra APK
set APK_PATH=app\build\outputs\apk\release\app-release-unsigned.apk
if not exist "%APK_PATH%" (
    echo ERROR: Release APK not found at %APK_PATH%
    echo.
    echo Please run: gradlew.bat assembleRelease
    echo.
    pause
    exit /b 1
)

:: Tên file APK đã ký
set SIGNED_APK=app\build\outputs\apk\release\app-release-signed.apk

:: Xóa APK đã ký cũ nếu có
if exist "%SIGNED_APK%" del "%SIGNED_APK%"

echo Signing APK...
echo.

:: Sử dụng apksigner (Android SDK Build Tools)
set APKSIGNER_PATH=
if exist "%LOCALAPPDATA%\Android\Sdk\build-tools" (
    for /f "delims=" %%i in ('dir /b /ad /o-n "%LOCALAPPDATA%\Android\Sdk\build-tools" 2^>nul') do (
        set APKSIGNER_PATH=%LOCALAPPDATA%\Android\Sdk\build-tools\%%i\apksigner.bat
        goto :found_apksigner
    )
)

:found_apksigner
if exist "%APKSIGNER_PATH%" (
    echo Using apksigner...
    echo.
    call "%APKSIGNER_PATH%" sign --ks truecaller.keystore --ks-key-alias truecaller --out "%SIGNED_APK%" "%APK_PATH%"
    if %ERRORLEVEL% EQU 0 (
        echo.
        echo ========================================
        echo APK signed successfully!
        echo.
        echo Signed APK: %CD%\%SIGNED_APK%
        echo ========================================
        goto :verify
    )
)

:: Fallback: sử dụng jarsigner
echo Using jarsigner (fallback)...
echo.
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 -keystore truecaller.keystore "%APK_PATH%" truecaller

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Failed to sign APK!
    pause
    exit /b 1
)

:: Đổi tên file sau khi ký
move "%APK_PATH%" "%SIGNED_APK%" >nul

echo.
echo ========================================
echo APK signed successfully!
echo.
echo Signed APK: %CD%\%SIGNED_APK%
echo ========================================

:verify
echo.
echo Verifying signature...
if exist "%APKSIGNER_PATH%" (
    call "%APKSIGNER_PATH%" verify "%SIGNED_APK%"
) else (
    jarsigner -verify -verbose -certs "%SIGNED_APK%"
)

echo.
pause

