package com.jacopomii.googledialermod;

import android.annotation.SuppressLint;

@SuppressLint("SdCardPath")
public interface Constants {
    String DIALER_PACKAGE_NAME = "com.google.android.dialer";
    String DIALER_GOOGLE_PLAY_LINK = "https://play.google.com/store/apps/details?id=" + DIALER_PACKAGE_NAME;
    String DIALER_DATA_DATA = "/data/data/com.google.android.dialer";
    String DIALER_DATA_FILES = DIALER_DATA_DATA + "/files";
    String DIALER_CALLRECORDINGPROMPT = DIALER_DATA_FILES + "/callrecordingprompt";
    String DIALER_PHENOTYPE_CACHE = DIALER_DATA_FILES + "/phenotype";
    String PHENOTYPE_DB = "/data/data/com.google.android.gms/databases/phenotype.db";
    String VENDING_PACKAGE_NAME = "com.android.vending";
}
