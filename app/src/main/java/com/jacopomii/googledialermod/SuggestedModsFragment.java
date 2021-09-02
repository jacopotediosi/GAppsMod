package com.jacopomii.googledialermod;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

public class SuggestedModsFragment extends Fragment {
    private View mView;
    private Switch mForceEnableCallRecordingSwitch;
    private DBFlagsSingleton mDBFlagsSingleton;
    private final String[] ENABLE_CALL_RECORDING_FLAGS = {"G__enable_call_recording", "G__force_within_call_recording_geofence_value", "G__use_call_recording_geofence_overrides", "G__force_within_crosby_geofence_value"};

    public SuggestedModsFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.suggested_mods_fragment, container, false);

        mDBFlagsSingleton = DBFlagsSingleton.getInstance(requireActivity());

        mForceEnableCallRecordingSwitch = mView.findViewById(R.id.force_enable_call_recording_switch);

        refreshSwitchesStatus();

        mForceEnableCallRecordingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            for (String flag : ENABLE_CALL_RECORDING_FLAGS) {
                mDBFlagsSingleton.updateDBFlag(flag, isChecked);
            }
        });

        // TODO: switch / button to delete recordcallingprompt

        return mView;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.suggested_mods_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.delete_icon) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage(R.string.delete_all_mods_alert)
                    .setNegativeButton(getString(android.R.string.cancel), (dialog, which) -> {
                    })
                    .setPositiveButton(getString(android.R.string.ok), (dialog, which) -> {
                        mDBFlagsSingleton.deleteAllFlagOverrides();
                        refreshSwitchesStatus();
                    });
            AlertDialog alert = builder.create();
            alert.show();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    public void refreshSwitchesStatus() {
        mForceEnableCallRecordingSwitch.setChecked(
                mDBFlagsSingleton.areAllFlagsTrue(ENABLE_CALL_RECORDING_FLAGS)
        );

    }
}