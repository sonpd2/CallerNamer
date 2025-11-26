package com.zobaer53.incomingcall;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ContactHelper {
    private static final String TAG = "ContactHelper";
    private ContentResolver contentResolver;

    public ContactHelper(Context context) {
        this.contentResolver = context.getContentResolver();
    }

    /**
     * Kiểm tra xem số điện thoại đã có trong danh bạ chưa
     */
    public boolean isContactExists(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return false;
        }

        // Chuẩn hóa số điện thoại (loại bỏ ký tự không phải số)
        String normalizedPhone = phoneNumber.replaceAll("[^0-9]", "");

        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(normalizedPhone));
        String[] projection = new String[]{ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.DISPLAY_NAME};

        try (Cursor cursor = contentResolver.query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking contact: " + e.getMessage());
        }
        return false;
    }

    /**
     * Thêm contact vào danh bạ
     */
    public boolean addContact(String name, String phoneNumber, String company, String position, List<String> emails) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return false;
        }

        // Kiểm tra xem đã có trong danh bạ chưa
        if (isContactExists(phoneNumber)) {
            Log.d(TAG, "Contact already exists: " + phoneNumber);
            return false;
        }

        ArrayList<ContentProviderOperation> operations = new ArrayList<>();

        // Tạo raw contact mới
        int rawContactInsertIndex = operations.size();
        operations.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build());

        // Thêm tên
        if (name != null && !name.isEmpty()) {
            operations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                    .build());
        }

        // Thêm số điện thoại
        operations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build());

        // Thêm công ty (Organization)
        if (company != null && !company.isEmpty()) {
            operations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, company)
                    .withValue(ContactsContract.CommonDataKinds.Organization.TITLE, position != null ? position : "")
                    .withValue(ContactsContract.CommonDataKinds.Organization.TYPE, ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
                    .build());
        }

        // Thêm emails
        if (emails != null && !emails.isEmpty()) {
            for (String email : emails) {
                if (email != null && !email.isEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    operations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Email.DATA, email)
                            .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                            .build());
                }
            }
        }

        try {
            contentResolver.applyBatch(ContactsContract.AUTHORITY, operations);
            Log.d(TAG, "Contact added successfully: " + name + " - " + phoneNumber);
            return true;
        } catch (RemoteException | OperationApplicationException e) {
            Log.e(TAG, "Error adding contact: " + e.getMessage());
            return false;
        }
    }
}

