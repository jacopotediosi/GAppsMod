package com.jacopomii.googledialermod.ui.fragment;

import static android.Manifest.permission.CAPTURE_AUDIO_OUTPUT;
import static com.jacopomii.googledialermod.data.Constants.DIALER_CALLRECORDINGPROMPT;
import static com.jacopomii.googledialermod.data.Constants.DIALER_GOOGLE_PLAY_BETA_LINK;
import static com.jacopomii.googledialermod.data.Constants.DIALER_GOOGLE_PLAY_LINK;
import static com.jacopomii.googledialermod.data.Constants.DIALER_PACKAGE_NAME;
import static com.jacopomii.googledialermod.util.Utils.copyFile;
import static com.jacopomii.googledialermod.util.Utils.openGooglePlay;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.protobuf.ByteString;
import com.jacopomii.googledialermod.ICoreRootService;
import com.jacopomii.googledialermod.R;
import com.jacopomii.googledialermod.databinding.FragmentSuggestedModsBinding;
import com.jacopomii.googledialermod.protos.Call_screen_i18n_config;
import com.jacopomii.googledialermod.ui.activity.MainActivity;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.nio.ExtendedFile;
import com.topjohnwu.superuser.nio.FileSystemManager;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

@SuppressWarnings("FieldCanBeLocal")
public class SuggestedModsFragment extends Fragment {
    private FragmentSuggestedModsBinding binding;

    private ICoreRootService coreRootServiceIpc;
    private FileSystemManager coreRootServiceFSManager;

    // The following boolean flags force enable or disable Call Recording features
    private final String[] ENABLE_CALL_RECORDING_FLAGS = {
            // Enable Call Recording feature
            "G__enable_call_recording",
            "enable_call_recording_system_feature",
            // Enable Call Recording also for Google Fi / Fides (e2e calls, etc)
            "CallRecording__enable_call_recording_for_fi",
            // Bypass country-related restrictions for call recording feature
            "G__force_within_call_recording_geofence_value",
            // Bypass country-related restrictions for automatic call recording ("always record") feature
            "G__force_within_crosby_geofence_value",
            // Allow the usage of the above two "force geofence" flags
            "G__use_call_recording_geofence_overrides",
            // Show call recording button
            "enable_tidepods_call_recording"
    };

    // The following extensionVal flags concern the announcement audio played when a call recording starts or ends
    private final String[] SILENCE_CALL_RECORDING_ALERTS_FLAGS = {
            // The following flag contains a protobuf list of countries where the use of embedded audio is enforced.
            // If its value is blank, the Dialer will by default use TTS to generate audio call recording alerts.
            "CallRecording__call_recording_countries_with_built_in_audio_file",
            // The following flags are no longer used in recent versions of the Dialer and remain here for backwards compatibility.
            // They were used to contain a protobuf list of countries where the use of embedded or TTS audio was enforced.
            "CallRecording__call_recording_force_enable_built_in_audio_file_countries",
            "CallRecording__call_recording_force_enable_tts_countries",
            // The following flag contains a protobuf hashset with country-language matches, used by Dialer to generate call recording audio alerts via TTS
            // in the right language. If its value is empty, TTS will always fall back to en_US (hardcoded in the Dialer sources).
            "CallRecording__call_recording_countries"
    };
    private final String CALLRECORDINGPROMPT_STARTING_VOICE_US = "starting_voice-en_US.wav";
    private final String CALLRECORDINGPROMPT_ENDING_VOICE_US = "ending_voice-en_US.wav";
    // Dialer versionCode 10681248 (94.x) is the last version in which we can silence call recording alerts. In newer versions Google patched our hack.
    private final int SILENCE_CALL_RECORDING_ALERTS_MAX_VERSION = 10681248;

    // The following boolean flags enable or disable Call Screen / Revelio features
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
            "enable_revelio_transcript"
    };

    // The following extensionVal flag contains a protobuf (see call_screen_i18n.proto for its definition)
    // which matches the languages to be used for the Call Screen feature to the supported countries
    private final String CALL_SCREEN_I18N_CONFIG_FLAG = "CallScreenI18n__call_screen_i18n_config";

    public SuggestedModsFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            coreRootServiceIpc = ((MainActivity) activity).getCoreRootServiceIpc();
            coreRootServiceFSManager = ((MainActivity) activity).getCoreRootServiceFSManager();
        } else {
            throw new RuntimeException("SuggestedModsFragment can be attached only to the MainActivity");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSuggestedModsBinding.inflate(getLayoutInflater());

        try {
            // Check if Google Dialer is installed
            requireContext().getPackageManager().getApplicationInfo(DIALER_PACKAGE_NAME, 0);

            // Check if Google Dialer has CAPTURE_AUDIO_OUTPUT permission
            if (requireContext().getPackageManager().checkPermission(CAPTURE_AUDIO_OUTPUT, DIALER_PACKAGE_NAME) != PackageManager.PERMISSION_GRANTED)
                binding.dialerPermissionAlert.setVisibility(View.VISIBLE);

            // forceEnableCallRecordingSwitch
            MaterialSwitch forceEnableCallRecordingSwitch = binding.forceEnableCallRecordingCard.getSwitch();
            forceEnableCallRecordingSwitch.setChecked(true);
            try {
                forceEnableCallRecordingSwitch.setChecked(
                        coreRootServiceIpc.phenotypeDBAreAllBooleanFlagsTrue(DIALER_PACKAGE_NAME, ENABLE_CALL_RECORDING_FLAGS)
                );
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            forceEnableCallRecordingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> forceEnableCallRecording(isChecked));
            forceEnableCallRecordingSwitch.setEnabled(true);

            // silenceCallRecordingAlertsSwitch
            MaterialSwitch silenceCallRecordingAlertsSwitch = binding.silenceCallRecordingAlertsCard.getSwitch();
            boolean mSilenceCallRecordingAlertsSwitchNewStatus = false;
            try {
                ExtendedFile startingVoiceFile = coreRootServiceFSManager.getFile(DIALER_CALLRECORDINGPROMPT, CALLRECORDINGPROMPT_STARTING_VOICE_US);
                ExtendedFile endingVoiceFile = coreRootServiceFSManager.getFile(DIALER_CALLRECORDINGPROMPT, CALLRECORDINGPROMPT_STARTING_VOICE_US);

                if (startingVoiceFile.exists() && endingVoiceFile.exists()) {
                    InputStream silentVoiceInputStream;

                    InputStream startingVoiceInputStream = startingVoiceFile.newInputStream();
                    silentVoiceInputStream = getResources().openRawResource(R.raw.silent_wav);
                    boolean isStartingVoiceSilenced = IOUtils.contentEquals(silentVoiceInputStream, startingVoiceInputStream);
                    startingVoiceInputStream.close();
                    silentVoiceInputStream.close();

                    InputStream endingVoiceInputStream = endingVoiceFile.newInputStream();
                    silentVoiceInputStream = getResources().openRawResource(R.raw.silent_wav);
                    boolean isEndingVoiceSilenced = IOUtils.contentEquals(silentVoiceInputStream, endingVoiceInputStream);
                    endingVoiceInputStream.close();
                    silentVoiceInputStream.close();

                    mSilenceCallRecordingAlertsSwitchNewStatus = coreRootServiceIpc.phenotypeDBAreAllStringFlagsEmpty(DIALER_PACKAGE_NAME, SILENCE_CALL_RECORDING_ALERTS_FLAGS) &&
                            isStartingVoiceSilenced && isEndingVoiceSilenced;
                }
            } catch (IOException | RemoteException e) {
                e.printStackTrace();
            }

            try {
                // If Dialer version > SILENCE_CALL_RECORDING_ALERTS_MAX_VERSION the silenceCallRecordingAlertsSwitch must remain disabled
                if (requireContext().getPackageManager().getPackageInfo(DIALER_PACKAGE_NAME, 0).versionCode > SILENCE_CALL_RECORDING_ALERTS_MAX_VERSION) {
                    // If the silenceCallRecordingAlertsSwitch was enabled in previous versions of GoogleDialerMod, the silenceCallRecordingAlerts mod must be automatically disabled
                    if (mSilenceCallRecordingAlertsSwitchNewStatus) {
                        silenceCallRecordingAlerts(false);
                    }
                    // Otherwise, the silenceCallRecordingAlertsSwitch should be loaded as usual
                } else {
                    silenceCallRecordingAlertsSwitch.setChecked(mSilenceCallRecordingAlertsSwitchNewStatus);
                    silenceCallRecordingAlertsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> silenceCallRecordingAlerts(isChecked));
                    silenceCallRecordingAlertsSwitch.setEnabled(true);
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            // forceEnableCallScreenSwitch
            MaterialSwitch forceEnableCallScreenSwitch = binding.forceEnableCallScreenCard.getSwitch();
            try {
                forceEnableCallScreenSwitch.setChecked(
                        coreRootServiceIpc.phenotypeDBAreAllBooleanFlagsTrue(DIALER_PACKAGE_NAME, ENABLE_CALL_SCREEN_FLAGS) &&
                                coreRootServiceIpc.phenotypeDBAreAllFlagsOverridden(DIALER_PACKAGE_NAME, new String[]{CALL_SCREEN_I18N_CONFIG_FLAG})
                );
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            forceEnableCallScreenSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> forceEnableCallScreen(isChecked));
            forceEnableCallScreenSwitch.setEnabled(true);
        } catch (PackageManager.NameNotFoundException e) {
            binding.dialerBetaButton.setOnClickListener(v -> openGooglePlay(requireContext(), DIALER_GOOGLE_PLAY_BETA_LINK));
            binding.dialerInstallButton.setOnClickListener(v -> openGooglePlay(requireContext(), DIALER_GOOGLE_PLAY_LINK));
            binding.dialerNotInstalledAlert.setVisibility(View.VISIBLE);
        }

        return binding.getRoot();
    }

    private void forceEnableCallRecording(boolean enable) {
        if (enable) {
            for (String flag : ENABLE_CALL_RECORDING_FLAGS) {
                try {
                    coreRootServiceIpc.phenotypeDBUpdateBooleanFlag(DIALER_PACKAGE_NAME, flag, true);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                coreRootServiceIpc.phenotypeDBDeleteFlagOverrides(DIALER_PACKAGE_NAME, ENABLE_CALL_RECORDING_FLAGS);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void silenceCallRecordingAlerts(boolean silence) {
        if (silence) {
            for (String flag : SILENCE_CALL_RECORDING_ALERTS_FLAGS) {
                try {
                    coreRootServiceIpc.phenotypeDBUpdateStringFlag(DIALER_PACKAGE_NAME, flag, "");
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            try {
                // Create CALLRECORDINGPROMPT folder
                ExtendedFile callRecordingPromptDir = coreRootServiceFSManager.getFile(DIALER_CALLRECORDINGPROMPT);
                if ( callRecordingPromptDir.mkdir() || (callRecordingPromptDir.exists() && callRecordingPromptDir.isDirectory()) ) {
                    // Overwrite the two alert files with an empty audio
                    ExtendedFile startingVoice = coreRootServiceFSManager.getFile(callRecordingPromptDir, CALLRECORDINGPROMPT_STARTING_VOICE_US);
                    ExtendedFile endingVoice = coreRootServiceFSManager.getFile(callRecordingPromptDir, CALLRECORDINGPROMPT_ENDING_VOICE_US);
                    copyFile(getResources().openRawResource(R.raw.silent_wav), startingVoice.newOutputStream());
                    copyFile(getResources().openRawResource(R.raw.silent_wav), endingVoice.newOutputStream());

                    // Set the right permissions to files and folders
                    final int uid = requireActivity().getPackageManager().getApplicationInfo(DIALER_PACKAGE_NAME, 0).uid;
                    Shell.cmd(
                            String.format("chown -R %s:%s %s", uid, uid, DIALER_CALLRECORDINGPROMPT),
                            String.format("chmod 755 %s", DIALER_CALLRECORDINGPROMPT),
                            String.format("chmod 444 %s/*", DIALER_CALLRECORDINGPROMPT),
                            String.format("restorecon -R %s", DIALER_CALLRECORDINGPROMPT)
                    ).exec();
                }
            } catch (PackageManager.NameNotFoundException | IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                coreRootServiceIpc.phenotypeDBDeleteFlagOverrides(DIALER_PACKAGE_NAME, SILENCE_CALL_RECORDING_ALERTS_FLAGS);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            ExtendedFile callRecordingPromptFolder = coreRootServiceFSManager.getFile(DIALER_CALLRECORDINGPROMPT);
            if (callRecordingPromptFolder.exists()) {
                //noinspection ResultOfMethodCallIgnored
                callRecordingPromptFolder.delete();
            }
        }
    }

    private void forceEnableCallScreen(boolean enable) {
        if (enable) {
            // Ask the user what language the Call Screen feature should use
            String[] supportedLanguages = {"en", "en-AU", "en-GB", "en-IN", "ja-JP", "fr-FR", "hi-IN", "de-DE", "it-IT", "es-ES"};
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.choose_a_language_for_call_screen)
                    .setCancelable(false)
                    .setItems(supportedLanguages, (dialog, choice) -> {
                        // Update boolean flags
                        for (String flag : ENABLE_CALL_SCREEN_FLAGS) {
                            try {
                                coreRootServiceIpc.phenotypeDBUpdateBooleanFlag(DIALER_PACKAGE_NAME, flag, true);
                            } catch (RemoteException e) {
                                throw new RuntimeException(e);
                            }
                        }

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
                        try {
                            coreRootServiceIpc.phenotypeDBUpdateExtensionFlag(DIALER_PACKAGE_NAME, CALL_SCREEN_I18N_CONFIG_FLAG, call_screen_i18n_config.toByteArray());
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }).create().show();
        } else {
            // Remove flag overrides
            try {
                coreRootServiceIpc.phenotypeDBDeleteFlagOverrides(DIALER_PACKAGE_NAME, ENABLE_CALL_SCREEN_FLAGS);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            try {
                coreRootServiceIpc.phenotypeDBDeleteFlagOverrides(DIALER_PACKAGE_NAME, new String[]{CALL_SCREEN_I18N_CONFIG_FLAG});
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}