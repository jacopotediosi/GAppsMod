package com.jacopomii.googledialermod;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.jacopomii.googledialermod.Utils.checkIsDialerInstalled;
import static com.jacopomii.googledialermod.Utils.checkIsLatestGithubVersion;
import static com.jacopomii.googledialermod.Utils.checkIsPhenotypeDBInstalled;
import static com.jacopomii.googledialermod.Utils.copyFile;
import static com.jacopomii.googledialermod.Utils.revertAllMods;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.topjohnwu.superuser.Shell;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private Toolbar mToolbar;
    private TabLayout mTabLayout;
    private ViewPager2 mViewPager;
    private ViewPagerAdapter mViewPagerAdapter;

    static {
        // Set Libsu settings before the main shell can be created
        Shell.enableVerboseLogging = true;
        Shell.setDefaultBuilder(Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER)
                .setTimeout(10)
        );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Shell.getShell().isRoot()) {
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
                alert.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view ->
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_link)))));

                alert.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(view -> {
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

            alert.setOnShowListener(dialogInterface -> alert.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(
                    view ->
                            startActivity(
                                    new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_link)))
                            )
                    )
            );

            alert.show();
        } else {
            copyAssets();

            checkIsLatestGithubVersion(this);

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_info_icon) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setPositiveButton(android.R.string.ok, null)
                    .setView(R.layout.information_dialog);
            AlertDialog alert = builder.create();
            alert.show();

            // Links aren't clickable workaround
            TextView whatIsItExplanation = alert.findViewById(R.id.what_is_it_explanation);
            if (whatIsItExplanation != null)
                whatIsItExplanation.setMovementMethod(LinkMovementMethod.getInstance());
            TextView madeWithLove = alert.findViewById(R.id.made_with_love_by_jacopo_tediosi);
            if (madeWithLove != null)
                madeWithLove.setMovementMethod(LinkMovementMethod.getInstance());

            return true;
        } else if (item.getItemId() == R.id.menu_delete_icon) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.delete_all_mods_alert)
                    .setNegativeButton(getString(android.R.string.cancel), (dialog, which) -> {
                    })
                    .setPositiveButton(getString(android.R.string.ok), (dialog, which) -> {
                        revertAllMods(this);
                        // Restart the activity to be sure UI is updated
                        finish();
                        startActivity(getIntent());
                    });
            AlertDialog alert = builder.create();
            alert.show();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
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

            Shell.cmd("chmod 755 " + dataDir + "/sqlite3").exec();
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