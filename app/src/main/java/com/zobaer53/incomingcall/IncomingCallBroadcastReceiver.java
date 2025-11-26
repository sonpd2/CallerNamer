package com.zobaer53.incomingcall;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

public class IncomingCallBroadcastReceiver extends BroadcastReceiver {
    private static WindowManager windowManager;
    @SuppressLint("StaticFieldLeak")
    private static ViewGroup windowLayout;

    private static final float WINDOW_WIDTH_RATIO = 0.8f;
    private static final String TAG = "ducdv";
    private WindowManager.LayoutParams params;
    private float x;
    private float y;
    private String name;
    private String position;
    private String company;
    private List<String> emails;
    private String currentPhoneNumber; // Lưu số điện thoại hiện tại để lưu lịch sử
    private Context currentContext; // Lưu context để lưu lịch sử

    // API config will be loaded from SharedPreferences
    private ApiConfigHelper configHelper;


    @Override
    public void onReceive(Context context, Intent intent) {
        // Initialize configHelper with context
        if (configHelper == null) {
            configHelper = new ApiConfigHelper(context);
        }
        
        if (intent.getAction().equals("android.intent.action.PHONE_STATE")) {
            String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            if (number != null) {
                if (intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                        .equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                    showWindow(context, number);
                } else if (intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                        .equals(TelephonyManager.EXTRA_STATE_IDLE) ||
                        intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                                .equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                    closeWindow();
                }
            }
        }
    }

    private class ApiPostRequestTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String apiUrl = params[0];
            String jsonData = params[1];
            String apiKey = params[2];
            String apiUser = params[3];
            String cookie = params[4];

            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();

            MediaType mediaType = MediaType.get("application/json");
            RequestBody body = RequestBody.create(jsonData, mediaType);

            Request.Builder requestBuilder = new Request.Builder()
                    .url(apiUrl)
                    .method("POST", body)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:145.0) Gecko/20100101 Firefox/145.0")
                    .addHeader("Accept", "application/json")
                    // Chỉ dùng gzip và deflate - OkHttp tự động hỗ trợ
                    .addHeader("Accept-Encoding", "gzip, deflate")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Referer", "https://vops.viettel.vn/contact/my-contact")
                    .addHeader("X-User-Id", apiUser)
                    .addHeader("X-Auth-Token", apiKey)
                    .addHeader("Origin", "https://vops.viettel.vn");
            
            // Add Cookie header if not empty
            if (cookie != null && !cookie.trim().isEmpty()) {
                requestBuilder.addHeader("Cookie", cookie);
            }
            
            Request request = requestBuilder.build();

            try {
                Response response = client.newCall(request).execute();
                int statusCode = response.code();
                
                // Log tất cả headers
                Log.d(TAG, "=== Response Headers ===");
                for (String name : response.headers().names()) {
                    Log.d(TAG, name + ": " + response.header(name));
                }
                
                // Log Content-Encoding để debug
                String contentEncoding = response.header("Content-Encoding");
                Log.d(TAG, "Response Content-Encoding: " + contentEncoding);
                Log.d(TAG, "Content-Type: " + response.header("Content-Type"));
                
                String responseBody = null;
                if (response.body() != null) {
                    try {
                        // Đọc raw bytes trước
                        byte[] bytes = response.body().bytes();
                        Log.d(TAG, "Response body raw bytes length: " + bytes.length);
                        
                        // Log hex dump của 100 bytes đầu tiên
                        StringBuilder hexDump = new StringBuilder();
                        int dumpLength = Math.min(100, bytes.length);
                        for (int i = 0; i < dumpLength; i++) {
                            hexDump.append(String.format("%02X ", bytes[i] & 0xFF));
                            if ((i + 1) % 16 == 0) {
                                hexDump.append("\n");
                            }
                        }
                        Log.d(TAG, "Response hex dump (first " + dumpLength + " bytes):\n" + hexDump.toString());
                        
                        // Kiểm tra magic number của gzip (1F 8B)
                        if (bytes.length >= 2 && bytes[0] == 0x1F && bytes[1] == (byte)0x8B) {
                            Log.d(TAG, "Detected GZIP compression (magic: 1F 8B)");
                            // Thử giải nén gzip
                            try {
                                GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(bytes));
                                BufferedReader reader = new BufferedReader(new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8));
                                StringBuilder decompressed = new StringBuilder();
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    decompressed.append(line);
                                }
                                reader.close();
                                responseBody = decompressed.toString();
                                Log.d(TAG, "GZIP decompressed successfully, length: " + responseBody.length());
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to decompress GZIP: " + e.getMessage(), e);
                                // Fallback: thử decode như UTF-8
                                responseBody = new String(bytes, StandardCharsets.UTF_8);
                            }
                        } else {
                            // Không phải gzip, thử decode như UTF-8
                            responseBody = new String(bytes, StandardCharsets.UTF_8);
                            Log.d(TAG, "Response decoded as UTF-8, length: " + responseBody.length());
                        }
                        
                        // Log preview của response body
                        if (responseBody != null && responseBody.length() > 0) {
                            String preview = responseBody.length() > 200 ? responseBody.substring(0, 200) : responseBody;
                            Log.d(TAG, "Response body preview (first 200 chars): " + preview);
                            // Log một số ký tự cuối
                            if (responseBody.length() > 200) {
                                String tail = responseBody.substring(Math.max(0, responseBody.length() - 50));
                                Log.d(TAG, "Response body tail (last 50 chars): " + tail);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading response body: " + e.getMessage(), e);
                        e.printStackTrace();
                        responseBody = "Error reading response: " + e.getMessage();
                    }
                } else {
                    responseBody = "No response body";
                    Log.d(TAG, "Response body is null");
                }
                
                if (response.isSuccessful()) {
                    Log.d(TAG, "API Success - Status Code: " + statusCode);
                    return responseBody;
                } else {
                    String errorMessage = "HTTP " + statusCode + " " + response.message() + " | Body: " + responseBody;
                    Log.e(TAG, "API Error - " + errorMessage);
                    return "ERROR:" + statusCode + ":" + responseBody;
                }
            } catch (IOException e) {
                String errorMessage = "IOException: " + e.getMessage();
                Log.e(TAG, "Network Error - " + errorMessage, e);
                e.printStackTrace();
                return "ERROR:0:" + errorMessage;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                Log.d(TAG, "API Response received, length: " + result.length());
                
                // Kiểm tra nếu là lỗi (bắt đầu bằng "ERROR:")
                if (result.startsWith("ERROR:")) {
                    handleErrorResponse(result);
                } else {
                    parseJson(result);
                }
            } else {
                Log.e(TAG, "API Response is null");
                displayError("Không nhận được phản hồi từ API");
            }
        }
    }

    private void handleErrorResponse(String errorResult) {
        // Format: "ERROR:statusCode:body"
        String[] parts = errorResult.split(":", 3);
        String statusCode = parts.length > 1 ? parts[1] : "Unknown";
        String errorBody = parts.length > 2 ? parts[2] : errorResult;
        
        String errorMessage = "Lỗi HTTP " + statusCode;
        if (errorBody.length() > 0 && errorBody.length() < 100) {
            errorMessage += "\n" + errorBody;
        } else if (errorBody.length() >= 100) {
            errorMessage += "\n" + errorBody.substring(0, 100) + "...";
        }
        
        Log.e(TAG, "Error Response - Status: " + statusCode + ", Body: " + errorBody);
        displayError(errorMessage);
    }
    
    private void displayError(String errorMessage) {
        if (windowLayout != null) {
            TextView nameTextView = windowLayout.findViewById(R.id.name);
            TextView posTextView = windowLayout.findViewById(R.id.position);
            TextView compTextView = windowLayout.findViewById(R.id.company);
            
            if (nameTextView != null) {
                nameTextView.setText("Lỗi API");
            }
            if (posTextView != null) {
                posTextView.setText(errorMessage);
            }
            if (compTextView != null) {
                compTextView.setText("Kiểm tra Logcat để xem chi tiết");
            }
        }
    }
    
    private void parseJson(String response){
        try {
            JSONObject jsonObject = new JSONObject(response);
            
            // Kiểm tra success field
            boolean success = jsonObject.optBoolean("success", false);
            if (!success) {
                String errorMsg = "API trả về success: false";
                Log.e(TAG, errorMsg);
                displayError(errorMsg);
                return;
            }
            
            JSONArray contacts = jsonObject.getJSONArray("contacts");
            int totalUsers = jsonObject.optInt("totalUsers", 0);
            Log.d(TAG, "Total users: " + totalUsers);
            
            if (contacts.length() > 0) {
                // Lấy contact đầu tiên
                JSONObject contact = contacts.getJSONObject(0);
                
                // Lấy các trường, xử lý trường hợp null
                name = contact.optString("name", "");
                position = contact.optString("position", "");
                company = contact.optString("company", "");
                
                // Lấy emails
                emails = new ArrayList<>();
                if (contact.has("emails")) {
                    try {
                        JSONArray emailsArray = contact.getJSONArray("emails");
                        for (int i = 0; i < emailsArray.length(); i++) {
                            String email = emailsArray.optString(i, "");
                            if (!email.isEmpty()) {
                                emails.add(email);
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing emails: " + e.getMessage());
                    }
                }
                
                // Lấy thông tin bổ sung nếu có
                String employeeCode = contact.optString("employeeCode", "");
                String status = contact.optString("status", "");
                
                Log.d(TAG, "Contact info - Name: " + name + ", Position: " + position + ", Company: " + company);
                Log.d(TAG, "Emails: " + emails);
                Log.d(TAG, "Employee Code: " + employeeCode + ", Status: " + status);
                
                // Cập nhật UI chỉ khi windowLayout đã được tạo
                if (windowLayout != null) {
                    TextView nameTextView = windowLayout.findViewById(R.id.name);
                    TextView posTextView = windowLayout.findViewById(R.id.position);
                    TextView compTextView = windowLayout.findViewById(R.id.company);
                    TextView emailTextView = windowLayout.findViewById(R.id.email);
                    
                    if (nameTextView != null) {
                        nameTextView.setText(name.isEmpty() ? "Không có tên" : name);
                    }
                    if (posTextView != null) {
                        posTextView.setText(position.isEmpty() ? "Không có chức vụ" : position);
                    }
                    if (compTextView != null) {
                        compTextView.setText(company.isEmpty() ? "Không có công ty" : company);
                    }
                    
                    // Hiển thị emails
                    if (emailTextView != null) {
                        if (emails != null && !emails.isEmpty()) {
                            StringBuilder emailsText = new StringBuilder();
                            for (int i = 0; i < emails.size(); i++) {
                                if (i > 0) emailsText.append(", ");
                                emailsText.append(emails.get(i));
                            }
                            emailTextView.setText(emailsText.toString());
                            emailTextView.setVisibility(View.VISIBLE);
                        } else {
                            emailTextView.setVisibility(View.GONE);
                        }
                    }
                }
                
                // Lưu vào lịch sử
                if (currentPhoneNumber != null && currentContext != null) {
                    try {
                        CallHistoryHelper historyHelper = new CallHistoryHelper(currentContext);
                        historyHelper.addHistory(currentPhoneNumber, name, position, company, emails, true);
                    } catch (Exception e) {
                        Log.e(TAG, "Error saving history: " + e.getMessage());
                    }
                }
            } else {
                String noContactMsg = "Không tìm thấy thông tin (totalUsers: " + totalUsers + ")";
                Log.d(TAG, noContactMsg);
                if (windowLayout != null) {
                    TextView nameTextView = windowLayout.findViewById(R.id.name);
                    TextView posTextView = windowLayout.findViewById(R.id.position);
                    if (nameTextView != null) {
                        nameTextView.setText("Không tìm thấy");
                    }
                    if (posTextView != null) {
                        posTextView.setText("Số điện thoại không có trong hệ thống");
                    }
                }
                
                // Lưu vào lịch sử (không tìm thấy)
                if (currentPhoneNumber != null && currentContext != null) {
                    try {
                        CallHistoryHelper historyHelper = new CallHistoryHelper(currentContext);
                        historyHelper.addHistory(currentPhoneNumber, "", "", "", new ArrayList<>(), false);
                    } catch (Exception e) {
                        Log.e(TAG, "Error saving history: " + e.getMessage());
                    }
                }
            }
        } catch (JSONException e) {
            String errorMsg = "Lỗi parse JSON: " + e.getMessage();
            Log.e(TAG, errorMsg, e);
            displayError(errorMsg + "\nResponse: " + response.substring(0, Math.min(200, response.length())));
        }
    }

    private void showWindow(final Context context, String phone) {
        // Ensure configHelper is initialized
        if (configHelper == null) {
            configHelper = new ApiConfigHelper(context);
        }

        // Lưu số điện thoại và context hiện tại
        currentPhoneNumber = phone;
        currentContext = context;

        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowLayout = (ViewGroup) View.inflate(context, R.layout.window_call_info, null);
        getLayoutParams();
        setOnTouchListener();
        
        // Set phone number
        TextView numberTextView = windowLayout.findViewById(R.id.number);
        if (phone != null) {
            numberTextView.setText(phone);
        }
        
        // Set initial "Đang tìm kiếm..." message
        TextView nameTextView = windowLayout.findViewById(R.id.name);
        TextView posTextView = windowLayout.findViewById(R.id.position);
        TextView compTextView = windowLayout.findViewById(R.id.company);
        TextView emailTextView = windowLayout.findViewById(R.id.email);
        
        if (nameTextView != null) {
            nameTextView.setText("Đang tìm kiếm thông tin...");
        }
        if (posTextView != null) {
            posTextView.setText("");
        }
        if (compTextView != null) {
            compTextView.setText("");
        }
        if (emailTextView != null) {
            emailTextView.setVisibility(View.GONE);
        }
        
        Button cancelButton = windowLayout.findViewById(R.id.cancel);
        cancelButton.setOnClickListener(view -> closeWindow());
        windowManager.addView(windowLayout, params);

        // Tìm kiếm trong lịch sử trước
        if(phone != null) {
            CallHistoryHelper historyHelper = new CallHistoryHelper(context);
            CallHistoryHelper.CallHistoryItem historyItem = historyHelper.findInHistory(phone);
            
            if (historyItem != null && historyItem.found) {
                // Tìm thấy trong lịch sử, hiển thị luôn
                Log.d(TAG, "Found in history: " + phone);
                displayContactInfo(historyItem.name, historyItem.position, historyItem.company, historyItem.emails);
            } else {
                // Không tìm thấy trong lịch sử, gọi API
                Log.d(TAG, "Not found in history, calling API: " + phone);
                
                // Get API config from SharedPreferences
                String apiUrl = configHelper.getApiUrl();
                String apiKey = configHelper.getApiKey();
                String apiUser = configHelper.getApiUser();
                String cookie = configHelper.getCookie();
                
                // Cắt 9 số cuối để tìm kiếm (tránh trường hợp +84, 0084, etc.)
                String searchPhone = extractLast9Digits(phone);
                Log.d(TAG, "Original phone: " + phone + ", Search phone: " + searchPhone);
                
                // JSON data to send in the request
                String jsonData = "{\"org_paths\":[],\"text\":\""+ searchPhone +"\",\"page\":1,\"maxRecord\":20}";
                // Execute the AsyncTask to make the API POST request
                new ApiPostRequestTask().execute(apiUrl, jsonData, apiKey, apiUser, cookie);
            }
        }
    }
    
    /**
     * Hiển thị thông tin contact lên UI
     */
    private void displayContactInfo(String contactName, String contactPosition, String contactCompany, List<String> contactEmails) {
        if (windowLayout == null) return;
        
        TextView nameTextView = windowLayout.findViewById(R.id.name);
        TextView posTextView = windowLayout.findViewById(R.id.position);
        TextView compTextView = windowLayout.findViewById(R.id.company);
        TextView emailTextView = windowLayout.findViewById(R.id.email);
        
        if (nameTextView != null) {
            nameTextView.setText(contactName.isEmpty() ? "Không có tên" : contactName);
        }
        if (posTextView != null) {
            posTextView.setText(contactPosition.isEmpty() ? "Không có chức vụ" : contactPosition);
        }
        if (compTextView != null) {
            compTextView.setText(contactCompany.isEmpty() ? "Không có công ty" : contactCompany);
        }
        
        // Hiển thị emails
        if (emailTextView != null) {
            if (contactEmails != null && !contactEmails.isEmpty()) {
                StringBuilder emailsText = new StringBuilder();
                for (int i = 0; i < contactEmails.size(); i++) {
                    if (i > 0) emailsText.append(", ");
                    emailsText.append(contactEmails.get(i));
                }
                emailTextView.setText(emailsText.toString());
                emailTextView.setVisibility(View.VISIBLE);
            } else {
                emailTextView.setVisibility(View.GONE);
            }
        }
    }
    
    /**
     * Cắt 9 số cuối của số điện thoại để tìm kiếm
     * Ví dụ: +84978220328 -> 0978220328
     *        0084978220328 -> 0978220328
     *        0978220328 -> 0978220328
     */
    private String extractLast9Digits(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return "";
        }
        
        // Loại bỏ tất cả ký tự không phải số
        String digitsOnly = phoneNumber.replaceAll("[^0-9]", "");
        
        // Lấy 9 số cuối cùng
        if (digitsOnly.length() >= 9) {
            return digitsOnly.substring(digitsOnly.length() - 9);
        } else {
            // Nếu số điện thoại ngắn hơn 9 số, trả về toàn bộ
            return digitsOnly;
        }
    }

    private void getLayoutParams() {
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getWindowsTypeParameter(),
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;
        params.format = 1;
        params.width = getWindowWidth();
    }

    private int getWindowsTypeParameter() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        return WindowManager.LayoutParams.TYPE_PHONE;
    }

    private int getWindowWidth() {
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        return (int) (WINDOW_WIDTH_RATIO * (double) metrics.widthPixels);
    }

    private void setOnTouchListener() {
        windowLayout.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x = event.getRawX();
                    y = event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    updateWindowLayoutParams(event);
                    break;
                case MotionEvent.ACTION_UP:
                    view.performClick();
                default:
                    break;
            }
            return false;
        });
    }

    private void updateWindowLayoutParams(MotionEvent event) {
        params.x = params.x - (int) (x - event.getRawX());
        params.y = params.y - (int) (y - event.getRawY());
        windowManager.updateViewLayout(windowLayout, params);
        x = event.getRawX();
        y = event.getRawY();
    }

    private void closeWindow() {
        if (windowLayout != null) {
            windowManager.removeView(windowLayout);
            windowLayout = null;
        }
    }
    /*
     * Returns contact's id
     */
    @SuppressLint("Range")
    private String getContactId(String phoneNumber, Context context) {
        ContentResolver mResolver = context.getContentResolver();

        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber));

        Cursor cursor = mResolver.query(uri, new String[] {
                ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup._ID }, null, null, null);

        String contactId = "";

        if (cursor.moveToFirst()) {
            do {
                contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup._ID));
            } while (cursor.moveToNext());
        }

        cursor.close();
        cursor = null;
        return contactId;
    }

    /*
     * Returns contact's name
     */
    private String getContactName(String contactId, Context context) {

        String[] projection = new String[] { ContactsContract.Contacts.DISPLAY_NAME };
        Cursor cursor = context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, projection,
                ContactsContract.Contacts._ID + "=?", new String[] { contactId }, null);
        String name = "";
        if (cursor.moveToFirst()) {
            name = cursor.getString(0);
        }

        cursor.close();
        cursor = null;
        return name;
    }
}