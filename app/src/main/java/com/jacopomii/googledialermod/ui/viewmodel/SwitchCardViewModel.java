package com.jacopomii.googledialermod.ui.viewmodel;

public class SwitchCardViewModel {
    private String mSwitchText;
    private boolean mSwitchChecked;

    public SwitchCardViewModel(String switchText, boolean switchChecked) {
        mSwitchText = switchText;
        mSwitchChecked = switchChecked;
    }

    public String getSwitchText() {
        return mSwitchText;
    }

    public void setSwitchText(String switchText) {
        mSwitchText = switchText;
    }

    public boolean getSwitchChecked() {
        return mSwitchChecked;
    }

    public void setSwitchChecked(boolean switchChecked) {
        mSwitchChecked = switchChecked;
    }
}
