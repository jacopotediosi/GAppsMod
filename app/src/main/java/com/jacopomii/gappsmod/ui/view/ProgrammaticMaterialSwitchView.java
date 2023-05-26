package com.jacopomii.gappsmod.ui.view;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.materialswitch.MaterialSwitch;

/**
 * A {@link MaterialSwitch} that allows to programmatically set the checked / unchecked state
 * without triggering the onCheckedChangeListener.
 */
public class ProgrammaticMaterialSwitchView extends MaterialSwitch {
    private OnCheckedChangeListener mOnCheckedChangeListener = null;

    public ProgrammaticMaterialSwitchView(@NonNull Context context) {
        super(context);
    }

    public ProgrammaticMaterialSwitchView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ProgrammaticMaterialSwitchView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setOnCheckedChangeListener(@Nullable OnCheckedChangeListener listener) {
        mOnCheckedChangeListener = listener;
        super.setOnCheckedChangeListener(listener);
    }

    /**
     * Programmatically change the checked state of the switch without calling any
     * onCheckedChangeListener. Please note that any previously set onCheckedChangeListener will be
     * preserved, even if this method does not call it.
     *
     * @param checked {@code true} to check the switch, {@code false} to uncheck it.
     */
    public void setCheckedProgrammatically(boolean checked) {
        super.setOnCheckedChangeListener(null);
        super.setChecked(checked);
        super.setOnCheckedChangeListener(mOnCheckedChangeListener);
    }
}
