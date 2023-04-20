package com.jacopomii.googledialermod.ui.view;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.materialswitch.MaterialSwitch;

/**
 * A MaterialSwitch that allows to programmatically set the checked / unchecked state without triggering the onCheckedChangeListener
 */
public class ProgrammaticMaterialSwitch extends MaterialSwitch {
    private OnCheckedChangeListener mOnCheckedChangeListener = null;

    public ProgrammaticMaterialSwitch(@NonNull Context context) {
        super(context);
    }

    public ProgrammaticMaterialSwitch(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ProgrammaticMaterialSwitch(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setOnCheckedChangeListener(@Nullable OnCheckedChangeListener listener) {
        mOnCheckedChangeListener = listener;
        super.setOnCheckedChangeListener(listener);
    }

    /**
     * Programmatically change the checked state of the switch without calling any onCheckedChangeListener.
     * Please note that any previously set onCheckedChangeListener is preserved, even if this method does not call it.
     *
     * @param checked {@code true} to check the switch, {@code false} to uncheck it.
     */
    public void setCheckedProgrammatically(boolean checked) {
        super.setOnCheckedChangeListener(null);
        super.setChecked(checked);
        super.setOnCheckedChangeListener(mOnCheckedChangeListener);
    }
}
