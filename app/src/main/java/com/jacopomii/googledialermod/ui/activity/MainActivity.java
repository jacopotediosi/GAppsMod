package com.jacopomii.googledialermod.ui.activity;

import static com.jacopomii.googledialermod.data.Constants.DIALER_CALLRECORDINGPROMPT;
import static com.jacopomii.googledialermod.data.Constants.DIALER_PACKAGE_NAME;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.method.LinkMovementMethod;
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
import com.jacopomii.googledialermod.ICoreRootService;
import com.jacopomii.googledialermod.ui.adapter.MainViewPagerAdapter;
import com.jacopomii.googledialermod.R;
import com.jacopomii.googledialermod.service.CoreRootService;
import com.topjohnwu.superuser.ipc.RootService;
import com.topjohnwu.superuser.nio.ExtendedFile;
import com.topjohnwu.superuser.nio.FileSystemManager;

public class MainActivity extends AppCompatActivity {
    private Toolbar mToolbar;
    private TabLayout mTabLayout;
    private ViewPager2 mViewPager;
    private MainViewPagerAdapter mViewPagerAdapter;
    private View mProgressBar;

    private boolean coreRootServiceBound = false;
    private ServiceConnection coreRootServiceConnection;
    private ICoreRootService coreRootServiceIpc;
    private FileSystemManager coreRootServiceFSManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mToolbar = findViewById(R.id.toolbar);
        mTabLayout = findViewById(R.id.tab_layout);
        mViewPager = findViewById(R.id.viewpager);
        mProgressBar = findViewById(R.id.progress_bar);

        setSupportActionBar(mToolbar);

        mViewPagerAdapter = new MainViewPagerAdapter(this);

        // Start CoreRootService connection
        Intent intent = new Intent(this, CoreRootService.class);
        coreRootServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                // Set references to the remote coreRootService
                coreRootServiceBound = true;
                coreRootServiceIpc = ICoreRootService.Stub.asInterface(service);
                try {
                    coreRootServiceFSManager = FileSystemManager.getRemote(coreRootServiceIpc.getFileSystemService());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                // Update UI
                mViewPager.setAdapter(mViewPagerAdapter);

                mProgressBar.setVisibility(View.GONE);

                mViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        RadioGroup radioGroupSearch = findViewById(R.id.radio_group_search);
                        radioGroupSearch.setVisibility(View.GONE);
                        radioGroupSearch.check(R.id.radiobutton_all);
                    }
                });

                new TabLayoutMediator(mTabLayout, mViewPager, (tab, position) -> tab.setText(mViewPagerAdapter.getItemTitle(position))).attach();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                coreRootServiceBound = false;
                coreRootServiceIpc = null;
                coreRootServiceFSManager = null;
            }
        };
        RootService.bind(intent, coreRootServiceConnection);
    }

    public FileSystemManager getCoreRootServiceFSManager() {
        return coreRootServiceFSManager;
    }

    public ICoreRootService getCoreRootServiceIpc() {
        return coreRootServiceIpc;
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
                        // Delete all flag overrides from Phenotype DB
                        try {
                            coreRootServiceIpc.phenotypeDBDeleteAllFlagOverrides(DIALER_PACKAGE_NAME);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }

                        // Delete the Dialer callrecordingprompt folder
                        ExtendedFile callRecordingPromptFolder = coreRootServiceFSManager.getFile(DIALER_CALLRECORDINGPROMPT);
                        if (callRecordingPromptFolder.exists()) {
                            //noinspection ResultOfMethodCallIgnored
                            callRecordingPromptFolder.delete();
                        }

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

    @Override
    protected void onDestroy() {
        if (coreRootServiceBound)
            RootService.unbind(coreRootServiceConnection);
        super.onDestroy();
    }
}