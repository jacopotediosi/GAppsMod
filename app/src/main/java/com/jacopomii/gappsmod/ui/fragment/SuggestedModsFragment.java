package com.jacopomii.gappsmod.ui.fragment;

import static android.Manifest.permission.CAPTURE_AUDIO_OUTPUT;
import static com.jacopomii.gappsmod.data.Constants.DIALER_ANDROID_PACKAGE_NAME;
import static com.jacopomii.gappsmod.data.Constants.DIALER_CALLRECORDINGPROMPT;
import static com.jacopomii.gappsmod.data.Constants.DIALER_PHENOTYPE_PACKAGE_NAME;
import static com.jacopomii.gappsmod.data.Constants.GOOGLE_PLAY_BETA_LINK;
import static com.jacopomii.gappsmod.data.Constants.GOOGLE_PLAY_DETAILS_LINK;
import static com.jacopomii.gappsmod.data.Constants.MESSAGES_ANDROID_PACKAGE_NAME;
import static com.jacopomii.gappsmod.data.Constants.MESSAGES_PHENOTYPE_PACKAGE_NAME;
import static com.jacopomii.gappsmod.util.Utils.copyFile;
import static com.jacopomii.gappsmod.util.Utils.openGooglePlay;

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
import com.jacopomii.gappsmod.ICoreRootService;
import com.jacopomii.gappsmod.R;
import com.jacopomii.gappsmod.databinding.FragmentSuggestedModsBinding;
import com.jacopomii.gappsmod.protos.Call_screen_i18n_config;
import com.jacopomii.gappsmod.ui.activity.MainActivity;
import com.jacopomii.gappsmod.ui.view.ProgrammaticMaterialSwitchView;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.nio.ExtendedFile;
import com.topjohnwu.superuser.nio.FileSystemManager;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("FieldCanBeLocal")
public class SuggestedModsFragment extends Fragment {
    private FragmentSuggestedModsBinding mBinding;

    private ICoreRootService mCoreRootServiceIpc;
    private FileSystemManager mCoreRootServiceFSManager;

    // The following boolean flags force-enable Call Recording features in Dialer app
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

    // The following boolean flags force-enable Call Screen / Revelio features in Dialer app
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

    // The following boolean flag force-enables debug menu in Messages app
    private final HashMap<String, Boolean> MESSAGES_ENABLE_DEBUG_MENU_FLAGS = new HashMap<String, Boolean>() {{
        put("bugle_phenotype__debug_menu_default_available", true);
    }};

    // The following boolean flag force-enables marking conversations as unread in Messages app
    private final HashMap<String, Boolean> MESSAGES_ENABLE_MARKING_CONVERSATIONS_UNREAD_FLAGS = new HashMap<String, Boolean>() {{
        put("bugle_phenotype__enable_mark_as_unread", true);
    }};

    // The following boolean flags force-enable Message Organization (Super Sort) features in Messages app
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

    // The following boolean flag force-enables verified SMS settings menu in Messages app
    private final HashMap<String, Boolean> MESSAGES_ENABLE_VERIFIED_SMS_FLAGS = new HashMap<String, Boolean>() {{
        put("bugle_phenotype__enabled_verified_sms", true);
    }};

    // The following boolean flag force-enables sending images via GPhotos links in Messages app
    private final HashMap<String, Boolean> MESSAGES_ENABLE_IMAGES_VIA_GPHOTOS_FLAGS = new HashMap<String, Boolean>() {{
        put("bugle_phenotype__enable_google_photos_image_by_link", true);
    }};

    // The following boolean flags force-enable nudges and birthday reminders in Messages app
    private final HashMap<String, Boolean> MESSAGES_ENABLE_NUDGES_FLAGS = new HashMap<String, Boolean>() {{
        // Enable nudges and birthday reminders
        put("bugle_phenotype__enable_nudge", true);
        put("bugle_phenotype__enable_birthday_nudge", true);
        put("bugle_phenotype__enable_birthday_suggestions", true);
        // Enable banners
        put("bugle_phenotype__enable_nudge_banner", true);
        put("bugle_phenotype__enable_birthday_banner", true);
        put("bugle_phenotype__enable_save_birthday_banner", true);
        // Enable settings pages
        put("bugle_phenotype__enable_birthday_nudge_setting", true);
        put("bugle_phenotype__enable_birthday_banner_settings_button", true);
        // Leave the settings menu lines separate
        put("bugle_phenotype__combing_nudge_settings", false);
    }};

    // The following boolean flags force-enable spotlights suggestions settings menu in Messages app
    private final HashMap<String, Boolean> MESSAGES_ENABLE_SPOTLIGHTS_FLAGS = new HashMap<String, Boolean>() {{
        // Enable spotlights
        put("bugle_phenotype__enable_spotlights", true);
        // Enable spotlights settings page
        put("bugle_phenotype__enable_spotlight_settings_page", true);
        // Enable new settings page layout, otherwise it won't show up
        put("bugle_phenotype__enable_smarts_settings_page_v2", true);
        // Enable additional spotlights features
        put("bugle_phenotype__enable_spotlights_google_search", true);
    }};

    // The following boolean flag force-enables smart compose (predictive writing) settings menu in Messages app
    private final HashMap<String, Boolean> MESSAGES_ENABLE_SMART_COMPOSE_FLAGS = new HashMap<String, Boolean>() {{
        put("bugle_phenotype__enable_smart_compose", true);
    }};

    // The following boolean flags force-enable magic compose (draft suggestions with Bard AI) in Messages app
    private final HashMap<String, Boolean> MESSAGES_ENABLE_MAGIC_COMPOSE_FLAGS = new HashMap<String, Boolean>() {{
        // Enable magic compose view
        put("bugle_phenotype__enable_magic_compose_view", true);
        // Idk what is it, but it has to be true to effectively enable magic compose
        put("bugle_phenotype__enable_combined_magic_compose", true);
        // Enable all additional functionalities for magic compose (e.g., feedback and multiple writing styles)
        put("bugle_phenotype__enable_additional_functionalities_for_magic_compose", true);
        // Enable magic compose also in xms
        put("bugle_phenotype__magic_compose_enabled_in_xms", true);
    }};

    // The following boolean flag force-enables smart actions (smart reply) in notifications in Messages app
    private final HashMap<String, Boolean> MESSAGES_ENABLE_SMART_ACTIONS_IN_NOTIFICATIONS_FLAGS = new HashMap<String, Boolean>() {{
        put("bugle_phenotype__enable_smart_actions_in_notifications", true);
    }};

    // The following boolean flag force-enables suggested stickers settings menu in Messages app
    private final HashMap<String, Boolean> MESSAGES_ENABLE_SUGGESTED_STICKERS_FLAGS = new HashMap<String, Boolean>() {{
        put("bugle_phenotype__sticker_suggestions_setting_enabled", true);
    }};

    public SuggestedModsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            mCoreRootServiceIpc = ((MainActivity) activity).getCoreRootServiceIpc();
            mCoreRootServiceFSManager = ((MainActivity) activity).getCoreRootServiceFSManager();
        } else {
            throw new RuntimeException("SuggestedModsFragment can be attached only to the MainActivity");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = FragmentSuggestedModsBinding.inflate(getLayoutInflater());

        // GDialer
        try {
            // Beta and Install buttons actions
            mBinding.dialerAppHeader.getBetaButton().setOnClickListener(v -> openGooglePlay(requireContext(), GOOGLE_PLAY_BETA_LINK + DIALER_ANDROID_PACKAGE_NAME));
            mBinding.dialerAppHeader.getInstallButton().setOnClickListener(v -> openGooglePlay(requireContext(), GOOGLE_PLAY_DETAILS_LINK + DIALER_ANDROID_PACKAGE_NAME));

            // Check if application is installed
            requireContext().getPackageManager().getApplicationInfo(DIALER_ANDROID_PACKAGE_NAME, 0);

            // Check if application has CAPTURE_AUDIO_OUTPUT permission
            if (requireContext().getPackageManager().checkPermission(CAPTURE_AUDIO_OUTPUT, DIALER_ANDROID_PACKAGE_NAME) != PackageManager.PERMISSION_GRANTED)
                mBinding.dialerPermissionAlert.setVisibility(View.VISIBLE);

            // dialerForceEnableCallRecordingSwitch
            ProgrammaticMaterialSwitchView dialerForceEnableCallRecordingSwitch = mBinding.dialerForceEnableCallRecording.getSwitch();
            boolean dialerForceEnableCallRecordingSwitchChecked = modCheckAreAllFlagsOverridden(DIALER_PHENOTYPE_PACKAGE_NAME, new ArrayList<>(DIALER_ENABLE_CALL_RECORDING_FLAGS.keySet()));
            dialerForceEnableCallRecordingSwitch.setCheckedProgrammatically(dialerForceEnableCallRecordingSwitchChecked);
            dialerForceEnableCallRecordingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> modSetBooleanFlags(isChecked, DIALER_PHENOTYPE_PACKAGE_NAME, DIALER_ENABLE_CALL_RECORDING_FLAGS));
            dialerForceEnableCallRecordingSwitch.setEnabled(true);

            // dialerSilenceCallRecordingAlertsSwitch
            ProgrammaticMaterialSwitchView dialerSilenceCallRecordingAlertsSwitch = mBinding.dialerSilenceCallRecordingAlerts.getSwitch();
            boolean dialerSilenceCallRecordingAlertsSwitchChecked = false;
            try {
                ExtendedFile startingVoiceFile = mCoreRootServiceFSManager.getFile(DIALER_CALLRECORDINGPROMPT, DIALER_CALLRECORDINGPROMPT_STARTING_VOICE_US);
                ExtendedFile endingVoiceFile = mCoreRootServiceFSManager.getFile(DIALER_CALLRECORDINGPROMPT, DIALER_CALLRECORDINGPROMPT_STARTING_VOICE_US);

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

                    dialerSilenceCallRecordingAlertsSwitchChecked = modCheckAreAllFlagsOverridden(DIALER_PHENOTYPE_PACKAGE_NAME, new ArrayList<>(DIALER_SILENCE_CALL_RECORDING_ALERTS_FLAGS.keySet())) &&
                            isStartingVoiceSilenced && isEndingVoiceSilenced;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                // If Dialer version > SILENCE_CALL_RECORDING_ALERTS_MAX_VERSION the dialerSilenceCallRecordingAlertsSwitch must remain disabled
                if (requireContext().getPackageManager().getPackageInfo(DIALER_ANDROID_PACKAGE_NAME, 0).versionCode > DIALER_SILENCE_CALL_RECORDING_ALERTS_MAX_VERSION) {
                    // If the dialerSilenceCallRecordingAlertsSwitch was enabled in previous versions of GAppsMod, the silenceCallRecordingAlerts mod must be automatically disabled
                    if (dialerSilenceCallRecordingAlertsSwitchChecked) {
                        dialerSilenceCallRecordingAlerts(false);
                    }
                    // Otherwise, the dialerSilenceCallRecordingAlertsSwitch should be loaded as usual
                } else {
                    dialerSilenceCallRecordingAlertsSwitch.setCheckedProgrammatically(dialerSilenceCallRecordingAlertsSwitchChecked);
                    dialerSilenceCallRecordingAlertsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> dialerSilenceCallRecordingAlerts(isChecked));
                    dialerSilenceCallRecordingAlertsSwitch.setEnabled(true);
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            // dialerForceEnableCallScreenSwitch
            ProgrammaticMaterialSwitchView dialerForceEnableCallScreenSwitch = mBinding.dialerForceEnableCallScreen.getSwitch();
            boolean dialerForceEnableCallScreenSwitchChecked = modCheckAreAllFlagsOverridden(DIALER_PHENOTYPE_PACKAGE_NAME, Collections.singletonList(DIALER_CALL_SCREEN_I18N_CONFIG_FLAG));
            dialerForceEnableCallScreenSwitch.setCheckedProgrammatically(dialerForceEnableCallScreenSwitchChecked);
            dialerForceEnableCallScreenSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> dialerForceEnableCallScreen(isChecked));
            dialerForceEnableCallScreenSwitch.setEnabled(true);
        } catch (PackageManager.NameNotFoundException e) {
            mBinding.dialerNotInstalledAlert.setVisibility(View.VISIBLE);
        }

        // GMessages
        try {
            // Beta and Install buttons actions
            mBinding.messagesAppHeader.getBetaButton().setOnClickListener(v -> openGooglePlay(requireContext(), GOOGLE_PLAY_BETA_LINK + MESSAGES_ANDROID_PACKAGE_NAME));
            mBinding.messagesAppHeader.getInstallButton().setOnClickListener(v -> openGooglePlay(requireContext(), GOOGLE_PLAY_DETAILS_LINK + MESSAGES_ANDROID_PACKAGE_NAME));

            // Check if application is installed
            requireContext().getPackageManager().getApplicationInfo(MESSAGES_ANDROID_PACKAGE_NAME, 0);

            // messagesForceEnableDebugMenuSwitch
            ProgrammaticMaterialSwitchView messagesForceEnableDebugMenuSwitch = mBinding.messagesForceEnableDebugMenu.getSwitch();
            boolean messagesForceEnableDebugMenuSwitchChecked = modCheckAreAllFlagsOverridden(MESSAGES_PHENOTYPE_PACKAGE_NAME, new ArrayList<>(MESSAGES_ENABLE_DEBUG_MENU_FLAGS.keySet()));
            messagesForceEnableDebugMenuSwitch.setCheckedProgrammatically(messagesForceEnableDebugMenuSwitchChecked);
            messagesForceEnableDebugMenuSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> modSetBooleanFlags(isChecked, MESSAGES_PHENOTYPE_PACKAGE_NAME, MESSAGES_ENABLE_DEBUG_MENU_FLAGS));
            messagesForceEnableDebugMenuSwitch.setEnabled(true);

            // messagesForceEnableMarkingMessageThreadsUnreadSwitch
            ProgrammaticMaterialSwitchView messagesForceEnableMarkingMessageThreadsUnreadSwitch = mBinding.messagesForceEnableMarkingMessageThreadsUnread.getSwitch();
            boolean messagesForceEnableMarkingMessageThreadsUnreadSwitchChecked = modCheckAreAllFlagsOverridden(MESSAGES_PHENOTYPE_PACKAGE_NAME, new ArrayList<>(MESSAGES_ENABLE_MARKING_CONVERSATIONS_UNREAD_FLAGS.keySet()));
            messagesForceEnableMarkingMessageThreadsUnreadSwitch.setCheckedProgrammatically(messagesForceEnableMarkingMessageThreadsUnreadSwitchChecked);
            messagesForceEnableMarkingMessageThreadsUnreadSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> modSetBooleanFlags(isChecked, MESSAGES_PHENOTYPE_PACKAGE_NAME, MESSAGES_ENABLE_MARKING_CONVERSATIONS_UNREAD_FLAGS));
            messagesForceEnableMarkingMessageThreadsUnreadSwitch.setEnabled(true);

            // messagesForceEnableMessageOrganizationSwitch
            ProgrammaticMaterialSwitchView messagesForceEnableMessageOrganizationSwitch = mBinding.messagesForceEnableMessageOrganization.getSwitch();
            boolean messagesForceEnableMessageOrganizationSwitchChecked = modCheckAreAllFlagsOverridden(MESSAGES_PHENOTYPE_PACKAGE_NAME, new ArrayList<>(MESSAGES_ENABLE_MESSAGE_ORGANIZATION_FLAGS.keySet()));
            messagesForceEnableMessageOrganizationSwitch.setCheckedProgrammatically(messagesForceEnableMessageOrganizationSwitchChecked);
            messagesForceEnableMessageOrganizationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> modSetBooleanFlags(isChecked, MESSAGES_PHENOTYPE_PACKAGE_NAME, MESSAGES_ENABLE_MESSAGE_ORGANIZATION_FLAGS));
            messagesForceEnableMessageOrganizationSwitch.setEnabled(true);

            // messagesForceEnableVerifiedSmsSwitch
            ProgrammaticMaterialSwitchView messagesForceEnableVerifiedSmsSwitch = mBinding.messagesForceEnableVerifiedSms.getSwitch();
            boolean messagesForceEnableVerifiedSmsSwitchChecked = modCheckAreAllFlagsOverridden(MESSAGES_PHENOTYPE_PACKAGE_NAME, new ArrayList<>(MESSAGES_ENABLE_VERIFIED_SMS_FLAGS.keySet()));
            messagesForceEnableVerifiedSmsSwitch.setCheckedProgrammatically(messagesForceEnableVerifiedSmsSwitchChecked);
            messagesForceEnableVerifiedSmsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> modSetBooleanFlags(isChecked, MESSAGES_PHENOTYPE_PACKAGE_NAME, MESSAGES_ENABLE_VERIFIED_SMS_FLAGS));
            messagesForceEnableVerifiedSmsSwitch.setEnabled(true);

            // messagesForceEnableGphotosSwitch
            ProgrammaticMaterialSwitchView messagesForceEnableGphotosSwitch = mBinding.messagesForceEnableGphotos.getSwitch();
            boolean messagesForceEnableGphotosSwitchChecked = modCheckAreAllFlagsOverridden(MESSAGES_PHENOTYPE_PACKAGE_NAME, new ArrayList<>(MESSAGES_ENABLE_IMAGES_VIA_GPHOTOS_FLAGS.keySet()));
            messagesForceEnableGphotosSwitch.setCheckedProgrammatically(messagesForceEnableGphotosSwitchChecked);
            messagesForceEnableGphotosSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> modSetBooleanFlags(isChecked, MESSAGES_PHENOTYPE_PACKAGE_NAME, MESSAGES_ENABLE_IMAGES_VIA_GPHOTOS_FLAGS));
            messagesForceEnableGphotosSwitch.setEnabled(true);

            // messagesForceEnableNudgesSwitch
            ProgrammaticMaterialSwitchView messagesForceEnableNudgesSwitch = mBinding.messagesForceEnableNudges.getSwitch();
            boolean messagesForceEnableNudgesSwitchChecked = modCheckAreAllFlagsOverridden(MESSAGES_PHENOTYPE_PACKAGE_NAME, new ArrayList<>(MESSAGES_ENABLE_NUDGES_FLAGS.keySet()));
            messagesForceEnableNudgesSwitch.setCheckedProgrammatically(messagesForceEnableNudgesSwitchChecked);
            messagesForceEnableNudgesSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> modSetBooleanFlags(isChecked, MESSAGES_PHENOTYPE_PACKAGE_NAME, MESSAGES_ENABLE_NUDGES_FLAGS));
            messagesForceEnableNudgesSwitch.setEnabled(true);

            // messagesForceEnableSpotlightsSwitch
            ProgrammaticMaterialSwitchView messagesForceEnableSpotlightsSwitch = mBinding.messagesForceEnableSpotlights.getSwitch();
            boolean messagesForceEnableSpotlightsSwitchChecked = modCheckAreAllFlagsOverridden(MESSAGES_PHENOTYPE_PACKAGE_NAME, new ArrayList<>(MESSAGES_ENABLE_SPOTLIGHTS_FLAGS.keySet()));
            messagesForceEnableSpotlightsSwitch.setCheckedProgrammatically(messagesForceEnableSpotlightsSwitchChecked);
            messagesForceEnableSpotlightsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> modSetBooleanFlags(isChecked, MESSAGES_PHENOTYPE_PACKAGE_NAME, MESSAGES_ENABLE_SPOTLIGHTS_FLAGS));
            messagesForceEnableSpotlightsSwitch.setEnabled(true);

            // messagesForceEnableSmartComposeSwitch
            ProgrammaticMaterialSwitchView messagesForceEnableSmartComposeSwitch = mBinding.messagesForceEnableSmartCompose.getSwitch();
            boolean messagesForceEnableSmartComposeSwitchChecked = modCheckAreAllFlagsOverridden(MESSAGES_PHENOTYPE_PACKAGE_NAME, new ArrayList<>(MESSAGES_ENABLE_SMART_COMPOSE_FLAGS.keySet()));
            messagesForceEnableSmartComposeSwitch.setCheckedProgrammatically(messagesForceEnableSmartComposeSwitchChecked);
            messagesForceEnableSmartComposeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> modSetBooleanFlags(isChecked, MESSAGES_PHENOTYPE_PACKAGE_NAME, MESSAGES_ENABLE_SMART_COMPOSE_FLAGS));
            messagesForceEnableSmartComposeSwitch.setEnabled(true);

            // messagesForceEnableMagicComposeSwitch
            ProgrammaticMaterialSwitchView messagesForceEnableMagicComposeSwitch = mBinding.messagesForceEnableMagicCompose.getSwitch();
            boolean messagesForceEnableMagicComposeSwitchChecked = modCheckAreAllFlagsOverridden(MESSAGES_PHENOTYPE_PACKAGE_NAME, new ArrayList<>(MESSAGES_ENABLE_MAGIC_COMPOSE_FLAGS.keySet()));
            messagesForceEnableMagicComposeSwitch.setCheckedProgrammatically(messagesForceEnableMagicComposeSwitchChecked);
            messagesForceEnableMagicComposeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> modSetBooleanFlags(isChecked, MESSAGES_PHENOTYPE_PACKAGE_NAME, MESSAGES_ENABLE_MAGIC_COMPOSE_FLAGS));
            messagesForceEnableMagicComposeSwitch.setEnabled(true);

            // messagesForceEnableSmartActionsInNotificationsSwitch
            ProgrammaticMaterialSwitchView messagesForceEnableSmartActionsInNotificationsSwitch = mBinding.messagesForceEnableSmartActionsInNotifications.getSwitch();
            boolean messagesForceEnableSmartActionsInNotificationsSwitchChecked = modCheckAreAllFlagsOverridden(MESSAGES_PHENOTYPE_PACKAGE_NAME, new ArrayList<>(MESSAGES_ENABLE_SMART_ACTIONS_IN_NOTIFICATIONS_FLAGS.keySet()));
            messagesForceEnableSmartActionsInNotificationsSwitch.setCheckedProgrammatically(messagesForceEnableSmartActionsInNotificationsSwitchChecked);
            messagesForceEnableSmartActionsInNotificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> modSetBooleanFlags(isChecked, MESSAGES_PHENOTYPE_PACKAGE_NAME, MESSAGES_ENABLE_SMART_ACTIONS_IN_NOTIFICATIONS_FLAGS));
            messagesForceEnableSmartActionsInNotificationsSwitch.setEnabled(true);

            // messagesForceEnableSuggestedStickersSwitch
            ProgrammaticMaterialSwitchView messagesForceEnableSuggestedStickersSwitch = mBinding.messagesForceEnableSuggestedStickers.getSwitch();
            boolean messagesForceEnableSuggestedStickersSwitchChecked = modCheckAreAllFlagsOverridden(MESSAGES_PHENOTYPE_PACKAGE_NAME, new ArrayList<>(MESSAGES_ENABLE_SUGGESTED_STICKERS_FLAGS.keySet()));
            messagesForceEnableSuggestedStickersSwitch.setCheckedProgrammatically(messagesForceEnableSuggestedStickersSwitchChecked);
            messagesForceEnableSuggestedStickersSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> modSetBooleanFlags(isChecked, MESSAGES_PHENOTYPE_PACKAGE_NAME, MESSAGES_ENABLE_SUGGESTED_STICKERS_FLAGS));
            messagesForceEnableSuggestedStickersSwitch.setEnabled(true);
        } catch (PackageManager.NameNotFoundException e) {
            mBinding.messagesNotInstalledAlert.setVisibility(View.VISIBLE);
        }

        return mBinding.getRoot();
    }

    private boolean modCheckAreAllFlagsOverridden(String phenotypePackageName, List<String> flags) {
        try {
            return mCoreRootServiceIpc.phenotypeDBAreAllFlagsOverridden(phenotypePackageName, flags);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void modSetBooleanFlags(boolean enableMod, String phenotypePackageName, HashMap<String, Boolean> flags) {
        if (enableMod) {
            for (Map.Entry<String, Boolean> flag : flags.entrySet()) {
                try {
                    mCoreRootServiceIpc.phenotypeDBOverrideBooleanFlag(phenotypePackageName, flag.getKey(), flag.getValue());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        } else {
            modDeleteFlagOverrides(phenotypePackageName, new ArrayList<>(flags.keySet()));
        }
    }

    private void modSetStringFlags(boolean enableMod, String phenotypePackageName, HashMap<String, String> flags) {
        if (enableMod) {
            for (Map.Entry<String, String> flag : flags.entrySet()) {
                try {
                    mCoreRootServiceIpc.phenotypeDBOverrideStringFlag(phenotypePackageName, flag.getKey(), flag.getValue());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        } else {
            modDeleteFlagOverrides(phenotypePackageName, new ArrayList<>(flags.keySet()));
        }
    }

    private void modDeleteFlagOverrides(String phenotypePackageName, List<String> flags) {
        try {
            mCoreRootServiceIpc.phenotypeDBDeleteFlagOverrides(phenotypePackageName, flags);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void dialerSilenceCallRecordingAlerts(boolean enableMod) {
        // Set flags (or delete overrides)
        modSetStringFlags(enableMod, DIALER_PHENOTYPE_PACKAGE_NAME, DIALER_SILENCE_CALL_RECORDING_ALERTS_FLAGS);

        // Apply additional mods
        if (enableMod) {
            try {
                // Create CALLRECORDINGPROMPT folder
                ExtendedFile callRecordingPromptDir = mCoreRootServiceFSManager.getFile(DIALER_CALLRECORDINGPROMPT);
                if (callRecordingPromptDir.mkdir() || (callRecordingPromptDir.exists() && callRecordingPromptDir.isDirectory())) {
                    // Overwrite the two alert files with an empty audio
                    ExtendedFile startingVoice = mCoreRootServiceFSManager.getFile(callRecordingPromptDir, DIALER_CALLRECORDINGPROMPT_STARTING_VOICE_US);
                    ExtendedFile endingVoice = mCoreRootServiceFSManager.getFile(callRecordingPromptDir, DIALER_CALLRECORDINGPROMPT_ENDING_VOICE_US);
                    copyFile(getResources().openRawResource(R.raw.silent_wav), startingVoice.newOutputStream());
                    copyFile(getResources().openRawResource(R.raw.silent_wav), endingVoice.newOutputStream());

                    // Set the right permissions to files and folders
                    final int uid = requireActivity().getPackageManager().getApplicationInfo(DIALER_ANDROID_PACKAGE_NAME, 0).uid;
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
            // Delete callrecordingprompt folder
            ExtendedFile callRecordingPromptFolder = mCoreRootServiceFSManager.getFile(DIALER_CALLRECORDINGPROMPT);
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
                        modSetBooleanFlags(true, DIALER_PHENOTYPE_PACKAGE_NAME, DIALER_ENABLE_CALL_SCREEN_FLAGS);

                        // Override the call screen i18n config extension flag with the user desired language
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
                            mCoreRootServiceIpc.phenotypeDBOverrideExtensionFlag(DIALER_PHENOTYPE_PACKAGE_NAME, DIALER_CALL_SCREEN_I18N_CONFIG_FLAG, call_screen_i18n_config.toByteArray());
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel())
                    .setOnCancelListener(dialog -> mBinding.dialerForceEnableCallScreen.getSwitch().setCheckedProgrammatically(false))
                    .show();
        } else {
            // Delete flag overrides
            modDeleteFlagOverrides(DIALER_PHENOTYPE_PACKAGE_NAME, new ArrayList<>(DIALER_ENABLE_CALL_SCREEN_FLAGS.keySet()));
            modDeleteFlagOverrides(DIALER_PHENOTYPE_PACKAGE_NAME, Collections.singletonList(DIALER_CALL_SCREEN_I18N_CONFIG_FLAG));
        }
    }
}