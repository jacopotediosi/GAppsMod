package com.jacopomii.gappsmod.ui.fragment;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.jacopomii.gappsmod.databinding.FragmentInformationBinding;

public class InformationFragment extends Fragment {
    FragmentInformationBinding mBinding;

    public InformationFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = FragmentInformationBinding.inflate(getLayoutInflater());

        // Links aren't clickable workaround
        mBinding.madeWithLoveByJacopoTediosi.setMovementMethod(LinkMovementMethod.getInstance());

        return mBinding.getRoot();
    }
}