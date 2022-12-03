package com.pa.safetyhubmod;

import static com.pa.safetyhubmod.Utils.deleteCallrecordingpromptFolder;
import static com.pa.safetyhubmod.Utils.revertAllMods;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.google.protobuf.ByteString;
import com.pa.safetyhubmod.protos.Call_screen_i18n_config;
import com.topjohnwu.superuser.Shell;

public class SuggestedModsFragment extends Fragment {
    private View mView;
    private SwitchCompat mForceEnableCallRecordingSwitch;
    private SwitchCompat mSilenceCallRecordingAlertsSwitch;
    private SwitchCompat mForceEnableCallScreenSwitch;
    private DBFlagsSingleton mDBFlagsSingleton;
    private final String[] ENABLE_CALL_RECORDING_FLAGS = {
            // Enable Call Recording feature
            "G__enable_call_recording",
            "enable_call_recording_system_feature",
            // Enable Call Recording also for Google Fi / Fides (e2e calls, etc)
            "CallRecording__enable_call_recording_for_fi",
            // Bypass country-related restrictions
            "G__force_within_call_recording_geofence_value",
            "G__use_call_recording_geofence_overrides",
            "G__force_within_crosby_geofence_value",
            // Show call recording button
            "enable_tidepods_call_recording"
    };
    private final String[] SILENCE_CALL_RECORDING_ALERTS_FLAGS = {
            // Following flags contain a serialized and base64 encoded list of countries in which the use of embedded audio or audio generated with TTS is forced
            // If their hashsets are all empty, dialer will use by default the TTS to generate audio call recording announcements
            "CallRecording__call_recording_countries_with_built_in_audio_file",
            "CallRecording__call_recording_force_enable_built_in_audio_file_countries",
            "CallRecording__call_recording_force_enable_tts_countries",
            // Following flags contain a serialized and base64 encoded hashset with country-language matches, used by Dialer to generate call recording audio announcements via TTS
            // If their hashsets are empty, TTS will always fallback to en_US
            "CallRecording__call_recording_countries",
            "CallRecording__crosby_countries"
    };
    private final String[] ENABLE_CALL_SCREEN_FLAGS = {
            // Enable Call Screen feature for both calls and video-calls
            "G__speak_easy_enabled",
            "enable_video_calling_screen",
            // Bypass Call Screen locale restrictions
            "G__speak_easy_bypass_locale_check",
            // Enable translations for additional locales
            "enable_call_screen_i18n_tidepods",
            // Enable the "listen in" button, which is located at the bottom right during screening
            "G__speak_easy_enable_listen_in_button",
            // Enable the Call Screen Demo page in Dialer settings
            "enable_call_screen_demo",
            // Enable the "See transcript" button in call history, which allows to read call screen transcripts and listen to recordings
            "G__enable_speakeasy_details",
            // Enable Revelio,an advanced version of the Call Screen which allows to automatically filter calls
            "G__enable_revelio",
            "G__enable_revelio_on_bluetooth",
            "G__enable_revelio_on_wired_headset",
            // Bypass Revelio locale restrictions
            "G__bypass_revelio_roaming_check",
            // Enable translations for additional locales also for Revelio
            "G__enable_tidepods_revelio",
            // Enable the Dialer settings option to save screened call audio (it does not depend on the Call Recording feature, but depends on Revelio)
            "G__enable_call_screen_saving_audio",
            // Enable the saving of the transcript also for Revelio
            "enable_revelio_transcript",
            // Enable Change Greeting Voice feature
            "G__voicemail_change_greeting_enabled"
    };
    private final String CALL_SCREEN_I18N_CONFIG_FLAG = "CallScreenI18n__call_screen_i18n_config";
    private CompoundButton.OnCheckedChangeListener mForceEnableCallRecordingSwitchOnCheckedChangeListener;
    private CompoundButton.OnCheckedChangeListener mSilenceCallRecordingAlertsSwitchOnCheckedChangeListener;
    private CompoundButton.OnCheckedChangeListener mForceEnableCallScreenSwitchOnCheckedChangeListener;

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
        mSilenceCallRecordingAlertsSwitch = mView.findViewById(R.id.silence_call_recording_alerts_switch);
        mForceEnableCallScreenSwitch = mView.findViewById(R.id.force_enable_call_screen_switch);

        mForceEnableCallRecordingSwitchOnCheckedChangeListener = (buttonView, isChecked) -> {
            for (String flag : ENABLE_CALL_RECORDING_FLAGS) {
                mDBFlagsSingleton.updateDBFlag(flag, isChecked);
            }
        };
        mForceEnableCallRecordingSwitch.setOnCheckedChangeListener(mForceEnableCallRecordingSwitchOnCheckedChangeListener);

        mSilenceCallRecordingAlertsSwitchOnCheckedChangeListener = (buttonView, isChecked) -> {
            if (isChecked) {
                for (String flag : SILENCE_CALL_RECORDING_ALERTS_FLAGS) {
                    mDBFlagsSingleton.updateDBFlag(flag, "");
                }
                try {
                    final String dataDir = requireActivity().getApplicationInfo().dataDir;
                    final int uid = requireActivity().getPackageManager().getApplicationInfo("com.google.android.apps.safetyhub", 0).uid;
                    //TODO: eventualmente splittare in più comandi?
                    Shell.cmd("rm -r /data/data/com.google.android.apps.safetyhub/files/callrecordingprompt; " +
                            "mkdir /data/data/com.google.android.apps.safetyhub/files/callrecordingprompt; " +
                            "cp " + dataDir + "/silent_wav.wav /data/data/com.google.android.apps.safetyhub/files/callrecordingprompt/starting_voice-en_US.wav; " +
                            "cp " + dataDir + "/silent_wav.wav /data/data/com.google.android.apps.safetyhub/files/callrecordingprompt/ending_voice-en_US.wav; " +
                            "chown -R " + uid + ":" + uid + " /data/data/com.google.android.apps.safetyhub/files/callrecordingprompt; " +
                            "chmod -R 755 /data/data/com.google.android.apps.safetyhub/files/callrecordingprompt; " +
                            "chmod 444 /data/data/com.google.android.apps.safetyhub/files/callrecordingprompt/*; " +
                            "restorecon -R /data/data/com.google.android.apps.safetyhub/files/callrecordingprompt").exec();
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                mDBFlagsSingleton.deleteFlagOverrides(SILENCE_CALL_RECORDING_ALERTS_FLAGS);
                deleteCallrecordingpromptFolder();
            }
        };
        mSilenceCallRecordingAlertsSwitch.setOnCheckedChangeListener(mSilenceCallRecordingAlertsSwitchOnCheckedChangeListener);

        mForceEnableCallScreenSwitchOnCheckedChangeListener = (buttonView, isChecked) -> {
            if (isChecked) {
                // Ask the user what language the Call Screen feature should use
                String[] supportedLanguages = {"en", "en-AU", "en-GB", "en-IN", "ja-JP", "fr-FR", "hi-IN", "de-DE", "it-IT", "es-ES"};
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.choose_a_language)
                        .setItems(supportedLanguages, (dialog, choice) -> {
                            // Update boolean flags
                            for (String flag : ENABLE_CALL_SCREEN_FLAGS)
                                mDBFlagsSingleton.updateDBFlag(flag, true);

                            // Override the call screen i18n config flag with the user desired language
                            TelephonyManager telephonyManager = (TelephonyManager) requireActivity().getSystemService(Context.TELEPHONY_SERVICE);
                            String simCountryIso = telephonyManager.getSimCountryIso();

                            String chosenLanguage = supportedLanguages[choice];

                            Call_screen_i18n_config call_screen_i18n_config = Call_screen_i18n_config.newBuilder()
                                    .addCountryConfigs(
                                            Call_screen_i18n_config.CountryConfig.newBuilder()
                                                    .setCountry(simCountryIso)
                                                    .setLanguageConfig(
                                                            Call_screen_i18n_config.LanguageConfig.newBuilder()
                                                                    .addLanguages(
                                                                            Call_screen_i18n_config.Language.newBuilder()
                                                                                    .setLanguageCode(chosenLanguage)
                                                                                    .setA6(
                                                                                            Call_screen_i18n_config.A6.newBuilder()
                                                                                                    .setA7(ByteString.copyFrom(new byte[]{2}))
                                                                                    )
                                                                    )
                                                    )
                                    ).build();
                            mDBFlagsSingleton.updateDBFlag(CALL_SCREEN_I18N_CONFIG_FLAG, call_screen_i18n_config.toByteArray());
                        }).create().show();
            } else {
                // Update boolean flags
                for (String flag : ENABLE_CALL_SCREEN_FLAGS)
                    mDBFlagsSingleton.updateDBFlag(flag, false);
                // Remove the call screen i18n config flag overrides
                mDBFlagsSingleton.deleteFlagOverrides("CallScreenI18n__call_screen_i18n_config");
            }
        };
        mForceEnableCallScreenSwitch.setOnCheckedChangeListener(mForceEnableCallScreenSwitchOnCheckedChangeListener);

        refreshSwitchesStatus();

        return mView;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.suggested_mods_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_info_icon) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            builder.setPositiveButton(android.R.string.ok, null)
                    .setView(R.layout.information_dialog);
            AlertDialog alert = builder.create();
            alert.show();

            // Links aren't clickable workaround
            TextView whatIsItExplanation = alert.findViewById(R.id.what_is_it_explanation);
            if (whatIsItExplanation != null)
                whatIsItExplanation.setMovementMethod(LinkMovementMethod.getInstance());
            TextView madeWithLove = alert.findViewById(R.id.made_with_love_by_jacopo_tediosi);
            if (madeWithLove != null)
                madeWithLove.setMovementMethod(LinkMovementMethod.getInstance());

            return true;
        } else if (item.getItemId() == R.id.menu_delete_icon) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            builder.setMessage(R.string.delete_all_mods_alert)
                    .setNegativeButton(getString(android.R.string.cancel), (dialog, which) -> {
                    })
                    .setPositiveButton(getString(android.R.string.ok), (dialog, which) -> {
                        revertAllMods(requireContext());
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
        // mForceEnableCallRecordingSwitch
        mForceEnableCallRecordingSwitch.setOnCheckedChangeListener(null);
        mForceEnableCallRecordingSwitch.setChecked(
                mDBFlagsSingleton.areAllBooleanFlagsTrue(ENABLE_CALL_RECORDING_FLAGS)
        );
        mForceEnableCallRecordingSwitch.setOnCheckedChangeListener(mForceEnableCallRecordingSwitchOnCheckedChangeListener);

        // mSilenceCallRecordingAlertsSwitch
        int startingVoiceSize = -1;
        Shell.Result result;
        try {
            result = Shell.cmd("stat -c%s /data/data/com.google.android.apps.safetyhub/files/callrecordingprompt/starting_voice-en_US.wav").exec();
            if (!result.isSuccess()) // Fallback if stat is not a command
                result = Shell.cmd("ls -lS starting_voice-en_US.wav | awk '{print $5}'").exec();
            startingVoiceSize = Integer.parseInt(result.getOut().get(0));
        } catch (Exception ignored) {}
        mSilenceCallRecordingAlertsSwitch.setOnCheckedChangeListener(null);
        mSilenceCallRecordingAlertsSwitch.setChecked(
                mDBFlagsSingleton.areAllStringFlagsEmpty(SILENCE_CALL_RECORDING_ALERTS_FLAGS) &&
                startingVoiceSize > 0 && startingVoiceSize <= 100
        );
        mSilenceCallRecordingAlertsSwitch.setOnCheckedChangeListener(mSilenceCallRecordingAlertsSwitchOnCheckedChangeListener);

        // mForceEnableCallScreenSwitch
        mForceEnableCallScreenSwitch.setOnCheckedChangeListener(null);
        mForceEnableCallScreenSwitch.setChecked(
                mDBFlagsSingleton.areAllBooleanFlagsTrue(ENABLE_CALL_SCREEN_FLAGS) && mDBFlagsSingleton.areAllFlagsOverridden(CALL_SCREEN_I18N_CONFIG_FLAG)
        );
        mForceEnableCallScreenSwitch.setOnCheckedChangeListener(mForceEnableCallScreenSwitchOnCheckedChangeListener);
    }
}
