package com.sonpd2.incomingcall;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class CallHistoryFragment extends Fragment {

    private CallHistoryHelper historyHelper;
    private RecyclerView recyclerView;
    private CallHistoryAdapter adapter;
    private TextView textEmpty;
    private MaterialButton btnClearHistory;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_call_history, container, false);

        historyHelper = new CallHistoryHelper(requireContext());

        recyclerView = view.findViewById(R.id.recyclerViewHistory);
        textEmpty = view.findViewById(R.id.textEmpty);
        btnClearHistory = view.findViewById(R.id.btnClearHistory);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CallHistoryAdapter();
        recyclerView.setAdapter(adapter);

        btnClearHistory.setOnClickListener(v -> {
            historyHelper.clearHistory();
            adapter.refresh();
            Toast.makeText(requireContext(), "Đã xóa lịch sử", Toast.LENGTH_SHORT).show();
        });

        loadHistory();

        return view;
    }

    private void loadHistory() {
        List<CallHistoryHelper.CallHistoryItem> history = historyHelper.getHistory();
        adapter.setHistory(history);
        textEmpty.setVisibility(history.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHistory();
    }

    private class CallHistoryAdapter extends RecyclerView.Adapter<CallHistoryAdapter.ViewHolder> {
        private List<CallHistoryHelper.CallHistoryItem> history;

        public void setHistory(List<CallHistoryHelper.CallHistoryItem> history) {
            this.history = history;
            notifyDataSetChanged();
        }

        public void refresh() {
            List<CallHistoryHelper.CallHistoryItem> newHistory = historyHelper.getHistory();
            setHistory(newHistory);
            textEmpty.setVisibility(newHistory.isEmpty() ? View.VISIBLE : View.GONE);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_call_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            CallHistoryHelper.CallHistoryItem item = history.get(position);
            holder.textPhoneNumber.setText(item.phoneNumber);
            holder.textName.setText(item.name.isEmpty() ? "Không có tên" : item.name);
            holder.textPosition.setText(item.position.isEmpty() ? "Không có chức vụ" : item.position);
            holder.textCompany.setText(item.company.isEmpty() ? "Không có công ty" : item.company);
            holder.textTimestamp.setText(item.timestamp);
            
            // Hiển thị emails
            if (item.emails != null && !item.emails.isEmpty()) {
                StringBuilder emailsText = new StringBuilder("Email: ");
                for (int i = 0; i < item.emails.size(); i++) {
                    if (i > 0) emailsText.append(", ");
                    emailsText.append(item.emails.get(i));
                }
                holder.textEmails.setText(emailsText.toString());
                holder.textEmails.setVisibility(View.VISIBLE);
            } else {
                holder.textEmails.setVisibility(View.GONE);
            }
            
            if (item.found) {
                holder.textStatus.setText("✓ Tìm thấy");
                holder.textStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else {
                holder.textStatus.setText("✗ Không tìm thấy");
                holder.textStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }
            
            // Kiểm tra và hiển thị nút thêm vào danh bạ
            checkAndShowAddContactButton(holder, item);
        }
        
        private void checkAndShowAddContactButton(ViewHolder holder, CallHistoryHelper.CallHistoryItem item) {
            // Kiểm tra quyền WRITE_CONTACTS
            boolean hasPermission = ContextCompat.checkSelfPermission(requireContext(), 
                    Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED;
            
            if (!hasPermission) {
                holder.btnAddContact.setVisibility(View.GONE);
                return;
            }
            
            // Kiểm tra xem số điện thoại đã có trong danh bạ chưa
            ContactHelper contactHelper = new ContactHelper(requireContext());
            boolean exists = contactHelper.isContactExists(item.phoneNumber);
            
            if (exists) {
                holder.btnAddContact.setVisibility(View.GONE);
            } else {
                holder.btnAddContact.setVisibility(View.VISIBLE);
                holder.btnAddContact.setOnClickListener(v -> {
                    // Thêm vào danh bạ
                    String name = item.name.isEmpty() ? item.phoneNumber : item.name;
                    boolean success = contactHelper.addContact(name, item.phoneNumber, item.company, item.position, item.emails);
                    
                    if (success) {
                        Toast.makeText(requireContext(), "Đã thêm vào danh bạ", Toast.LENGTH_SHORT).show();
                        holder.btnAddContact.setVisibility(View.GONE);
                        // Refresh để cập nhật trạng thái
                        notifyItemChanged(holder.getAdapterPosition());
                    } else {
                        Toast.makeText(requireContext(), "Lỗi khi thêm vào danh bạ", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return history != null ? history.size() : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textPhoneNumber;
            TextView textName;
            TextView textPosition;
            TextView textCompany;
            TextView textEmails;
            TextView textTimestamp;
            TextView textStatus;
            MaterialButton btnAddContact;

            ViewHolder(View itemView) {
                super(itemView);
                textPhoneNumber = itemView.findViewById(R.id.textPhoneNumber);
                textName = itemView.findViewById(R.id.textName);
                textPosition = itemView.findViewById(R.id.textPosition);
                textCompany = itemView.findViewById(R.id.textCompany);
                textEmails = itemView.findViewById(R.id.textEmails);
                textTimestamp = itemView.findViewById(R.id.textTimestamp);
                textStatus = itemView.findViewById(R.id.textStatus);
                btnAddContact = itemView.findViewById(R.id.btnAddContact);
            }
        }
    }
}

