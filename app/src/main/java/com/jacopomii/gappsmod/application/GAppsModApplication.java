package com.jacopomii.gappsmod.application;

import android.app.Application;

import com.google.android.material.color.DynamicColors;

public class GAppsModApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}