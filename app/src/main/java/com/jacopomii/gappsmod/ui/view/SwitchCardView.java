package com.jacopomii.gappsmod.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.jacopomii.gappsmod.R;
import com.jacopomii.gappsmod.databinding.SwitchCardBinding;

/**
 * A card that contains a {@link android.widget.TextView} and a
 * {@link ProgrammaticMaterialSwitchView} on a single line.
 * The text will be rendered in a separate textview from the switch to prevent accidentally
 * clicking on the text from triggering the switch.
 */
public class SwitchCardView extends LinearLayout {
    final SwitchCardBinding mBinding;

    public SwitchCardView(Context context) {
        super(context);

        mBinding = SwitchCardBinding.inflate(LayoutInflater.from(context), this, true);
    }

    public SwitchCardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        mBinding = SwitchCardBinding.inflate(LayoutInflater.from(context), this, true);

        final TypedArray xmlAttrs = context.obtainStyledAttributes(attrs, R.styleable.SwitchCardView);
        final String text = xmlAttrs.getString(R.styleable.SwitchCardView_text);
        final boolean enabled = xmlAttrs.getBoolean(R.styleable.SwitchCardView_enabled, true);
        xmlAttrs.recycle();

        mBinding.switchCardTextview.setText(text);
        mBinding.switchCardSwitch.setEnabled(enabled);
    }

    public ProgrammaticMaterialSwitchView getSwitch() {
        return mBinding.switchCardSwitch;
    }
}
