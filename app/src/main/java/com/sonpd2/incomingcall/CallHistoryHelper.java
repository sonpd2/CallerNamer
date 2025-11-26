package com.sonpd2.incomingcall;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CallHistoryHelper {
    private static final String PREFS_NAME = "call_history";
    private static final String KEY_HISTORY = "history";
    private static final int MAX_HISTORY_SIZE = 50; // Giới hạn 50 bản ghi

    private SharedPreferences prefs;

    public CallHistoryHelper(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static class CallHistoryItem {
        public String phoneNumber;
        public String name;
        public String position;
        public String company;
        public List<String> emails;
        public String timestamp;
        public boolean found;

        public CallHistoryItem(String phoneNumber, String name, String position, String company, List<String> emails, boolean found) {
            this.phoneNumber = phoneNumber;
            this.name = name;
            this.position = position;
            this.company = company;
            this.emails = emails != null ? emails : new ArrayList<>();
            this.found = found;
            this.timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        }
    }

    public void addHistory(String phoneNumber, String name, String position, String company, List<String> emails, boolean found) {
        try {
            JSONArray historyArray = getHistoryArray();
            
            // Tạo item mới
            JSONObject newItem = new JSONObject();
            newItem.put("phoneNumber", phoneNumber);
            newItem.put("name", name != null ? name : "");
            newItem.put("position", position != null ? position : "");
            newItem.put("company", company != null ? company : "");
            newItem.put("found", found);
            newItem.put("timestamp", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date()));
            
            // Thêm emails array
            JSONArray emailsArray = new JSONArray();
            if (emails != null) {
                for (String email : emails) {
                    if (email != null && !email.isEmpty()) {
                        emailsArray.put(email);
                    }
                }
            }
            newItem.put("emails", emailsArray);
            
            // Thêm vào đầu mảng
            JSONArray newArray = new JSONArray();
            newArray.put(newItem);
            
            // Giữ tối đa MAX_HISTORY_SIZE items
            for (int i = 0; i < historyArray.length() && i < MAX_HISTORY_SIZE - 1; i++) {
                newArray.put(historyArray.getJSONObject(i));
            }
            
            prefs.edit().putString(KEY_HISTORY, newArray.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public List<CallHistoryItem> getHistory() {
        List<CallHistoryItem> history = new ArrayList<>();
        try {
            JSONArray historyArray = getHistoryArray();
            for (int i = 0; i < historyArray.length(); i++) {
                JSONObject item = historyArray.getJSONObject(i);
                
                // Đọc emails array
                List<String> emails = new ArrayList<>();
                if (item.has("emails")) {
                    JSONArray emailsArray = item.getJSONArray("emails");
                    for (int j = 0; j < emailsArray.length(); j++) {
                        emails.add(emailsArray.getString(j));
                    }
                }
                
                CallHistoryItem historyItem = new CallHistoryItem(
                    item.getString("phoneNumber"),
                    item.optString("name", ""),
                    item.optString("position", ""),
                    item.optString("company", ""),
                    emails,
                    item.optBoolean("found", false)
                );
                historyItem.timestamp = item.optString("timestamp", "");
                history.add(historyItem);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return history;
    }

    private JSONArray getHistoryArray() {
        String historyJson = prefs.getString(KEY_HISTORY, "[]");
        try {
            return new JSONArray(historyJson);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    public void clearHistory() {
        prefs.edit().putString(KEY_HISTORY, "[]").apply();
    }

    /**
     * Tìm kiếm thông tin trong lịch sử theo số điện thoại
     * Trả về null nếu không tìm thấy
     */
    public CallHistoryItem findInHistory(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return null;
        }

        // Chuẩn hóa số điện thoại (lấy 9 số cuối)
        String normalizedPhone = phoneNumber.replaceAll("[^0-9]", "");
        if (normalizedPhone.length() >= 9) {
            normalizedPhone = normalizedPhone.substring(normalizedPhone.length() - 9);
        }

        try {
            JSONArray historyArray = getHistoryArray();
            for (int i = 0; i < historyArray.length(); i++) {
                JSONObject item = historyArray.getJSONObject(i);
                String itemPhone = item.getString("phoneNumber");
                
                // Chuẩn hóa số điện thoại trong lịch sử
                String normalizedItemPhone = itemPhone.replaceAll("[^0-9]", "");
                if (normalizedItemPhone.length() >= 9) {
                    normalizedItemPhone = normalizedItemPhone.substring(normalizedItemPhone.length() - 9);
                }
                
                // So sánh 9 số cuối
                if (normalizedPhone.equals(normalizedItemPhone)) {
                    // Đọc emails array
                    List<String> emails = new ArrayList<>();
                    if (item.has("emails")) {
                        JSONArray emailsArray = item.getJSONArray("emails");
                        for (int j = 0; j < emailsArray.length(); j++) {
                            emails.add(emailsArray.getString(j));
                        }
                    }
                    
                    CallHistoryItem historyItem = new CallHistoryItem(
                        item.getString("phoneNumber"),
                        item.optString("name", ""),
                        item.optString("position", ""),
                        item.optString("company", ""),
                        emails,
                        item.optBoolean("found", false)
                    );
                    historyItem.timestamp = item.optString("timestamp", "");
                    return historyItem;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}

