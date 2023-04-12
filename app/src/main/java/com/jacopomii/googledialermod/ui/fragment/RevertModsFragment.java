package com.jacopomii.googledialermod.ui.fragment;

import static com.jacopomii.googledialermod.data.Constants.DIALER_CALLRECORDINGPROMPT;

import android.app.Activity;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.jacopomii.googledialermod.ICoreRootService;
import com.jacopomii.googledialermod.R;
import com.jacopomii.googledialermod.databinding.FragmentRevertModsBinding;
import com.jacopomii.googledialermod.ui.activity.MainActivity;
import com.topjohnwu.superuser.nio.ExtendedFile;
import com.topjohnwu.superuser.nio.FileSystemManager;

import es.dmoral.toasty.Toasty;

public class RevertModsFragment extends Fragment {
    FragmentRevertModsBinding binding;

    private ICoreRootService coreRootServiceIpc;
    private FileSystemManager coreRootServiceFSManager;

    public RevertModsFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            coreRootServiceIpc = ((MainActivity) activity).getCoreRootServiceIpc();
            coreRootServiceFSManager = ((MainActivity) activity).getCoreRootServiceFSManager();
        } else {
            throw new RuntimeException("RevertModsFragment can be attached only to the MainActivity");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRevertModsBinding.inflate(getLayoutInflater());

        binding.revertAllModsButton.setOnClickListener(v ->
                new AlertDialog.Builder(requireContext())
                        .setMessage(R.string.revert_all_mods_alert)
                        .setNegativeButton(getString(R.string.no), (dialog, which) -> {
                        })
                        .setPositiveButton(getString(R.string.yes), (dialog, which) -> {

                            try {
                                // Delete all flag overrides from Phenotype DB
                                coreRootServiceIpc.phenotypeDBDeleteAllFlagOverrides();

                                // Delete the com.google.android.dialer callrecordingprompt folder
                                ExtendedFile callRecordingPromptFolder = coreRootServiceFSManager.getFile(DIALER_CALLRECORDINGPROMPT);
                                if (callRecordingPromptFolder.exists()) {
                                    //noinspection ResultOfMethodCallIgnored
                                    callRecordingPromptFolder.delete();
                                }

                                // UI confirmation to the user
                                Toasty.success(requireContext(), getString(R.string.done), Toast.LENGTH_LONG, true).show();
                            } catch (RemoteException e) {
                                e.printStackTrace();
                                Toasty.error(requireContext(), getString(R.string.an_error_has_occurred), Toast.LENGTH_LONG, true).show();
                            }
                        }).create().show()
        );

        return binding.getRoot();
    }
}