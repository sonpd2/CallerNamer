@echo off
echo ========================================
echo   True Caller - Create Keystore
echo ========================================
echo.

:: Chuyển đến thư mục chứa script
cd /d "%~dp0"

:: Kiểm tra xem keystore đã tồn tại chưa
if exist "truecaller.keystore" (
    echo WARNING: truecaller.keystore already exists!
    echo.
    set /p overwrite="Do you want to overwrite it? (y/n): "
    if /i not "%overwrite%"=="y" (
        echo Cancelled.
        pause
        exit /b 0
    )
    del truecaller.keystore
)

echo Creating keystore...
echo.
echo You will be prompted to enter:
echo   - Keystore password (at least 6 characters)
echo   - Key password (can be same as keystore password)
echo   - Your name and organization information
echo.

:: Tạo keystore
keytool -genkeypair -v -keystore truecaller.keystore -alias truecaller -keyalg RSA -keysize 2048 -validity 10000

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Keystore created successfully!
    echo File: %CD%\truecaller.keystore
    echo ========================================
) else (
    echo.
    echo ERROR: Failed to create keystore!
    echo Make sure Java is installed and keytool is in PATH.
)

echo.
pause

