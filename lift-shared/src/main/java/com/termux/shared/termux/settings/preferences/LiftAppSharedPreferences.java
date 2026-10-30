package com.lift.shared.lift.settings.preferences;

import android.content.Context;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lift.shared.android.PackageUtils;
import com.lift.shared.settings.preferences.AppSharedPreferences;
import com.lift.shared.settings.preferences.SharedPreferenceUtils;
import com.lift.shared.lift.LiftConstants;
import com.lift.shared.logger.Logger;
import com.lift.shared.data.DataUtils;
import com.lift.shared.lift.LiftUtils;
import com.lift.shared.lift.settings.preferences.LiftPreferenceConstants.LIFT_APP;

public class LiftAppSharedPreferences extends AppSharedPreferences {

    private int MIN_FONTSIZE;
    private int MAX_FONTSIZE;
    private int DEFAULT_FONTSIZE;

    private static final String LOG_TAG = "LiftAppSharedPreferences";

    private LiftAppSharedPreferences(@NonNull Context context) {
        super(context,
            SharedPreferenceUtils.getPrivateSharedPreferences(context,
                LiftConstants.LIFT_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION),
            SharedPreferenceUtils.getPrivateAndMultiProcessSharedPreferences(context,
                LiftConstants.LIFT_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION));

        setFontVariables(context);
    }

    /**
     * Get {@link LiftAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link LiftConstants#LIFT_PACKAGE_NAME}.
     * @return Returns the {@link LiftAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    @Nullable
    public static LiftAppSharedPreferences build(@NonNull final Context context) {
        Context liftPackageContext = PackageUtils.getContextForPackage(context, LiftConstants.LIFT_PACKAGE_NAME);
        if (liftPackageContext == null)
            return null;
        else
            return new LiftAppSharedPreferences(liftPackageContext);
    }

    /**
     * Get {@link LiftAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link LiftConstants#LIFT_PACKAGE_NAME}.
     * @param exitAppOnError If {@code true} and failed to get package context, then a dialog will
     *                       be shown which when dismissed will exit the app.
     * @return Returns the {@link LiftAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    public static LiftAppSharedPreferences build(@NonNull final Context context, final boolean exitAppOnError) {
        Context liftPackageContext = LiftUtils.getContextForPackageOrExitApp(context, LiftConstants.LIFT_PACKAGE_NAME, exitAppOnError);
        if (liftPackageContext == null)
            return null;
        else
            return new LiftAppSharedPreferences(liftPackageContext);
    }



    public boolean shouldShowTerminalToolbar() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, LIFT_APP.KEY_SHOW_TERMINAL_TOOLBAR, LIFT_APP.DEFAULT_VALUE_SHOW_TERMINAL_TOOLBAR);
    }

    public void setShowTerminalToolbar(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, LIFT_APP.KEY_SHOW_TERMINAL_TOOLBAR, value, false);
    }

    public boolean toogleShowTerminalToolbar() {
        boolean currentValue = shouldShowTerminalToolbar();
        setShowTerminalToolbar(!currentValue);
        return !currentValue;
    }



    public boolean isTerminalMarginAdjustmentEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, LIFT_APP.KEY_TERMINAL_MARGIN_ADJUSTMENT, LIFT_APP.DEFAULT_TERMINAL_MARGIN_ADJUSTMENT);
    }

    public void setTerminalMarginAdjustment(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, LIFT_APP.KEY_TERMINAL_MARGIN_ADJUSTMENT, value, false);
    }



    public boolean isSoftKeyboardEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, LIFT_APP.KEY_SOFT_KEYBOARD_ENABLED, LIFT_APP.DEFAULT_VALUE_KEY_SOFT_KEYBOARD_ENABLED);
    }

    public void setSoftKeyboardEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, LIFT_APP.KEY_SOFT_KEYBOARD_ENABLED, value, false);
    }

    public boolean isSoftKeyboardEnabledOnlyIfNoHardware() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, LIFT_APP.KEY_SOFT_KEYBOARD_ENABLED_ONLY_IF_NO_HARDWARE, LIFT_APP.DEFAULT_VALUE_KEY_SOFT_KEYBOARD_ENABLED_ONLY_IF_NO_HARDWARE);
    }

    public void setSoftKeyboardEnabledOnlyIfNoHardware(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, LIFT_APP.KEY_SOFT_KEYBOARD_ENABLED_ONLY_IF_NO_HARDWARE, value, false);
    }



    public boolean shouldKeepScreenOn() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, LIFT_APP.KEY_KEEP_SCREEN_ON, LIFT_APP.DEFAULT_VALUE_KEEP_SCREEN_ON);
    }

    public void setKeepScreenOn(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, LIFT_APP.KEY_KEEP_SCREEN_ON, value, false);
    }



    public static int[] getDefaultFontSizes(Context context) {
        float dipInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, context.getResources().getDisplayMetrics());

        int[] sizes = new int[3];

        // This is a bit arbitrary and sub-optimal. We want to give a sensible default for minimum font size
        // to prevent invisible text due to zoom be mistake:
        sizes[1] = (int) (4f * dipInPixels); // min

        // http://www.google.com/design/spec/style/typography.html#typography-line-height
        int defaultFontSize = Math.round(12 * dipInPixels);
        // Make it divisible by 2 since that is the minimal adjustment step:
        if (defaultFontSize % 2 == 1) defaultFontSize--;

        sizes[0] = defaultFontSize; // default

        sizes[2] = 256; // max

        return sizes;
    }

    public void setFontVariables(Context context) {
        int[] sizes = getDefaultFontSizes(context);

        DEFAULT_FONTSIZE = sizes[0];
        MIN_FONTSIZE = sizes[1];
        MAX_FONTSIZE = sizes[2];
    }

    public int getFontSize() {
        int fontSize = SharedPreferenceUtils.getIntStoredAsString(mSharedPreferences, LIFT_APP.KEY_FONTSIZE, DEFAULT_FONTSIZE);
        return DataUtils.clamp(fontSize, MIN_FONTSIZE, MAX_FONTSIZE);
    }

    public void setFontSize(int value) {
        SharedPreferenceUtils.setIntStoredAsString(mSharedPreferences, LIFT_APP.KEY_FONTSIZE, value, false);
    }

    public void changeFontSize(boolean increase) {
        int fontSize = getFontSize();

        fontSize += (increase ? 1 : -1) * 2;
        fontSize = Math.max(MIN_FONTSIZE, Math.min(fontSize, MAX_FONTSIZE));

        setFontSize(fontSize);
    }



    public String getCurrentSession() {
        return SharedPreferenceUtils.getString(mSharedPreferences, LIFT_APP.KEY_CURRENT_SESSION, null, true);
    }

    public void setCurrentSession(String value) {
        SharedPreferenceUtils.setString(mSharedPreferences, LIFT_APP.KEY_CURRENT_SESSION, value, false);
    }



    public int getLogLevel() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, LIFT_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
    }

    public void setLogLevel(Context context, int logLevel) {
        logLevel = Logger.setLogLevel(context, logLevel);
        SharedPreferenceUtils.setInt(mSharedPreferences, LIFT_APP.KEY_LOG_LEVEL, logLevel, false);
    }



    public int getLastNotificationId() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, LIFT_APP.KEY_LAST_NOTIFICATION_ID, LIFT_APP.DEFAULT_VALUE_KEY_LAST_NOTIFICATION_ID);
    }

    public void setLastNotificationId(int notificationId) {
        SharedPreferenceUtils.setInt(mSharedPreferences, LIFT_APP.KEY_LAST_NOTIFICATION_ID, notificationId, false);
    }


    public synchronized int getAndIncrementAppShellNumberSinceBoot() {
        // Keep value at MAX_VALUE on integer overflow and not 0, since not first shell
        return SharedPreferenceUtils.getAndIncrementInt(mSharedPreferences, LIFT_APP.KEY_APP_SHELL_NUMBER_SINCE_BOOT,
            LIFT_APP.DEFAULT_VALUE_APP_SHELL_NUMBER_SINCE_BOOT, true, Integer.MAX_VALUE);
    }

    public synchronized void resetAppShellNumberSinceBoot() {
        SharedPreferenceUtils.setInt(mSharedPreferences, LIFT_APP.KEY_APP_SHELL_NUMBER_SINCE_BOOT,
            LIFT_APP.DEFAULT_VALUE_APP_SHELL_NUMBER_SINCE_BOOT, true);
    }

    public synchronized int getAndIncrementTerminalSessionNumberSinceBoot() {
        // Keep value at MAX_VALUE on integer overflow and not 0, since not first shell
        return SharedPreferenceUtils.getAndIncrementInt(mSharedPreferences, LIFT_APP.KEY_TERMINAL_SESSION_NUMBER_SINCE_BOOT,
            LIFT_APP.DEFAULT_VALUE_TERMINAL_SESSION_NUMBER_SINCE_BOOT, true, Integer.MAX_VALUE);
    }

    public synchronized void resetTerminalSessionNumberSinceBoot() {
        SharedPreferenceUtils.setInt(mSharedPreferences, LIFT_APP.KEY_TERMINAL_SESSION_NUMBER_SINCE_BOOT,
            LIFT_APP.DEFAULT_VALUE_TERMINAL_SESSION_NUMBER_SINCE_BOOT, true);
    }


    public boolean isTerminalViewKeyLoggingEnabled() {
        return SharedPreferenceUtils.getBoolean(mSharedPreferences, LIFT_APP.KEY_TERMINAL_VIEW_KEY_LOGGING_ENABLED, LIFT_APP.DEFAULT_VALUE_TERMINAL_VIEW_KEY_LOGGING_ENABLED);
    }

    public void setTerminalViewKeyLoggingEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, LIFT_APP.KEY_TERMINAL_VIEW_KEY_LOGGING_ENABLED, value, false);
    }



    public boolean arePluginErrorNotificationsEnabled(boolean readFromFile) {
        if (readFromFile)
            return SharedPreferenceUtils.getBoolean(mMultiProcessSharedPreferences, LIFT_APP.KEY_PLUGIN_ERROR_NOTIFICATIONS_ENABLED, LIFT_APP.DEFAULT_VALUE_PLUGIN_ERROR_NOTIFICATIONS_ENABLED);
        else
            return SharedPreferenceUtils.getBoolean(mSharedPreferences, LIFT_APP.KEY_PLUGIN_ERROR_NOTIFICATIONS_ENABLED, LIFT_APP.DEFAULT_VALUE_PLUGIN_ERROR_NOTIFICATIONS_ENABLED);
    }

    public void setPluginErrorNotificationsEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, LIFT_APP.KEY_PLUGIN_ERROR_NOTIFICATIONS_ENABLED, value, false);
    }



    public boolean areCrashReportNotificationsEnabled(boolean readFromFile) {
        if (readFromFile)
            return SharedPreferenceUtils.getBoolean(mMultiProcessSharedPreferences, LIFT_APP.KEY_CRASH_REPORT_NOTIFICATIONS_ENABLED, LIFT_APP.DEFAULT_VALUE_CRASH_REPORT_NOTIFICATIONS_ENABLED);
       else
            return SharedPreferenceUtils.getBoolean(mSharedPreferences, LIFT_APP.KEY_CRASH_REPORT_NOTIFICATIONS_ENABLED, LIFT_APP.DEFAULT_VALUE_CRASH_REPORT_NOTIFICATIONS_ENABLED);
    }

    public void setCrashReportNotificationsEnabled(boolean value) {
        SharedPreferenceUtils.setBoolean(mSharedPreferences, LIFT_APP.KEY_CRASH_REPORT_NOTIFICATIONS_ENABLED, value, false);
    }

}
