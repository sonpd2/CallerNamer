# Hướng Dẫn Chạy Project CallerNamer

## Yêu Cầu Hệ Thống

1. **Android Studio** (bản mới nhất khuyến nghị) - Tải tại: https://developer.android.com/studio
2. **Android SDK** - Cài đặt qua Android Studio
   - API Level 31 (compileSdk 31)
   - Build Tools
3. **JDK 8** hoặc cao hơn - Cài đặt cùng Android Studio
4. **Thiết bị Android** hoặc **Android Emulator**
   - Minimum SDK: 21 (Android 5.0 Lollipop)
   - Target SDK: 31 (Android 12)

## Các Bước Chạy Project

### Bước 1: Mở Project trong Android Studio

1. Khởi động **Android Studio**
2. Chọn **File** → **Open**
3. Duyệt đến thư mục `CallerNamer` và chọn thư mục project
4. Đợi Android Studio đồng bộ project (sync Gradle)

### Bước 2: Đồng Bộ Dependencies

- Nếu Android Studio không tự động sync, chọn **File** → **Sync Project with Gradle Files**
- Đợi quá trình tải dependencies hoàn tất

### Bước 3: Thiết Lập Thiết Bị

**Tùy chọn A: Sử dụng Emulator**
1. Chọn **Tools** → **Device Manager**
2. Tạo Virtual Device mới (nếu chưa có)
3. Chọn một thiết bị ảo (khuyến nghị: Pixel 5 hoặc tương tự)
4. Chọn System Image (API 31 hoặc cao hơn)

**Tùy chọn B: Sử dụng Thiết Bị Thật**
1. Bật **USB Debugging** trên điện thoại:
   - Vào **Settings** → **About phone** → Nhấn 7 lần vào **Build number**
   - Quay lại **Settings** → **Developer options** → Bật **USB debugging**
2. Kết nối điện thoại với máy tính qua USB
3. Chấp nhận "Allow USB debugging" trên điện thoại

### Bước 4: Chạy Ứng Dụng

1. Chọn thiết bị/emulator từ dropdown ở thanh toolbar
2. Nhấn nút **Run** (▶️) hoặc nhấn `Shift + F10` (Windows/Linux) hoặc `Ctrl + R` (Mac)
3. Hoặc chọn **Run** → **Run 'app'**

### Bước 5: Cấp Quyền Ứng Dụng

Sau khi ứng dụng cài đặt và chạy, bạn cần cấp các quyền sau:

1. **READ_PHONE_STATE** - Đọc trạng thái cuộc gọi
2. **READ_CALL_LOG** - Đọc nhật ký cuộc gọi
3. **SYSTEM_ALERT_WINDOW** - Hiển thị cửa sổ nổi
4. **READ_CONTACTS** - Đọc danh bạ

Ứng dụng sẽ tự động yêu cầu các quyền này khi cần thiết.

**Lưu ý quan trọng cho quyền SYSTEM_ALERT_WINDOW:**
- Vào **Settings** → **Apps** → **CallerNamer** → **Display over other apps**
- Bật quyền này để ứng dụng có thể hiển thị cửa sổ nổi

## Kiểm Tra Cài Đặt

Sau khi chạy thành công:
- Ứng dụng sẽ xuất hiện trên thiết bị
- Khi có cuộc gọi đến, một cửa sổ nổi sẽ hiển thị thông tin người gọi
- Cửa sổ có thể kéo thả được
- Hiển thị tên (nếu có trong danh bạ) và số điện thoại

## Xử Lý Lỗi Thường Gặp

### Lỗi Gradle Sync
- Kiểm tra kết nối internet
- Xóa `.gradle` folder và sync lại
- Kiểm tra phiên bản Android Gradle Plugin

### Lỗi Build
- Clean project: **Build** → **Clean Project**
- Rebuild project: **Build** → **Rebuild Project**
- Invalidate caches: **File** → **Invalidate Caches / Restart**

### Lỗi Permissions
- Đảm bảo đã cấp tất cả quyền cần thiết
- Đặc biệt là quyền "Display over other apps"

## Chạy Project Bằng Command Line

### Yêu Cầu Trước Khi Chạy

1. **Android SDK** đã được cài đặt (thông thường tại: `%LOCALAPPDATA%\Android\Sdk`)
2. **Java JDK 8+** đã được cài đặt và cấu hình `JAVA_HOME`
3. **ADB (Android Debug Bridge)** - có sẵn trong Android SDK
4. **Thiết bị Android** hoặc **Emulator** đã kết nối và bật USB Debugging

### Thiết Lập Biến Môi Trường (Windows)

Thêm vào **Environment Variables**:

```powershell
# Thêm vào PATH (thay đổi đường dẫn theo vị trí cài đặt của bạn)
%LOCALAPPDATA%\Android\Sdk\platform-tools
%LOCALAPPDATA%\Android\Sdk\tools
%LOCALAPPDATA%\Android\Sdk\tools\bin

# Thiết lập ANDROID_HOME
ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk

# Thiết lập JAVA_HOME (nếu chưa có)
JAVA_HOME=C:\Program Files\Java\jdk-11.0.x
```

**Cách thiết lập:**
1. Nhấn `Win + R` → gõ `sysdm.cpl` → Enter
2. Tab **Advanced** → **Environment Variables**
3. Thêm các biến trên vào **System variables** → **Path**

### Các Lệnh Cơ Bản

#### 1. Kiểm Tra Kết Nối Thiết Bị

```powershell
# Kiểm tra thiết bị đã kết nối chưa
adb devices

# Kết quả mong đợi:
# List of devices attached
# emulator-5554   device
# hoặc
# ABC123XYZ       device
```

#### 2. Clean Project (Xóa Build Cũ)

```powershell
# Di chuyển đến thư mục project
cd C:\Users\device\Desktop\mmo\CallerNamer

# Clean project
.\gradlew clean
```

#### 3. Build APK Debug

```powershell
# Build APK debug (không ký)
.\gradlew assembleDebug

# APK sẽ được tạo tại:
# app\build\outputs\apk\debug\app-debug.apk
```

#### 4. Build APK Release

```powershell
# Build APK release (cần keystore để ký)
.\gradlew assembleRelease

# APK sẽ được tạo tại:
# app\build\outputs\apk\release\app-release.apk
```

#### 5. Cài Đặt APK Lên Thiết Bị

**Cách 1: Build và cài đặt trực tiếp**

```powershell
# Build và cài đặt lên thiết bị đã kết nối
.\gradlew installDebug

# Hoặc với release (nếu đã build)
.\gradlew installRelease
```

**Cách 2: Cài đặt APK đã build sẵn**

```powershell
# Cài đặt APK từ file đã build
adb install app\build\outputs\apk\debug\app-debug.apk

# Ghi đè ứng dụng cũ (nếu đã cài)
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

#### 6. Chạy Ứng Dụng

```powershell
# Chạy ứng dụng (build, cài đặt và mở)
.\gradlew installDebug
adb shell am start -n com.zobaer53.incomingcall/.MainActivity
```

Hoặc gộp thành một lệnh:

```powershell
# Build, cài đặt và chạy
.\gradlew installDebug && adb shell am start -n com.zobaer53.incomingcall/.MainActivity
```

#### 7. Xem Logs (Logcat)

```powershell
# Xem tất cả logs
adb logcat

# Xem logs với filter
adb logcat -s "AndroidRuntime:*" "*:E"

# Lọc logs theo package name
adb logcat | findstr "com.zobaer53.incomingcall"

# Xóa logs cũ và xem logs mới
adb logcat -c && adb logcat
```

#### 8. Gỡ Cài Đặt Ứng Dụng

```powershell
# Gỡ cài đặt ứng dụng
adb uninstall com.zobaer53.incomingcall
```

### Lệnh Nâng Cao

#### Build và Cài Đặt Tự Động (One-liner)

```powershell
# Clean, build và cài đặt
.\gradlew clean installDebug
```

#### Khởi Động Emulator Từ Command Line

```powershell
# Liệt kê các AVD có sẵn
emulator -list-avds

# Khởi động emulator (thay tên AVD)
emulator -avd Pixel_5_API_31

# Hoặc
%LOCALAPPDATA%\Android\Sdk\emulator\emulator -avd Pixel_5_API_31
```

#### Kiểm Tra Thông Tin Thiết Bị

```powershell
# Kiểm tra phiên bản Android
adb shell getprop ro.build.version.release

# Kiểm tra API level
adb shell getprop ro.build.version.sdk

# Kiểm tra model thiết bị
adb shell getprop ro.product.model

# Kiểm tra danh sách ứng dụng đã cài
adb shell pm list packages | findstr "zobaer53"
```

#### Cấp Quyền Tự Động (Nếu Cần)

```powershell
# Cấp quyền READ_PHONE_STATE
adb shell pm grant com.zobaer53.incomingcall android.permission.READ_PHONE_STATE

# Cấp quyền READ_CALL_LOG
adb shell pm grant com.zobaer53.incomingcall android.permission.READ_CALL_LOG

# Cấp quyền READ_CONTACTS
adb shell pm grant com.zobaer53.incomingcall android.permission.READ_CONTACTS

# Lưu ý: Quyền SYSTEM_ALERT_WINDOW cần cấp thủ công trên thiết bị
```

### Quy Trình Hoàn Chỉnh

**Bước 1: Kiểm tra kết nối**
```powershell
adb devices
```

**Bước 2: Build và cài đặt**
```powershell
.\gradlew clean installDebug
```

**Bước 3: Chạy ứng dụng**
```powershell
adb shell am start -n com.zobaer53.incomingcall/.MainActivity
```

**Bước 4: Xem logs (tùy chọn)**
```powershell
# Mở terminal mới để xem logs
adb logcat | findstr "com.zobaer53.incomingcall"
```

### Script Batch (Windows)

Tạo file `build-and-run.bat`:

```batch
@echo off
echo Building and installing CallerNamer...
call gradlew.bat clean installDebug
if %ERRORLEVEL% EQU 0 (
    echo Installation successful!
    echo Starting application...
    adb shell am start -n com.zobaer53.incomingcall/.MainActivity
    echo Done!
) else (
    echo Build failed!
    pause
)
```

Sử dụng: Chạy `build-and-run.bat` trong thư mục project.

### Lưu Ý Quan Trọng

1. **Gradle Wrapper**: Project đã có Gradle Wrapper (`gradlew.bat`), không cần cài Gradle riêng
2. **Lần đầu chạy**: Gradle sẽ tải dependencies, có thể mất vài phút
3. **USB Debugging**: Đảm bảo thiết bị đã bật USB Debugging
4. **Quyền**: Một số quyền cần cấp thủ công trên thiết bị (Settings → Apps → CallerNamer)

### Xử Lý Lỗi Command Line

**Lỗi: 'adb' is not recognized**
- Thêm `%LOCALAPPDATA%\Android\Sdk\platform-tools` vào PATH

**Lỗi: 'gradlew' is not recognized**
- Đảm bảo đang ở đúng thư mục project
- Sử dụng `.\gradlew.bat` thay vì `gradlew`

**Lỗi: No devices found**
- Kiểm tra `adb devices`
- Đảm bảo USB Debugging đã bật
- Thử cắm lại USB hoặc restart adb: `adb kill-server && adb start-server`

**Lỗi: Build failed**
- Kiểm tra kết nối internet (để tải dependencies)
- Kiểm tra Java version: `java -version`
- Clean project: `.\gradlew clean`

## Thông Tin Project

- **Package Name**: `com.zobaer53.incomingcall`
- **Min SDK**: 21
- **Target SDK**: 31
- **Compile SDK**: 31
- **Language**: Java
- **Gradle Version**: 7.0.2

