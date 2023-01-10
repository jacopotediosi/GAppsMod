package com.jacopomii.googledialermod;

import static com.jacopomii.googledialermod.Constants.DIALER_DATA_DATA;
import static com.jacopomii.googledialermod.Constants.DIALER_GOOGLE_PLAY_BETA_LINK;
import static com.jacopomii.googledialermod.Constants.DIALER_GOOGLE_PLAY_LINK;
import static com.jacopomii.googledialermod.Constants.DIALER_PACKAGE_NAME;
import static com.jacopomii.googledialermod.Constants.GMS_GOOGLE_PLAY_LINK;
import static com.jacopomii.googledialermod.Constants.PHENOTYPE_DB;
import static com.jacopomii.googledialermod.Utils.checkUpdateAvailable;
import static com.jacopomii.googledialermod.Utils.copyFile;
import static com.jacopomii.googledialermod.Utils.openGooglePlay;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.topjohnwu.superuser.Shell;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;

@SuppressLint("CustomSplashScreen")
public class SplashScreenActivity extends AppCompatActivity {
    private final SplashScreenActivity splashScreenActivity = this;

    private final CountDownLatch rootCheckPassed = new CountDownLatch(1);
    private final CountDownLatch dialerCheckPassed = new CountDownLatch(1);
    private final CountDownLatch phenotypeCheckPassed = new CountDownLatch(1);
    private final CountDownLatch copyAssetsFinished = new CountDownLatch(1);
    private final CountDownLatch updateCheckFinished = new CountDownLatch(1);

    static {
        // Set Libsu settings before creating the main shell
        Shell.enableVerboseLogging = BuildConfig.DEBUG;
        Shell.setDefaultBuilder(Shell.Builder.create().setFlags(Shell.FLAG_MOUNT_MASTER));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

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
                                .setPositiveButton(R.string.exit, (dialog, i) -> finishAffinity())
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

                    // Check the Phenotype DB
                    if (checkPhenotypeDB()) {
                        phenotypeCheckPassed.countDown();
                    } else {
                        runOnUiThread(() ->
                                new AlertDialog.Builder(splashScreenActivity)
                                    .setCancelable(false)
                                    .setMessage(getString(R.string.phenotype_db_does_not_exist))
                                    .setNegativeButton(R.string.install_from_google_play, (dialogInterface, i) -> openGooglePlay(splashScreenActivity, GMS_GOOGLE_PLAY_LINK))
                                    .setPositiveButton(R.string.exit, (dialog, which) -> finishAffinity())
                                    .show());
                    }

                    // Update the UI
                    setCheckUIDone(R.id.circular_phenotype, R.id.done_phenotype, phenotypeCheckPassed.getCount()==0);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        // Copy assets
        new Thread() {
            @Override
            public void run() {
                try {
                    // Wait for root check to pass
                    rootCheckPassed.await();

                    // Copy application assets
                    try {
                        if (copyAssets()) {
                            copyAssetsFinished.countDown();
                        } else {
                            runOnUiThread(() ->
                                    new AlertDialog.Builder(splashScreenActivity)
                                        .setCancelable(false)
                                        .setMessage(R.string.cpu_arch_not_supported)
                                        .setPositiveButton(getString(R.string.exit), (dialog, which) -> finishAffinity())
                                        .show());
                        }
                    } catch (IOException e) {
                        runOnUiThread(() ->
                                new AlertDialog.Builder(splashScreenActivity)
                                    .setCancelable(false)
                                    .setMessage(R.string.splash_screen_copy_assets_error)
                                    .setNegativeButton(R.string.github, (dialogInterface, i) ->
                                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_link))))
                                    )
                                    .setPositiveButton(getString(R.string.exit), (dialog, which) -> finishAffinity())
                                    .show());
                    }

                    // Update the UI
                    setCheckUIDone(R.id.circular_copy_assets, R.id.done_copy_assets, copyAssetsFinished.getCount()==0);
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
                    dialerCheckPassed.await();
                    phenotypeCheckPassed.await();
                    copyAssetsFinished.await();
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
        return Shell.cmd(String.format("test -d %s", DIALER_DATA_DATA)).exec().isSuccess();
    }

    private boolean checkPhenotypeDB() {
        return Shell.cmd(String.format("test -f %s", PHENOTYPE_DB)).exec().isSuccess();
    }

    private boolean copyAssets() throws IOException {
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

        if (inputStream == null)
            return false;

        outputFile = new File(dataDir, "sqlite3");
        outputStream = new FileOutputStream(outputFile);
        copyFile(inputStream, outputStream);
        Shell.cmd("chmod 755 " + dataDir + "/sqlite3").exec();
        return true;
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
}