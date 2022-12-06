package com.jacopomii.googledialermod;

import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;

import com.topjohnwu.superuser.ipc.RootService;
import com.topjohnwu.superuser.nio.FileSystemManager;

public class RootFileSystemService extends RootService {

    @Override
    public IBinder onBind(@NonNull Intent intent) {
        return FileSystemManager.getService();
    }
}