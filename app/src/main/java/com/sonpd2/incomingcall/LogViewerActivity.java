package com.sonpd2.incomingcall;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.appbar.MaterialToolbar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class LogViewerActivity extends AppCompatActivity {

    private TextView textLog;
    private static final String TAG = "ducdv";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Force light mode, disable dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        
        setContentView(R.layout.activity_log_viewer);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        textLog = findViewById(R.id.textLog);
        textLog.setMovementMethod(new ScrollingMovementMethod());

        loadLogs();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_log_viewer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            loadLogs();
            return true;
        } else if (item.getItemId() == R.id.action_clear) {
            textLog.setText("");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadLogs() {
        try {
            Process process = Runtime.getRuntime().exec("logcat -d -v time *:V");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            StringBuilder log = new StringBuilder();
            String line;
            int lineCount = 0;
            int maxLines = 1000; // Giới hạn 1000 dòng

            // Đọc từ cuối lên để lấy log mới nhất
            while ((line = bufferedReader.readLine()) != null && lineCount < maxLines) {
                // Lọc log của app
                if (line.contains("ducdv") || line.contains("com.sonpd2.incomingcall")) {
                    log.insert(0, line + "\n");
                    lineCount++;
                }
            }

            if (log.length() == 0) {
                log.append("Chưa có log. Hãy thử lại sau khi có cuộc gọi đến.");
            }

            textLog.setText(log.toString());
            // Scroll to top
            textLog.scrollTo(0, 0);

        } catch (IOException e) {
            textLog.setText("Lỗi đọc log: " + e.getMessage() + "\n\n" +
                    "Lưu ý: Cần quyền root hoặc sử dụng adb logcat để xem log đầy đủ.");
        }
    }
}

