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
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

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

    // Replace with your API endpoint and API key
    String apiUrl = "https://vops.viettel.vn/api/v1/contacts.search-by-org";
    String apiKey = "Z8BXBjCoFMbppDNLkdjdVidTSAQwpV2h-Ff_EpNC1f-";
    String apiUser = "jE5TLGK33qi7GR3nW";


    @Override
    public void onReceive(Context context, Intent intent) {
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

            OkHttpClient client = new OkHttpClient();

            MediaType JSON = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(jsonData, JSON);

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("X-Auth-Token", apiKey)
                    .addHeader("X-User-Id", apiUser)
                    .post(body)
                    .build();

            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    return response.body().string();
                } else {
                    return "Error: " + response.code() + " " + response.message();
                }
            } catch (IOException e) {
                e.printStackTrace();
                return "Error: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                // Process the API response here
                //resultTextView.setText(result);
                Log.d(TAG, "Response: " + result);
                parseJson(result);
            } else {
                //resultTextView.setText("Error fetching data from the API.");
                Log.d(TAG, "Response: " + "Error fetching data from the API.");
            }
        }
    }

    private void parseJson(String response){
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray results = jsonObject.getJSONArray("contacts");
            for (int i = 0; i < results.length(); i++) {
                JSONObject jo = results.getJSONObject(i);
                name = jo.getString("name");
                position = jo.getString("position");
                company = jo.getString("company");
                Log.d(TAG, "Hi: " + name  +" "+ position +" "+ company);
                TextView nameTextView = windowLayout.findViewById(R.id.name);
                TextView posTextView = windowLayout.findViewById(R.id.position);
                TextView compTextView = windowLayout.findViewById(R.id.company);
                nameTextView.setText(name);
                posTextView.setText(position);
                compTextView.setText(company);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void showWindow(final Context context, String phone) {

        if(phone!= null) {
            // JSON data to send in the request
            String jsonData = "{\"org_paths\":[],\"text\":\""+ phone +"\",\"page\":1}";
            // Execute the AsyncTask to make the API POST request
            new ApiPostRequestTask().execute(apiUrl, jsonData, apiKey, apiUser);
        }else
            name = "Unknown";

        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowLayout = (ViewGroup) View.inflate(context, R.layout.window_call_info, null);
        getLayoutParams();
        setOnTouchListener();
        TextView numberTextView = windowLayout.findViewById(R.id.number);
        numberTextView.setText(phone);
        Button cancelButton = windowLayout.findViewById(R.id.cancel);
        cancelButton.setOnClickListener(view -> closeWindow());
        windowManager.addView(windowLayout, params);
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
