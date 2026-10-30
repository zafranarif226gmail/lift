package com.lift.shared.lift.settings.preferences;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lift.shared.android.PackageUtils;
import com.lift.shared.settings.preferences.AppSharedPreferences;
import com.lift.shared.settings.preferences.SharedPreferenceUtils;
import com.lift.shared.lift.LiftConstants;
import com.lift.shared.lift.LiftUtils;
import com.lift.shared.lift.settings.preferences.LiftPreferenceConstants.LIFT_TASKER_APP;
import com.lift.shared.logger.Logger;

public class LiftTaskerAppSharedPreferences extends AppSharedPreferences {

    private static final String LOG_TAG = "LiftTaskerAppSharedPreferences";

    private  LiftTaskerAppSharedPreferences(@NonNull Context context) {
        super(context,
            SharedPreferenceUtils.getPrivateSharedPreferences(context,
                LiftConstants.LIFT_TASKER_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION),
            SharedPreferenceUtils.getPrivateAndMultiProcessSharedPreferences(context,
                LiftConstants.LIFT_TASKER_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION));
    }

    /**
     * Get {@link LiftTaskerAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link LiftConstants#LIFT_TASKER_PACKAGE_NAME}.
     * @return Returns the {@link LiftTaskerAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    @Nullable
    public static LiftTaskerAppSharedPreferences build(@NonNull final Context context) {
        Context liftTaskerPackageContext = PackageUtils.getContextForPackage(context, LiftConstants.LIFT_TASKER_PACKAGE_NAME);
        if (liftTaskerPackageContext == null)
            return null;
        else
            return new LiftTaskerAppSharedPreferences(liftTaskerPackageContext);
    }

    /**
     * Get {@link LiftTaskerAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link LiftConstants#LIFT_TASKER_PACKAGE_NAME}.
     * @param exitAppOnError If {@code true} and failed to get package context, then a dialog will
     *                       be shown which when dismissed will exit the app.
     * @return Returns the {@link LiftTaskerAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    public static  LiftTaskerAppSharedPreferences build(@NonNull final Context context, final boolean exitAppOnError) {
        Context liftTaskerPackageContext = LiftUtils.getContextForPackageOrExitApp(context, LiftConstants.LIFT_TASKER_PACKAGE_NAME, exitAppOnError);
        if (liftTaskerPackageContext == null)
            return null;
        else
            return new LiftTaskerAppSharedPreferences(liftTaskerPackageContext);
    }



    public int getLogLevel(boolean readFromFile) {
        if (readFromFile)
            return SharedPreferenceUtils.getInt(mMultiProcessSharedPreferences, LIFT_TASKER_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
        else
            return SharedPreferenceUtils.getInt(mSharedPreferences, LIFT_TASKER_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
    }

    public void setLogLevel(Context context, int logLevel, boolean commitToFile) {
        logLevel = Logger.setLogLevel(context, logLevel);
        SharedPreferenceUtils.setInt(mSharedPreferences, LIFT_TASKER_APP.KEY_LOG_LEVEL, logLevel, commitToFile);
    }



    public int getLastPendingIntentRequestCode() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, LIFT_TASKER_APP.KEY_LAST_PENDING_INTENT_REQUEST_CODE, LIFT_TASKER_APP.DEFAULT_VALUE_KEY_LAST_PENDING_INTENT_REQUEST_CODE);
    }

    public void setLastPendingIntentRequestCode(int lastPendingIntentRequestCode) {
        SharedPreferenceUtils.setInt(mSharedPreferences, LIFT_TASKER_APP.KEY_LAST_PENDING_INTENT_REQUEST_CODE, lastPendingIntentRequestCode, false);
    }

}
