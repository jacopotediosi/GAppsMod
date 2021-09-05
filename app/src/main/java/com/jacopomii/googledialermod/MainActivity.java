package com.jacopomii.googledialermod;

import static com.jacopomii.googledialermod.Utils.checkIsDeviceRooted;
import static com.jacopomii.googledialermod.Utils.copyFile;
import static com.jacopomii.googledialermod.Utils.runSuWithCmd;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// TODO: dialog explaining how it works (restart the app a couple of times etc)
// TODO: dialog to remember to kill app a couple of times when exiting
// TODO: Magisk module as https://forum.xda-developers.com/t/module-detach3-detach-market-links.3447494/

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private Toolbar mToolbar;
    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private ViewPagerAdapter mViewPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsDeviceRooted()) {
            Log.e(TAG, "onCreate: cannot obtain root permissions");

            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setMessage(R.string.root_access_denied)
                    .setCancelable(false)
                    .setPositiveButton(getString(android.R.string.ok), (dialog, which) -> finishAffinity());

            AlertDialog alert = builder.create();
            alert.show();
        } else {
            copyAssets();

            setContentView(R.layout.activity_main);

            mToolbar = findViewById(R.id.toolbar);
            mTabLayout = findViewById(R.id.tablayout);
            mViewPager = findViewById(R.id.viewpager);

            setSupportActionBar(mToolbar);

            mViewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());

            SuggestedModsFragment suggestedModsFragment = new SuggestedModsFragment();
            AllSwitchesFragment allSwitchesFragment = new AllSwitchesFragment();

            mViewPagerAdapter.AddFragment(suggestedModsFragment, getString(R.string.suggested_mods));
            mViewPagerAdapter.AddFragment(allSwitchesFragment, getString(R.string.all_switches));

            mViewPager.setAdapter(mViewPagerAdapter);

            mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

                @Override
                public void onPageScrollStateChanged(int state) {}

                @Override
                public void onPageSelected(int position) {
                    RadioGroup radioGroupSearch = findViewById(R.id.radiogroup_search);
                    radioGroupSearch.setVisibility(View.GONE);
                    radioGroupSearch.check(R.id.radiobutton_all);

                    switch (position) {
                        case 0:
                            suggestedModsFragment.refreshSwitchesStatus();
                            break;
                        case 1:
                            allSwitchesFragment.refreshAdapter();
                    }
                }
            });

            mTabLayout.setupWithViewPager(mViewPager);
        }
    }

    private void copyAssets() {
        final String dataDir = getApplicationInfo().dataDir;
        InputStream inputStream = null;
        OutputStream outputStream;
        File outputFile;

        for (String supportedAbi : Build.SUPPORTED_ABIS) {
            if (supportedAbi.contains("arm")) {
                inputStream = getResources().openRawResource(R.raw.sqlite3_arm);
                break;
            } else if (supportedAbi.contains("x86")) {
                inputStream = getResources().openRawResource(R.raw.sqlite3_x86);
                break;
            }
        }

        if(inputStream != null) {
            Log.v(TAG, "copyAssets: copying sqlite3 to data directory");
            try {
                outputFile = new File(dataDir, "sqlite3");
                outputStream = new FileOutputStream(outputFile);
                copyFile(inputStream, outputStream);
            } catch (IOException e) {
                Log.e(TAG, "copyAssets: failed to copy asset file: sqlite3", e);
            }

            runSuWithCmd("chmod 755 " + dataDir + "/sqlite3");

            outputFile = new File(dataDir, "silent_wav.wav");
            if (!outputFile.exists()) {
                Log.v(TAG, "copyAssets: copying silent_wav to data directory");
                try {
                    inputStream = getResources().openRawResource(R.raw.silent_wav);
                    outputStream = new FileOutputStream(outputFile);
                    copyFile(inputStream, outputStream);
                } catch (IOException e) {
                    Log.e(TAG, "copyAssets: failed to copy asset file: silent_wav", e);
                }
            }
        } else {
            Log.e(TAG, "copyAssets: CPU arch not supported");

            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setMessage(R.string.cpu_arch_not_supported)
                    .setCancelable(false)
                    .setPositiveButton(getString(android.R.string.ok), (dialog, which) -> finishAffinity());

            AlertDialog alert = builder.create();
            alert.show();
        }
    }
}