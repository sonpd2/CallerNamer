package com.zobaer53.incomingcall;

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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;

import com.zobaer53.incomingcall.permission.GoingToSettingsSnackbar;
import com.zobaer53.incomingcall.permission.RuntimePermissionRequester;
import com.zobaer53.incomingcall.permission.SpecialPermissionRequester;

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
            Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
    };
    private ArrayList<String> notGrantedPermissions;

    private Button getRuntimePermissions;
    private Button getSpecialPermissions;
    private TextView textPhoneStateStatus;
    private TextView textCallLogStatus;
    private TextView textContactsStatus;
    private TextView textWriteContactsStatus;
    private TextView textSpecialStatus;
    private TextView textBatteryStatus;
    private Button btnRequestBatteryOptimization;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        
        // Lịch sử (mở tab lịch sử trong ApiConfigActivity)
        MaterialCardView cardHistory = findViewById(R.id.cardHistory);
        cardHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ApiConfigActivity.class);
            intent.putExtra("tab", 1); // Tab lịch sử
            startActivity(intent);
        });
        
        // Quyền truy cập
        MaterialCardView cardPermissions = findViewById(R.id.cardPermissions);
        cardPermissions.setOnClickListener(v -> {
            // Scroll to permission buttons
            getRuntimePermissions.setVisibility(View.VISIBLE);
            getSpecialPermissions.setVisibility(View.VISIBLE);
        });

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
}
