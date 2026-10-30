package com.lift.shared.lift.shell.command.environment;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lift.shared.android.PackageUtils;
import com.lift.shared.android.SELinuxUtils;
import com.lift.shared.data.DataUtils;
import com.lift.shared.shell.command.environment.ShellEnvironmentUtils;
import com.lift.shared.lift.LiftBootstrap;
import com.lift.shared.lift.LiftConstants;
import com.lift.shared.lift.LiftUtils;
import com.lift.shared.lift.shell.am.LiftAmSocketServer;

import java.util.HashMap;

/**
 * Environment for {@link LiftConstants#LIFT_PACKAGE_NAME} app.
 */
public class LiftAppShellEnvironment {

    /** Lift app environment variables. */
    public static HashMap<String, String> liftAppEnvironment;

    /** Environment variable for the Lift app version. */
    public static final String ENV_LIFT_VERSION = LiftConstants.LIFT_ENV_PREFIX_ROOT + "_VERSION";

    /** Environment variable prefix for the Lift app. */
    public static final String LIFT_APP_ENV_PREFIX = LiftConstants.LIFT_ENV_PREFIX_ROOT + "_APP__";

    /** Environment variable for the Lift app version name. */
    public static final String ENV_LIFT_APP__VERSION_NAME = LIFT_APP_ENV_PREFIX + "VERSION_NAME";
    /** Environment variable for the Lift app version code. */
    public static final String ENV_LIFT_APP__VERSION_CODE = LIFT_APP_ENV_PREFIX + "VERSION_CODE";
    /** Environment variable for the Lift app package name. */
    public static final String ENV_LIFT_APP__PACKAGE_NAME = LIFT_APP_ENV_PREFIX + "PACKAGE_NAME";
    /** Environment variable for the Lift app process id. */
    public static final String ENV_LIFT_APP__PID = LIFT_APP_ENV_PREFIX + "PID";
    /** Environment variable for the Lift app uid. */
    public static final String ENV_LIFT_APP__UID = LIFT_APP_ENV_PREFIX + "UID";
    /** Environment variable for the Lift app targetSdkVersion. */
    public static final String ENV_LIFT_APP__TARGET_SDK = LIFT_APP_ENV_PREFIX + "TARGET_SDK";
    /** Environment variable for the Lift app is debuggable apk build. */
    public static final String ENV_LIFT_APP__IS_DEBUGGABLE_BUILD = LIFT_APP_ENV_PREFIX + "IS_DEBUGGABLE_BUILD";
    /** Environment variable for the Lift app {@link LiftConstants} APK_RELEASE_*. */
    public static final String ENV_LIFT_APP__APK_RELEASE = LIFT_APP_ENV_PREFIX + "APK_RELEASE";
    /** Environment variable for the Lift app install path. */
    public static final String ENV_LIFT_APP__APK_PATH = LIFT_APP_ENV_PREFIX + "APK_PATH";
    /** Environment variable for the Lift app is installed on external/portable storage. */
    public static final String ENV_LIFT_APP__IS_INSTALLED_ON_EXTERNAL_STORAGE = LIFT_APP_ENV_PREFIX + "IS_INSTALLED_ON_EXTERNAL_STORAGE";

    /** Environment variable for the Lift app process selinux context. */
    public static final String ENV_LIFT_APP__SE_PROCESS_CONTEXT = LIFT_APP_ENV_PREFIX + "SE_PROCESS_CONTEXT";
    /** Environment variable for the Lift app data files selinux context. */
    public static final String ENV_LIFT_APP__SE_FILE_CONTEXT = LIFT_APP_ENV_PREFIX + "SE_FILE_CONTEXT";
    /** Environment variable for the Lift app seInfo tag found in selinux policy used to set app process and app data files selinux context. */
    public static final String ENV_LIFT_APP__SE_INFO = LIFT_APP_ENV_PREFIX + "SE_INFO";
    /** Environment variable for the Lift app user id. */
    public static final String ENV_LIFT_APP__USER_ID = LIFT_APP_ENV_PREFIX + "USER_ID";
    /** Environment variable for the Lift app profile owner. */
    public static final String ENV_LIFT_APP__PROFILE_OWNER = LIFT_APP_ENV_PREFIX + "PROFILE_OWNER";

    /** Environment variable for the Lift app {@link LiftBootstrap#LIFT_APP_PACKAGE_MANAGER}. */
    public static final String ENV_LIFT_APP__PACKAGE_MANAGER = LIFT_APP_ENV_PREFIX + "PACKAGE_MANAGER";
    /** Environment variable for the Lift app {@link LiftBootstrap#LIFT_APP_PACKAGE_VARIANT}. */
    public static final String ENV_LIFT_APP__PACKAGE_VARIANT = LIFT_APP_ENV_PREFIX + "PACKAGE_VARIANT";
    /** Environment variable for the Lift app files directory. */
    public static final String ENV_LIFT_APP__FILES_DIR = LIFT_APP_ENV_PREFIX + "FILES_DIR";


    /** Environment variable for the Lift app {@link LiftAmSocketServer#getLiftAppAMSocketServerEnabled(Context)}. */
    public static final String ENV_LIFT_APP__AM_SOCKET_SERVER_ENABLED = LIFT_APP_ENV_PREFIX + "AM_SOCKET_SERVER_ENABLED";



    /** Get shell environment for Lift app. */
    @Nullable
    public static HashMap<String, String> getEnvironment(@NonNull Context currentPackageContext) {
        setLiftAppEnvironment(currentPackageContext);
        return liftAppEnvironment;
    }

    /** Set Lift app environment variables in {@link #liftAppEnvironment}. */
    public synchronized static void setLiftAppEnvironment(@NonNull Context currentPackageContext) {
        boolean isLiftApp = LiftConstants.LIFT_PACKAGE_NAME.equals(currentPackageContext.getPackageName());

        // If current package context is of lift app and its environment is already set, then no need to set again since it won't change
        // Other apps should always set environment again since lift app may be installed/updated/deleted in background
        if (liftAppEnvironment != null && isLiftApp)
            return;

        liftAppEnvironment = null;

        String packageName = LiftConstants.LIFT_PACKAGE_NAME;
        PackageInfo packageInfo = PackageUtils.getPackageInfoForPackage(currentPackageContext, packageName);
        if (packageInfo == null) return;
        ApplicationInfo applicationInfo = PackageUtils.getApplicationInfoForPackage(currentPackageContext, packageName);
        if (applicationInfo == null || !applicationInfo.enabled) return;

        HashMap<String, String> environment = new HashMap<>();

        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_LIFT_VERSION, PackageUtils.getVersionNameForPackage(packageInfo));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_LIFT_APP__VERSION_NAME, PackageUtils.getVersionNameForPackage(packageInfo));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_LIFT_APP__VERSION_CODE, String.valueOf(PackageUtils.getVersionCodeForPackage(packageInfo)));

        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_LIFT_APP__PACKAGE_NAME, packageName);
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_LIFT_APP__PID, LiftUtils.getLiftAppPID(currentPackageContext));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_LIFT_APP__UID, String.valueOf(PackageUtils.getUidForPackage(applicationInfo)));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_LIFT_APP__TARGET_SDK, String.valueOf(PackageUtils.getTargetSDKForPackage(applicationInfo)));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_LIFT_APP__IS_DEBUGGABLE_BUILD, PackageUtils.isAppForPackageADebuggableBuild(applicationInfo));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_LIFT_APP__APK_PATH, PackageUtils.getBaseAPKPathForPackage(applicationInfo));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_LIFT_APP__IS_INSTALLED_ON_EXTERNAL_STORAGE, PackageUtils.isAppInstalledOnExternalStorage(applicationInfo));

        putLiftAPKSignature(currentPackageContext, environment);

        Context liftPackageContext = LiftUtils.getLiftPackageContext(currentPackageContext);
        if (liftPackageContext != null) {
            // An app that does not have the same sharedUserId as lift app will not be able to get
            // get lift context's classloader to get BuildConfig.LIFT_PACKAGE_VARIANT via reflection.
            // Check LiftBootstrap.setLiftPackageManagerAndVariantFromLiftApp()
            if (LiftBootstrap.LIFT_APP_PACKAGE_MANAGER != null)
                environment.put(ENV_LIFT_APP__PACKAGE_MANAGER, LiftBootstrap.LIFT_APP_PACKAGE_MANAGER.getName());
            if (LiftBootstrap.LIFT_APP_PACKAGE_VARIANT != null)
                environment.put(ENV_LIFT_APP__PACKAGE_VARIANT, LiftBootstrap.LIFT_APP_PACKAGE_VARIANT.getName());

            // Will not be set for plugins
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_LIFT_APP__AM_SOCKET_SERVER_ENABLED,
                LiftAmSocketServer.getLiftAppAMSocketServerEnabled(currentPackageContext));

            String filesDirPath = currentPackageContext.getFilesDir().getAbsolutePath();
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_LIFT_APP__FILES_DIR, filesDirPath);

            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_LIFT_APP__SE_PROCESS_CONTEXT, SELinuxUtils.getContext());
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_LIFT_APP__SE_FILE_CONTEXT, SELinuxUtils.getFileContext(filesDirPath));

            String seInfoUser = PackageUtils.getApplicationInfoSeInfoUserForPackage(applicationInfo);
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_LIFT_APP__SE_INFO, PackageUtils.getApplicationInfoSeInfoForPackage(applicationInfo) +
                (DataUtils.isNullOrEmpty(seInfoUser) ? "" : seInfoUser));

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_LIFT_APP__USER_ID, String.valueOf(PackageUtils.getUserIdForPackage(currentPackageContext)));
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_LIFT_APP__PROFILE_OWNER, PackageUtils.getProfileOwnerPackageNameForUser(currentPackageContext));
        }

        liftAppEnvironment = environment;
    }

    /** Put {@link #ENV_LIFT_APP__APK_RELEASE} in {@code environment}. */
    public static void putLiftAPKSignature(@NonNull Context currentPackageContext,
                                             @NonNull HashMap<String, String> environment) {
        String signingCertificateSHA256Digest = PackageUtils.getSigningCertificateSHA256DigestForPackage(currentPackageContext,
            LiftConstants.LIFT_PACKAGE_NAME);
        if (signingCertificateSHA256Digest != null) {
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_LIFT_APP__APK_RELEASE,
                LiftUtils.getAPKRelease(signingCertificateSHA256Digest).replaceAll("[^a-zA-Z]", "_").toUpperCase());
        }
    }

    /** Update {@link #ENV_LIFT_APP__AM_SOCKET_SERVER_ENABLED} value in {@code environment}. */
    public synchronized static void updateLiftAppAMSocketServerEnabled(@NonNull Context currentPackageContext) {
        if (liftAppEnvironment == null) return;
        liftAppEnvironment.remove(ENV_LIFT_APP__AM_SOCKET_SERVER_ENABLED);
        ShellEnvironmentUtils.putToEnvIfSet(liftAppEnvironment, ENV_LIFT_APP__AM_SOCKET_SERVER_ENABLED,
            LiftAmSocketServer.getLiftAppAMSocketServerEnabled(currentPackageContext));
    }

}
