package com.sonpd2.incomingcall;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiCaller {
    private static final String TAG = "ApiCaller";
    
    public interface ApiCallback {
        void onSuccess(String name, String position, String company, List<String> emails);
        void onError(String errorMessage);
        void onNotFound();
    }
    
    /**
     * Gọi API để tìm kiếm thông tin theo số điện thoại
     */
    public static void searchPhoneNumber(Context context, String phoneNumber, ApiCallback callback) {
        ApiConfigHelper configHelper = new ApiConfigHelper(context);
        
        String apiUrl = configHelper.getApiUrl();
        String apiKey = configHelper.getApiKey();
        String apiUser = configHelper.getApiUser();
        String cookie = configHelper.getCookie();
        
        // Cắt 9 số cuối để tìm kiếm
        String searchPhone = extractLast9Digits(phoneNumber);
        Log.d(TAG, "Searching for phone: " + searchPhone);
        
        // JSON data to send in the request
        String jsonData = "{\"org_paths\":[],\"text\":\""+ searchPhone +"\",\"page\":1,\"maxRecord\":20}";
        
        // Execute the AsyncTask to make the API POST request
        new ApiPostRequestTask(callback).execute(apiUrl, jsonData, apiKey, apiUser, cookie);
    }
    
    /**
     * Cắt 9 số cuối của số điện thoại để tìm kiếm
     */
    private static String extractLast9Digits(String phoneNumber) {
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
    
    private static class ApiPostRequestTask extends AsyncTask<String, Void, String> {
        private ApiCallback callback;
        
        public ApiPostRequestTask(ApiCallback callback) {
            this.callback = callback;
        }
        
        @Override
        protected String doInBackground(String... params) {
            String apiUrl = params[0];
            String jsonData = params[1];
            String apiKey = params[2];
            String apiUser = params[3];
            String cookie = params[4];

            OkHttpClient client = new OkHttpClient().newBuilder().build();

            MediaType mediaType = MediaType.get("application/json");
            RequestBody body = RequestBody.create(jsonData, mediaType);

            Request.Builder requestBuilder = new Request.Builder()
                    .url(apiUrl)
                    .method("POST", body)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:145.0) Gecko/20100101 Firefox/145.0")
                    .addHeader("Accept", "application/json")
                    .addHeader("Accept-Encoding", "gzip, deflate")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Referer", "https://vops.viettel.vn/contact/my-contact")
                    .addHeader("X-User-Id", apiUser)
                    .addHeader("X-Auth-Token", apiKey)
                    .addHeader("Origin", "https://vops.viettel.vn");
            
            if (cookie != null && !cookie.trim().isEmpty()) {
                requestBuilder.addHeader("Cookie", cookie);
            }
            
            Request request = requestBuilder.build();

            try {
                Response response = client.newCall(request).execute();
                int statusCode = response.code();
                
                String responseBody = null;
                if (response.body() != null) {
                    try {
                        byte[] bytes = response.body().bytes();
                        
                        // Kiểm tra magic number của gzip (1F 8B)
                        if (bytes.length >= 2 && bytes[0] == 0x1F && bytes[1] == (byte)0x8B) {
                            // Giải nén gzip
                            GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(bytes));
                            BufferedReader reader = new BufferedReader(new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8));
                            StringBuilder decompressed = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                decompressed.append(line);
                            }
                            reader.close();
                            responseBody = decompressed.toString();
                        } else {
                            responseBody = new String(bytes, StandardCharsets.UTF_8);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading response body: " + e.getMessage(), e);
                        responseBody = "Error reading response: " + e.getMessage();
                    }
                } else {
                    responseBody = "No response body";
                }
                
                if (response.isSuccessful()) {
                    return responseBody;
                } else {
                    String errorMessage = "HTTP " + statusCode + " " + response.message();
                    return "ERROR:" + statusCode + ":" + responseBody;
                }
            } catch (IOException e) {
                String errorMessage = "IOException: " + e.getMessage();
                Log.e(TAG, "Network Error - " + errorMessage, e);
                return "ERROR:0:" + errorMessage;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null || callback == null) {
                return;
            }
            
            // Kiểm tra nếu là lỗi
            if (result.startsWith("ERROR:")) {
                String[] parts = result.split(":", 3);
                String errorMessage = "Lỗi HTTP " + (parts.length > 1 ? parts[1] : "Unknown");
                if (parts.length > 2 && parts[2].length() < 100) {
                    errorMessage += ": " + parts[2];
                }
                callback.onError(errorMessage);
                return;
            }
            
            // Parse JSON response
            try {
                JSONObject jsonObject = new JSONObject(result);
                
                boolean success = jsonObject.optBoolean("success", false);
                if (!success) {
                    callback.onError("API trả về success: false");
                    return;
                }
                
                JSONArray contacts = jsonObject.getJSONArray("contacts");
                int totalUsers = jsonObject.optInt("totalUsers", 0);
                
                if (contacts.length() > 0) {
                    // Lấy contact đầu tiên
                    JSONObject contact = contacts.getJSONObject(0);
                    
                    String name = contact.optString("name", "");
                    String position = contact.optString("position", "");
                    String company = contact.optString("company", "");
                    
                    // Lấy emails
                    List<String> emails = new ArrayList<>();
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
                    
                    callback.onSuccess(name, position, company, emails);
                } else {
                    callback.onNotFound();
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing JSON: " + e.getMessage(), e);
                callback.onError("Lỗi parse JSON: " + e.getMessage());
            }
        }
    }
}

