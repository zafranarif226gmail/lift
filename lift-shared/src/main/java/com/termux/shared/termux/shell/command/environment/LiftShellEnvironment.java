package com.lift.shared.lift.shell.command.environment;

import android.content.Context;

import androidx.annotation.NonNull;

import com.lift.shared.errors.Error;
import com.lift.shared.file.FileUtils;
import com.lift.shared.logger.Logger;
import com.lift.shared.shell.command.ExecutionCommand;
import com.lift.shared.shell.command.environment.AndroidShellEnvironment;
import com.lift.shared.shell.command.environment.ShellEnvironmentUtils;
import com.lift.shared.shell.command.environment.ShellCommandShellEnvironment;
import com.lift.shared.lift.LiftBootstrap;
import com.lift.shared.lift.LiftConstants;
import com.lift.shared.lift.shell.LiftShellUtils;

import java.nio.charset.Charset;
import java.util.HashMap;

/**
 * Environment for Lift.
 */
public class LiftShellEnvironment extends AndroidShellEnvironment {

    private static final String LOG_TAG = "LiftShellEnvironment";

    /** Environment variable for the lift {@link LiftConstants#LIFT_PREFIX_DIR_PATH}. */
    public static final String ENV_PREFIX = "PREFIX";

    public LiftShellEnvironment() {
        super();
        shellCommandShellEnvironment = new LiftShellCommandShellEnvironment();
    }


    /** Init {@link LiftShellEnvironment} constants and caches. */
    public synchronized static void init(@NonNull Context currentPackageContext) {
        LiftAppShellEnvironment.setLiftAppEnvironment(currentPackageContext);
    }

    /** Init {@link LiftShellEnvironment} constants and caches. */
    public synchronized static void writeEnvironmentToFile(@NonNull Context currentPackageContext) {
        HashMap<String, String> environmentMap = new LiftShellEnvironment().getEnvironment(currentPackageContext, false);
        String environmentString = ShellEnvironmentUtils.convertEnvironmentToDotEnvFile(environmentMap);

        // Write environment string to temp file and then move to final location since otherwise
        // writing may happen while file is being sourced/read
        Error error = FileUtils.writeTextToFile("lift.env.tmp", LiftConstants.LIFT_ENV_TEMP_FILE_PATH,
            Charset.defaultCharset(), environmentString, false);
        if (error != null) {
            Logger.logErrorExtended(LOG_TAG, error.toString());
            return;
        }

        error = FileUtils.moveRegularFile("lift.env.tmp", LiftConstants.LIFT_ENV_TEMP_FILE_PATH, LiftConstants.LIFT_ENV_FILE_PATH, true);
        if (error != null) {
            Logger.logErrorExtended(LOG_TAG, error.toString());
        }
    }

    /** Get shell environment for Lift. */
    @NonNull
    @Override
    public HashMap<String, String> getEnvironment(@NonNull Context currentPackageContext, boolean isFailSafe) {

        // Lift environment builds upon the Android environment
        HashMap<String, String> environment = super.getEnvironment(currentPackageContext, isFailSafe);

        HashMap<String, String> liftAppEnvironment = LiftAppShellEnvironment.getEnvironment(currentPackageContext);
        if (liftAppEnvironment != null)
            environment.putAll(liftAppEnvironment);

        HashMap<String, String> liftApiAppEnvironment = LiftAPIShellEnvironment.getEnvironment(currentPackageContext);
        if (liftApiAppEnvironment != null)
            environment.putAll(liftApiAppEnvironment);

        environment.put(ENV_HOME, LiftConstants.LIFT_HOME_DIR_PATH);
        environment.put(ENV_PREFIX, LiftConstants.LIFT_PREFIX_DIR_PATH);

        // If failsafe is not enabled, then we keep default PATH and TMPDIR so that system binaries can be used
        if (!isFailSafe) {
            environment.put(ENV_TMPDIR, LiftConstants.LIFT_TMP_PREFIX_DIR_PATH);
            if (LiftBootstrap.isAppPackageVariantAPTAndroid5()) {
                // Lift in android 5/6 era shipped busybox binaries in applets directory
                environment.put(ENV_PATH, LiftConstants.LIFT_BIN_PREFIX_DIR_PATH + ":" + LiftConstants.LIFT_BIN_PREFIX_DIR_PATH + "/applets");
                environment.put(ENV_LD_LIBRARY_PATH, LiftConstants.LIFT_LIB_PREFIX_DIR_PATH);
            } else {
                // Lift binaries on Android 7+ rely on DT_RUNPATH, so LD_LIBRARY_PATH should be unset by default
                environment.put(ENV_PATH, LiftConstants.LIFT_BIN_PREFIX_DIR_PATH);
                environment.remove(ENV_LD_LIBRARY_PATH);
            }
        }

        return environment;
    }


    @NonNull
    @Override
    public String getDefaultWorkingDirectoryPath() {
        return LiftConstants.LIFT_HOME_DIR_PATH;
    }

    @NonNull
    @Override
    public String getDefaultBinPath() {
        return LiftConstants.LIFT_BIN_PREFIX_DIR_PATH;
    }

    @NonNull
    @Override
    public String[] setupShellCommandArguments(@NonNull String executable, String[] arguments) {
        return LiftShellUtils.setupShellCommandArguments(executable, arguments);
    }

}
