package com.jacopomii.googledialermod.ui.activity;

import static com.jacopomii.googledialermod.data.Constants.GMS_ANDROID_PACKAGE_NAME;
import static com.jacopomii.googledialermod.data.Constants.GOOGLE_PLAY_DETAILS_LINK;
import static com.jacopomii.googledialermod.data.Constants.PHENOTYPE_DB;
import static com.jacopomii.googledialermod.util.Utils.checkUpdateAvailable;
import static com.jacopomii.googledialermod.util.Utils.openGooglePlay;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.jacopomii.googledialermod.BuildConfig;
import com.jacopomii.googledialermod.ICoreRootService;
import com.jacopomii.googledialermod.R;
import com.jacopomii.googledialermod.service.CoreRootService;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ipc.RootService;
import com.topjohnwu.superuser.nio.FileSystemManager;

import java.util.concurrent.CountDownLatch;

@SuppressLint("CustomSplashScreen")
public class SplashScreenActivity extends AppCompatActivity {
    static {
        // Set Libsu settings before creating the main shell
        Shell.enableVerboseLogging = BuildConfig.DEBUG;
        Shell.setDefaultBuilder(Shell.Builder.create().setFlags(Shell.FLAG_MOUNT_MASTER));
    }

    private final CountDownLatch mRootCheckPassed = new CountDownLatch(1);
    private final CountDownLatch mCoreRootServiceConnected = new CountDownLatch(1);
    private final CountDownLatch mPhenotypeCheckPassed = new CountDownLatch(1);
    private final CountDownLatch mUpdateCheckFinished = new CountDownLatch(1);

    private boolean mCoreRootServiceBound = false;
    private ServiceConnection mCoreRootServiceConnection;
    private FileSystemManager mCoreRootServiceFSManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        // Start CoreRootService connection
        Intent intent = new Intent(this, CoreRootService.class);
        mCoreRootServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                // Set references to the remote coreRootService
                mCoreRootServiceBound = true;
                ICoreRootService ipc = ICoreRootService.Stub.asInterface(service);
                try {
                    mCoreRootServiceFSManager = FileSystemManager.getRemote(ipc.getFileSystemService());
                    mCoreRootServiceConnected.countDown();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                // Update the UI
                setCheckUIDone(R.id.circular_root_service, R.id.done_root_service, mCoreRootServiceConnected.getCount() == 0);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mCoreRootServiceBound = false;
                mCoreRootServiceFSManager = null;
            }
        };
        RootService.bind(intent, mCoreRootServiceConnection);

        // Root permission check
        new Thread() {
            @Override
            public void run() {
                // Check root
                if (checkRoot()) {
                    mRootCheckPassed.countDown();
                } else {
                    runOnUiThread(() ->
                            new MaterialAlertDialogBuilder(SplashScreenActivity.this)
                                    .setCancelable(false)
                                    .setMessage(R.string.root_access_denied)
                                    .setPositiveButton(R.string.exit, (dialog, i) -> System.exit(0))
                                    .show());
                }

                // Update the UI
                setCheckUIDone(R.id.circular_root, R.id.done_root, mRootCheckPassed.getCount() == 0);
            }
        }.start();

        // Phenotype DB check
        new Thread() {
            @Override
            public void run() {
                try {
                    // Wait for root check to pass
                    mRootCheckPassed.await();
                    // Wait for coreRootService to connect
                    mCoreRootServiceConnected.await();

                    // Check the Phenotype DB
                    if (checkPhenotypeDB()) {
                        mPhenotypeCheckPassed.countDown();
                    } else {
                        runOnUiThread(() ->
                                new MaterialAlertDialogBuilder(SplashScreenActivity.this)
                                        .setCancelable(false)
                                        .setMessage(getString(R.string.phenotype_db_does_not_exist))
                                        .setNegativeButton(R.string.install, (dialogInterface, i) -> openGooglePlay(SplashScreenActivity.this, GOOGLE_PLAY_DETAILS_LINK + GMS_ANDROID_PACKAGE_NAME))
                                        .setPositiveButton(R.string.exit, (dialog, which) -> System.exit(0))
                                        .show());
                    }

                    // Update the UI
                    setCheckUIDone(R.id.circular_phenotype, R.id.done_phenotype, mPhenotypeCheckPassed.getCount() == 0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        // Update available check
        new Thread() {
            @Override
            public void run() {
                // Check if updates are available
                if (!checkUpdateAvailable(SplashScreenActivity.this)) {
                    mUpdateCheckFinished.countDown();
                } else {
                    runOnUiThread(() ->
                            new MaterialAlertDialogBuilder(SplashScreenActivity.this)
                                    .setCancelable(false)
                                    .setMessage(R.string.new_version_alert)
                                    .setNegativeButton(
                                            R.string.github,
                                            (dialogInterface, i) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_link) + "/releases")))
                                    )
                                    .setPositiveButton(R.string.continue_anyway, (dialogInterface, i) -> mUpdateCheckFinished.countDown())
                                    .show());
                }

                // Update the UI
                setCheckUIDone(R.id.circular_updates, R.id.done_updates, mUpdateCheckFinished.getCount() == 0);
            }
        }.start();

        // End splash screen and go to the main activity
        new Thread() {
            @Override
            public void run() {
                try {
                    // Wait for all checks to pass and for all operations to finish
                    mRootCheckPassed.await();
                    mCoreRootServiceConnected.await();
                    mPhenotypeCheckPassed.await();
                    mUpdateCheckFinished.await();

                    // This is just for aesthetics: I don't want the splashscreen to be too fast
                    Thread.sleep(1000);

                    // Start the main activity
                    Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private boolean checkRoot() {
        return Shell.getShell().isRoot();
    }

    private boolean checkPhenotypeDB() {
        return mCoreRootServiceFSManager.getFile(PHENOTYPE_DB).exists();
    }

    private void setCheckUIDone(int circularID, int doneImageID, boolean success) {
        CircularProgressIndicator circular = findViewById(circularID);
        ImageView doneImage = findViewById(doneImageID);
        runOnUiThread(() -> {
            circular.setVisibility(View.GONE);
            doneImage.setImageResource(success ? R.drawable.ic_success_24 : R.drawable.ic_fail_24);
            doneImage.setVisibility(View.VISIBLE);
        });
    }

    @Override
    protected void onDestroy() {
        if (mCoreRootServiceBound)
            RootService.unbind(mCoreRootServiceConnection);
        super.onDestroy();
    }
}