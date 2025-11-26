package com.zobaer53.incomingcall;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ConfigPagerAdapter extends FragmentStateAdapter {

    public ConfigPagerAdapter(FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new ApiConfigFragment();
            case 1:
                return new CallHistoryFragment();
            default:
                return new ApiConfigFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}

