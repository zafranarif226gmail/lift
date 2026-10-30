package com.lift.shared.lift.settings.preferences;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lift.shared.logger.Logger;
import com.lift.shared.android.PackageUtils;
import com.lift.shared.settings.preferences.AppSharedPreferences;
import com.lift.shared.settings.preferences.SharedPreferenceUtils;
import com.lift.shared.lift.LiftUtils;
import com.lift.shared.lift.settings.preferences.LiftPreferenceConstants.LIFT_API_APP;
import com.lift.shared.lift.LiftConstants;

public class LiftAPIAppSharedPreferences extends AppSharedPreferences {

    private static final String LOG_TAG = "LiftAPIAppSharedPreferences";

    private LiftAPIAppSharedPreferences(@NonNull Context context) {
        super(context,
            SharedPreferenceUtils.getPrivateSharedPreferences(context,
                LiftConstants.LIFT_API_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION),
            SharedPreferenceUtils.getPrivateAndMultiProcessSharedPreferences(context,
                LiftConstants.LIFT_API_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION));
    }

    /**
     * Get {@link LiftAPIAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link LiftConstants#LIFT_API_PACKAGE_NAME}.
     * @return Returns the {@link LiftAPIAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    @Nullable
    public static LiftAPIAppSharedPreferences build(@NonNull final Context context) {
        Context liftAPIPackageContext = PackageUtils.getContextForPackage(context, LiftConstants.LIFT_API_PACKAGE_NAME);
        if (liftAPIPackageContext == null)
            return null;
        else
            return new LiftAPIAppSharedPreferences(liftAPIPackageContext);
    }

    /**
     * Get {@link LiftAPIAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link LiftConstants#LIFT_API_PACKAGE_NAME}.
     * @param exitAppOnError If {@code true} and failed to get package context, then a dialog will
     *                       be shown which when dismissed will exit the app.
     * @return Returns the {@link LiftAPIAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    public static LiftAPIAppSharedPreferences build(@NonNull final Context context, final boolean exitAppOnError) {
        Context liftAPIPackageContext = LiftUtils.getContextForPackageOrExitApp(context, LiftConstants.LIFT_API_PACKAGE_NAME, exitAppOnError);
        if (liftAPIPackageContext == null)
            return null;
        else
            return new LiftAPIAppSharedPreferences(liftAPIPackageContext);
    }



    public int getLogLevel(boolean readFromFile) {
        if (readFromFile)
            return SharedPreferenceUtils.getInt(mMultiProcessSharedPreferences, LIFT_API_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
        else
            return SharedPreferenceUtils.getInt(mSharedPreferences, LIFT_API_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
    }

    public void setLogLevel(Context context, int logLevel, boolean commitToFile) {
        logLevel = Logger.setLogLevel(context, logLevel);
        SharedPreferenceUtils.setInt(mSharedPreferences, LIFT_API_APP.KEY_LOG_LEVEL, logLevel, commitToFile);
    }


    public int getLastPendingIntentRequestCode() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, LIFT_API_APP.KEY_LAST_PENDING_INTENT_REQUEST_CODE, LIFT_API_APP.DEFAULT_VALUE_KEY_LAST_PENDING_INTENT_REQUEST_CODE);
    }

    public void setLastPendingIntentRequestCode(int lastPendingIntentRequestCode) {
        SharedPreferenceUtils.setInt(mSharedPreferences, LIFT_API_APP.KEY_LAST_PENDING_INTENT_REQUEST_CODE, lastPendingIntentRequestCode, true);
    }

}
