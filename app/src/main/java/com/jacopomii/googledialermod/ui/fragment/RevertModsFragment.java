package com.jacopomii.googledialermod.ui.fragment;

import static com.jacopomii.googledialermod.data.Constants.DIALER_CALLRECORDINGPROMPT;
import static com.jacopomii.googledialermod.data.Constants.DIALER_PHENOTYPE_PACKAGE_NAME;
import static com.jacopomii.googledialermod.util.Utils.showSelectPackageDialog;

import android.app.Activity;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.jacopomii.googledialermod.ICoreRootService;
import com.jacopomii.googledialermod.R;
import com.jacopomii.googledialermod.databinding.FragmentRevertModsBinding;
import com.jacopomii.googledialermod.ui.activity.MainActivity;
import com.topjohnwu.superuser.nio.ExtendedFile;
import com.topjohnwu.superuser.nio.FileSystemManager;

import es.dmoral.toasty.Toasty;

public class RevertModsFragment extends Fragment {
    FragmentRevertModsBinding mBinding;

    private ICoreRootService mCoreRootServiceIpc;
    private FileSystemManager mCoreRootServiceFSManager;

    public RevertModsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            mCoreRootServiceIpc = ((MainActivity) activity).getCoreRootServiceIpc();
            mCoreRootServiceFSManager = ((MainActivity) activity).getCoreRootServiceFSManager();
        } else {
            throw new RuntimeException("RevertModsFragment can be attached only to the MainActivity");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // View bindings
        mBinding = FragmentRevertModsBinding.inflate(getLayoutInflater());


        // Select package for revert mods for specific package
        TextView selectPackage = mBinding.selectPackage;

        // Select package onClick
        selectPackage.setOnClickListener(v -> {
            showSelectPackageDialog(getContext(), mCoreRootServiceIpc, item -> {
                // Select package dialog onItemClickListener
                // The item received by the listener here is the Phenotype package name chosen by the user

                // Update the select package textview
                selectPackage.setText((String) item);

                // Enable the revertModsSelectedPackageButton
                mBinding.revertModsSelectedPackageButton.setEnabled(true);
            });
        });


        // Revert mods for the selected package button
        mBinding.revertModsSelectedPackageButton.setOnClickListener(v -> {
                    String selectedPhenotypePackageName = selectPackage.getText().toString();
                    new MaterialAlertDialogBuilder(requireContext())
                            .setMessage(String.format(getResources().getString(R.string.revert_mods_for_the_selected_package_confirm), selectedPhenotypePackageName))
                            .setNegativeButton(getString(R.string.no), (dialog, which) -> {
                            })
                            .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                                try {
                                    // Delete all flag overrides for the selected package from Phenotype DB
                                    mCoreRootServiceIpc.phenotypeDBDeleteAllFlagOverridesByPhenotypePackageName(selectedPhenotypePackageName);

                                    // If the selected package was the Dialer
                                    if (selectedPhenotypePackageName.equals(DIALER_PHENOTYPE_PACKAGE_NAME)) {
                                        // Delete the com.google.android.dialer callrecordingprompt folder (if it exists)
                                        ExtendedFile callRecordingPromptFolder = mCoreRootServiceFSManager.getFile(DIALER_CALLRECORDINGPROMPT);
                                        if (callRecordingPromptFolder.exists()) {
                                            //noinspection ResultOfMethodCallIgnored
                                            callRecordingPromptFolder.delete();
                                        }
                                    }

                                    // UI confirmation to the user
                                    Toasty.success(requireContext(), getString(R.string.done), Toast.LENGTH_LONG, true).show();
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                    Toasty.error(requireContext(), getString(R.string.an_error_has_occurred), Toast.LENGTH_LONG, true).show();
                                }
                            }).show();
                }
        );


        // Revert all mods button
        mBinding.revertAllModsButton.setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setMessage(R.string.revert_mods_for_all_packages_confirm)
                        .setNegativeButton(getString(R.string.no), (dialog, which) -> {
                        })
                        .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                            try {
                                // Delete all flag overrides from Phenotype DB
                                mCoreRootServiceIpc.phenotypeDBDeleteAllFlagOverrides();

                                // Delete the com.google.android.dialer callrecordingprompt folder
                                ExtendedFile callRecordingPromptFolder = mCoreRootServiceFSManager.getFile(DIALER_CALLRECORDINGPROMPT);
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
                        }).show()
        );

        return mBinding.getRoot();
    }
}