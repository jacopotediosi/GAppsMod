package com.jacopomii.googledialermod;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 1)
            return new AllSwitchesFragment();
        return new SuggestedModsFragment();
    }
}