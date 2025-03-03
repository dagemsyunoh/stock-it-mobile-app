package com.lock.stockit;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (LoaderActivity.admin) {
            switch (position) {
                case 1: return new Receipt();
                case 2: return new Inventory();
                case 3: return new More();
                case 0:
                default: return new Home();
            }
        }
        else {
            switch (position) {
                case 1: return new Receipt();
                case 2: return new More();
                case 0:
                default: return new Home();
            }
        }
    }

    @Override
    public int getItemCount() {
        if (LoaderActivity.admin) {
            return 4;
        }
        else {
            return 3;
        }
    }
}
