package com.jacopomii.gappsmod.ui.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.jacopomii.gappsmod.ICoreRootService;
import com.jacopomii.gappsmod.R;
import com.jacopomii.gappsmod.databinding.ActivityMainBinding;
import com.jacopomii.gappsmod.service.CoreRootService;
import com.topjohnwu.superuser.ipc.RootService;
import com.topjohnwu.superuser.nio.FileSystemManager;

public class MainActivity extends AppCompatActivity {
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding mBinding;

    private boolean mCoreRootServiceBound = false;
    private ServiceConnection mCoreRootServiceConnection;
    private ICoreRootService mCoreRootServiceIpc;
    private FileSystemManager mCoreRootServiceFSManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // The savedInstanceState must not be used, otherwise the views (and the fragments contained
        // by this activity) are restored before the RootService is started, causing NPE.
        super.onCreate(null);

        // Enable edge-to-edge: allows drawing under system bars, preventing Android from
        // automatically applying the fitSystemWindows property to the root view.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Start CoreRootService connection
        Intent intent = new Intent(this, CoreRootService.class);
        mCoreRootServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                try {
                    // Set references to the remote coreRootService
                    mCoreRootServiceBound = true;
                    mCoreRootServiceIpc = ICoreRootService.Stub.asInterface(service);
                    mCoreRootServiceFSManager = FileSystemManager.getRemote(mCoreRootServiceIpc.getFileSystemService());

                    // Inflate the activity layout and set the content view
                    mBinding = ActivityMainBinding.inflate(getLayoutInflater());
                    setContentView(mBinding.getRoot());

                    // Set the toolbar
                    setSupportActionBar(mBinding.toolbar);

                    // Set the drawer
                    DrawerLayout drawer = mBinding.drawerLayout;
                    mAppBarConfiguration = new AppBarConfiguration.Builder(
                            R.id.nav_suggested_mods,
                            R.id.nav_boolean_mods,
                            R.id.nav_revert_mods,
                            R.id.nav_information
                    ).setOpenableLayout(drawer).build();

                    // Pass through the window insets to the navHostFragment child views, except the top system bar
                    ViewCompat.setOnApplyWindowInsetsListener(mBinding.navHostFragment, (view, insets) -> {
                        WindowInsetsCompat insetsCompat = new WindowInsetsCompat.Builder(insets)
                                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(
                                        insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime()).left,
                                        0,
                                        insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime()).right,
                                        insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime()).bottom))
                                .build();
                        return ViewCompat.onApplyWindowInsets(view, insetsCompat);
                    });

                    // Set the navigation controller
                    NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(mBinding.navHostFragment.getId());
                    if (navHostFragment != null) {
                        NavController navController = navHostFragment.getNavController();
                        NavigationUI.setupActionBarWithNavController(MainActivity.this, navController, mAppBarConfiguration);
                        NavigationUI.setupWithNavController(mBinding.navView, navController);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mCoreRootServiceBound = false;
                mCoreRootServiceIpc = null;
                mCoreRootServiceFSManager = null;
            }
        };
        RootService.bind(intent, mCoreRootServiceConnection);
    }

    public FileSystemManager getCoreRootServiceFSManager() {
        return mCoreRootServiceFSManager;
    }

    public ICoreRootService getCoreRootServiceIpc() {
        return mCoreRootServiceIpc;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, mBinding.navHostFragment.getId());
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        if (mBinding.drawerLayout.isOpen())
            mBinding.drawerLayout.close();
        else
            finishAffinity();
    }

    @Override
    protected void onDestroy() {
        if (mCoreRootServiceBound)
            RootService.unbind(mCoreRootServiceConnection);
        super.onDestroy();
    }
}