package com.lock.stockit.Adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.lock.stockit.HomeFragment;
import com.lock.stockit.InventoryFragment;
import com.lock.stockit.LoaderActivity;
import com.lock.stockit.MoreFragment;
import com.lock.stockit.ReceiptFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull @Override
    public Fragment createFragment(int position) {
        if (LoaderActivity.admin) {
            switch (position) {
                case 1: return new ReceiptFragment();
                case 2: return new InventoryFragment();
                case 3: return new MoreFragment();
                case 0:
                default: return new HomeFragment();
            }
        } else {
            switch (position) {
                case 1: return new ReceiptFragment();
                case 2: return new MoreFragment();
                case 0:
                default: return new HomeFragment();
            }
        }
    }

    @Override
    public int getItemCount() {
        if (LoaderActivity.admin) return 4;
        else return 3;
    }
}
