package com.jacopomii.googledialermod;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class ViewPagerAdapter extends FragmentPagerAdapter {

    private final List<Fragment> mLstFragment = new ArrayList<>();
    private final List<String> mLstTitles = new ArrayList<>();


    public ViewPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public int getCount() {
        return mLstFragment.size();
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return mLstFragment.get(position);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mLstTitles.get(position);
    }

    public void AddFragment(Fragment fragment, String title) {
        mLstFragment.add(fragment);
        mLstTitles.add(title);
    }
}