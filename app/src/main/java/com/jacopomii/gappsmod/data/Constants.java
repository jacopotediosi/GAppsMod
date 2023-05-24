package com.jacopomii.gappsmod.data;

import android.annotation.SuppressLint;

@SuppressLint("SdCardPath")
public interface Constants {
    // Android package names
    String GMS_ANDROID_PACKAGE_NAME = "com.google.android.gms";
    String VENDING_ANDROID_PACKAGE_NAME = "com.android.vending";
    String DIALER_ANDROID_PACKAGE_NAME = "com.google.android.dialer";
    String MESSAGES_ANDROID_PACKAGE_NAME = "com.google.android.apps.messaging";

    // Phenotype package names
    String DIALER_PHENOTYPE_PACKAGE_NAME = "com.google.android.dialer";
    String MESSAGES_PHENOTYPE_PACKAGE_NAME = "com.google.android.apps.messaging#com.google.android.apps.messaging";

    // Google Play links
    String GOOGLE_PLAY_DETAILS_LINK = "https://play.google.com/store/apps/details?id=";
    String GOOGLE_PLAY_BETA_LINK = "https://play.google.com/apps/testing/";

    // Data / data folders
    String DATA_DATA_PREFIX = "/data/data/";
    String DIALER_CALLRECORDINGPROMPT = DATA_DATA_PREFIX + DIALER_ANDROID_PACKAGE_NAME + "/files/callrecordingprompt";
    String GMS_PHENOTYPE_DB = DATA_DATA_PREFIX + GMS_ANDROID_PACKAGE_NAME + "/databases/phenotype.db";
    String VENDING_PHENOTYPE_DB = DATA_DATA_PREFIX + VENDING_ANDROID_PACKAGE_NAME + "/databases/phenotype.db";
}
