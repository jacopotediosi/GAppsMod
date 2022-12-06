package com.jacopomii.googledialermod;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.topjohnwu.superuser.nio.FileSystemManager;

public class RootServiceConnection implements ServiceConnection {
    private FileSystemManager fileSystemManager = null;

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        try {
            fileSystemManager = FileSystemManager.getRemote(service);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        fileSystemManager = null;
    }

    public FileSystemManager getRemoteFS() {
        return fileSystemManager;
    }
}