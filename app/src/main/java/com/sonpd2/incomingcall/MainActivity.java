package com.sonpd2.incomingcall;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import com.sonpd2.incomingcall.permission.GoingToSettingsSnackbar;
import com.sonpd2.incomingcall.permission.RuntimePermissionRequester;
import com.sonpd2.incomingcall.permission.SpecialPermissionRequester;

public class MainActivity extends AppCompatActivity {

    private SpecialPermissionRequester specialPermissionRequester;
    private RuntimePermissionRequester runtimePermissionRequester;
    private NotificationManager mNotificationManager;
    private static final String CHANNEL_ID = "app_channel";

    private final String[] permissions = new String[]{
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Manifest.permission.POST_NOTIFICATIONS
    };
    private ArrayList<String> notGrantedPermissions;

    private Button getRuntimePermissions;
    private Button getSpecialPermissions;
    private TextView textPhoneStateStatus;
    private TextView textCallLogStatus;
    private TextView textContactsStatus;
    private TextView textWriteContactsStatus;
    private TextView textNotificationStatus;
    private TextView textSpecialStatus;
    private TextView textBatteryStatus;
    private Button btnRequestBatteryOptimization;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Force light mode, disable dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        
        setContentView(R.layout.activity_main);
        
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        createNotificationChannel();
        showRecordingNotification();
        specialPermissionRequester = new SpecialPermissionRequester(this);
        runtimePermissionRequester = new RuntimePermissionRequester(this);

        textPhoneStateStatus = findViewById(R.id.textPhoneStateStatus);
        textCallLogStatus = findViewById(R.id.textCallLogStatus);
        textContactsStatus = findViewById(R.id.textContactsStatus);
        textWriteContactsStatus = findViewById(R.id.textWriteContactsStatus);
        textNotificationStatus = findViewById(R.id.textNotificationStatus);
        textSpecialStatus = findViewById(R.id.textSpecialStatus);
        textBatteryStatus = findViewById(R.id.textBatteryStatus);
        btnRequestBatteryOptimization = findViewById(R.id.btnRequestBatteryOptimization);
        
        // Set click listener for battery optimization button
        if (btnRequestBatteryOptimization != null) {
            btnRequestBatteryOptimization.setOnClickListener(v -> requestBatteryOptimizationIfNeeded());
        }
        
        getRuntimePermissions = findViewById(R.id.requestRuntimePermission);
        setOnGetPermissionsClickListener();
        getSpecialPermissions = findViewById(R.id.requestSpecialPermission);
        setOnGetSpecialPermissionsClickListener();
        
        // Cấu hình API
        MaterialCardView cardApiConfig = findViewById(R.id.cardApiConfig);
        cardApiConfig.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ApiConfigActivity.class);
            startActivity(intent);
        });
        
        // Xem Log
        MaterialCardView cardViewLog = findViewById(R.id.cardViewLog);
        cardViewLog.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LogViewerActivity.class);
            startActivity(intent);
        });
        
        // Lịch sử (mở activity lịch sử riêng)
        MaterialCardView cardHistory = findViewById(R.id.cardHistory);
        cardHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CallHistoryActivity.class);
            startActivity(intent);
        });
        
        // Quyền truy cập
        MaterialCardView cardPermissions = findViewById(R.id.cardPermissions);
        cardPermissions.setOnClickListener(v -> {
            // Scroll to permission buttons
            getRuntimePermissions.setVisibility(View.VISIBLE);
            getSpecialPermissions.setVisibility(View.VISIBLE);
        });
        
        // Tìm kiếm
        MaterialCardView cardSearch = findViewById(R.id.cardSearch);
        cardSearch.setOnClickListener(v -> showSearchDialog());

        if (!specialPermissionRequester.checkSystemAlertWindowPermission()) {
            specialPermissionRequester.requestSystemAlertWindowPermission();
        }
        if (!runtimePermissionRequester.checkSelfPermissions(permissions)) {
            runtimePermissionRequester.requestPermissions();
        }
        
        // Tự động request bỏ qua battery optimization
        requestBatteryOptimizationIfNeeded();
        
        // Khởi động foreground service để chạy nền
        startForegroundService();
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkAllPermissions();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        checkAllPermissions(); // Refresh when returning from settings
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        notGrantedPermissions =
                runtimePermissionRequester.onRequestPermissionsResult(requestCode, permissions, grantResults);

        setGetRuntimePermissionsVisibility(notGrantedPermissions.isEmpty());
        checkAllPermissions(); // Refresh status after permission result
    }

    private void setOnGetPermissionsClickListener() {
        getRuntimePermissions.setOnClickListener(view -> {
            checkPermissionsForRationale();
            checkDeniedPermissions(view);
        });
    }

    private void setOnGetSpecialPermissionsClickListener() {
        getSpecialPermissions.setOnClickListener(view ->
                specialPermissionRequester.requestSystemAlertWindowPermission());
    }

    private void checkAllPermissions() {
        // Check individual runtime permissions
        checkPermissionStatus(Manifest.permission.READ_PHONE_STATE, textPhoneStateStatus);
        checkPermissionStatus(Manifest.permission.READ_CALL_LOG, textCallLogStatus);
        checkPermissionStatus(Manifest.permission.READ_CONTACTS, textContactsStatus);
        checkPermissionStatus(Manifest.permission.WRITE_CONTACTS, textWriteContactsStatus);
        
        // Check POST_NOTIFICATIONS (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkPermissionStatus(Manifest.permission.POST_NOTIFICATIONS, textNotificationStatus);
        } else {
            // Android < 13 không cần permission này
            if (textNotificationStatus != null) {
                textNotificationStatus.setText("✓ Không cần (Android < 13)");
                textNotificationStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }
        }
        
        // Check special permissions
        checkSpecialPermissions();
        checkBatteryOptimization();
        
        // Check overall runtime permissions
        Boolean isAllRuntimePermissionsGranted = runtimePermissionRequester.checkSelfPermissions(permissions);
        setGetRuntimePermissionsVisibility(isAllRuntimePermissionsGranted);
    }
    
    private void checkPermissionStatus(String permission, TextView statusView) {
        if (statusView == null) return;
        
        int status = ContextCompat.checkSelfPermission(this, permission);
        boolean granted = status == PackageManager.PERMISSION_GRANTED;
        
        statusView.setText(granted ? "✓ Đã cấp" : "✗ Chưa cấp");
        statusView.setTextColor(granted ? 
            getResources().getColor(android.R.color.holo_green_dark) : 
            getResources().getColor(android.R.color.holo_red_dark));
    }
    
    private void checkSpecialPermissions() {
        Boolean isSpecialPermissionGranted = specialPermissionRequester.checkSystemAlertWindowPermission();
        setGetSpecialPermissionsVisibility(isSpecialPermissionGranted);
        if (textSpecialStatus != null) {
            textSpecialStatus.setText(isSpecialPermissionGranted ? "✓ Đã cấp" : "✗ Chưa cấp");
            textSpecialStatus.setTextColor(isSpecialPermissionGranted ? 
                getResources().getColor(android.R.color.holo_green_dark) : 
                getResources().getColor(android.R.color.holo_red_dark));
        }
    }
    
    private void checkBatteryOptimization() {
        if (textBatteryStatus == null) return;
        
        boolean isIgnoringBatteryOptimizations = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            if (powerManager != null) {
                isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(getPackageName());
            }
        } else {
            // Android < M doesn't have battery optimization
            isIgnoringBatteryOptimizations = true;
        }
        
        textBatteryStatus.setText(isIgnoringBatteryOptimizations ? "✓ Đã bỏ qua" : "✗ Chưa bỏ qua");
        textBatteryStatus.setTextColor(isIgnoringBatteryOptimizations ? 
            getResources().getColor(android.R.color.holo_green_dark) : 
            getResources().getColor(android.R.color.holo_red_dark));
        
        // Hiển thị/ẩn nút request dựa trên trạng thái
        if (btnRequestBatteryOptimization != null) {
            btnRequestBatteryOptimization.setVisibility(isIgnoringBatteryOptimizations ? View.GONE : View.VISIBLE);
        }
    }
    
    private void requestBatteryOptimizationIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                try {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    // Nếu không thể mở dialog, mở settings trực tiếp
                    try {
                        Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                        startActivity(intent);
                    } catch (Exception ex) {
                        // Ignore
                    }
                }
            }
        }
    }
    
    private void startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent serviceIntent = new Intent(this, CallMonitorService.class);
            startForegroundService(serviceIntent);
        } else {
            Intent serviceIntent = new Intent(this, CallMonitorService.class);
            startService(serviceIntent);
        }
    }

    private void setGetSpecialPermissionsVisibility(Boolean hide) {
        if (hide) {
            getSpecialPermissions.setVisibility(View.GONE);
        } else {
            getSpecialPermissions.setVisibility(View.VISIBLE);
        }
    }

    private void setGetRuntimePermissionsVisibility(Boolean hide) {
        if (hide) {
            getRuntimePermissions.setVisibility(View.GONE);
        } else {
            getRuntimePermissions.setVisibility(View.VISIBLE);
        }
    }

    private void checkPermissionsForRationale() {
        ArrayList<String> permissionsForRationale = runtimePermissionRequester.getPermissionsForRationale(notGrantedPermissions);
        if (!permissionsForRationale.isEmpty()) {
            runtimePermissionRequester.setPermissionsForRequest(permissionsForRationale);
            runtimePermissionRequester.requestPermissions();
        }
    }

    private void checkDeniedPermissions(View view) {
        ArrayList<String> deniedPermissions = runtimePermissionRequester.getDeniedPermissions(notGrantedPermissions);
        if (!deniedPermissions.isEmpty()) {
            GoingToSettingsSnackbar goingToSettingsSnackbar = new GoingToSettingsSnackbar(this, view);
            goingToSettingsSnackbar.showSnackbar("You must grant permissions in Settings!", "Settings");
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Application Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Channel for application notifications");
            mNotificationManager.createNotificationChannel(channel);
        }
    }

    private void showRecordingNotification(){
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(
                this, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Application Name")
                .setContentText("Application started")
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        
        mNotificationManager.notify(1, builder.build());
    }
    
    private void showSearchDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_search_input, null);
        
        TextInputEditText editPhoneNumber = dialogView.findViewById(R.id.editPhoneNumber);
        MaterialButton btnSearch = dialogView.findViewById(R.id.btnSearch);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Tìm kiếm số điện thoại")
                .setView(dialogView)
                .setCancelable(true)
                .create();
        
        // Xử lý khi nhấn Enter
        editPhoneNumber.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String phoneNumber = editPhoneNumber.getText().toString().trim();
                if (!phoneNumber.isEmpty()) {
                    dialog.dismiss();
                    searchByApi(phoneNumber);
                }
                return true;
            }
            return false;
        });
        
        btnSearch.setOnClickListener(v -> {
            String phoneNumber = editPhoneNumber.getText().toString().trim();
            if (phoneNumber.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập số điện thoại", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            searchByApi(phoneNumber);
        });
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    private void searchByApi(String phoneNumber) {
        // Ẩn bàn phím
        View view = getCurrentFocus();
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) 
                getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
        
        // Hiển thị loading
        Toast.makeText(this, "Đang tìm kiếm...", Toast.LENGTH_SHORT).show();
        
        // Gọi API
        ApiCaller.searchPhoneNumber(this, phoneNumber, new ApiCaller.ApiCallback() {
            @Override
            public void onSuccess(String name, String position, String company, List<String> emails) {
                // Lưu vào lịch sử
                CallHistoryHelper historyHelper = new CallHistoryHelper(MainActivity.this);
                historyHelper.addHistory(phoneNumber, name, position, company, emails, true);
                
                // Hiển thị dialog kết quả
                showSearchResultDialog(phoneNumber, name, position, company, emails);
            }
            
            @Override
            public void onError(String errorMessage) {
                // Lưu vào lịch sử (không tìm thấy)
                CallHistoryHelper historyHelper = new CallHistoryHelper(MainActivity.this);
                historyHelper.addHistory(phoneNumber, "", "", "", new ArrayList<>(), false);
                
                // Hiển thị lỗi
                Toast.makeText(MainActivity.this, "Lỗi: " + errorMessage, Toast.LENGTH_LONG).show();
            }
            
            @Override
            public void onNotFound() {
                // Lưu vào lịch sử (không tìm thấy)
                CallHistoryHelper historyHelper = new CallHistoryHelper(MainActivity.this);
                historyHelper.addHistory(phoneNumber, "", "", "", new ArrayList<>(), false);
                
                // Hiển thị thông báo
                Toast.makeText(MainActivity.this, "Không tìm thấy thông tin", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showSearchResultDialog(String phoneNumber, String name, String position, String company, List<String> emails) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_search_result, null);
        
        TextView textPhoneNumber = dialogView.findViewById(R.id.textDialogPhoneNumber);
        TextView textName = dialogView.findViewById(R.id.textDialogName);
        TextView textPosition = dialogView.findViewById(R.id.textDialogPosition);
        TextView textCompany = dialogView.findViewById(R.id.textDialogCompany);
        TextView textEmails = dialogView.findViewById(R.id.textDialogEmails);
        MaterialButton btnClose = dialogView.findViewById(R.id.btnDialogClose);
        MaterialButton btnAddContact = dialogView.findViewById(R.id.btnDialogAddContact);
        
        textPhoneNumber.setText(phoneNumber);
        textName.setText(name.isEmpty() ? "Không có tên" : name);
        textPosition.setText(position.isEmpty() ? "Không có chức vụ" : position);
        textCompany.setText(company.isEmpty() ? "Không có công ty" : company);
        
        if (emails != null && !emails.isEmpty()) {
            StringBuilder emailsText = new StringBuilder("Email: ");
            for (int i = 0; i < emails.size(); i++) {
                if (i > 0) emailsText.append(", ");
                emailsText.append(emails.get(i));
            }
            textEmails.setText(emailsText.toString());
            textEmails.setVisibility(View.VISIBLE);
        } else {
            textEmails.setVisibility(View.GONE);
        }
        
        // Kiểm tra quyền và hiển thị nút thêm vào danh bạ
        boolean hasPermission = ContextCompat.checkSelfPermission(this, 
                Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED;
        
        if (hasPermission) {
            ContactHelper contactHelper = new ContactHelper(this);
            boolean exists = contactHelper.isContactExists(phoneNumber);
            
            if (!exists) {
                btnAddContact.setVisibility(View.VISIBLE);
                btnAddContact.setOnClickListener(v -> {
                    String contactName = name.isEmpty() ? phoneNumber : name;
                    boolean success = contactHelper.addContact(contactName, phoneNumber, company, position, emails);
                    
                    if (success) {
                        Toast.makeText(this, "Đã thêm vào danh bạ", Toast.LENGTH_SHORT).show();
                        btnAddContact.setVisibility(View.GONE);
                    } else {
                        Toast.makeText(this, "Lỗi khi thêm vào danh bạ", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
        
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
}
