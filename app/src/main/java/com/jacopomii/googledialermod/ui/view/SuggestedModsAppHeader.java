package com.jacopomii.googledialermod.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.jacopomii.googledialermod.R;
import com.jacopomii.googledialermod.databinding.SuggestedModsAppHeaderBinding;

/**
 * The application name header used by the Suggested Mods fragment.
 * It includes a large title for the app name and two buttons "Beta" and "Install".
 */
public class SuggestedModsAppHeader extends LinearLayout {
    final SuggestedModsAppHeaderBinding binding;

    public SuggestedModsAppHeader(Context context) {
        super(context);

        binding = SuggestedModsAppHeaderBinding.inflate(LayoutInflater.from(context), this, true);
    }

    public SuggestedModsAppHeader(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        binding = SuggestedModsAppHeaderBinding.inflate(LayoutInflater.from(context), this, true);

        final TypedArray xmlAttrs = context.obtainStyledAttributes(attrs, R.styleable.SuggestedModsAppHeader);
        final String appName = xmlAttrs.getString(R.styleable.SuggestedModsAppHeader_app_name);
        xmlAttrs.recycle();

        binding.appName.setText(appName);
    }

    public MaterialButton getBetaButton() {
        return binding.betaButton;
    }

    public MaterialButton getInstallButton() {
        return binding.installButton;
    }
}
