package com.jacopomii.googledialermod;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MainViewPagerAdapter extends FragmentStateAdapter {
    private final FragmentActivity fragmentActivity;

    public MainViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        this.fragmentActivity = fragmentActivity;
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    public String getItemTitle(int position) {
        if (position == 1) {
            return fragmentActivity.getString(R.string.boolean_mods);
        } else {
            return fragmentActivity.getString(R.string.suggested_mods);
        }
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 1)
            return new BooleanModsFragment();
        return new SuggestedModsFragment();
    }
}