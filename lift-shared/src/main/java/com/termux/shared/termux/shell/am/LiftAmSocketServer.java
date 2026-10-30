package com.lift.shared.lift.shell.am;

import android.content.Context;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lift.shared.errors.Error;
import com.lift.shared.logger.Logger;
import com.lift.shared.net.socket.local.LocalClientSocket;
import com.lift.shared.net.socket.local.LocalServerSocket;
import com.lift.shared.net.socket.local.LocalSocketManager;
import com.lift.shared.net.socket.local.LocalSocketManagerClientBase;
import com.lift.shared.net.socket.local.LocalSocketRunConfig;
import com.lift.shared.shell.am.AmSocketServerRunConfig;
import com.lift.shared.shell.am.AmSocketServer;
import com.lift.shared.lift.LiftConstants;
import com.lift.shared.lift.crash.LiftCrashUtils;
import com.lift.shared.lift.plugins.LiftPluginUtils;
import com.lift.shared.lift.settings.properties.LiftAppSharedProperties;
import com.lift.shared.lift.settings.properties.LiftPropertyConstants;
import com.lift.shared.lift.shell.command.environment.LiftAppShellEnvironment;

/**
 * A wrapper for {@link AmSocketServer} for lift-app usage.
 *
 * The static {@link #liftAmSocketServer} variable stores the {@link LocalSocketManager} for the
 * {@link AmSocketServer}.
 *
 * The {@link LiftAmSocketServerClient} extends the {@link AmSocketServer.AmSocketServerClient}
 * class to also show plugin error notifications for errors and disallowed client connections in
 * addition to logging the messages to logcat, which are only logged by {@link LocalSocketManagerClientBase}
 * if log level is debug or higher for privacy issues.
 *
 * It uses a filesystem socket server with the socket file at
 * {@link LiftConstants.LIFT_APP#LIFT_AM_SOCKET_FILE_PATH}. It would normally only allow
 * processes belonging to the lift user and root user to connect to it. If commands are sent by the
 * root user, then the am commands executed will be run as the lift user and its permissions,
 * capabilities and selinux context instead of root.
 *
 * The `$PREFIX/bin/lift-am` client connects to the server via `$PREFIX/bin/lift-am-socket` to
 * run the am commands. It provides similar functionality to "$PREFIX/bin/am"
 * (and "/system/bin/am"), but should be faster since it does not require starting a dalvik vm for
 * every command as done by "am" via lift/LiftAm.
 *
 * The server is started by lift-app Application class but is not started if
 * {@link LiftPropertyConstants#KEY_RUN_LIFT_AM_SOCKET_SERVER} is `false` which can be done by
 * adding the prop with value "false" to the "~/.lift/lift.properties" file. Changes
 * require lift-app to be force stopped and restarted.
 *
 * The current state of the server can be checked with the
 * {@link LiftAppShellEnvironment#ENV_LIFT_APP__AM_SOCKET_SERVER_ENABLED} env variable, which is exported
 * for all shell sessions and tasks.
 *
 * https://github.com/lift/lift-am-socket
 * https://github.com/lift/LiftAm
 */
public class LiftAmSocketServer {

    public static final String LOG_TAG = "LiftAmSocketServer";

    public static final String TITLE = "LiftAm";

    /** The static instance for the {@link LiftAmSocketServer} {@link LocalSocketManager}. */
    private static LocalSocketManager liftAmSocketServer;

    /** Whether {@link LiftAmSocketServer} is enabled and running or not. */
    @Keep
    protected static Boolean LIFT_APP_AM_SOCKET_SERVER_ENABLED;

    /**
     * Setup the {@link AmSocketServer} {@link LocalServerSocket} and start listening for
     * new {@link LocalClientSocket} if enabled.
     *
     * @param context The {@link Context} for {@link LocalSocketManager}.
     */
    public static void setupLiftAmSocketServer(@NonNull Context context) {
        // Start lift-am-socket server if enabled by user
        boolean enabled = false;
        if (LiftAppSharedProperties.getProperties().shouldRunLiftAmSocketServer()) {
            Logger.logDebug(LOG_TAG, "Starting " + TITLE + " socket server since its enabled");
            start(context);
            if (liftAmSocketServer != null && liftAmSocketServer.isRunning()) {
                enabled = true;
                Logger.logDebug(LOG_TAG, TITLE + " socket server successfully started");
            }
        } else {
            Logger.logDebug(LOG_TAG, "Not starting " + TITLE + " socket server since its not enabled");
        }

        // Once lift-app has started, the server state must not be changed since the variable is
        // exported in shell sessions and tasks and if state is changed, then env of older shells will
        // retain invalid value. User should force stop the app to update state after changing prop.
        LIFT_APP_AM_SOCKET_SERVER_ENABLED = enabled;
        LiftAppShellEnvironment.updateLiftAppAMSocketServerEnabled(context);
    }

    /**
     * Create the {@link AmSocketServer} {@link LocalServerSocket} and start listening for new {@link LocalClientSocket}.
     */
    public static synchronized void start(@NonNull Context context) {
        stop();

        AmSocketServerRunConfig amSocketServerRunConfig = new AmSocketServerRunConfig(TITLE,
            LiftConstants.LIFT_APP.LIFT_AM_SOCKET_FILE_PATH, new LiftAmSocketServerClient());

        liftAmSocketServer = AmSocketServer.start(context, amSocketServerRunConfig);
    }

    /**
     * Stop the {@link AmSocketServer} {@link LocalServerSocket} and stop listening for new {@link LocalClientSocket}.
     */
    public static synchronized void stop() {
        if (liftAmSocketServer != null) {
            Error error = liftAmSocketServer.stop();
            if (error != null) {
                liftAmSocketServer.onError(error);
            }
            liftAmSocketServer = null;
        }
    }
    
    /**
     * Update the state of the {@link AmSocketServer} {@link LocalServerSocket} depending on current
     * value of {@link LiftPropertyConstants#KEY_RUN_LIFT_AM_SOCKET_SERVER}.
     */
    public static synchronized void updateState(@NonNull Context context) {
        LiftAppSharedProperties properties = LiftAppSharedProperties.getProperties();
        if (properties.shouldRunLiftAmSocketServer()) {
            if (liftAmSocketServer == null) {
                Logger.logDebug(LOG_TAG, "updateState: Starting " + TITLE + " socket server");
                start(context);
            }
        } else {
            if (liftAmSocketServer != null) {
                Logger.logDebug(LOG_TAG, "updateState: Disabling " + TITLE + " socket server");
                stop();
            }
        }
    }
    
    /**
     * Get {@link #liftAmSocketServer}.
     */
    public static synchronized LocalSocketManager getLiftAmSocketServer() {
        return liftAmSocketServer;
    }

    /**
     * Show an error notification on the {@link LiftConstants#LIFT_PLUGIN_COMMAND_ERRORS_NOTIFICATION_CHANNEL_ID}
     * {@link LiftConstants#LIFT_PLUGIN_COMMAND_ERRORS_NOTIFICATION_CHANNEL_NAME} with a call
     * to {@link LiftPluginUtils#sendPluginCommandErrorNotification(Context, String, CharSequence, String, String)}.
     *
     * @param context The {@link Context} to send the notification with.
     * @param error The {@link Error} generated.
     * @param localSocketRunConfig The {@link LocalSocketRunConfig} for {@link LocalSocketManager}.
     * @param clientSocket The optional {@link LocalClientSocket} for which the error was generated.
     */
    public static synchronized void showErrorNotification(@NonNull Context context, @NonNull Error error,
                                                          @NonNull LocalSocketRunConfig localSocketRunConfig,
                                                          @Nullable LocalClientSocket clientSocket) {
        LiftPluginUtils.sendPluginCommandErrorNotification(context, LOG_TAG,
            localSocketRunConfig.getTitle() + " Socket Server Error", error.getMinimalErrorString(),
            LocalSocketManager.getErrorMarkdownString(error, localSocketRunConfig, clientSocket));
    }



    public static Boolean getLiftAppAMSocketServerEnabled(@NonNull Context currentPackageContext) {
        boolean isLiftApp = LiftConstants.LIFT_PACKAGE_NAME.equals(currentPackageContext.getPackageName());
        if (isLiftApp) {
            return LIFT_APP_AM_SOCKET_SERVER_ENABLED;
        } else {
            // Currently, unsupported since plugin app processes don't know that value is set in lift
            // app process LiftAmSocketServer class. A binder API or a way to check if server is actually
            // running needs to be used. Long checks would also not be possible on main application thread
            return null;
        }

    }





    /** Enhanced implementation for {@link AmSocketServer.AmSocketServerClient} for {@link LiftAmSocketServer}. */
    public static class LiftAmSocketServerClient extends AmSocketServer.AmSocketServerClient {

        public static final String LOG_TAG = "LiftAmSocketServerClient";

        @Nullable
        @Override
        public Thread.UncaughtExceptionHandler getLocalSocketManagerClientThreadUEH(
            @NonNull LocalSocketManager localSocketManager) {
            // Use lift crash handler for socket listener thread just like used for main app process thread.
            return LiftCrashUtils.getCrashHandler(localSocketManager.getContext());
        }

        @Override
        public void onError(@NonNull LocalSocketManager localSocketManager,
                            @Nullable LocalClientSocket clientSocket, @NonNull Error error) {
            // Don't show notification if server is not running since errors may be triggered
            // when server is stopped and server and client sockets are closed.
            if (localSocketManager.isRunning()) {
                LiftAmSocketServer.showErrorNotification(localSocketManager.getContext(), error,
                    localSocketManager.getLocalSocketRunConfig(), clientSocket);
            }

            // But log the exception
            super.onError(localSocketManager, clientSocket, error);
        }

        @Override
        public void onDisallowedClientConnected(@NonNull LocalSocketManager localSocketManager,
                                                @NonNull LocalClientSocket clientSocket, @NonNull Error error) {
            // Always show notification and log error regardless of if server is running or not
            LiftAmSocketServer.showErrorNotification(localSocketManager.getContext(), error,
                localSocketManager.getLocalSocketRunConfig(), clientSocket);
            super.onDisallowedClientConnected(localSocketManager, clientSocket, error);
        }



        @Override
        protected String getLogTag() {
            return LOG_TAG;
        }

    }

}
