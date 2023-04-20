package com.jacopomii.googledialermod.ui.fragment;

import static android.Manifest.permission.CAPTURE_AUDIO_OUTPUT;
import static com.jacopomii.googledialermod.data.Constants.DIALER_CALLRECORDINGPROMPT;
import static com.jacopomii.googledialermod.data.Constants.DIALER_PACKAGE_NAME;
import static com.jacopomii.googledialermod.data.Constants.GOOGLE_PLAY_BETA_LINK;
import static com.jacopomii.googledialermod.data.Constants.GOOGLE_PLAY_DETAILS_LINK;
import static com.jacopomii.googledialermod.data.Constants.MESSAGES_PACKAGE_NAME;
import static com.jacopomii.googledialermod.data.Constants.MESSAGES_PACKAGE_NAME_PHENOTYPE_DB;
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
import com.google.protobuf.ByteString;
import com.jacopomii.googledialermod.ICoreRootService;
import com.jacopomii.googledialermod.R;
import com.jacopomii.googledialermod.databinding.FragmentSuggestedModsBinding;
import com.jacopomii.googledialermod.protos.Call_screen_i18n_config;
import com.jacopomii.googledialermod.ui.activity.MainActivity;
import com.jacopomii.googledialermod.ui.view.ProgrammaticMaterialSwitch;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.nio.ExtendedFile;
import com.topjohnwu.superuser.nio.FileSystemManager;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("FieldCanBeLocal")
public class SuggestedModsFragment extends Fragment {
    private FragmentSuggestedModsBinding binding;

    private ICoreRootService coreRootServiceIpc;
    private FileSystemManager coreRootServiceFSManager;

    // The following boolean flags force enable Call Recording features in Dialer app
    private final HashMap<String, Boolean> DIALER_ENABLE_CALL_RECORDING_FLAGS = new HashMap<String, Boolean>() {{
        // Enable Call Recording feature
        put("G__enable_call_recording", true);
        put("enable_call_recording_system_feature", true);
        // Enable Call Recording also for Google Fi / Fides (e2e calls, etc)
        put("CallRecording__enable_call_recording_for_fi", true);
        // Bypass country-related restrictions for call recording feature
        put("G__force_within_call_recording_geofence_value", true);
        // Bypass country-related restrictions for automatic call recording ("always record") feature
        put("G__force_within_crosby_geofence_value", true);
        // Allow the usage of the above two "force geofence" flags
        put("G__use_call_recording_geofence_overrides", true);
        // Show call recording button
        put("enable_tidepods_call_recording", true);
    }};

    // The following extensionVal flags concern the announcement audio played when a call recording starts or ends in Dialer app.
    // To silence announcements, we set them as empty stringVal flags.
    private final HashMap<String, String> DIALER_SILENCE_CALL_RECORDING_ALERTS_FLAGS = new HashMap<String, String>() {{
        // The following flag contains a protobuf list of countries where the use of embedded audio is enforced.
        // If its value is an empty string, the Dialer will by default use TTS to generate audio call recording alerts.
        put("CallRecording__call_recording_countries_with_built_in_audio_file", "");
        // The following flags are no longer used in recent versions of the Dialer and remain here for backwards compatibility.
        // They were used to contain a protobuf list of countries where the use of embedded or TTS audio was enforced.
        put("CallRecording__call_recording_force_enable_built_in_audio_file_countries", "");
        put("CallRecording__call_recording_force_enable_tts_countries", "");
        // The following flag contains a protobuf hashset with country-language matches, used by Dialer to generate call recording audio alerts via TTS
        // in the right language. If its value is an empty string, TTS will always fall back to en_US (hardcoded in the Dialer sources).
        put("CallRecording__call_recording_countries", "");
    }};
    private final String DIALER_CALLRECORDINGPROMPT_STARTING_VOICE_US = "starting_voice-en_US.wav";
    private final String DIALER_CALLRECORDINGPROMPT_ENDING_VOICE_US = "ending_voice-en_US.wav";
    // Dialer versionCode 10681248 (94.x) is the last version in which we can silence call recording alerts. In newer versions Google patched our hack.
    private final int DIALER_SILENCE_CALL_RECORDING_ALERTS_MAX_VERSION = 10681248;

    // The following boolean flags force enable Call Screen / Revelio features in Dialer app
    private final HashMap<String, Boolean> DIALER_ENABLE_CALL_SCREEN_FLAGS = new HashMap<String, Boolean>() {{
        // Enable Call Screen feature for both calls and video-calls
        put("G__speak_easy_enabled", true);
        put("enable_video_calling_screen", true);
        // Bypass Call Screen locale restrictions
        put("G__speak_easy_bypass_locale_check", true);
        // Enable translations for additional locales
        put("enable_call_screen_i18n_tidepods", true);
        // Enable the "listen in" button, which is located at the bottom right during screening
        put("G__speak_easy_enable_listen_in_button", true);
        // Enable the Call Screen Demo page in Dialer settings
        put("enable_call_screen_demo", true);
        // Enable the "See transcript" button in call history, which allows to read call screen transcripts and listen to recordings
        put("G__enable_speakeasy_details", true);
        // Enable Revelio,an advanced version of the Call Screen which allows to automatically filter calls
        put("G__enable_revelio", true);
        put("G__enable_revelio_on_bluetooth", true);
        put("G__enable_revelio_on_wired_headset", true);
        // Bypass Revelio locale restrictions
        put("G__bypass_revelio_roaming_check", true);
        // Enable translations for additional locales also for Revelio
        put("G__enable_tidepods_revelio", true);
        // Enable the Dialer settings option to save screened call audio (it does not depend on the Call Recording feature, but depends on Revelio)
        put("G__enable_call_screen_saving_audio", true);
        // Enable the saving of the transcript also for Revelio
        put("enable_revelio_transcript", true);
    }};

    // The following extensionVal flag contains a protobuf (see call_screen_i18n.proto for its definition)
    // which matches the languages to be used for the Call Screen feature to the supported countries in Dialer app
    private final String DIALER_CALL_SCREEN_I18N_CONFIG_FLAG = "CallScreenI18n__call_screen_i18n_config";

    // The following boolean flags force enable Message Organization (Super Sort) features in Messages app
    private final HashMap<String, Boolean> MESSAGES_ENABLE_MESSAGE_ORGANIZATION_FLAGS = new HashMap<String, Boolean>() {{
        // Enable super sort
        put("bugle_phenotype__conversation_labels_enabled", true);
        // Enable "all" category (this flag may be superfluous)
        put("bugle_phenotype__supersort_badge_all_filter", true);
        // Enable donation banner
        put("bugle_phenotype__supersort_enable_update_donation_banner", true);
        // Enable OTP auto deletion
        put("bugle_phenotype__enable_otp_auto_deletion", true);
        // Classify messages also in the foreground (don't use only workmanager)
        put("bugle_phenotype__supersort_use_only_work_manager", false);
        // I don't know what is it. In the Messages code I see that it's about "generating annotations" for "money, coupon, account number, percentage"
        put("bugle_phenotype__enable_supersort_annotators", true);
        // QPBC = Participant Based Quick Classification. I don't know how it works.
        put("bugle_phenotype__supersort_enable_qpbc", true);
    }};

    public SuggestedModsFragment() {
    }

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

        // GDialer
        try {
            // Check if application is installed
            requireContext().getPackageManager().getApplicationInfo(DIALER_PACKAGE_NAME, 0);

            // Check if application has CAPTURE_AUDIO_OUTPUT permission
            if (requireContext().getPackageManager().checkPermission(CAPTURE_AUDIO_OUTPUT, DIALER_PACKAGE_NAME) != PackageManager.PERMISSION_GRANTED)
                binding.dialerPermissionAlert.setVisibility(View.VISIBLE);

            // forceEnableCallRecordingSwitch
            ProgrammaticMaterialSwitch forceEnableCallRecordingSwitch = binding.forceEnableCallRecordingCard.getSwitch();
            boolean forceEnableCallRecordingSwitchChecked = false;
            try {
                forceEnableCallRecordingSwitchChecked = coreRootServiceIpc.phenotypeDBAreAllFlagsOverridden(DIALER_PACKAGE_NAME, new ArrayList<>(DIALER_ENABLE_CALL_RECORDING_FLAGS.keySet()));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            forceEnableCallRecordingSwitch.setCheckedProgrammatically(forceEnableCallRecordingSwitchChecked);
            forceEnableCallRecordingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> dialerForceEnableCallRecording(isChecked));
            forceEnableCallRecordingSwitch.setEnabled(true);

            // silenceCallRecordingAlertsSwitch
            ProgrammaticMaterialSwitch silenceCallRecordingAlertsSwitch = binding.silenceCallRecordingAlertsCard.getSwitch();
            boolean mSilenceCallRecordingAlertsSwitchChecked = false;
            try {
                ExtendedFile startingVoiceFile = coreRootServiceFSManager.getFile(DIALER_CALLRECORDINGPROMPT, DIALER_CALLRECORDINGPROMPT_STARTING_VOICE_US);
                ExtendedFile endingVoiceFile = coreRootServiceFSManager.getFile(DIALER_CALLRECORDINGPROMPT, DIALER_CALLRECORDINGPROMPT_STARTING_VOICE_US);

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

                    mSilenceCallRecordingAlertsSwitchChecked = coreRootServiceIpc.phenotypeDBAreAllFlagsOverridden(DIALER_PACKAGE_NAME, new ArrayList<>(DIALER_SILENCE_CALL_RECORDING_ALERTS_FLAGS.keySet())) &&
                            isStartingVoiceSilenced && isEndingVoiceSilenced;
                }
            } catch (IOException | RemoteException e) {
                e.printStackTrace();
            }

            try {
                // If Dialer version > SILENCE_CALL_RECORDING_ALERTS_MAX_VERSION the silenceCallRecordingAlertsSwitch must remain disabled
                if (requireContext().getPackageManager().getPackageInfo(DIALER_PACKAGE_NAME, 0).versionCode > DIALER_SILENCE_CALL_RECORDING_ALERTS_MAX_VERSION) {
                    // If the silenceCallRecordingAlertsSwitch was enabled in previous versions of GoogleDialerMod, the silenceCallRecordingAlerts mod must be automatically disabled
                    if (mSilenceCallRecordingAlertsSwitchChecked) {
                        dialerSilenceCallRecordingAlerts(false);
                    }
                    // Otherwise, the silenceCallRecordingAlertsSwitch should be loaded as usual
                } else {
                    silenceCallRecordingAlertsSwitch.setCheckedProgrammatically(mSilenceCallRecordingAlertsSwitchChecked);
                    silenceCallRecordingAlertsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> dialerSilenceCallRecordingAlerts(isChecked));
                    silenceCallRecordingAlertsSwitch.setEnabled(true);
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            // forceEnableCallScreenSwitch
            ProgrammaticMaterialSwitch forceEnableCallScreenSwitch = binding.forceEnableCallScreenCard.getSwitch();
            boolean forceEnableCallScreenSwitchChecked = false;
            try {
                forceEnableCallScreenSwitchChecked = coreRootServiceIpc.phenotypeDBAreAllFlagsOverridden(DIALER_PACKAGE_NAME, new ArrayList<>(DIALER_ENABLE_CALL_SCREEN_FLAGS.keySet())) &&
                        coreRootServiceIpc.phenotypeDBAreAllFlagsOverridden(DIALER_PACKAGE_NAME, Collections.singletonList(DIALER_CALL_SCREEN_I18N_CONFIG_FLAG));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            forceEnableCallScreenSwitch.setCheckedProgrammatically(forceEnableCallScreenSwitchChecked);
            forceEnableCallScreenSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> dialerForceEnableCallScreen(isChecked));
            forceEnableCallScreenSwitch.setEnabled(true);
        } catch (PackageManager.NameNotFoundException e) {
            binding.dialerBetaButton.setOnClickListener(v -> openGooglePlay(requireContext(), GOOGLE_PLAY_BETA_LINK + DIALER_PACKAGE_NAME));
            binding.dialerInstallButton.setOnClickListener(v -> openGooglePlay(requireContext(), GOOGLE_PLAY_DETAILS_LINK + DIALER_PACKAGE_NAME));
            binding.dialerNotInstalledAlert.setVisibility(View.VISIBLE);
        }

        // GMessages
        try {
            // Check if application is installed
            requireContext().getPackageManager().getApplicationInfo(MESSAGES_PACKAGE_NAME, 0);

            // forceEnableMessageOrganizationSwitch
            ProgrammaticMaterialSwitch forceEnableMessageOrganizationSwitch = binding.forceEnableMessageOrganizationCard.getSwitch();
            boolean forceEnableMessageOrganizationSwitchChecked = false;
            try {
                forceEnableMessageOrganizationSwitchChecked = coreRootServiceIpc.phenotypeDBAreAllFlagsOverridden(MESSAGES_PACKAGE_NAME_PHENOTYPE_DB, new ArrayList<>(MESSAGES_ENABLE_MESSAGE_ORGANIZATION_FLAGS.keySet()));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            forceEnableMessageOrganizationSwitch.setCheckedProgrammatically(forceEnableMessageOrganizationSwitchChecked);
            forceEnableMessageOrganizationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> messagesForceEnableMessageOrganization(isChecked));
            forceEnableMessageOrganizationSwitch.setEnabled(true);
        } catch (PackageManager.NameNotFoundException e) {
            binding.messagesBetaButton.setOnClickListener(v -> openGooglePlay(requireContext(), GOOGLE_PLAY_BETA_LINK + MESSAGES_PACKAGE_NAME));
            binding.messagesInstallButton.setOnClickListener(v -> openGooglePlay(requireContext(), GOOGLE_PLAY_DETAILS_LINK + MESSAGES_PACKAGE_NAME));
            binding.messagesNotInstalledAlert.setVisibility(View.VISIBLE);
        }

        return binding.getRoot();
    }

    private void dialerForceEnableCallRecording(boolean enableMod) {
        if (enableMod) {
            for (Map.Entry<String, Boolean> entry : DIALER_ENABLE_CALL_RECORDING_FLAGS.entrySet()) {
                try {
                    coreRootServiceIpc.phenotypeDBOverrideBooleanFlag(DIALER_PACKAGE_NAME, entry.getKey(), entry.getValue());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        } else {
            // Delete flag overrides
            try {
                coreRootServiceIpc.phenotypeDBDeleteFlagOverrides(DIALER_PACKAGE_NAME, new ArrayList<>(DIALER_ENABLE_CALL_RECORDING_FLAGS.keySet()));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void dialerSilenceCallRecordingAlerts(boolean enableMod) {
        if (enableMod) {
            for (Map.Entry<String, String> entry : DIALER_SILENCE_CALL_RECORDING_ALERTS_FLAGS.entrySet()) {
                try {
                    coreRootServiceIpc.phenotypeDBOverrideStringFlag(DIALER_PACKAGE_NAME, entry.getKey(), entry.getValue());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            try {
                // Create CALLRECORDINGPROMPT folder
                ExtendedFile callRecordingPromptDir = coreRootServiceFSManager.getFile(DIALER_CALLRECORDINGPROMPT);
                if (callRecordingPromptDir.mkdir() || (callRecordingPromptDir.exists() && callRecordingPromptDir.isDirectory())) {
                    // Overwrite the two alert files with an empty audio
                    ExtendedFile startingVoice = coreRootServiceFSManager.getFile(callRecordingPromptDir, DIALER_CALLRECORDINGPROMPT_STARTING_VOICE_US);
                    ExtendedFile endingVoice = coreRootServiceFSManager.getFile(callRecordingPromptDir, DIALER_CALLRECORDINGPROMPT_ENDING_VOICE_US);
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
            // Delete flag overrides
            try {
                coreRootServiceIpc.phenotypeDBDeleteFlagOverrides(DIALER_PACKAGE_NAME, new ArrayList<>(DIALER_SILENCE_CALL_RECORDING_ALERTS_FLAGS.keySet()));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            // Delete callrecordingprompt folder
            ExtendedFile callRecordingPromptFolder = coreRootServiceFSManager.getFile(DIALER_CALLRECORDINGPROMPT);
            if (callRecordingPromptFolder.exists()) {
                //noinspection ResultOfMethodCallIgnored
                callRecordingPromptFolder.delete();
            }
        }
    }

    private void dialerForceEnableCallScreen(boolean enableMod) {
        if (enableMod) {
            // Ask the user what language the Call Screen feature should use
            String[] supportedLanguages = {"en", "en-AU", "en-GB", "en-IN", "ja-JP", "fr-FR", "hi-IN", "de-DE", "it-IT", "es-ES"};
            final int[] chosenLanguageIndex = {0};
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.choose_a_language_for_call_screen)
                    .setSingleChoiceItems(supportedLanguages, chosenLanguageIndex[0], (dialog, which) -> chosenLanguageIndex[0] = which)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        // Update boolean flags
                        for (Map.Entry<String, Boolean> entry : DIALER_ENABLE_CALL_SCREEN_FLAGS.entrySet()) {
                            try {
                                coreRootServiceIpc.phenotypeDBOverrideBooleanFlag(DIALER_PACKAGE_NAME, entry.getKey(), entry.getValue());
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }

                        // Override the call screen i18n config flag with the user desired language
                        TelephonyManager telephonyManager = (TelephonyManager) requireActivity().getSystemService(Context.TELEPHONY_SERVICE);
                        String simCountryIso = telephonyManager.getSimCountryIso();

                        String chosenLanguage = supportedLanguages[chosenLanguageIndex[0]];

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
                            coreRootServiceIpc.phenotypeDBOverrideExtensionFlag(DIALER_PACKAGE_NAME, DIALER_CALL_SCREEN_I18N_CONFIG_FLAG, call_screen_i18n_config.toByteArray());
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel())
                    .setOnCancelListener(dialog -> binding.forceEnableCallScreenCard.getSwitch().setCheckedProgrammatically(false))
                    .show();
        } else {
            // Delete flag overrides
            try {
                coreRootServiceIpc.phenotypeDBDeleteFlagOverrides(DIALER_PACKAGE_NAME, new ArrayList<>(DIALER_ENABLE_CALL_SCREEN_FLAGS.keySet()));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            try {
                coreRootServiceIpc.phenotypeDBDeleteFlagOverrides(DIALER_PACKAGE_NAME, Collections.singletonList(DIALER_CALL_SCREEN_I18N_CONFIG_FLAG));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void messagesForceEnableMessageOrganization(boolean enableMod) {
        if (enableMod) {
            for (Map.Entry<String, Boolean> entry : MESSAGES_ENABLE_MESSAGE_ORGANIZATION_FLAGS.entrySet()) {
                try {
                    coreRootServiceIpc.phenotypeDBOverrideBooleanFlag(MESSAGES_PACKAGE_NAME_PHENOTYPE_DB, entry.getKey(), entry.getValue());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        } else {
            // Delete flag overrides
            try {
                coreRootServiceIpc.phenotypeDBDeleteFlagOverrides(MESSAGES_PACKAGE_NAME_PHENOTYPE_DB, new ArrayList<>(MESSAGES_ENABLE_MESSAGE_ORGANIZATION_FLAGS.keySet()));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}