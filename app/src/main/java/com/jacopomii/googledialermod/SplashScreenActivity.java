package com.jacopomii.googledialermod;

import static com.jacopomii.googledialermod.Constants.DIALER_DATA_DATA;
import static com.jacopomii.googledialermod.Constants.DIALER_GOOGLE_PLAY_BETA_LINK;
import static com.jacopomii.googledialermod.Constants.DIALER_GOOGLE_PLAY_LINK;
import static com.jacopomii.googledialermod.Constants.DIALER_PACKAGE_NAME;
import static com.jacopomii.googledialermod.Constants.GMS_GOOGLE_PLAY_LINK;
import static com.jacopomii.googledialermod.Constants.PHENOTYPE_DB;
import static com.jacopomii.googledialermod.Utils.checkUpdateAvailable;
import static com.jacopomii.googledialermod.Utils.openGooglePlay;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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

    private final SplashScreenActivity splashScreenActivity = this;

    private final CountDownLatch rootCheckPassed = new CountDownLatch(1);
    private final CountDownLatch coreRootServiceConnected = new CountDownLatch(1);
    private final CountDownLatch dialerCheckPassed = new CountDownLatch(1);
    private final CountDownLatch phenotypeCheckPassed = new CountDownLatch(1);
    private final CountDownLatch updateCheckFinished = new CountDownLatch(1);

    private boolean coreRootServiceBound = false;
    private ServiceConnection coreRootServiceConnection;
    private FileSystemManager coreRootServiceFSManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        // Start CoreRootService connection
        Intent intent = new Intent(this, CoreRootService.class);
        coreRootServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                // Set references to the remote coreRootService
                coreRootServiceBound = true;
                ICoreRootService ipc = ICoreRootService.Stub.asInterface(service);
                try {
                    coreRootServiceFSManager = FileSystemManager.getRemote(ipc.getFileSystemService());
                    coreRootServiceConnected.countDown();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                // Update the UI
                setCheckUIDone(R.id.circular_root_service, R.id.done_root_service, coreRootServiceConnected.getCount()==0);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                coreRootServiceBound = false;
                coreRootServiceFSManager = null;
            }
        };
        RootService.bind(intent, coreRootServiceConnection);

        // Root permission check
        new Thread() {
            @Override
            public void run() {
                // Check root
                if (checkRoot()) {
                    rootCheckPassed.countDown();
                } else {
                    runOnUiThread(() ->
                            new AlertDialog.Builder(splashScreenActivity)
                                .setCancelable(false)
                                .setMessage(R.string.root_access_denied)
                                .setPositiveButton(R.string.exit, (dialog, i) -> System.exit(0))
                                .show());
                }

                // Update the UI
                setCheckUIDone(R.id.circular_root, R.id.done_root, rootCheckPassed.getCount()==0);
            }
        }.start();

        // Dialer installation check
        new Thread() {
            @Override
            public void run() {
                try {
                    // Wait for root check to pass
                    rootCheckPassed.await();
                    // Wait for coreRootService to connect
                    coreRootServiceConnected.await();

                    // Check the Dialer installation
                    if (checkDialerInstallation()) {
                        dialerCheckPassed.countDown();
                    } else {
                        runOnUiThread(() ->
                                new AlertDialog.Builder(splashScreenActivity)
                                    .setCancelable(false)
                                    .setMessage(getString(R.string.dialer_not_installed_error))
                                    .setPositiveButton(R.string.install_from_google_play, (dialogInterface, i) -> openGooglePlay(splashScreenActivity, DIALER_GOOGLE_PLAY_LINK))
                                    .setNegativeButton(R.string.join_beta_program, (dialogInterface, i) -> openGooglePlay(splashScreenActivity, DIALER_GOOGLE_PLAY_BETA_LINK))
                                    .setNeutralButton(R.string.continue_anyway, (dialogInterface, i) -> dialerCheckPassed.countDown())
                                    .show());
                    }

                    // Update the UI
                    setCheckUIDone(R.id.circular_dialer, R.id.done_dialer, dialerCheckPassed.getCount()==0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        // Phenotype DB check
        new Thread() {
            @Override
            public void run() {
                try {
                    // Wait for root check to pass
                    rootCheckPassed.await();
                    // Wait for coreRootService to connect
                    coreRootServiceConnected.await();

                    // Check the Phenotype DB
                    if (checkPhenotypeDB()) {
                        phenotypeCheckPassed.countDown();
                    } else {
                        runOnUiThread(() ->
                                new AlertDialog.Builder(splashScreenActivity)
                                    .setCancelable(false)
                                    .setMessage(getString(R.string.phenotype_db_does_not_exist))
                                    .setNegativeButton(R.string.install_from_google_play, (dialogInterface, i) -> openGooglePlay(splashScreenActivity, GMS_GOOGLE_PLAY_LINK))
                                    .setPositiveButton(R.string.exit, (dialog, which) -> System.exit(0))
                                    .show());
                    }

                    // Update the UI
                    setCheckUIDone(R.id.circular_phenotype, R.id.done_phenotype, phenotypeCheckPassed.getCount()==0);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        // Update available check
        new Thread() {
            @Override
            public void run() {
                // Check if updates are available
                if (!checkUpdateAvailable(splashScreenActivity)) {
                    updateCheckFinished.countDown();
                } else {
                    runOnUiThread(() ->
                            new AlertDialog.Builder(splashScreenActivity)
                                    .setCancelable(false)
                                    .setMessage(R.string.new_version_alert)
                                    .setNegativeButton(
                                            R.string.github,
                                            (dialogInterface, i) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_link)+"/releases")))
                                    )
                                    .setPositiveButton(R.string.continue_anyway, (dialogInterface, i) -> updateCheckFinished.countDown())
                                    .show());
                }

                // Update the UI
                setCheckUIDone(R.id.circular_updates, R.id.done_updates, updateCheckFinished.getCount()==0);
            }
        }.start();

        // End splash screen and go to the main activity
        new Thread() {
            @Override
            public void run() {
                try {
                    // Wait for all checks to pass and for all operations to finish
                    rootCheckPassed.await();
                    coreRootServiceConnected.await();
                    dialerCheckPassed.await();
                    phenotypeCheckPassed.await();
                    updateCheckFinished.await();

                    // This is just for aesthetics: I don't want the splashscreen to be too fast
                    Thread.sleep(1000);

                    // Start the main activity
                    Intent intent = new Intent(splashScreenActivity, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
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

    private boolean checkDialerInstallation() {
        try {
            getApplication().getPackageManager().getApplicationInfo(DIALER_PACKAGE_NAME, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return coreRootServiceFSManager.getFile(DIALER_DATA_DATA).exists();
    }

    private boolean checkPhenotypeDB() {
        return coreRootServiceFSManager.getFile(PHENOTYPE_DB).exists();
    }

    private void setCheckUIDone(int circularID, int doneImageID, boolean success) {
        View circular = findViewById(circularID);
        ImageView doneImage = findViewById(doneImageID);
        runOnUiThread(() -> {
            circular.setVisibility(View.GONE);
            doneImage.setImageResource(success ? R.drawable.ic_success_24 : R.drawable.ic_fail_24);
            doneImage.setVisibility(View.VISIBLE);
        });
    }

    @Override
    protected void onDestroy() {
        if (coreRootServiceBound)
            RootService.unbind(coreRootServiceConnection);
        super.onDestroy();
    }
}