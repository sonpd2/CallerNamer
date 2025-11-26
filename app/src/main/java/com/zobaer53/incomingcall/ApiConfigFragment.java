package com.zobaer53.incomingcall;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ApiConfigFragment extends Fragment {

    private ApiConfigHelper configHelper;
    private TextInputEditText editApiUrl;
    private TextInputEditText editApiKey;
    private TextInputEditText editApiUser;
    private TextInputEditText editCookie;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_api_config, container, false);

        configHelper = new ApiConfigHelper(requireContext());

        editApiUrl = view.findViewById(R.id.editApiUrl);
        editApiKey = view.findViewById(R.id.editApiKey);
        editApiUser = view.findViewById(R.id.editApiUser);
        editCookie = view.findViewById(R.id.editCookie);

        MaterialButton btnSave = view.findViewById(R.id.btnSave);
        MaterialButton btnReset = view.findViewById(R.id.btnReset);

        loadCurrentValues();

        btnSave.setOnClickListener(v -> {
            saveConfig();
            Toast.makeText(requireContext(), "Đã lưu cấu hình", Toast.LENGTH_SHORT).show();
        });

        btnReset.setOnClickListener(v -> {
            configHelper.resetToDefaults();
            loadCurrentValues();
            Toast.makeText(requireContext(), "Đã đặt lại mặc định", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    private void loadCurrentValues() {
        editApiUrl.setText(configHelper.getApiUrl());
        editApiKey.setText(configHelper.getApiKey());
        editApiUser.setText(configHelper.getApiUser());
        editCookie.setText(configHelper.getCookie());
    }

    private void saveConfig() {
        String apiUrl = editApiUrl.getText() != null ? editApiUrl.getText().toString().trim() : "";
        String apiKey = editApiKey.getText() != null ? editApiKey.getText().toString().trim() : "";
        String apiUser = editApiUser.getText() != null ? editApiUser.getText().toString().trim() : "";
        String cookie = editCookie.getText() != null ? editCookie.getText().toString().trim() : "";

        if (apiUrl.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng nhập API URL", Toast.LENGTH_SHORT).show();
            return;
        }

        configHelper.setApiUrl(apiUrl);
        configHelper.setApiKey(apiKey);
        configHelper.setApiUser(apiUser);
        configHelper.setCookie(cookie);
    }
}

