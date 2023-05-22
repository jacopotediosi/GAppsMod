package com.jacopomii.gappsmod.data;

public class BooleanFlag {
    private final String mFlagName;
    private boolean mFlagValue;

    public BooleanFlag(String flagName, boolean flagValue) {
        mFlagName = flagName;
        mFlagValue = flagValue;
    }

    public String getFlagName() {
        return mFlagName;
    }

    public boolean getFlagValue() {
        return mFlagValue;
    }

    public void setFlagValue(boolean flagValue) {
        mFlagValue = flagValue;
    }
}
