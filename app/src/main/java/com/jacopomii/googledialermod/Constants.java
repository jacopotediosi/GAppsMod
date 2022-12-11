package com.jacopomii.googledialermod;

import android.annotation.SuppressLint;

@SuppressLint("SdCardPath")
public interface Constants {
    // Package names
    String DIALER_PACKAGE_NAME = "com.google.android.dialer";
    String GMS_PACKAGE_NAME = "com.google.android.gms";
    String VENDING_PACKAGE_NAME = "com.android.vending";

    // Google Play links
    String GOOGLE_PLAY_DETAILS_LINK = "https://play.google.com/store/apps/details?id=";
    String DIALER_GOOGLE_PLAY_LINK = GOOGLE_PLAY_DETAILS_LINK + DIALER_PACKAGE_NAME;
    String GMS_GOOGLE_PLAY_LINK = GOOGLE_PLAY_DETAILS_LINK + GMS_PACKAGE_NAME;

    // Data / data folders
    String DATA_DATA_PREFIX = "/data/data/";
    String DIALER_DATA_DATA = DATA_DATA_PREFIX + DIALER_PACKAGE_NAME;
    String GMS_DATA_DATA = DATA_DATA_PREFIX + GMS_PACKAGE_NAME;
    String DIALER_DATA_FILES = DIALER_DATA_DATA + "/files";
    String DIALER_CALLRECORDINGPROMPT = DIALER_DATA_FILES + "/callrecordingprompt";
    String DIALER_PHENOTYPE_CACHE = DIALER_DATA_FILES + "/phenotype";
    String PHENOTYPE_DB = GMS_DATA_DATA + "/databases/phenotype.db";

}
