package com.jacopomii.googledialermod.data;

public class PhenotypeDBPackageName {
    private final String mPhenotypePackageName;
    private final String mAndroidPackageName;

    public PhenotypeDBPackageName(String phenotypePackageName, String androidPackageName) {
        mPhenotypePackageName = phenotypePackageName;
        mAndroidPackageName = androidPackageName;
    }

    public String getPhenotypePackageName() {
        return mPhenotypePackageName;
    }

    public String getAndroidPackageName() {
        return mAndroidPackageName;
    }
}
