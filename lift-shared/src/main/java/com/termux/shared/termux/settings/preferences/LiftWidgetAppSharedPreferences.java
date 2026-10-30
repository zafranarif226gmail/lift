package com.lift.shared.lift.settings.preferences;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lift.shared.logger.Logger;
import com.lift.shared.android.PackageUtils;
import com.lift.shared.settings.preferences.AppSharedPreferences;
import com.lift.shared.settings.preferences.SharedPreferenceUtils;
import com.lift.shared.lift.LiftUtils;
import com.lift.shared.lift.settings.preferences.LiftPreferenceConstants.LIFT_WIDGET_APP;
import com.lift.shared.lift.LiftConstants;

import java.util.UUID;

public class LiftWidgetAppSharedPreferences extends AppSharedPreferences {

    private static final String LOG_TAG = "LiftWidgetAppSharedPreferences";

    private LiftWidgetAppSharedPreferences(@NonNull Context context) {
        super(context,
            SharedPreferenceUtils.getPrivateSharedPreferences(context,
                LiftConstants.LIFT_WIDGET_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION),
            SharedPreferenceUtils.getPrivateAndMultiProcessSharedPreferences(context,
                LiftConstants.LIFT_WIDGET_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION));
    }

    /**
     * Get {@link LiftWidgetAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link LiftConstants#LIFT_WIDGET_PACKAGE_NAME}.
     * @return Returns the {@link LiftWidgetAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    @Nullable
    public static LiftWidgetAppSharedPreferences build(@NonNull final Context context) {
        Context liftWidgetPackageContext = PackageUtils.getContextForPackage(context, LiftConstants.LIFT_WIDGET_PACKAGE_NAME);
        if (liftWidgetPackageContext == null)
            return null;
        else
            return new LiftWidgetAppSharedPreferences(liftWidgetPackageContext);
    }

    /**
     * Get the {@link LiftWidgetAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link LiftConstants#LIFT_WIDGET_PACKAGE_NAME}.
     * @param exitAppOnError If {@code true} and failed to get package context, then a dialog will
     *                       be shown which when dismissed will exit the app.
     * @return Returns the {@link LiftWidgetAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    public static LiftWidgetAppSharedPreferences build(@NonNull final Context context, final boolean exitAppOnError) {
        Context liftWidgetPackageContext = LiftUtils.getContextForPackageOrExitApp(context, LiftConstants.LIFT_WIDGET_PACKAGE_NAME, exitAppOnError);
        if (liftWidgetPackageContext == null)
            return null;
        else
            return new LiftWidgetAppSharedPreferences(liftWidgetPackageContext);
    }



    public static String getGeneratedToken(@NonNull Context context) {
        LiftWidgetAppSharedPreferences preferences = LiftWidgetAppSharedPreferences.build(context, true);
        if (preferences == null) return null;
        return preferences.getGeneratedToken();
    }

    public String getGeneratedToken() {
        String token =  SharedPreferenceUtils.getString(mSharedPreferences, LIFT_WIDGET_APP.KEY_TOKEN, null, true);
        if (token == null) {
            token = UUID.randomUUID().toString();
            SharedPreferenceUtils.setString(mSharedPreferences, LIFT_WIDGET_APP.KEY_TOKEN, token, true);
        }
        return token;
    }



    public int getLogLevel(boolean readFromFile) {
        if (readFromFile)
            return SharedPreferenceUtils.getInt(mMultiProcessSharedPreferences, LIFT_WIDGET_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
        else
            return SharedPreferenceUtils.getInt(mSharedPreferences, LIFT_WIDGET_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
    }

    public void setLogLevel(Context context, int logLevel, boolean commitToFile) {
        logLevel = Logger.setLogLevel(context, logLevel);
        SharedPreferenceUtils.setInt(mSharedPreferences, LIFT_WIDGET_APP.KEY_LOG_LEVEL, logLevel, commitToFile);
    }

}
