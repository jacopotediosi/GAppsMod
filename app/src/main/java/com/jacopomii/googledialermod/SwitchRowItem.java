package com.pa.safetyhubmod;

public class SwitchRowItem {
    private String mSwitchText;
    private boolean mSwitchChecked;

    public SwitchRowItem(String switchText, boolean switchChecked) {
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
