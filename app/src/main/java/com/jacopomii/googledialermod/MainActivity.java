package com.jacopomii.googledialermod;

import static com.jacopomii.googledialermod.Utils.checkIsDeviceRooted;
import static com.jacopomii.googledialermod.Utils.checkIsDialerInstalled;
import static com.jacopomii.googledialermod.Utils.checkIsPhenotypeDBInstalled;
import static com.jacopomii.googledialermod.Utils.copyFile;
import static com.jacopomii.googledialermod.Utils.runSuWithCmd;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

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
    private ViewPager2 mViewPager;
    private ViewPagerAdapter mViewPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsDeviceRooted()) {
            Log.e(TAG, "onCreate: cannot obtain root permissions");

            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setMessage(R.string.root_access_denied)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> finishAffinity());

            AlertDialog alert = builder.create();
            alert.show();
        } else if (!checkIsDialerInstalled(this)) {
            Log.e(TAG, "onCreate: dialer app not installed");

            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setMessage(getString(R.string.dialer_not_installed_error))
                    .setCancelable(false)
                    .setNeutralButton(R.string.github, null)
                    .setNegativeButton(R.string.play_store, null)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> finishAffinity());

            AlertDialog alert = builder.create();

            alert.setOnShowListener(dialogInterface -> {
                ((AlertDialog) alert).getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener((View.OnClickListener) view ->
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_link)))));

                ((AlertDialog) alert).getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener((View.OnClickListener) view -> {
                    try {
                        Intent appStoreIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.dialer"));
                        appStoreIntent.setPackage("com.android.vending");
                        startActivity(appStoreIntent);
                    } catch (ActivityNotFoundException exception) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.dialer")));
                    }
                });
            });

            alert.show();
        } else if (!checkIsPhenotypeDBInstalled()) {
            Log.e(TAG, "onCreate: phenotype db not exists ");

            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setMessage(getString(R.string.phenotype_db_doesnt_exist_error))
                    .setCancelable(false)
                    .setNeutralButton(R.string.github, null)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> finishAffinity());

            AlertDialog alert = builder.create();

            alert.setOnShowListener(dialogInterface -> ((AlertDialog) alert).getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(
                    (View.OnClickListener) view ->
                            startActivity(
                                    new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_link)))
                            )
                    )
            );

            alert.show();
        } else {
            copyAssets();

            setContentView(R.layout.activity_main);

            mToolbar = findViewById(R.id.toolbar);
            mTabLayout = findViewById(R.id.tablayout);
            mViewPager = findViewById(R.id.viewpager);

            setSupportActionBar(mToolbar);

            mViewPagerAdapter = new ViewPagerAdapter(this);

            mViewPager.setAdapter(mViewPagerAdapter);

            mViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    RadioGroup radioGroupSearch = findViewById(R.id.radiogroup_search);
                    radioGroupSearch.setVisibility(View.GONE);
                    radioGroupSearch.check(R.id.radiobutton_all);

                    switch (position) {
                        case 0:
                            SuggestedModsFragment suggestedModsFragment = (SuggestedModsFragment) getSupportFragmentManager().findFragmentByTag("f" + position);
                            if (suggestedModsFragment != null)
                                suggestedModsFragment.refreshSwitchesStatus();
                            break;
                        case 1:
                            AllSwitchesFragment allSwitchesFragment = (AllSwitchesFragment)getSupportFragmentManager().findFragmentByTag("f" + position);
                            if (allSwitchesFragment != null)
                                allSwitchesFragment.refreshAdapter();
                            break;
                    }
                }
            });

            new TabLayoutMediator(mTabLayout, mViewPager, (tab, position) -> {
                if (position == 1) {
                    tab.setText(R.string.all_switches);
                } else {
                    tab.setText(R.string.suggested_mods);
                }
            }).attach();
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