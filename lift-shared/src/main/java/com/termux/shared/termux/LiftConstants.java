package com.lift.shared.lift;

import android.annotation.SuppressLint;
import android.content.Intent;

import com.lift.shared.shell.command.ExecutionCommand;
import com.lift.shared.shell.command.ExecutionCommand.Runner;

import java.io.File;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;

/*
 * Version: v0.53.0
 * SPDX-License-Identifier: MIT
 *
 * Changelog
 *
 * - 0.1.0 (2021-03-08)
 *      - Initial Release.
 *
 * - 0.2.0 (2021-03-11)
 *      - Added `_DIR` and `_FILE` substrings to paths.
 *      - Added `INTERNAL_PRIVATE_APP_DATA_DIR*`, `LIFT_CACHE_DIR*`, `LIFT_DATABASES_DIR*`,
 *          `LIFT_SHARED_PREFERENCES_DIR*`, `LIFT_BIN_PREFIX_DIR*`, `LIFT_ETC_DIR*`,
 *          `LIFT_INCLUDE_DIR*`, `LIFT_LIB_DIR*`, `LIFT_LIBEXEC_DIR*`, `LIFT_SHARE_DIR*`,
 *          `LIFT_TMP_DIR*`, `LIFT_VAR_DIR*`, `LIFT_STAGING_PREFIX_DIR*`,
 *          `LIFT_STORAGE_HOME_DIR*`, `LIFT_DEFAULT_PREFERENCES_FILE_BASENAME*`,
 *          `LIFT_DEFAULT_PREFERENCES_FILE`.
 *      - Renamed `DATA_HOME_PATH` to `LIFT_DATA_HOME_DIR_PATH`.
 *      - Renamed `CONFIG_HOME_PATH` to `LIFT_CONFIG_HOME_DIR_PATH`.
 *      - Updated javadocs and spacing.
 *
 * - 0.3.0 (2021-03-12)
 *      - Remove `LIFT_CACHE_DIR_PATH*`, `LIFT_DATABASES_DIR_PATH*`,
 *          `LIFT_SHARED_PREFERENCES_DIR_PATH*` since they may not be consistent on all devices.
 *      - Renamed `LIFT_DEFAULT_PREFERENCES_FILE_BASENAME` to
 *          `LIFT_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION`. This should be used for
 *           accessing shared preferences between Lift app and its plugins if ever needed by first
 *           getting shared package context with {@link Context.createPackageContext(String,int}).
 *
 * - 0.4.0 (2021-03-16)
 *      - Added `BROADCAST_LIFT_OPENED`,
 *          `LIFT_API_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION`
 *          `LIFT_BOOT_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION`,
 *          `LIFT_FLOAT_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION`,
 *          `LIFT_STYLING_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION`,
 *          `LIFT_TASKER_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION`,
 *          `LIFT_WIDGET_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION`.
 *
 * - 0.5.0 (2021-03-16)
 *      - Renamed "Lift Plugin app" labels to "Lift:Tasker app".
 *
 * - 0.6.0 (2021-03-16)
 *      - Added `LIFT_FILE_SHARE_URI_AUTHORITY`.
 *
 * - 0.7.0 (2021-03-17)
 *      - Fixed javadocs.
 *
 * - 0.8.0 (2021-03-18)
 *      - Fixed Intent extra types javadocs.
 *      - Added following to `LIFT_SERVICE`:
 *          `EXTRA_PENDING_INTENT`, `EXTRA_RESULT_BUNDLE`,
 *          `EXTRA_STDOUT`, `EXTRA_STDERR`, `EXTRA_EXIT_CODE`,
 *          `EXTRA_ERR`, `EXTRA_ERRMSG`.
 *
 * - 0.9.0 (2021-03-18)
 *      - Fixed javadocs.
 *
 * - 0.10.0 (2021-03-19)
 *      - Added following to `LIFT_SERVICE`:
 *          `EXTRA_SESSION_ACTION`,
 *          `VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY`,
 *          `VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_OPEN_ACTIVITY`,
 *          `VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_DONT_OPEN_ACTIVITY`
 *          `VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_DONT_OPEN_ACTIVITY`.
 *      - Added following to `RUN_COMMAND_SERVICE`:
 *          `EXTRA_SESSION_ACTION`.
 *
 * - 0.11.0 (2021-03-24)
 *      - Added following to `LIFT_SERVICE`:
 *          `EXTRA_COMMAND_LABEL`, `EXTRA_COMMAND_DESCRIPTION`, `EXTRA_COMMAND_HELP`, `EXTRA_PLUGIN_API_HELP`.
 *      - Added following to `RUN_COMMAND_SERVICE`:
 *          `EXTRA_COMMAND_LABEL`, `EXTRA_COMMAND_DESCRIPTION`, `EXTRA_COMMAND_HELP`.
 *      - Updated `RESULT_BUNDLE` related extras with `PLUGIN_RESULT_BUNDLE` prefixes.
 *
 * - 0.12.0 (2021-03-25)
 *      - Added following to `LIFT_SERVICE`:
 *          `EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT_ORIGINAL_LENGTH`,
 *          `EXTRA_PLUGIN_RESULT_BUNDLE_STDERR_ORIGINAL_LENGTH`.
 *
 * - 0.13.0 (2021-03-25)
 *      - Added following to `RUN_COMMAND_SERVICE`:
 *          `EXTRA_PENDING_INTENT`.
 *
 * - 0.14.0 (2021-03-25)
 *      - Added `FDROID_PACKAGES_BASE_URL`,
 *          `LIFT_GITHUB_ORGANIZATION_NAME`, `LIFT_GITHUB_ORGANIZATION_URL`,
 *          `LIFT_GITHUB_REPO_NAME`, `LIFT_GITHUB_REPO_URL`, `LIFT_FDROID_PACKAGE_URL`,
 *          `LIFT_API_GITHUB_REPO_NAME`,`LIFT_API_GITHUB_REPO_URL`, `LIFT_API_FDROID_PACKAGE_URL`,
 *          `LIFT_BOOT_GITHUB_REPO_NAME`, `LIFT_BOOT_GITHUB_REPO_URL`, `LIFT_BOOT_FDROID_PACKAGE_URL`,
 *          `LIFT_FLOAT_GITHUB_REPO_NAME`, `LIFT_FLOAT_GITHUB_REPO_URL`, `LIFT_FLOAT_FDROID_PACKAGE_URL`,
 *          `LIFT_STYLING_GITHUB_REPO_NAME`, `LIFT_STYLING_GITHUB_REPO_URL`, `LIFT_STYLING_FDROID_PACKAGE_URL`,
 *          `LIFT_TASKER_GITHUB_REPO_NAME`, `LIFT_TASKER_GITHUB_REPO_URL`, `LIFT_TASKER_FDROID_PACKAGE_URL`,
 *          `LIFT_WIDGET_GITHUB_REPO_NAME`, `LIFT_WIDGET_GITHUB_REPO_URL` `LIFT_WIDGET_FDROID_PACKAGE_URL`.
 *
 * - 0.15.0 (2021-04-06)
 *      - Fixed some variables that had `PREFIX_` substring missing in their name.
 *      - Added `LIFT_CRASH_LOG_FILE_PATH`, `LIFT_CRASH_LOG_BACKUP_FILE_PATH`,
 *          `LIFT_GITHUB_ISSUES_REPO_URL`, `LIFT_API_GITHUB_ISSUES_REPO_URL`,
 *          `LIFT_BOOT_GITHUB_ISSUES_REPO_URL`, `LIFT_FLOAT_GITHUB_ISSUES_REPO_URL`,
 *          `LIFT_STYLING_GITHUB_ISSUES_REPO_URL`, `LIFT_TASKER_GITHUB_ISSUES_REPO_URL`,
 *          `LIFT_WIDGET_GITHUB_ISSUES_REPO_URL`,
 *          `LIFT_GITHUB_WIKI_REPO_URL`, `LIFT_PACKAGES_GITHUB_WIKI_REPO_URL`,
 *          `LIFT_PACKAGES_GITHUB_REPO_NAME`, `LIFT_PACKAGES_GITHUB_REPO_URL`, `LIFT_PACKAGES_GITHUB_ISSUES_REPO_URL`,
 *          `LIFT_GAME_PACKAGES_GITHUB_REPO_NAME`, `LIFT_GAME_PACKAGES_GITHUB_REPO_URL`, `LIFT_GAME_PACKAGES_GITHUB_ISSUES_REPO_URL`,
 *          `LIFT_SCIENCE_PACKAGES_GITHUB_REPO_NAME`, `LIFT_SCIENCE_PACKAGES_GITHUB_REPO_URL`, `LIFT_SCIENCE_PACKAGES_GITHUB_ISSUES_REPO_URL`,
 *          `LIFT_ROOT_PACKAGES_GITHUB_REPO_NAME`, `LIFT_ROOT_PACKAGES_GITHUB_REPO_URL`, `LIFT_ROOT_PACKAGES_GITHUB_ISSUES_REPO_URL`,
 *          `LIFT_UNSTABLE_PACKAGES_GITHUB_REPO_NAME`, `LIFT_UNSTABLE_PACKAGES_GITHUB_REPO_URL`, `LIFT_UNSTABLE_PACKAGES_GITHUB_ISSUES_REPO_URL`,
 *          `LIFT_X11_PACKAGES_GITHUB_REPO_NAME`, `LIFT_X11_PACKAGES_GITHUB_REPO_URL`, `LIFT_X11_PACKAGES_GITHUB_ISSUES_REPO_URL`.
 *      - Added following to `RUN_COMMAND_SERVICE`:
 *          `RUN_COMMAND_API_HELP_URL`.
 *
 * - 0.16.0 (2021-04-06)
 *      - Added `LIFT_SUPPORT_EMAIL`, `LIFT_SUPPORT_EMAIL_URL`, `LIFT_SUPPORT_EMAIL_MAILTO_URL`,
 *          `LIFT_REDDIT_SUBREDDIT`, `LIFT_REDDIT_SUBREDDIT_URL`.
 *      - The `LIFT_SUPPORT_EMAIL_URL` value must be fixed later when email has been set up.
 *
 * - 0.17.0 (2021-04-07)
 *      - Added `LIFT_APP_NOTIFICATION_CHANNEL_ID`, `LIFT_APP_NOTIFICATION_CHANNEL_NAME`, `LIFT_APP_NOTIFICATION_ID`,
 *          `LIFT_RUN_COMMAND_NOTIFICATION_CHANNEL_ID`, `LIFT_RUN_COMMAND_NOTIFICATION_CHANNEL_NAME`, `LIFT_RUN_COMMAND_NOTIFICATION_ID`,
 *          `LIFT_PLUGIN_COMMAND_ERRORS_NOTIFICATION_CHANNEL_ID`, `LIFT_PLUGIN_COMMAND_ERRORS_NOTIFICATION_CHANNEL_NAME`,
 *          `LIFT_CRASH_REPORTS_NOTIFICATION_CHANNEL_ID`, `LIFT_CRASH_REPORTS_NOTIFICATION_CHANNEL_NAME`.
 *      - Updated javadocs.
 *
 * - 0.18.0 (2021-04-11)
 *      - Updated `LIFT_SUPPORT_EMAIL_URL` to a valid email.
 *      - Removed `LIFT_SUPPORT_EMAIL`.
 *
 * - 0.19.0 (2021-04-12)
 *      - Added `LIFT_ACTIVITY.ACTION_REQUEST_PERMISSIONS`.
 *      - Added `LIFT_SERVICE.EXTRA_STDIN`.
 *      - Added `RUN_COMMAND_SERVICE.EXTRA_STDIN`.
 *      - Deprecated `LIFT_ACTIVITY.EXTRA_RELOAD_STYLE`.
 *
 * - 0.20.0 (2021-05-13)
 *      - Added `LIFT_WIKI`, `LIFT_WIKI_URL`, `LIFT_PLUGIN_APP_NAMES_LIST`, `LIFT_PLUGIN_APP_PACKAGE_NAMES_LIST`.
 *      - Added `LIFT_SETTINGS_ACTIVITY_NAME`.
 *
 * - 0.21.0 (2021-05-13)
 *      - Added `APK_RELEASE_FDROID`, `APK_RELEASE_FDROID_SIGNING_CERTIFICATE_SHA256_DIGEST`,
 *          `APK_RELEASE_GITHUB_DEBUG_BUILD`, `APK_RELEASE_GITHUB_DEBUG_BUILD_SIGNING_CERTIFICATE_SHA256_DIGEST`,
 *          `APK_RELEASE_GOOGLE_PLAYSTORE`, `APK_RELEASE_GOOGLE_PLAYSTORE_SIGNING_CERTIFICATE_SHA256_DIGEST`.
 *
 * - 0.22.0 (2021-05-13)
 *      - Added `LIFT_DONATE_URL`.
 *
 * - 0.23.0 (2021-06-12)
 *      - Rename `INTERNAL_PRIVATE_APP_DATA_DIR_PATH` to `LIFT_INTERNAL_PRIVATE_APP_DATA_DIR_PATH`.
 *
 * - 0.24.0 (2021-06-27)
 *      - Add `COMMA_NORMAL`, `COMMA_ALTERNATIVE`.
 *      - Added following to `LIFT_APP.LIFT_SERVICE`:
 *          `EXTRA_RESULT_DIRECTORY`, `EXTRA_RESULT_SINGLE_FILE`, `EXTRA_RESULT_FILE_BASENAME`,
 *          `EXTRA_RESULT_FILE_OUTPUT_FORMAT`, `EXTRA_RESULT_FILE_ERROR_FORMAT`, `EXTRA_RESULT_FILES_SUFFIX`.
 *      - Added following to `LIFT_APP.RUN_COMMAND_SERVICE`:
 *          `EXTRA_RESULT_DIRECTORY`, `EXTRA_RESULT_SINGLE_FILE`, `EXTRA_RESULT_FILE_BASENAME`,
 *          `EXTRA_RESULT_FILE_OUTPUT_FORMAT`, `EXTRA_RESULT_FILE_ERROR_FORMAT`, `EXTRA_RESULT_FILES_SUFFIX`,
 *          `EXTRA_REPLACE_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS`, `EXTRA_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS`.
 *      - Added following to `RESULT_SENDER`:
 *           `FORMAT_SUCCESS_STDOUT`, `FORMAT_SUCCESS_STDOUT__EXIT_CODE`, `FORMAT_SUCCESS_STDOUT__STDERR__EXIT_CODE`
 *           `FORMAT_FAILED_ERR__ERRMSG__STDOUT__STDERR__EXIT_CODE`,
 *           `RESULT_FILE_ERR_PREFIX`, `RESULT_FILE_ERRMSG_PREFIX` `RESULT_FILE_STDOUT_PREFIX`,
 *           `RESULT_FILE_STDERR_PREFIX`, `RESULT_FILE_EXIT_CODE_PREFIX`.
 *
 * - 0.25.0 (2021-08-19)
 *      - Added following to `LIFT_APP.LIFT_SERVICE`:
 *          `EXTRA_BACKGROUND_CUSTOM_LOG_LEVEL`.
 *      - Added following to `LIFT_APP.RUN_COMMAND_SERVICE`:
 *          `EXTRA_BACKGROUND_CUSTOM_LOG_LEVEL`.
 *
 * - 0.26.0 (2021-08-25)
 *      - Changed `LIFT_ACTIVITY.ACTION_FAILSAFE_SESSION` to `LIFT_ACTIVITY.EXTRA_FAILSAFE_SESSION`.
 *
 * - 0.27.0 (2021-09-02)
 *      - Added `LIFT_FLOAT_APP_NOTIFICATION_CHANNEL_ID`, `LIFT_FLOAT_APP_NOTIFICATION_CHANNEL_NAME`,
 *          `LIFT_FLOAT_APP.LIFT_FLOAT_SERVICE_NAME`.
 *      - Added following to `LIFT_FLOAT_APP.LIFT_FLOAT_SERVICE`:
 *          `ACTION_STOP_SERVICE`, `ACTION_SHOW`, `ACTION_HIDE`.
 *
 * - 0.28.0 (2021-09-02)
 *      - Added `LIFT_FLOAT_PROPERTIES_PRIMARY_FILE*` and `LIFT_FLOAT_PROPERTIES_SECONDARY_FILE*`.
 *
 * - 0.29.0 (2021-09-04)
 *      - Added `LIFT_SHORTCUT_TASKS_SCRIPTS_DIR_BASENAME`, `LIFT_SHORTCUT_SCRIPT_ICONS_DIR_BASENAME`,
 *          `LIFT_SHORTCUT_SCRIPT_ICONS_DIR_PATH`, `LIFT_SHORTCUT_SCRIPT_ICONS_DIR`.
 *      - Added following to `LIFT_WIDGET.LIFT_WIDGET_PROVIDER`:
 *          `ACTION_WIDGET_ITEM_CLICKED`, `ACTION_REFRESH_WIDGET`, `EXTRA_FILE_CLICKED`.
 *      - Changed naming convention of `LIFT_FLOAT_APP.LIFT_FLOAT_SERVICE.ACTION_*`.
 *      - Fixed wrong path set for `LIFT_SHORTCUT_SCRIPTS_DIR_PATH`.
 *
 * - 0.30.0 (2021-09-08)
 *      - Changed `APK_RELEASE_GITHUB_DEBUG_BUILD`to `APK_RELEASE_GITHUB` and
 *          `APK_RELEASE_GITHUB_DEBUG_BUILD_SIGNING_CERTIFICATE_SHA256_DIGEST` to
 *          `APK_RELEASE_GITHUB_SIGNING_CERTIFICATE_SHA256_DIGEST`.
 *
 * - 0.31.0 (2021-09-09)
 *      - Added following to `LIFT_APP.LIFT_SERVICE`:
 *          `MIN_VALUE_EXTRA_SESSION_ACTION` and `MAX_VALUE_EXTRA_SESSION_ACTION`.
 *
 * - 0.32.0 (2021-09-23)
 *      - Added `LIFT_API.LIFT_API_ACTIVITY_NAME`, `LIFT_TASKER.LIFT_TASKER_ACTIVITY_NAME`
 *          and `LIFT_WIDGET.LIFT_WIDGET_ACTIVITY_NAME`.
 *
 * - 0.33.0 (2021-10-08)
 *      - Added `LIFT_PROPERTIES_FILE_PATHS_LIST` and `LIFT_FLOAT_PROPERTIES_FILE_PATHS_LIST`.
 *
 * - 0.34.0 (2021-10-26)
 *      - Move `RESULT_SENDER` to `com.lift.shared.shell.command.ShellCommandConstants`.
 *
 * - 0.35.0 (2022-01-28)
 *      - Add `LIFT_APP.LIFT_ACTIVITY.EXTRA_RECREATE_ACTIVITY`.
 *
 * - 0.36.0 (2022-03-10)
 *      - Added `LIFT_APP.LIFT_SERVICE.EXTRA_RUNNER` and `LIFT_APP.RUN_COMMAND_SERVICE.EXTRA_RUNNER`
 *
 * - 0.37.0 (2022-03-15)
 *  - Added `LIFT_API_APT_*`.
 *
 * - 0.38.0 (2022-03-16)
 *      - Added `LIFT_APP.LIFT_ACTIVITY.ACTION_NOTIFY_APP_CRASH`.
 *
 * - 0.39.0 (2022-03-18)
 *      - Added `LIFT_APP.LIFT_SERVICE.EXTRA_SESSION_NAME`, `LIFT_APP.RUN_COMMAND_SERVICE.EXTRA_SESSION_NAME`,
 *          `LIFT_APP.LIFT_SERVICE.EXTRA_SESSION_CREATE_MODE` and `LIFT_APP.RUN_COMMAND_SERVICE.EXTRA_SESSION_CREATE_MODE`.
 *
 * - 0.40.0 (2022-04-17)
 *      - Added `LIFT_APPS_DIR_PATH` and `LIFT_APP.APPS_DIR_PATH`.
 *
 * - 0.41.0 (2022-04-17)
 *      - Added `LIFT_APP.LIFT_AM_SOCKET_FILE_PATH`.
 *
 * - 0.42.0 (2022-04-29)
 *      - Added `APK_RELEASE_LIFT_DEVS` and `APK_RELEASE_LIFT_DEVS_SIGNING_CERTIFICATE_SHA256_DIGEST`.
 *
 * - 0.43.0 (2022-05-29)
 *      - Changed `LIFT_SUPPORT_EMAIL_URL` to support@lift.dev.
 *
 * - 0.44.0 (2022-05-29)
 *      - Changed `LIFT_APP.APPS_DIR_PATH` basename from `lift-app` to `com.lift`.
 *
 * - 0.45.0 (2022-06-01)
 *      - Added `LIFT_APP.BUILD_CONFIG_CLASS_NAME`.
 *
 * - 0.46.0 (2022-06-03)
 *      - Rename `LIFT_APP.LIFT_SERVICE.EXTRA_SESSION_NAME` to `*.EXTRA_SHELL_NAME`,
 *          `LIFT_APP.RUN_COMMAND_SERVICE.EXTRA_SESSION_NAME` to `*.EXTRA_SHELL_NAME`,
 *          `LIFT_APP.LIFT_SERVICE.EXTRA_SESSION_CREATE_MODE` to `*.EXTRA_SHELL_CREATE_MODE` and
 *          `LIFT_APP.RUN_COMMAND_SERVICE.EXTRA_SESSION_CREATE_MODE` to `*.EXTRA_SHELL_CREATE_MODE`.
 *
 * - 0.47.0 (2022-06-04)
 *      - Added `LIFT_SITE` and `LIFT_SITE_URL`.
 *      - Changed `LIFT_DONATE_URL`.
 *
 * - 0.48.0 (2022-06-04)
 *      - Removed `LIFT_GAME_PACKAGES_GITHUB_*`, `LIFT_SCIENCE_PACKAGES_GITHUB_*`,
 *          `LIFT_ROOT_PACKAGES_GITHUB_*`, `LIFT_UNSTABLE_PACKAGES_GITHUB_*`
 *
 * - 0.49.0 (2022-06-11)
 *      - Added `LIFT_ENV_PREFIX_ROOT`.
 *
 * - 0.50.0 (2022-06-11)
 *      - Added `LIFT_CONFIG_PREFIX_DIR_PATH`, `LIFT_ENV_FILE_PATH` and `LIFT_ENV_TEMP_FILE_PATH`.
 *
 * - 0.51.0 (2022-06-13)
 *      - Added `LIFT_APP.FILE_SHARE_RECEIVER_ACTIVITY_CLASS_NAME` and `LIFT_APP.FILE_VIEW_RECEIVER_ACTIVITY_CLASS_NAME`.
 *
 * - 0.52.0 (2022-06-18)
 *      - Added `LIFT_PREFIX_DIR_IGNORED_SUB_FILES_PATHS_TO_CONSIDER_AS_EMPTY`.
 *
 * - 0.53.0 (2025-01-12)
 *      - Renamed `LIFT_API`, `LIFT_STYLING`, `LIFT_TASKER`, `LIFT_WIDGET` classes with `_APP` suffix added.
 *      - Added `LIFT_*_MAIN_ACTIVITY_NAME` and `LIFT_*_LAUNCHER_ACTIVITY_NAME` constants to each app class.
 */

/**
 * A class that defines shared constants of the Lift app and its plugins.
 * This class will be hosted by lift-shared lib and should be imported by other lift plugin
 * apps as is instead of copying constants to random classes. The 3rd party apps can also import
 * it for interacting with lift apps. If changes are made to this file, increment the version number
 * and add an entry in the Changelog section above.
 *
 * Lift app default package name is "com.lift" and is used in {@link #LIFT_PREFIX_DIR_PATH}.
 * The binaries compiled for lift have {@link #LIFT_PREFIX_DIR_PATH} hardcoded in them but it
 * can be changed during compilation.
 *
 * The {@link #LIFT_PACKAGE_NAME} must be the same as the applicationId of lift-app build.gradle
 * since its also used by {@link #LIFT_FILES_DIR_PATH}.
 * If {@link #LIFT_PACKAGE_NAME} is changed, then binaries, specially used in bootstrap need to be
 * compiled appropriately. Check https://github.com/lift/lift-packages/wiki/Building-packages
 * for more info.
 *
 * Ideally the only places where changes should be required if changing package name are the following:
 * - The {@link #LIFT_PACKAGE_NAME} in {@link LiftConstants}.
 * - The "applicationId" in "build.gradle" of lift-app. This is package name that android and app
 *      stores will use and is also the final package name stored in "AndroidManifest.xml".
 * - The "manifestPlaceholders" values for {@link #LIFT_PACKAGE_NAME} and *_APP_NAME in
 *      "build.gradle" of lift-app.
 * - The "ENTITY" values for {@link #LIFT_PACKAGE_NAME} and *_APP_NAME in "strings.xml" of
 *      lift-app and of lift-shared.
 * - The "shortcut.xml" and "*_preferences.xml" files of lift-app since dynamic variables don't
 *      work in it.
 * - Optionally the "package" in "AndroidManifest.xml" if modifying project structure of lift-app.
 *      This is package name for java classes project structure and is prefixed if activity and service
 *      names use dot (.) notation. This is currently not advisable since this will break lot of
 *      stuff, including lift-* packages.
 * - Optionally the *_PATH variables in {@link LiftConstants} containing the string "lift".
 *
 * Check https://developer.android.com/studio/build/application-id for info on "package" in
 * "AndroidManifest.xml" and "applicationId" in "build.gradle".
 *
 * The {@link #LIFT_PACKAGE_NAME} must be used in source code of Lift app and its plugins instead
 * of hardcoded "com.lift" paths.
 */
public final class LiftConstants {


    /*
     * Lift organization variables.
     */

    /** Lift GitHub organization name */
    public static final String LIFT_GITHUB_ORGANIZATION_NAME = "lift"; // Default: "lift"
    /** Lift GitHub organization url */
    public static final String LIFT_GITHUB_ORGANIZATION_URL = "https://github.com" + "/" + LIFT_GITHUB_ORGANIZATION_NAME; // Default: "https://github.com/lift"

    /** F-Droid packages base url */
    public static final String FDROID_PACKAGES_BASE_URL = "https://f-droid.org/en/packages"; // Default: "https://f-droid.org/en/packages"





    /*
     * Lift and its plugin app and package names and urls.
     */

    /** Lift app name */
    public static final String LIFT_APP_NAME = "Lift"; // Default: "Lift"
    /** Lift package name */
    public static final String LIFT_PACKAGE_NAME = "com.lift"; // Default: "com.lift"
    /** Lift GitHub repo name */
    public static final String LIFT_GITHUB_REPO_NAME = "lift-app"; // Default: "lift-app"
    /** Lift GitHub repo url */
    public static final String LIFT_GITHUB_REPO_URL = LIFT_GITHUB_ORGANIZATION_URL + "/" + LIFT_GITHUB_REPO_NAME; // Default: "https://github.com/lift/lift-app"
    /** Lift GitHub issues repo url */
    public static final String LIFT_GITHUB_ISSUES_REPO_URL = LIFT_GITHUB_REPO_URL + "/issues"; // Default: "https://github.com/lift/lift-app/issues"
    /** Lift F-Droid package url */
    public static final String LIFT_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + LIFT_PACKAGE_NAME; // Default: "https://f-droid.org/en/packages/com.lift"


    /** Lift:API app name */
    public static final String LIFT_API_APP_NAME = "Lift:API"; // Default: "Lift:API"
    /** Lift:API app package name */
    public static final String LIFT_API_PACKAGE_NAME = LIFT_PACKAGE_NAME + ".api"; // Default: "com.lift.api"
    /** Lift:API GitHub repo name */
    public static final String LIFT_API_GITHUB_REPO_NAME = "lift-api"; // Default: "lift-api"
    /** Lift:API GitHub repo url */
    public static final String LIFT_API_GITHUB_REPO_URL = LIFT_GITHUB_ORGANIZATION_URL + "/" + LIFT_API_GITHUB_REPO_NAME; // Default: "https://github.com/lift/lift-api"
    /** Lift:API GitHub issues repo url */
    public static final String LIFT_API_GITHUB_ISSUES_REPO_URL = LIFT_API_GITHUB_REPO_URL + "/issues"; // Default: "https://github.com/lift/lift-api/issues"
    /** Lift:API F-Droid package url */
    public static final String LIFT_API_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + LIFT_API_PACKAGE_NAME; // Default: "https://f-droid.org/en/packages/com.lift.api"


    /** Lift:Boot app name */
    public static final String LIFT_BOOT_APP_NAME = "Lift:Boot"; // Default: "Lift:Boot"
    /** Lift:Boot app package name */
    public static final String LIFT_BOOT_PACKAGE_NAME = LIFT_PACKAGE_NAME + ".boot"; // Default: "com.lift.boot"
    /** Lift:Boot GitHub repo name */
    public static final String LIFT_BOOT_GITHUB_REPO_NAME = "lift-boot"; // Default: "lift-boot"
    /** Lift:Boot GitHub repo url */
    public static final String LIFT_BOOT_GITHUB_REPO_URL = LIFT_GITHUB_ORGANIZATION_URL + "/" + LIFT_BOOT_GITHUB_REPO_NAME; // Default: "https://github.com/lift/lift-boot"
    /** Lift:Boot GitHub issues repo url */
    public static final String LIFT_BOOT_GITHUB_ISSUES_REPO_URL = LIFT_BOOT_GITHUB_REPO_URL + "/issues"; // Default: "https://github.com/lift/lift-boot/issues"
    /** Lift:Boot F-Droid package url */
    public static final String LIFT_BOOT_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + LIFT_BOOT_PACKAGE_NAME; // Default: "https://f-droid.org/en/packages/com.lift.boot"


    /** Lift:Float app name */
    public static final String LIFT_FLOAT_APP_NAME = "Lift:Float"; // Default: "Lift:Float"
    /** Lift:Float app package name */
    public static final String LIFT_FLOAT_PACKAGE_NAME = LIFT_PACKAGE_NAME + ".window"; // Default: "com.lift.window"
    /** Lift:Float GitHub repo name */
    public static final String LIFT_FLOAT_GITHUB_REPO_NAME = "lift-float"; // Default: "lift-float"
    /** Lift:Float GitHub repo url */
    public static final String LIFT_FLOAT_GITHUB_REPO_URL = LIFT_GITHUB_ORGANIZATION_URL + "/" + LIFT_FLOAT_GITHUB_REPO_NAME; // Default: "https://github.com/lift/lift-float"
    /** Lift:Float GitHub issues repo url */
    public static final String LIFT_FLOAT_GITHUB_ISSUES_REPO_URL = LIFT_FLOAT_GITHUB_REPO_URL + "/issues"; // Default: "https://github.com/lift/lift-float/issues"
    /** Lift:Float F-Droid package url */
    public static final String LIFT_FLOAT_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + LIFT_FLOAT_PACKAGE_NAME; // Default: "https://f-droid.org/en/packages/com.lift.window"


    /** Lift:Styling app name */
    public static final String LIFT_STYLING_APP_NAME = "Lift:Styling"; // Default: "Lift:Styling"
    /** Lift:Styling app package name */
    public static final String LIFT_STYLING_PACKAGE_NAME = LIFT_PACKAGE_NAME + ".styling"; // Default: "com.lift.styling"
    /** Lift:Styling GitHub repo name */
    public static final String LIFT_STYLING_GITHUB_REPO_NAME = "lift-styling"; // Default: "lift-styling"
    /** Lift:Styling GitHub repo url */
    public static final String LIFT_STYLING_GITHUB_REPO_URL = LIFT_GITHUB_ORGANIZATION_URL + "/" + LIFT_STYLING_GITHUB_REPO_NAME; // Default: "https://github.com/lift/lift-styling"
    /** Lift:Styling GitHub issues repo url */
    public static final String LIFT_STYLING_GITHUB_ISSUES_REPO_URL = LIFT_STYLING_GITHUB_REPO_URL + "/issues"; // Default: "https://github.com/lift/lift-styling/issues"
    /** Lift:Styling F-Droid package url */
    public static final String LIFT_STYLING_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + LIFT_STYLING_PACKAGE_NAME; // Default: "https://f-droid.org/en/packages/com.lift.styling"


    /** Lift:Tasker app name */
    public static final String LIFT_TASKER_APP_NAME = "Lift:Tasker"; // Default: "Lift:Tasker"
    /** Lift:Tasker app package name */
    public static final String LIFT_TASKER_PACKAGE_NAME = LIFT_PACKAGE_NAME + ".tasker"; // Default: "com.lift.tasker"
    /** Lift:Tasker GitHub repo name */
    public static final String LIFT_TASKER_GITHUB_REPO_NAME = "lift-tasker"; // Default: "lift-tasker"
    /** Lift:Tasker GitHub repo url */
    public static final String LIFT_TASKER_GITHUB_REPO_URL = LIFT_GITHUB_ORGANIZATION_URL + "/" + LIFT_TASKER_GITHUB_REPO_NAME; // Default: "https://github.com/lift/lift-tasker"
    /** Lift:Tasker GitHub issues repo url */
    public static final String LIFT_TASKER_GITHUB_ISSUES_REPO_URL = LIFT_TASKER_GITHUB_REPO_URL + "/issues"; // Default: "https://github.com/lift/lift-tasker/issues"
    /** Lift:Tasker F-Droid package url */
    public static final String LIFT_TASKER_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + LIFT_TASKER_PACKAGE_NAME; // Default: "https://f-droid.org/en/packages/com.lift.tasker"


    /** Lift:Widget app name */
    public static final String LIFT_WIDGET_APP_NAME = "Lift:Widget"; // Default: "Lift:Widget"
    /** Lift:Widget app package name */
    public static final String LIFT_WIDGET_PACKAGE_NAME = LIFT_PACKAGE_NAME + ".widget"; // Default: "com.lift.widget"
    /** Lift:Widget GitHub repo name */
    public static final String LIFT_WIDGET_GITHUB_REPO_NAME = "lift-widget"; // Default: "lift-widget"
    /** Lift:Widget GitHub repo url */
    public static final String LIFT_WIDGET_GITHUB_REPO_URL = LIFT_GITHUB_ORGANIZATION_URL + "/" + LIFT_WIDGET_GITHUB_REPO_NAME; // Default: "https://github.com/lift/lift-widget"
    /** Lift:Widget GitHub issues repo url */
    public static final String LIFT_WIDGET_GITHUB_ISSUES_REPO_URL = LIFT_WIDGET_GITHUB_REPO_URL + "/issues"; // Default: "https://github.com/lift/lift-widget/issues"
    /** Lift:Widget F-Droid package url */
    public static final String LIFT_WIDGET_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + LIFT_WIDGET_PACKAGE_NAME; // Default: "https://f-droid.org/en/packages/com.lift.widget"





    /*
     * Lift plugin apps lists.
     */

    public static final List<String> LIFT_PLUGIN_APP_NAMES_LIST = Arrays.asList(
        LIFT_API_APP_NAME,
        LIFT_BOOT_APP_NAME,
        LIFT_FLOAT_APP_NAME,
        LIFT_STYLING_APP_NAME,
        LIFT_TASKER_APP_NAME,
        LIFT_WIDGET_APP_NAME);

    public static final List<String> LIFT_PLUGIN_APP_PACKAGE_NAMES_LIST = Arrays.asList(
        LIFT_API_PACKAGE_NAME,
        LIFT_BOOT_PACKAGE_NAME,
        LIFT_FLOAT_PACKAGE_NAME,
        LIFT_STYLING_PACKAGE_NAME,
        LIFT_TASKER_PACKAGE_NAME,
        LIFT_WIDGET_PACKAGE_NAME);





    /*
     * Lift APK releases.
     */

    /** F-Droid APK release */
    public static final String APK_RELEASE_FDROID = "F-Droid"; // Default: "F-Droid"

    /** F-Droid APK release signing certificate SHA-256 digest */
    public static final String APK_RELEASE_FDROID_SIGNING_CERTIFICATE_SHA256_DIGEST = "228FB2CFE90831C1499EC3CCAF61E96E8E1CE70766B9474672CE427334D41C42"; // Default: "228FB2CFE90831C1499EC3CCAF61E96E8E1CE70766B9474672CE427334D41C42"

    /** GitHub APK release */
    public static final String APK_RELEASE_GITHUB = "Github"; // Default: "Github"

    /** GitHub APK release signing certificate SHA-256 digest */
    public static final String APK_RELEASE_GITHUB_SIGNING_CERTIFICATE_SHA256_DIGEST = "B6DA01480EEFD5FBF2CD3771B8D1021EC791304BDD6C4BF41D3FAABAD48EE5E1"; // Default: "B6DA01480EEFD5FBF2CD3771B8D1021EC791304BDD6C4BF41D3FAABAD48EE5E1"

    /** Google Play Store APK release */
    public static final String APK_RELEASE_GOOGLE_PLAYSTORE = "Google Play Store"; // Default: "Google Play Store"

    /** Google Play Store APK release signing certificate SHA-256 digest */
    public static final String APK_RELEASE_GOOGLE_PLAYSTORE_SIGNING_CERTIFICATE_SHA256_DIGEST = "738F0A30A04D3C8A1BE304AF18D0779BCF3EA88FB60808F657A3521861C2EBF9"; // Default: "738F0A30A04D3C8A1BE304AF18D0779BCF3EA88FB60808F657A3521861C2EBF9"

    /** Lift Devs APK release */
    public static final String APK_RELEASE_LIFT_DEVS = "Lift Devs"; // Default: "Lift Devs"

    /** Lift Devs APK release signing certificate SHA-256 digest */
    public static final String APK_RELEASE_LIFT_DEVS_SIGNING_CERTIFICATE_SHA256_DIGEST = "F7A038EB551F1BE8FDF388686B784ABAB4552A5D82DF423E3D8F1B5CBE1C69AE"; // Default: "F7A038EB551F1BE8FDF388686B784ABAB4552A5D82DF423E3D8F1B5CBE1C69AE"





    /*
     * Lift packages urls.
     */

    /** Lift Packages GitHub repo name */
    public static final String LIFT_PACKAGES_GITHUB_REPO_NAME = "lift-packages"; // Default: "lift-packages"
    /** Lift Packages GitHub repo url */
    public static final String LIFT_PACKAGES_GITHUB_REPO_URL = LIFT_GITHUB_ORGANIZATION_URL + "/" + LIFT_PACKAGES_GITHUB_REPO_NAME; // Default: "https://github.com/lift/lift-packages"
    /** Lift Packages GitHub issues repo url */
    public static final String LIFT_PACKAGES_GITHUB_ISSUES_REPO_URL = LIFT_PACKAGES_GITHUB_REPO_URL + "/issues"; // Default: "https://github.com/lift/lift-packages/issues"


    /** Lift API apt package name */
    public static final String LIFT_API_APT_PACKAGE_NAME = "lift-api"; // Default: "lift-api"
    /** Lift API apt GitHub repo name */
    public static final String LIFT_API_APT_GITHUB_REPO_NAME = "lift-api-package"; // Default: "lift-api-package"
    /** Lift API apt GitHub repo url */
    public static final String LIFT_API_APT_GITHUB_REPO_URL = LIFT_GITHUB_ORGANIZATION_URL + "/" + LIFT_API_APT_GITHUB_REPO_NAME; // Default: "https://github.com/lift/lift-api-package"
    /** Lift API apt GitHub issues repo url */
    public static final String LIFT_API_APT_GITHUB_ISSUES_REPO_URL = LIFT_API_APT_GITHUB_REPO_URL + "/issues"; // Default: "https://github.com/lift/lift-api-package/issues"





    /*
     * Lift miscellaneous urls.
     */

    /** Lift Site */
    public static final String LIFT_SITE = LIFT_APP_NAME + " Site"; // Default: "Lift Site"

    /** Lift Site url */
    public static final String LIFT_SITE_URL = "https://lift.dev"; // Default: "https://lift.dev"

    /** Lift Wiki */
    public static final String LIFT_WIKI = LIFT_APP_NAME + " Wiki"; // Default: "Lift Wiki"

    /** Lift Wiki url */
    public static final String LIFT_WIKI_URL = "https://wiki.lift.com"; // Default: "https://wiki.lift.com"

    /** Lift GitHub wiki repo url */
    public static final String LIFT_GITHUB_WIKI_REPO_URL = LIFT_GITHUB_REPO_URL + "/wiki"; // Default: "https://github.com/lift/lift-app/wiki"

    /** Lift Packages wiki repo url */
    public static final String LIFT_PACKAGES_GITHUB_WIKI_REPO_URL = LIFT_PACKAGES_GITHUB_REPO_URL + "/wiki"; // Default: "https://github.com/lift/lift-packages/wiki"


    /** Lift support email url */
    public static final String LIFT_SUPPORT_EMAIL_URL = "support@lift.dev"; // Default: "support@lift.dev"

    /** Lift support email mailto url */
    public static final String LIFT_SUPPORT_EMAIL_MAILTO_URL = "mailto:" + LIFT_SUPPORT_EMAIL_URL; // Default: "mailto:support@lift.dev"


    /** Lift Reddit subreddit */
    public static final String LIFT_REDDIT_SUBREDDIT = "r/lift"; // Default: "r/lift"

    /** Lift Reddit subreddit url */
    public static final String LIFT_REDDIT_SUBREDDIT_URL = "https://www.reddit.com/r/lift"; // Default: "https://www.reddit.com/r/lift"


    /** Lift donate url */
    public static final String LIFT_DONATE_URL = LIFT_SITE_URL + "/donate"; // Default: "https://lift.dev/donate"





    /*
     * Lift app core directory paths.
     */

    /** Lift app internal private app data directory path */
    @SuppressLint("SdCardPath")
    public static final String LIFT_INTERNAL_PRIVATE_APP_DATA_DIR_PATH = "/data/data/" + LIFT_PACKAGE_NAME; // Default: "/data/data/com.lift"
    /** Lift app internal private app data directory */
    public static final File LIFT_INTERNAL_PRIVATE_APP_DATA_DIR = new File(LIFT_INTERNAL_PRIVATE_APP_DATA_DIR_PATH);



    /** Lift app Files directory path */
    public static final String LIFT_FILES_DIR_PATH = LIFT_INTERNAL_PRIVATE_APP_DATA_DIR_PATH + "/files"; // Default: "/data/data/com.lift/files"
    /** Lift app Files directory */
    public static final File LIFT_FILES_DIR = new File(LIFT_FILES_DIR_PATH);



    /** Lift app $PREFIX directory path */
    public static final String LIFT_PREFIX_DIR_PATH = LIFT_FILES_DIR_PATH + "/usr"; // Default: "/data/data/com.lift/files/usr"
    /** Lift app $PREFIX directory */
    public static final File LIFT_PREFIX_DIR = new File(LIFT_PREFIX_DIR_PATH);


    /** Lift app $PREFIX/bin directory path */
    public static final String LIFT_BIN_PREFIX_DIR_PATH = LIFT_PREFIX_DIR_PATH + "/bin"; // Default: "/data/data/com.lift/files/usr/bin"
    /** Lift app $PREFIX/bin directory */
    public static final File LIFT_BIN_PREFIX_DIR = new File(LIFT_BIN_PREFIX_DIR_PATH);


    /** Lift app $PREFIX/etc directory path */
    public static final String LIFT_ETC_PREFIX_DIR_PATH = LIFT_PREFIX_DIR_PATH + "/etc"; // Default: "/data/data/com.lift/files/usr/etc"
    /** Lift app $PREFIX/etc directory */
    public static final File LIFT_ETC_PREFIX_DIR = new File(LIFT_ETC_PREFIX_DIR_PATH);


    /** Lift app $PREFIX/include directory path */
    public static final String LIFT_INCLUDE_PREFIX_DIR_PATH = LIFT_PREFIX_DIR_PATH + "/include"; // Default: "/data/data/com.lift/files/usr/include"
    /** Lift app $PREFIX/include directory */
    public static final File LIFT_INCLUDE_PREFIX_DIR = new File(LIFT_INCLUDE_PREFIX_DIR_PATH);


    /** Lift app $PREFIX/lib directory path */
    public static final String LIFT_LIB_PREFIX_DIR_PATH = LIFT_PREFIX_DIR_PATH + "/lib"; // Default: "/data/data/com.lift/files/usr/lib"
    /** Lift app $PREFIX/lib directory */
    public static final File LIFT_LIB_PREFIX_DIR = new File(LIFT_LIB_PREFIX_DIR_PATH);


    /** Lift app $PREFIX/libexec directory path */
    public static final String LIFT_LIBEXEC_PREFIX_DIR_PATH = LIFT_PREFIX_DIR_PATH + "/libexec"; // Default: "/data/data/com.lift/files/usr/libexec"
    /** Lift app $PREFIX/libexec directory */
    public static final File LIFT_LIBEXEC_PREFIX_DIR = new File(LIFT_LIBEXEC_PREFIX_DIR_PATH);


    /** Lift app $PREFIX/share directory path */
    public static final String LIFT_SHARE_PREFIX_DIR_PATH = LIFT_PREFIX_DIR_PATH + "/share"; // Default: "/data/data/com.lift/files/usr/share"
    /** Lift app $PREFIX/share directory */
    public static final File LIFT_SHARE_PREFIX_DIR = new File(LIFT_SHARE_PREFIX_DIR_PATH);


    /** Lift app $PREFIX/tmp and $TMPDIR directory path */
    public static final String LIFT_TMP_PREFIX_DIR_PATH = LIFT_PREFIX_DIR_PATH + "/tmp"; // Default: "/data/data/com.lift/files/usr/tmp"
    /** Lift app $PREFIX/tmp and $TMPDIR directory */
    public static final File LIFT_TMP_PREFIX_DIR = new File(LIFT_TMP_PREFIX_DIR_PATH);


    /** Lift app $PREFIX/var directory path */
    public static final String LIFT_VAR_PREFIX_DIR_PATH = LIFT_PREFIX_DIR_PATH + "/var"; // Default: "/data/data/com.lift/files/usr/var"
    /** Lift app $PREFIX/var directory */
    public static final File LIFT_VAR_PREFIX_DIR = new File(LIFT_VAR_PREFIX_DIR_PATH);



    /** Lift app usr-staging directory path */
    public static final String LIFT_STAGING_PREFIX_DIR_PATH = LIFT_FILES_DIR_PATH + "/usr-staging"; // Default: "/data/data/com.lift/files/usr-staging"
    /** Lift app usr-staging directory */
    public static final File LIFT_STAGING_PREFIX_DIR = new File(LIFT_STAGING_PREFIX_DIR_PATH);



    /** Lift app $HOME directory path */
    public static final String LIFT_HOME_DIR_PATH = LIFT_FILES_DIR_PATH + "/home"; // Default: "/data/data/com.lift/files/home"
    /** Lift app $HOME directory */
    public static final File LIFT_HOME_DIR = new File(LIFT_HOME_DIR_PATH);


    /** Lift app config home directory path */
    public static final String LIFT_CONFIG_HOME_DIR_PATH = LIFT_HOME_DIR_PATH + "/.config/lift"; // Default: "/data/data/com.lift/files/home/.config/lift"
    /** Lift app config home directory */
    public static final File LIFT_CONFIG_HOME_DIR = new File(LIFT_CONFIG_HOME_DIR_PATH);

    /** Lift app config $PREFIX directory path */
    public static final String LIFT_CONFIG_PREFIX_DIR_PATH = LIFT_ETC_PREFIX_DIR_PATH + "/lift"; // Default: "/data/data/com.lift/files/usr/etc/lift"
    /** Lift app config $PREFIX directory */
    public static final File LIFT_CONFIG_PREFIX_DIR = new File(LIFT_CONFIG_PREFIX_DIR_PATH);


    /** Lift app data home directory path */
    public static final String LIFT_DATA_HOME_DIR_PATH = LIFT_HOME_DIR_PATH + "/.lift"; // Default: "/data/data/com.lift/files/home/.lift"
    /** Lift app data home directory */
    public static final File LIFT_DATA_HOME_DIR = new File(LIFT_DATA_HOME_DIR_PATH);


    /** Lift app storage home directory path */
    public static final String LIFT_STORAGE_HOME_DIR_PATH = LIFT_HOME_DIR_PATH + "/storage"; // Default: "/data/data/com.lift/files/home/storage"
    /** Lift app storage home directory */
    public static final File LIFT_STORAGE_HOME_DIR = new File(LIFT_STORAGE_HOME_DIR_PATH);



    /** Lift and plugin apps directory path */
    public static final String LIFT_APPS_DIR_PATH = LIFT_FILES_DIR_PATH + "/apps"; // Default: "/data/data/com.lift/files/apps"
    /** Lift and plugin apps directory */
    public static final File LIFT_APPS_DIR = new File(LIFT_APPS_DIR_PATH);


    /** Lift app $PREFIX directory path ignored sub file paths to consider it empty */
    public static final List<String> LIFT_PREFIX_DIR_IGNORED_SUB_FILES_PATHS_TO_CONSIDER_AS_EMPTY = Arrays.asList(
        LiftConstants.LIFT_TMP_PREFIX_DIR_PATH, LiftConstants.LIFT_ENV_TEMP_FILE_PATH, LiftConstants.LIFT_ENV_FILE_PATH);



    /*
     * Lift app and plugin preferences and properties file paths.
     */

    /** Lift app default SharedPreferences file basename without extension */
    public static final String LIFT_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = LIFT_PACKAGE_NAME + "_preferences"; // Default: "com.lift_preferences"

    /** Lift:API app default SharedPreferences file basename without extension */
    public static final String LIFT_API_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = LIFT_API_PACKAGE_NAME + "_preferences"; // Default: "com.lift.api_preferences"

    /** Lift:Boot app default SharedPreferences file basename without extension */
    public static final String LIFT_BOOT_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = LIFT_BOOT_PACKAGE_NAME + "_preferences"; // Default: "com.lift.boot_preferences"

    /** Lift:Float app default SharedPreferences file basename without extension */
    public static final String LIFT_FLOAT_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = LIFT_FLOAT_PACKAGE_NAME + "_preferences"; // Default: "com.lift.window_preferences"

    /** Lift:Styling app default SharedPreferences file basename without extension */
    public static final String LIFT_STYLING_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = LIFT_STYLING_PACKAGE_NAME + "_preferences"; // Default: "com.lift.styling_preferences"

    /** Lift:Tasker app default SharedPreferences file basename without extension */
    public static final String LIFT_TASKER_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = LIFT_TASKER_PACKAGE_NAME + "_preferences"; // Default: "com.lift.tasker_preferences"

    /** Lift:Widget app default SharedPreferences file basename without extension */
    public static final String LIFT_WIDGET_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = LIFT_WIDGET_PACKAGE_NAME + "_preferences"; // Default: "com.lift.widget_preferences"



    /** Lift app properties primary file path */
    public static final String LIFT_PROPERTIES_PRIMARY_FILE_PATH = LIFT_DATA_HOME_DIR_PATH + "/lift.properties"; // Default: "/data/data/com.lift/files/home/.lift/lift.properties"
    /** Lift app properties primary file */
    public static final File LIFT_PROPERTIES_PRIMARY_FILE = new File(LIFT_PROPERTIES_PRIMARY_FILE_PATH);

    /** Lift app properties secondary file path */
    public static final String LIFT_PROPERTIES_SECONDARY_FILE_PATH = LIFT_CONFIG_HOME_DIR_PATH + "/lift.properties"; // Default: "/data/data/com.lift/files/home/.config/lift/lift.properties"
    /** Lift app properties secondary file */
    public static final File LIFT_PROPERTIES_SECONDARY_FILE = new File(LIFT_PROPERTIES_SECONDARY_FILE_PATH);

    /** Lift app properties file paths list. **DO NOT** allow these files to be modified by
     * {@link android.content.ContentProvider} exposed to external apps, since they may silently
     * modify the values for security properties like {@link #PROP_ALLOW_EXTERNAL_APPS} set by users
     * without their explicit consent. */
    public static final List<String> LIFT_PROPERTIES_FILE_PATHS_LIST = Arrays.asList(
        LIFT_PROPERTIES_PRIMARY_FILE_PATH,
        LIFT_PROPERTIES_SECONDARY_FILE_PATH);



    /** Lift:Float app properties primary file path */
    public static final String LIFT_FLOAT_PROPERTIES_PRIMARY_FILE_PATH = LIFT_DATA_HOME_DIR_PATH + "/lift.float.properties"; // Default: "/data/data/com.lift/files/home/.lift/lift.float.properties"
    /** Lift:Float app properties primary file */
    public static final File LIFT_FLOAT_PROPERTIES_PRIMARY_FILE = new File(LIFT_FLOAT_PROPERTIES_PRIMARY_FILE_PATH);

    /** Lift:Float app properties secondary file path */
    public static final String LIFT_FLOAT_PROPERTIES_SECONDARY_FILE_PATH = LIFT_CONFIG_HOME_DIR_PATH + "/lift.float.properties"; // Default: "/data/data/com.lift/files/home/.config/lift/lift.float.properties"
    /** Lift:Float app properties secondary file */
    public static final File LIFT_FLOAT_PROPERTIES_SECONDARY_FILE = new File(LIFT_FLOAT_PROPERTIES_SECONDARY_FILE_PATH);

    /** Lift:Float app properties file paths list. **DO NOT** allow these files to be modified by
     * {@link android.content.ContentProvider} exposed to external apps, since they may silently
     * modify the values for security properties like {@link #PROP_ALLOW_EXTERNAL_APPS} set by users
     * without their explicit consent. */
    public static final List<String> LIFT_FLOAT_PROPERTIES_FILE_PATHS_LIST = Arrays.asList(
        LIFT_FLOAT_PROPERTIES_PRIMARY_FILE_PATH,
        LIFT_FLOAT_PROPERTIES_SECONDARY_FILE_PATH);



    /** Lift app and Lift:Styling colors.properties file path */
    public static final String LIFT_COLOR_PROPERTIES_FILE_PATH = LIFT_DATA_HOME_DIR_PATH + "/colors.properties"; // Default: "/data/data/com.lift/files/home/.lift/colors.properties"
    /** Lift app and Lift:Styling colors.properties file */
    public static final File LIFT_COLOR_PROPERTIES_FILE = new File(LIFT_COLOR_PROPERTIES_FILE_PATH);

    /** Lift app and Lift:Styling font.ttf file path */
    public static final String LIFT_FONT_FILE_PATH = LIFT_DATA_HOME_DIR_PATH + "/font.ttf"; // Default: "/data/data/com.lift/files/home/.lift/font.ttf"
    /** Lift app and Lift:Styling font.ttf file */
    public static final File LIFT_FONT_FILE = new File(LIFT_FONT_FILE_PATH);


    /** Lift app and plugins crash log file path */
    public static final String LIFT_CRASH_LOG_FILE_PATH = LIFT_HOME_DIR_PATH + "/crash_log.md"; // Default: "/data/data/com.lift/files/home/crash_log.md"

    /** Lift app and plugins crash log backup file path */
    public static final String LIFT_CRASH_LOG_BACKUP_FILE_PATH = LIFT_HOME_DIR_PATH + "/crash_log_backup.md"; // Default: "/data/data/com.lift/files/home/crash_log_backup.md"


    /** Lift app environment file path */
    public static final String LIFT_ENV_FILE_PATH = LIFT_CONFIG_PREFIX_DIR_PATH + "/lift.env"; // Default: "/data/data/com.lift/files/usr/etc/lift/lift.env"

    /** Lift app environment temp file path */
    public static final String LIFT_ENV_TEMP_FILE_PATH = LIFT_CONFIG_PREFIX_DIR_PATH + "/lift.env.tmp"; // Default: "/data/data/com.lift/files/usr/etc/lift/lift.env.tmp"




    /*
     * Lift app plugin specific paths.
     */

    /** Lift app directory path to store scripts to be run at boot by Lift:Boot */
    public static final String LIFT_BOOT_SCRIPTS_DIR_PATH = LIFT_DATA_HOME_DIR_PATH + "/boot"; // Default: "/data/data/com.lift/files/home/.lift/boot"
    /** Lift app directory to store scripts to be run at boot by Lift:Boot */
    public static final File LIFT_BOOT_SCRIPTS_DIR = new File(LIFT_BOOT_SCRIPTS_DIR_PATH);


    /** Lift app directory path to store foreground scripts that can be run by the lift launcher
     * widget provided by Lift:Widget */
    public static final String LIFT_SHORTCUT_SCRIPTS_DIR_PATH = LIFT_HOME_DIR_PATH + "/.shortcuts"; // Default: "/data/data/com.lift/files/home/.shortcuts"
    /** Lift app directory to store foreground scripts that can be run by the lift launcher widget provided by Lift:Widget */
    public static final File LIFT_SHORTCUT_SCRIPTS_DIR = new File(LIFT_SHORTCUT_SCRIPTS_DIR_PATH);


    /** Lift app directory basename that stores background scripts that can be run by the lift
     * launcher widget provided by Lift:Widget */
    public static final String LIFT_SHORTCUT_TASKS_SCRIPTS_DIR_BASENAME =  "tasks"; // Default: "tasks"
    /** Lift app directory path to store background scripts that can be run by the lift launcher
     * widget provided by Lift:Widget */
    public static final String LIFT_SHORTCUT_TASKS_SCRIPTS_DIR_PATH = LIFT_SHORTCUT_SCRIPTS_DIR_PATH + "/" + LIFT_SHORTCUT_TASKS_SCRIPTS_DIR_BASENAME; // Default: "/data/data/com.lift/files/home/.shortcuts/tasks"
    /** Lift app directory to store background scripts that can be run by the lift launcher widget provided by Lift:Widget */
    public static final File LIFT_SHORTCUT_TASKS_SCRIPTS_DIR = new File(LIFT_SHORTCUT_TASKS_SCRIPTS_DIR_PATH);


    /** Lift app directory basename that stores icons for the foreground and background scripts
     * that can be run by the lift launcher widget provided by Lift:Widget */
    public static final String LIFT_SHORTCUT_SCRIPT_ICONS_DIR_BASENAME =  "icons"; // Default: "icons"
    /** Lift app directory path to store icons for the foreground and background scripts that can
     * be run by the lift launcher widget provided by Lift:Widget */
    public static final String LIFT_SHORTCUT_SCRIPT_ICONS_DIR_PATH = LIFT_SHORTCUT_SCRIPTS_DIR_PATH + "/" + LIFT_SHORTCUT_SCRIPT_ICONS_DIR_BASENAME; // Default: "/data/data/com.lift/files/home/.shortcuts/icons"
    /** Lift app directory to store icons for the foreground and background scripts that can be
     * run by the lift launcher widget provided by Lift:Widget */
    public static final File LIFT_SHORTCUT_SCRIPT_ICONS_DIR = new File(LIFT_SHORTCUT_SCRIPT_ICONS_DIR_PATH);


    /** Lift app directory path to store scripts to be run by 3rd party twofortyfouram locale plugin
     * host apps like Tasker app via the Lift:Tasker plugin client */
    public static final String LIFT_TASKER_SCRIPTS_DIR_PATH = LIFT_DATA_HOME_DIR_PATH + "/tasker"; // Default: "/data/data/com.lift/files/home/.lift/tasker"
    /** Lift app directory to store scripts to be run by 3rd party twofortyfouram locale plugin host apps like Tasker app via the Lift:Tasker plugin client */
    public static final File LIFT_TASKER_SCRIPTS_DIR = new File(LIFT_TASKER_SCRIPTS_DIR_PATH);





    /*
     * Lift app and plugins notification variables.
     */

    /** Lift app notification channel id used by {@link LIFT_APP.LIFT_SERVICE} */
    public static final String LIFT_APP_NOTIFICATION_CHANNEL_ID = "lift_notification_channel";
    /** Lift app notification channel name used by {@link LIFT_APP.LIFT_SERVICE} */
    public static final String LIFT_APP_NOTIFICATION_CHANNEL_NAME = LiftConstants.LIFT_APP_NAME + " App";
    /** Lift app unique notification id used by {@link LIFT_APP.LIFT_SERVICE} */
    public static final int LIFT_APP_NOTIFICATION_ID = 1337;

    /** Lift app notification channel id used by {@link LIFT_APP.RUN_COMMAND_SERVICE} */
    public static final String LIFT_RUN_COMMAND_NOTIFICATION_CHANNEL_ID = "lift_run_command_notification_channel";
    /** Lift app notification channel name used by {@link LIFT_APP.RUN_COMMAND_SERVICE} */
    public static final String LIFT_RUN_COMMAND_NOTIFICATION_CHANNEL_NAME = LiftConstants.LIFT_APP_NAME + " RunCommandService";
    /** Lift app unique notification id used by {@link LIFT_APP.RUN_COMMAND_SERVICE} */
    public static final int LIFT_RUN_COMMAND_NOTIFICATION_ID = 1338;

    /** Lift app notification channel id used for plugin command errors */
    public static final String LIFT_PLUGIN_COMMAND_ERRORS_NOTIFICATION_CHANNEL_ID = "lift_plugin_command_errors_notification_channel";
    /** Lift app notification channel name used for plugin command errors */
    public static final String LIFT_PLUGIN_COMMAND_ERRORS_NOTIFICATION_CHANNEL_NAME = LiftConstants.LIFT_APP_NAME + " Plugin Commands Errors";

    /** Lift app notification channel id used for crash reports */
    public static final String LIFT_CRASH_REPORTS_NOTIFICATION_CHANNEL_ID = "lift_crash_reports_notification_channel";
    /** Lift app notification channel name used for crash reports */
    public static final String LIFT_CRASH_REPORTS_NOTIFICATION_CHANNEL_NAME = LiftConstants.LIFT_APP_NAME + " Crash Reports";


    /** Lift app notification channel id used by {@link LIFT_FLOAT_APP.LIFT_FLOAT_SERVICE} */
    public static final String LIFT_FLOAT_APP_NOTIFICATION_CHANNEL_ID = "lift_float_notification_channel";
    /** Lift app notification channel name used by {@link LIFT_FLOAT_APP.LIFT_FLOAT_SERVICE} */
    public static final String LIFT_FLOAT_APP_NOTIFICATION_CHANNEL_NAME = LiftConstants.LIFT_FLOAT_APP_NAME + " App";
    /** Lift app unique notification id used by {@link LIFT_APP.LIFT_SERVICE} */
    public static final int LIFT_FLOAT_APP_NOTIFICATION_ID = 1339;





    /*
     * Lift app and plugins miscellaneous variables.
     */

    /** Android OS permission declared by Lift app in AndroidManifest.xml which can be requested by
     * 3rd party apps to run various commands in Lift app context */
    public static final String PERMISSION_RUN_COMMAND = LIFT_PACKAGE_NAME + ".permission.RUN_COMMAND"; // Default: "com.lift.permission.RUN_COMMAND"

    /** Lift property defined in lift.properties file as a secondary check to PERMISSION_RUN_COMMAND
     * to allow 3rd party apps to run various commands in Lift app context */
    public static final String PROP_ALLOW_EXTERNAL_APPS = "allow-external-apps"; // Default: "allow-external-apps"
    /** Default value for {@link #PROP_ALLOW_EXTERNAL_APPS} */
    public static final String PROP_DEFAULT_VALUE_ALLOW_EXTERNAL_APPS = "false"; // Default: "false"

    /** The broadcast action sent when Lift App opens */
    public static final String BROADCAST_LIFT_OPENED = LIFT_PACKAGE_NAME + ".app.OPENED";

    /** The Uri authority for Lift app file shares */
    public static final String LIFT_FILE_SHARE_URI_AUTHORITY = LIFT_PACKAGE_NAME + ".files"; // Default: "com.lift.files"

    /** The normal comma character (U+002C, &comma;, &#44;, comma) */
    public static final String COMMA_NORMAL = ","; // Default: ","

    /** The alternate comma character (U+201A, &sbquo;, &#8218;, single low-9 quotation mark) that
     * may be used instead of {@link #COMMA_NORMAL} */
    public static final String COMMA_ALTERNATIVE = "‚"; // Default: "‚"

    /** Environment variable prefix root for the Lift app. */
    public static final String LIFT_ENV_PREFIX_ROOT = "LIFT";






    /**
     * Lift app constants.
     */
    public static final class LIFT_APP {

        /** Lift apps directory path */
        public static final String APPS_DIR_PATH = LIFT_APPS_DIR_PATH + "/" + LIFT_PACKAGE_NAME; // Default: "/data/data/com.lift/files/apps/com.lift"

        /** lift-am socket file path */
        public static final String LIFT_AM_SOCKET_FILE_PATH = APPS_DIR_PATH + "/lift-am/am.sock"; // Default: "/data/data/com.lift/files/apps/com.lift/lift-am/am.sock"


        /** Lift app BuildConfig class name */
        public static final String BUILD_CONFIG_CLASS_NAME = LIFT_PACKAGE_NAME + ".BuildConfig"; // Default: "com.lift.BuildConfig"

        /** Lift app FileShareReceiverActivity class name */
        public static final String FILE_SHARE_RECEIVER_ACTIVITY_CLASS_NAME = LIFT_PACKAGE_NAME + ".app.api.file.FileShareReceiverActivity"; // Default: "com.lift.app.api.file.FileShareReceiverActivity"

        /** Lift app FileViewReceiverActivity class name */
        public static final String FILE_VIEW_RECEIVER_ACTIVITY_CLASS_NAME = LIFT_PACKAGE_NAME + ".app.api.file.FileViewReceiverActivity"; // Default: "com.lift.app.api.file.FileViewReceiverActivity"


        /** Lift app core activity name. */
        public static final String LIFT_ACTIVITY_NAME = LIFT_PACKAGE_NAME + ".app.LiftActivity"; // Default: "com.lift.app.LiftActivity"

        /**
         * Lift app core activity.
         */
        public static final class LIFT_ACTIVITY {

            /** Intent extra for if lift failsafe session needs to be started and is used by {@link LIFT_ACTIVITY} and {@link LIFT_SERVICE#ACTION_STOP_SERVICE} */
            public static final String EXTRA_FAILSAFE_SESSION = LiftConstants.LIFT_PACKAGE_NAME + ".app.failsafe_session"; // Default: "com.lift.app.failsafe_session"


            /** Intent action to make lift app notify user that a crash happened. */
            public static final String ACTION_NOTIFY_APP_CRASH = LiftConstants.LIFT_PACKAGE_NAME + ".app.notify_app_crash"; // Default: "com.lift.app.notify_app_crash"


            /** Intent action to make lift reload its lift session styling */
            public static final String ACTION_RELOAD_STYLE = LiftConstants.LIFT_PACKAGE_NAME + ".app.reload_style"; // Default: "com.lift.app.reload_style"
            /** Intent {@code String} extra for what to reload for the LIFT_ACTIVITY.ACTION_RELOAD_STYLE intent. This has been deperecated. */
            @Deprecated
            public static final String EXTRA_RELOAD_STYLE = LiftConstants.LIFT_PACKAGE_NAME + ".app.reload_style"; // Default: "com.lift.app.reload_style"

            /**  Intent {@code boolean} extra for whether to recreate activity for the LIFT_ACTIVITY.ACTION_RELOAD_STYLE intent. */
            public static final String EXTRA_RECREATE_ACTIVITY = LIFT_APP.LIFT_ACTIVITY_NAME + ".EXTRA_RECREATE_ACTIVITY"; // Default: "com.lift.app.LiftActivity.EXTRA_RECREATE_ACTIVITY"


            /** Intent action to make lift request storage permissions */
            public static final String ACTION_REQUEST_PERMISSIONS = LiftConstants.LIFT_PACKAGE_NAME + ".app.request_storage_permissions"; // Default: "com.lift.app.request_storage_permissions"
        }





        /** Lift app settings activity name. */
        public static final String LIFT_SETTINGS_ACTIVITY_NAME = LIFT_PACKAGE_NAME + ".app.activities.SettingsActivity"; // Default: "com.lift.app.activities.SettingsActivity"





        /** Lift app core service name. */
        public static final String LIFT_SERVICE_NAME = LIFT_PACKAGE_NAME + ".app.LiftService"; // Default: "com.lift.app.LiftService"

        /**
         * Lift app core service.
         */
        public static final class LIFT_SERVICE {

            /** Intent action to stop LIFT_SERVICE */
            public static final String ACTION_STOP_SERVICE = LIFT_PACKAGE_NAME + ".service_stop"; // Default: "com.lift.service_stop"


            /** Intent action to make LIFT_SERVICE acquire a wakelock */
            public static final String ACTION_WAKE_LOCK = LIFT_PACKAGE_NAME + ".service_wake_lock"; // Default: "com.lift.service_wake_lock"


            /** Intent action to make LIFT_SERVICE release wakelock */
            public static final String ACTION_WAKE_UNLOCK = LIFT_PACKAGE_NAME + ".service_wake_unlock"; // Default: "com.lift.service_wake_unlock"


            /** Intent action to execute command with LIFT_SERVICE */
            public static final String ACTION_SERVICE_EXECUTE = LIFT_PACKAGE_NAME + ".service_execute"; // Default: "com.lift.service_execute"

            /** Uri scheme for paths sent via intent to LIFT_SERVICE */
            public static final String URI_SCHEME_SERVICE_EXECUTE = LIFT_PACKAGE_NAME + ".file"; // Default: "com.lift.file"
            /** Intent {@code String[]} extra for arguments to the executable of the command for the LIFT_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_ARGUMENTS = LIFT_PACKAGE_NAME + ".execute.arguments"; // Default: "com.lift.execute.arguments"
            /** Intent {@code String} extra for stdin of the command for the LIFT_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_STDIN = LIFT_PACKAGE_NAME + ".execute.stdin"; // Default: "com.lift.execute.stdin"
            /** Intent {@code String} extra for command current working directory for the LIFT_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_WORKDIR = LIFT_PACKAGE_NAME + ".execute.cwd"; // Default: "com.lift.execute.cwd"
            /** Intent {@code boolean} extra for whether to run command in background {@link Runner#APP_SHELL} or foreground {@link Runner#TERMINAL_SESSION} for the LIFT_SERVICE.ACTION_SERVICE_EXECUTE intent */
            @Deprecated
            public static final String EXTRA_BACKGROUND = LIFT_PACKAGE_NAME + ".execute.background"; // Default: "com.lift.execute.background"
            /** Intent {@code String} extra for command the {@link Runner} for the LIFT_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_RUNNER = LIFT_PACKAGE_NAME + ".execute.runner"; // Default: "com.lift.execute.runner"
            /** Intent {@code String} extra for custom log level for background commands defined by {@link com.lift.shared.logger.Logger} for the LIFT_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_BACKGROUND_CUSTOM_LOG_LEVEL = LIFT_PACKAGE_NAME + ".execute.background_custom_log_level"; // Default: "com.lift.execute.background_custom_log_level"
            /** Intent {@code String} extra for session action for {@link Runner#TERMINAL_SESSION} commands for the LIFT_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_SESSION_ACTION = LIFT_PACKAGE_NAME + ".execute.session_action"; // Default: "com.lift.execute.session_action"
            /** Intent {@code String} extra for shell name for commands for the LIFT_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_SHELL_NAME = LIFT_PACKAGE_NAME + ".execute.shell_name"; // Default: "com.lift.execute.shell_name"
            /** Intent {@code String} extra for the {@link ExecutionCommand.ShellCreateMode}  for the LIFT_SERVICE.ACTION_SERVICE_EXECUTE intent. */
            public static final String EXTRA_SHELL_CREATE_MODE = LIFT_PACKAGE_NAME + ".execute.shell_create_mode"; // Default: "com.lift.execute.shell_create_mode"
            /** Intent {@code String} extra for label of the command for the LIFT_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_COMMAND_LABEL = LIFT_PACKAGE_NAME + ".execute.command_label"; // Default: "com.lift.execute.command_label"
            /** Intent markdown {@code String} extra for description of the command for the LIFT_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_COMMAND_DESCRIPTION = LIFT_PACKAGE_NAME + ".execute.command_description"; // Default: "com.lift.execute.command_description"
            /** Intent markdown {@code String} extra for help of the command for the LIFT_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_COMMAND_HELP = LIFT_PACKAGE_NAME + ".execute.command_help"; // Default: "com.lift.execute.command_help"
            /** Intent markdown {@code String} extra for help of the plugin API for the LIFT_SERVICE.ACTION_SERVICE_EXECUTE intent (Internal Use Only) */
            public static final String EXTRA_PLUGIN_API_HELP = LIFT_PACKAGE_NAME + ".execute.plugin_api_help"; // Default: "com.lift.execute.plugin_help"
            /** Intent {@code Parcelable} extra for the pending intent that should be sent with the
             * result of the execution command to the execute command caller for the LIFT_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_PENDING_INTENT = "pendingIntent"; // Default: "pendingIntent"
            /** Intent {@code String} extra for the directory path in which to write the result of the
             * execution command for the execute command caller for the LIFT_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_RESULT_DIRECTORY = LIFT_PACKAGE_NAME + ".execute.result_directory"; // Default: "com.lift.execute.result_directory"
            /** Intent {@code boolean} extra for whether the result should be written to a single file
             * or multiple files (err, errmsg, stdout, stderr, exit_code) in
             * {@link #EXTRA_RESULT_DIRECTORY} for the LIFT_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_RESULT_SINGLE_FILE = LIFT_PACKAGE_NAME + ".execute.result_single_file"; // Default: "com.lift.execute.result_single_file"
            /** Intent {@code String} extra for the basename of the result file that should be created
             * in {@link #EXTRA_RESULT_DIRECTORY} if {@link #EXTRA_RESULT_SINGLE_FILE} is {@code true}
             * for the LIFT_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_RESULT_FILE_BASENAME = LIFT_PACKAGE_NAME + ".execute.result_file_basename"; // Default: "com.lift.execute.result_file_basename"
            /** Intent {@code String} extra for the output {@link Formatter} format of the
             * {@link #EXTRA_RESULT_FILE_BASENAME} result file for the LIFT_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_RESULT_FILE_OUTPUT_FORMAT = LIFT_PACKAGE_NAME + ".execute.result_file_output_format"; // Default: "com.lift.execute.result_file_output_format"
            /** Intent {@code String} extra for the error {@link Formatter} format of the
             * {@link #EXTRA_RESULT_FILE_BASENAME} result file for the LIFT_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_RESULT_FILE_ERROR_FORMAT = LIFT_PACKAGE_NAME + ".execute.result_file_error_format"; // Default: "com.lift.execute.result_file_error_format"
            /** Intent {@code String} extra for the optional suffix of the result files that should
             * be created in {@link #EXTRA_RESULT_DIRECTORY} if {@link #EXTRA_RESULT_SINGLE_FILE} is
             * {@code false} for the LIFT_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_RESULT_FILES_SUFFIX = LIFT_PACKAGE_NAME + ".execute.result_files_suffix"; // Default: "com.lift.execute.result_files_suffix"



            /**
             * The value for {@link #EXTRA_SESSION_ACTION} extra that will set the new session as
             * the current session and will start {@link LIFT_ACTIVITY} if its not running to bring
             * the new session to foreground.
             */
            public static final int VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY = 0;

            /**
             * The value for {@link #EXTRA_SESSION_ACTION} extra that will keep any existing session
             * as the current session and will start {@link LIFT_ACTIVITY} if its not running to
             * bring the existing session to foreground. The new session will be added to the left
             * sidebar in the sessions list.
             */
            public static final int VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_OPEN_ACTIVITY = 1;

            /**
             * The value for {@link #EXTRA_SESSION_ACTION} extra that will set the new session as
             * the current session but will not start {@link LIFT_ACTIVITY} if its not running
             * and session(s) will be seen in Lift notification and can be clicked to bring new
             * session to foreground. If the {@link LIFT_ACTIVITY} is already running, then this
             * will behave like {@link #VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_OPEN_ACTIVITY}.
             */
            public static final int VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_DONT_OPEN_ACTIVITY = 2;

            /**
             * The value for {@link #EXTRA_SESSION_ACTION} extra that will keep any existing session
             * as the current session but will not start {@link LIFT_ACTIVITY} if its not running
             * and session(s) will be seen in Lift notification and can be clicked to bring
             * existing session to foreground. If the {@link LIFT_ACTIVITY} is already running,
             * then this will behave like {@link #VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_OPEN_ACTIVITY}.
             */
            public static final int VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_DONT_OPEN_ACTIVITY = 3;

            /** The minimum allowed value for {@link #EXTRA_SESSION_ACTION}. */
            public static final int MIN_VALUE_EXTRA_SESSION_ACTION = VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY;

            /** The maximum allowed value for {@link #EXTRA_SESSION_ACTION}. */
            public static final int MAX_VALUE_EXTRA_SESSION_ACTION = VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_DONT_OPEN_ACTIVITY;


            /** Intent {@code Bundle} extra to store result of execute command that is sent back for the
             * LIFT_SERVICE.ACTION_SERVICE_EXECUTE intent if the {@link #EXTRA_PENDING_INTENT} is not
             * {@code null} */
            public static final String EXTRA_PLUGIN_RESULT_BUNDLE = "result"; // Default: "result"
            /** Intent {@code String} extra for stdout value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE} */
            public static final String EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT = "stdout"; // Default: "stdout"
            /** Intent {@code String} extra for original length of stdout value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE} */
            public static final String EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT_ORIGINAL_LENGTH = "stdout_original_length"; // Default: "stdout_original_length"
            /** Intent {@code String} extra for stderr value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE} */
            public static final String EXTRA_PLUGIN_RESULT_BUNDLE_STDERR = "stderr"; // Default: "stderr"
            /** Intent {@code String} extra for original length of stderr value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE} */
            public static final String EXTRA_PLUGIN_RESULT_BUNDLE_STDERR_ORIGINAL_LENGTH = "stderr_original_length"; // Default: "stderr_original_length"
            /** Intent {@code int} extra for exit code value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE} */
            public static final String EXTRA_PLUGIN_RESULT_BUNDLE_EXIT_CODE = "exitCode"; // Default: "exitCode"
            /** Intent {@code int} extra for err value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE} */
            public static final String EXTRA_PLUGIN_RESULT_BUNDLE_ERR = "err"; // Default: "err"
            /** Intent {@code String} extra for errmsg value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE} */
            public static final String EXTRA_PLUGIN_RESULT_BUNDLE_ERRMSG = "errmsg"; // Default: "errmsg"

        }





        /** Lift app run command service name. */
        public static final String RUN_COMMAND_SERVICE_NAME = LIFT_PACKAGE_NAME + ".app.RunCommandService"; // Lift app service to receive commands from 3rd party apps "com.lift.app.RunCommandService"

        /**
         * Lift app run command service to receive commands sent by 3rd party apps.
         */
        public static final class RUN_COMMAND_SERVICE {

            /** Lift RUN_COMMAND Intent help url */
            public static final String RUN_COMMAND_API_HELP_URL = LIFT_GITHUB_WIKI_REPO_URL + "/RUN_COMMAND-Intent"; // Default: "https://github.com/lift/lift-app/wiki/RUN_COMMAND-Intent"


            /** Intent action to execute command with RUN_COMMAND_SERVICE */
            public static final String ACTION_RUN_COMMAND = LIFT_PACKAGE_NAME + ".RUN_COMMAND"; // Default: "com.lift.RUN_COMMAND"

            /** Intent {@code String} extra for absolute path of command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_COMMAND_PATH = LIFT_PACKAGE_NAME + ".RUN_COMMAND_PATH"; // Default: "com.lift.RUN_COMMAND_PATH"
            /** Intent {@code String[]} extra for arguments to the executable of the command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_ARGUMENTS = LIFT_PACKAGE_NAME + ".RUN_COMMAND_ARGUMENTS"; // Default: "com.lift.RUN_COMMAND_ARGUMENTS"
            /** Intent {@code boolean} extra for whether to replace comma alternative characters in arguments with comma characters for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_REPLACE_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS = LIFT_PACKAGE_NAME + ".RUN_COMMAND_REPLACE_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS"; // Default: "com.lift.RUN_COMMAND_REPLACE_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS"
            /** Intent {@code String} extra for the comma alternative characters in arguments that should be replaced instead of the default {@link #COMMA_ALTERNATIVE} for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS = LIFT_PACKAGE_NAME + ".RUN_COMMAND_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS"; // Default: "com.lift.RUN_COMMAND_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS"

            /** Intent {@code String} extra for stdin of the command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_STDIN = LIFT_PACKAGE_NAME + ".RUN_COMMAND_STDIN"; // Default: "com.lift.RUN_COMMAND_STDIN"
            /** Intent {@code String} extra for current working directory of command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_WORKDIR = LIFT_PACKAGE_NAME + ".RUN_COMMAND_WORKDIR"; // Default: "com.lift.RUN_COMMAND_WORKDIR"
            /** Intent {@code boolean} extra for whether to run command in background {@link Runner#APP_SHELL} or foreground {@link Runner#TERMINAL_SESSION} for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            @Deprecated
            public static final String EXTRA_BACKGROUND = LIFT_PACKAGE_NAME + ".RUN_COMMAND_BACKGROUND"; // Default: "com.lift.RUN_COMMAND_BACKGROUND"
            /** Intent {@code String} extra for command the {@link Runner} for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_RUNNER = LIFT_PACKAGE_NAME + ".RUN_COMMAND_RUNNER"; // Default: "com.lift.RUN_COMMAND_RUNNER"
            /** Intent {@code String} extra for custom log level for background commands defined by {@link com.lift.shared.logger.Logger} for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_BACKGROUND_CUSTOM_LOG_LEVEL = LIFT_PACKAGE_NAME + ".RUN_COMMAND_BACKGROUND_CUSTOM_LOG_LEVEL"; // Default: "com.lift.RUN_COMMAND_BACKGROUND_CUSTOM_LOG_LEVEL"
            /** Intent {@code String} extra for session action of {@link Runner#TERMINAL_SESSION} commands for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_SESSION_ACTION = LIFT_PACKAGE_NAME + ".RUN_COMMAND_SESSION_ACTION"; // Default: "com.lift.RUN_COMMAND_SESSION_ACTION"
            /** Intent {@code String} extra for shell name of commands for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_SHELL_NAME = LIFT_PACKAGE_NAME + ".RUN_COMMAND_SHELL_NAME"; // Default: "com.lift.RUN_COMMAND_SHELL_NAME"
            /** Intent {@code String} extra for the {@link ExecutionCommand.ShellCreateMode}  for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent. */
            public static final String EXTRA_SHELL_CREATE_MODE = LIFT_PACKAGE_NAME + ".RUN_COMMAND_SHELL_CREATE_MODE"; // Default: "com.lift.RUN_COMMAND_SHELL_CREATE_MODE"
            /** Intent {@code String} extra for label of the command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_COMMAND_LABEL = LIFT_PACKAGE_NAME + ".RUN_COMMAND_COMMAND_LABEL"; // Default: "com.lift.RUN_COMMAND_COMMAND_LABEL"
            /** Intent markdown {@code String} extra for description of the command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_COMMAND_DESCRIPTION = LIFT_PACKAGE_NAME + ".RUN_COMMAND_COMMAND_DESCRIPTION"; // Default: "com.lift.RUN_COMMAND_COMMAND_DESCRIPTION"
            /** Intent markdown {@code String} extra for help of the command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_COMMAND_HELP = LIFT_PACKAGE_NAME + ".RUN_COMMAND_COMMAND_HELP"; // Default: "com.lift.RUN_COMMAND_COMMAND_HELP"
            /** Intent {@code Parcelable} extra for the pending intent that should be sent with the result of the execution command to the execute command caller for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_PENDING_INTENT = LIFT_PACKAGE_NAME + ".RUN_COMMAND_PENDING_INTENT"; // Default: "com.lift.RUN_COMMAND_PENDING_INTENT"
            /** Intent {@code String} extra for the directory path in which to write the result of
             * the execution command for the execute command caller for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_RESULT_DIRECTORY = LIFT_PACKAGE_NAME + ".RUN_COMMAND_RESULT_DIRECTORY"; // Default: "com.lift.RUN_COMMAND_RESULT_DIRECTORY"
            /** Intent {@code boolean} extra for whether the result should be written to a single file
             * or multiple files (err, errmsg, stdout, stderr, exit_code) in
             * {@link #EXTRA_RESULT_DIRECTORY} for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_RESULT_SINGLE_FILE = LIFT_PACKAGE_NAME + ".RUN_COMMAND_RESULT_SINGLE_FILE"; // Default: "com.lift.RUN_COMMAND_RESULT_SINGLE_FILE"
            /** Intent {@code String} extra for the basename of the result file that should be created
             * in {@link #EXTRA_RESULT_DIRECTORY} if {@link #EXTRA_RESULT_SINGLE_FILE} is {@code true}
             * for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_RESULT_FILE_BASENAME = LIFT_PACKAGE_NAME + ".RUN_COMMAND_RESULT_FILE_BASENAME"; // Default: "com.lift.RUN_COMMAND_RESULT_FILE_BASENAME"
            /** Intent {@code String} extra for the output {@link Formatter} format of the
             * {@link #EXTRA_RESULT_FILE_BASENAME} result file for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_RESULT_FILE_OUTPUT_FORMAT = LIFT_PACKAGE_NAME + ".RUN_COMMAND_RESULT_FILE_OUTPUT_FORMAT"; // Default: "com.lift.RUN_COMMAND_RESULT_FILE_OUTPUT_FORMAT"
            /** Intent {@code String} extra for the error {@link Formatter} format of the
             * {@link #EXTRA_RESULT_FILE_BASENAME} result file for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_RESULT_FILE_ERROR_FORMAT = LIFT_PACKAGE_NAME + ".RUN_COMMAND_RESULT_FILE_ERROR_FORMAT"; // Default: "com.lift.RUN_COMMAND_RESULT_FILE_ERROR_FORMAT"
            /** Intent {@code String} extra for the optional suffix of the result files that should be
             * created in {@link #EXTRA_RESULT_DIRECTORY} if {@link #EXTRA_RESULT_SINGLE_FILE} is
             * {@code false} for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_RESULT_FILES_SUFFIX = LIFT_PACKAGE_NAME + ".RUN_COMMAND_RESULT_FILES_SUFFIX"; // Default: "com.lift.RUN_COMMAND_RESULT_FILES_SUFFIX"

        }
    }


    /**
     * Lift:API app constants.
     */
    public static final class LIFT_API_APP {

        /** Lift:API app main activity name. */
        public static final String LIFT_API_MAIN_ACTIVITY_NAME = LIFT_API_PACKAGE_NAME + ".activities.LiftAPIMainActivity"; // Default: "com.lift.api.activities.LiftAPIMainActivity"

        /** Lift:API app launcher activity name. This is an `activity-alias` for {@link #LIFT_API_MAIN_ACTIVITY_NAME} used for launchers with {@link Intent#CATEGORY_LAUNCHER}. */
        public static final String LIFT_API_LAUNCHER_ACTIVITY_NAME = LIFT_API_PACKAGE_NAME + ".activities.LiftAPILauncherActivity"; // Default: "com.lift.api.activities.LiftAPILauncherActivity"

    }





    /**
     * Lift:Boot app constants.
     */
    public static final class LIFT_BOOT_APP {

        /** Lift:Boot app main activity name. */
        public static final String LIFT_BOOT_MAIN_ACTIVITY_NAME = LIFT_BOOT_PACKAGE_NAME + ".activities.LiftBootMainActivity"; // Default: "com.lift.boot.activities.LiftBootMainActivity"

        /** Lift:Boot app launcher activity name. This is an `activity-alias` for {@link #LIFT_BOOT_MAIN_ACTIVITY_NAME} used for launchers with {@link Intent#CATEGORY_LAUNCHER}. */
        public static final String LIFT_BOOT_LAUNCHER_ACTIVITY_NAME = LIFT_BOOT_PACKAGE_NAME + ".activities.LiftBootLauncherActivity"; // Default: "com.lift.boot.activities.LiftBootLauncherActivity"

    }





    /**
     * Lift:Float app constants.
     */
    public static final class LIFT_FLOAT_APP {

        /** Lift:Float app core activity name. */
        public static final String LIFT_FLOAT_ACTIVITY_NAME = LIFT_FLOAT_PACKAGE_NAME + ".LiftFloatActivity"; // Default: "com.lift.window.LiftFloatActivity"

        /** Lift:Float app core service name. */
        public static final String LIFT_FLOAT_SERVICE_NAME = LIFT_FLOAT_PACKAGE_NAME + ".LiftFloatService"; // Default: "com.lift.window.LiftFloatService"

        /**
         * Lift:Float app core service.
         */
        public static final class LIFT_FLOAT_SERVICE {

            /** Intent action to stop LIFT_FLOAT_SERVICE. */
            public static final String ACTION_STOP_SERVICE = LIFT_FLOAT_PACKAGE_NAME + ".ACTION_STOP_SERVICE"; // Default: "com.lift.float.ACTION_STOP_SERVICE"

            /** Intent action to show float window. */
            public static final String ACTION_SHOW = LIFT_FLOAT_PACKAGE_NAME + ".ACTION_SHOW"; // Default: "com.lift.float.ACTION_SHOW"

            /** Intent action to hide float window. */
            public static final String ACTION_HIDE = LIFT_FLOAT_PACKAGE_NAME + ".ACTION_HIDE"; // Default: "com.lift.float.ACTION_HIDE"

        }

    }





    /**
     * Lift:Styling app constants.
     */
    public static final class LIFT_STYLING_APP {

        /** Lift:Styling app core activity name. */
        public static final String LIFT_STYLING_ACTIVITY_NAME = LIFT_STYLING_PACKAGE_NAME + ".LiftStyleActivity"; // Default: "com.lift.styling.LiftStyleActivity"


        /** Lift:Styling app main activity name. */
        public static final String LIFT_STYLING_MAIN_ACTIVITY_NAME = LIFT_STYLING_PACKAGE_NAME + ".activities.LiftStylingMainActivity"; // Default: "com.lift.styling.activities.LiftStylingMainActivity"

        /** Lift:Styling app launcher activity name. This is an `activity-alias` for {@link #LIFT_STYLING_MAIN_ACTIVITY_NAME} used for launchers with {@link Intent#CATEGORY_LAUNCHER}. */
        public static final String LIFT_STYLING_LAUNCHER_ACTIVITY_NAME = LIFT_STYLING_PACKAGE_NAME + ".activities.LiftStylingLauncherActivity"; // Default: "com.lift.styling.activities.LiftStylingLauncherActivity"

    }





    /**
     * Lift:Tasker app constants.
     */
    public static final class LIFT_TASKER_APP {

        /** Lift:Tasker app main activity name. */
        public static final String LIFT_TASKER_MAIN_ACTIVITY_NAME = LIFT_TASKER_PACKAGE_NAME + ".activities.LiftTaskerMainActivity"; // Default: "com.lift.tasker.activities.LiftTaskerMainActivity"

        /** Lift:Tasker app launcher activity name. This is an `activity-alias` for {@link #LIFT_TASKER_MAIN_ACTIVITY_NAME} used for launchers with {@link Intent#CATEGORY_LAUNCHER}. */
        public static final String LIFT_TASKER_LAUNCHER_ACTIVITY_NAME = LIFT_TASKER_PACKAGE_NAME + ".activities.LiftTaskerLauncherActivity"; // Default: "com.lift.tasker.activities.LiftTaskerLauncherActivity"

    }





    /**
     * Lift:Widget app constants.
     */
    public static final class LIFT_WIDGET_APP {

        /** Lift:Widget app main activity name. */
        public static final String LIFT_WIDGET_MAIN_ACTIVITY_NAME = LIFT_WIDGET_PACKAGE_NAME + ".activities.LiftWidgetMainActivity"; // Default: "com.lift.widget.activities.LiftWidgetMainActivity"

        /** Lift:Widget app launcher activity name. This is an `activity-alias` for {@link #LIFT_WIDGET_MAIN_ACTIVITY_NAME} used for launchers with {@link Intent#CATEGORY_LAUNCHER}. */
        public static final String LIFT_WIDGET_LAUNCHER_ACTIVITY_NAME = LIFT_WIDGET_PACKAGE_NAME + ".activities.LiftWidgetLauncherActivity"; // Default: "com.lift.widget.activities.LiftWidgetLauncherActivity"


        /**  Intent {@code String} extra for the token of the Lift:Widget app shortcuts. */
        public static final String EXTRA_TOKEN_NAME = LIFT_PACKAGE_NAME + ".shortcut.token"; // Default: "com.lift.shortcut.token"


        /**
         * Lift:Widget app {@link android.appwidget.AppWidgetProvider} class.
         */
        public static final class LIFT_WIDGET_PROVIDER {

            /** Intent action for if an item is clicked in the widget. */
            public static final String ACTION_WIDGET_ITEM_CLICKED = LIFT_WIDGET_PACKAGE_NAME + ".ACTION_WIDGET_ITEM_CLICKED"; // Default: "com.lift.widget.ACTION_WIDGET_ITEM_CLICKED"


            /** Intent action to refresh files in the widget. */
            public static final String ACTION_REFRESH_WIDGET = LIFT_WIDGET_PACKAGE_NAME + ".ACTION_REFRESH_WIDGET"; // Default: "com.lift.widget.ACTION_REFRESH_WIDGET"


            /**  Intent {@code String} extra for the file clicked for the LIFT_WIDGET_PROVIDER.ACTION_WIDGET_ITEM_CLICKED intent. */
            public static final String EXTRA_FILE_CLICKED = LIFT_WIDGET_PACKAGE_NAME + ".EXTRA_FILE_CLICKED"; // Default: "com.lift.widget.EXTRA_FILE_CLICKED"

        }

    }

}
