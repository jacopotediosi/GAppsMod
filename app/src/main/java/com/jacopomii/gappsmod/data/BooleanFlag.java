package com.jacopomii.gappsmod.data;

public class BooleanFlag {
    private final String mFlagName;
    private boolean mFlagValue;
    private boolean mFlagOverriddenAndChanged;

    public BooleanFlag(String flagName, boolean flagValue, boolean flagOverriddenAndChanged) {
        mFlagName = flagName;
        mFlagValue = flagValue;
        mFlagOverriddenAndChanged = flagOverriddenAndChanged;
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

    public void setFlagOverriddenAndChanged(boolean flagOverriddenAndChanged) {
        mFlagOverriddenAndChanged = flagOverriddenAndChanged;
    }

    public boolean getFlagOverriddenAndChanged() {
        return mFlagOverriddenAndChanged;
    }
}
