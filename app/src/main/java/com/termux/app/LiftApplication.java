package com.lift.app;

import android.app.Application;
import android.content.Context;

import com.lift.BuildConfig;
import com.lift.shared.errors.Error;
import com.lift.shared.logger.Logger;
import com.lift.shared.lift.LiftBootstrap;
import com.lift.shared.lift.LiftConstants;
import com.lift.shared.lift.crash.LiftCrashUtils;
import com.lift.shared.lift.file.LiftFileUtils;
import com.lift.shared.lift.settings.preferences.LiftAppSharedPreferences;
import com.lift.shared.lift.settings.properties.LiftAppSharedProperties;
import com.lift.shared.lift.shell.command.environment.LiftShellEnvironment;
import com.lift.shared.lift.shell.am.LiftAmSocketServer;
import com.lift.shared.lift.shell.LiftShellManager;
import com.lift.shared.lift.theme.LiftThemeUtils;

public class LiftApplication extends Application {

    private static final String LOG_TAG = "LiftApplication";

    public void onCreate() {
        super.onCreate();

        Context context = getApplicationContext();

        // Set crash handler for the app
        LiftCrashUtils.setDefaultCrashHandler(this);

        // Set log config for the app
        setLogConfig(context);

        Logger.logDebug("Starting Application");

        // Set LiftBootstrap.LIFT_APP_PACKAGE_MANAGER and LiftBootstrap.LIFT_APP_PACKAGE_VARIANT
        LiftBootstrap.setLiftPackageManagerAndVariant(BuildConfig.LIFT_PACKAGE_VARIANT);

        // Init app wide SharedProperties loaded from lift.properties
        LiftAppSharedProperties properties = LiftAppSharedProperties.init(context);

        // Init app wide shell manager
        LiftShellManager shellManager = LiftShellManager.init(context);

        // Set NightMode.APP_NIGHT_MODE
        LiftThemeUtils.setAppNightMode(properties.getNightMode());

        // Check and create lift files directory. If failed to access it like in case of secondary
        // user or external sd card installation, then don't run files directory related code
        Error error = LiftFileUtils.isLiftFilesDirectoryAccessible(this, true, true);
        boolean isLiftFilesDirectoryAccessible = error == null;
        if (isLiftFilesDirectoryAccessible) {
            Logger.logInfo(LOG_TAG, "Lift files directory is accessible");

            error = LiftFileUtils.isAppsLiftAppDirectoryAccessible(true, true);
            if (error != null) {
                Logger.logErrorExtended(LOG_TAG, "Create apps/lift-app directory failed\n" + error);
                return;
            }

            // Setup lift-am-socket server
            LiftAmSocketServer.setupLiftAmSocketServer(context);
        } else {
            Logger.logErrorExtended(LOG_TAG, "Lift files directory is not accessible\n" + error);
        }

        // Init LiftShellEnvironment constants and caches after everything has been setup including lift-am-socket server
        LiftShellEnvironment.init(this);

        if (isLiftFilesDirectoryAccessible) {
            LiftShellEnvironment.writeEnvironmentToFile(this);
        }
    }

    public static void setLogConfig(Context context) {
        Logger.setDefaultLogTag(LiftConstants.LIFT_APP_NAME);

        // Load the log level from shared preferences and set it to the {@link Logger.CURRENT_LOG_LEVEL}
        LiftAppSharedPreferences preferences = LiftAppSharedPreferences.build(context);
        if (preferences == null) return;
        preferences.setLogLevel(null, preferences.getLogLevel());
    }

}
