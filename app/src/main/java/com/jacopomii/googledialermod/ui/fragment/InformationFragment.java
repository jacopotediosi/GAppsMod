package com.jacopomii.googledialermod.ui.fragment;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.jacopomii.googledialermod.databinding.FragmentInformationBinding;

public class InformationFragment extends Fragment {
    FragmentInformationBinding binding;

    public InformationFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentInformationBinding.inflate(getLayoutInflater());

        // Links aren't clickable workaround
        binding.whatIsItExplanationBeta.setMovementMethod(LinkMovementMethod.getInstance());
        binding.madeWithLoveByJacopoTediosi.setMovementMethod(LinkMovementMethod.getInstance());

        return binding.getRoot();
    }
}