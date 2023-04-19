package com.jacopomii.googledialermod.data;

import android.annotation.SuppressLint;

@SuppressLint("SdCardPath")
public interface Constants {
    // Package names
    String GMS_PACKAGE_NAME = "com.google.android.gms";
    String VENDING_PACKAGE_NAME = "com.android.vending";
    String DIALER_PACKAGE_NAME = "com.google.android.dialer";
    String MESSAGES_PACKAGE_NAME = "com.google.android.apps.messaging";
    String MESSAGES_PACKAGE_NAME_PHENOTYPE_DB = "com.google.android.apps.messaging#com.google.android.apps.messaging";

    // Google Play links
    String GOOGLE_PLAY_DETAILS_LINK = "https://play.google.com/store/apps/details?id=";
    String GOOGLE_PLAY_BETA_LINK = "https://play.google.com/apps/testing/";

    // Data / data folders
    String DATA_DATA_PREFIX = "/data/data/";
    String DIALER_CALLRECORDINGPROMPT = DATA_DATA_PREFIX + DIALER_PACKAGE_NAME + "/files/callrecordingprompt";
    String PHENOTYPE_DB = DATA_DATA_PREFIX + GMS_PACKAGE_NAME + "/databases/phenotype.db";
}
