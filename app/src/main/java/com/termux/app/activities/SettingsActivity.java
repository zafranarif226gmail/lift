package com.lift.app.activities;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.lift.R;
import com.lift.shared.activities.ReportActivity;
import com.lift.shared.file.FileUtils;
import com.lift.shared.models.ReportInfo;
import com.lift.app.models.UserAction;
import com.lift.shared.interact.ShareUtils;
import com.lift.shared.android.PackageUtils;
import com.lift.shared.lift.settings.preferences.LiftAPIAppSharedPreferences;
import com.lift.shared.lift.settings.preferences.LiftFloatAppSharedPreferences;
import com.lift.shared.lift.settings.preferences.LiftTaskerAppSharedPreferences;
import com.lift.shared.lift.settings.preferences.LiftWidgetAppSharedPreferences;
import com.lift.shared.android.AndroidUtils;
import com.lift.shared.lift.LiftConstants;
import com.lift.shared.lift.LiftUtils;
import com.lift.shared.activity.media.AppCompatActivityUtils;
import com.lift.shared.theme.NightMode;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCompatActivityUtils.setNightMode(this, NightMode.getAppNightMode().getName(), true);

        setContentView(R.layout.activity_settings);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new RootPreferencesFragment())
                .commit();
        }

        AppCompatActivityUtils.setToolbar(this, com.lift.shared.R.id.toolbar);
        AppCompatActivityUtils.setShowBackButtonInActionBar(this, true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public static class RootPreferencesFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            Context context = getContext();
            if (context == null) return;

            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            new Thread() {
                @Override
                public void run() {
                    configureLiftAPIPreference(context);
                    configureLiftFloatPreference(context);
                    configureLiftTaskerPreference(context);
                    configureLiftWidgetPreference(context);
                    configureAboutPreference(context);
                    configureDonatePreference(context);
                }
            }.start();
        }

        private void configureLiftAPIPreference(@NonNull Context context) {
            Preference liftAPIPreference = findPreference("lift_api");
            if (liftAPIPreference != null) {
                LiftAPIAppSharedPreferences preferences = LiftAPIAppSharedPreferences.build(context, false);
                // If failed to get app preferences, then likely app is not installed, so do not show its preference
                liftAPIPreference.setVisible(preferences != null);
            }
        }

        private void configureLiftFloatPreference(@NonNull Context context) {
            Preference liftFloatPreference = findPreference("lift_float");
            if (liftFloatPreference != null) {
                LiftFloatAppSharedPreferences preferences = LiftFloatAppSharedPreferences.build(context, false);
                // If failed to get app preferences, then likely app is not installed, so do not show its preference
                liftFloatPreference.setVisible(preferences != null);
            }
        }

        private void configureLiftTaskerPreference(@NonNull Context context) {
            Preference liftTaskerPreference = findPreference("lift_tasker");
            if (liftTaskerPreference != null) {
                LiftTaskerAppSharedPreferences preferences = LiftTaskerAppSharedPreferences.build(context, false);
                // If failed to get app preferences, then likely app is not installed, so do not show its preference
                liftTaskerPreference.setVisible(preferences != null);
            }
        }

        private void configureLiftWidgetPreference(@NonNull Context context) {
            Preference liftWidgetPreference = findPreference("lift_widget");
            if (liftWidgetPreference != null) {
                LiftWidgetAppSharedPreferences preferences = LiftWidgetAppSharedPreferences.build(context, false);
                // If failed to get app preferences, then likely app is not installed, so do not show its preference
                liftWidgetPreference.setVisible(preferences != null);
            }
        }

        private void configureAboutPreference(@NonNull Context context) {
            Preference aboutPreference = findPreference("about");
            if (aboutPreference != null) {
                aboutPreference.setOnPreferenceClickListener(preference -> {
                    new Thread() {
                        @Override
                        public void run() {
                            String title = "About";

                            StringBuilder aboutString = new StringBuilder();
                            aboutString.append(LiftUtils.getAppInfoMarkdownString(context, LiftUtils.AppInfoMode.LIFT_AND_PLUGIN_PACKAGES));
                            aboutString.append("\n\n").append(AndroidUtils.getDeviceInfoMarkdownString(context, true));
                            aboutString.append("\n\n").append(LiftUtils.getImportantLinksMarkdownString(context));

                            String userActionName = UserAction.ABOUT.getName();

                            ReportInfo reportInfo = new ReportInfo(userActionName,
                                LiftConstants.LIFT_APP.LIFT_SETTINGS_ACTIVITY_NAME, title);
                            reportInfo.setReportString(aboutString.toString());
                            reportInfo.setReportSaveFileLabelAndPath(userActionName,
                                Environment.getExternalStorageDirectory() + "/" +
                                    FileUtils.sanitizeFileName(LiftConstants.LIFT_APP_NAME + "-" + userActionName + ".log", true, true));

                            ReportActivity.startReportActivity(context, reportInfo);
                        }
                    }.start();

                    return true;
                });
            }
        }

        private void configureDonatePreference(@NonNull Context context) {
            Preference donatePreference = findPreference("donate");
            if (donatePreference != null) {
                String signingCertificateSHA256Digest = PackageUtils.getSigningCertificateSHA256DigestForPackage(context);
                if (signingCertificateSHA256Digest != null) {
                    // If APK is a Google Playstore release, then do not show the donation link
                    // since Lift isn't exempted from the playstore policy donation links restriction
                    // Check Fund solicitations: https://pay.google.com/intl/en_in/about/policy/
                    String apkRelease = LiftUtils.getAPKRelease(signingCertificateSHA256Digest);
                    if (apkRelease == null || apkRelease.equals(LiftConstants.APK_RELEASE_GOOGLE_PLAYSTORE_SIGNING_CERTIFICATE_SHA256_DIGEST)) {
                        donatePreference.setVisible(false);
                        return;
                    } else {
                        donatePreference.setVisible(true);
                    }
                }

                donatePreference.setOnPreferenceClickListener(preference -> {
                    ShareUtils.openUrl(context, LiftConstants.LIFT_DONATE_URL);
                    return true;
                });
            }
        }
    }

}
