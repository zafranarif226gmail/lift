package com.lift.shared.lift.shell.command.environment;

import android.content.Context;
import android.content.pm.PackageInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lift.shared.android.PackageUtils;
import com.lift.shared.shell.command.environment.ShellEnvironmentUtils;
import com.lift.shared.lift.LiftConstants;
import com.lift.shared.lift.LiftUtils;

import java.util.HashMap;

/**
 * Environment for {@link LiftConstants#LIFT_API_PACKAGE_NAME} app.
 */
public class LiftAPIShellEnvironment {

    /** Environment variable prefix for the Lift:API app. */
    public static final String LIFT_API_APP_ENV_PREFIX = LiftConstants.LIFT_ENV_PREFIX_ROOT + "_API_APP__";

    /** Environment variable for the Lift:API app version. */
    public static final String ENV_LIFT_API_APP__VERSION_NAME = LIFT_API_APP_ENV_PREFIX + "VERSION_NAME";

    /** Get shell environment for Lift:API app. */
    @Nullable
    public static HashMap<String, String> getEnvironment(@NonNull Context currentPackageContext) {
        if (LiftUtils.isLiftAPIAppInstalled(currentPackageContext) != null) return null;

        String packageName = LiftConstants.LIFT_API_PACKAGE_NAME;
        PackageInfo packageInfo = PackageUtils.getPackageInfoForPackage(currentPackageContext, packageName);
        if (packageInfo == null) return null;

        HashMap<String, String> environment = new HashMap<>();

        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_LIFT_API_APP__VERSION_NAME, PackageUtils.getVersionNameForPackage(packageInfo));

        return environment;
    }

}
