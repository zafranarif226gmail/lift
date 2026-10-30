package com.lift.app;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lift.R;
import com.lift.app.event.SystemEventReceiver;
import com.lift.app.terminal.LiftTerminalSessionActivityClient;
import com.lift.app.terminal.LiftTerminalSessionServiceClient;
import com.lift.shared.lift.plugins.LiftPluginUtils;
import com.lift.shared.data.IntentUtils;
import com.lift.shared.net.uri.UriUtils;
import com.lift.shared.errors.Errno;
import com.lift.shared.shell.ShellUtils;
import com.lift.shared.shell.command.runner.app.AppShell;
import com.lift.shared.lift.settings.properties.LiftAppSharedProperties;
import com.lift.shared.lift.shell.command.environment.LiftShellEnvironment;
import com.lift.shared.lift.shell.LiftShellUtils;
import com.lift.shared.lift.LiftConstants;
import com.lift.shared.lift.LiftConstants.LIFT_APP.LIFT_ACTIVITY;
import com.lift.shared.lift.LiftConstants.LIFT_APP.LIFT_SERVICE;
import com.lift.shared.lift.settings.preferences.LiftAppSharedPreferences;
import com.lift.shared.lift.shell.LiftShellManager;
import com.lift.shared.lift.shell.command.runner.terminal.LiftSession;
import com.lift.shared.lift.terminal.LiftTerminalSessionClientBase;
import com.lift.shared.logger.Logger;
import com.lift.shared.notification.NotificationUtils;
import com.lift.shared.android.PermissionUtils;
import com.lift.shared.data.DataUtils;
import com.lift.shared.shell.command.ExecutionCommand;
import com.lift.shared.shell.command.ExecutionCommand.Runner;
import com.lift.shared.shell.command.ExecutionCommand.ShellCreateMode;
import com.lift.terminal.TerminalEmulator;
import com.lift.terminal.TerminalSession;
import com.lift.terminal.TerminalSessionClient;

import java.util.ArrayList;
import java.util.List;

/**
 * A service holding a list of {@link LiftSession} in {@link LiftShellManager#mLiftSessions} and background {@link AppShell}
 * in {@link LiftShellManager#mLiftTasks}, showing a foreground notification while running so that it is not terminated.
 * The user interacts with the session through {@link LiftActivity}, but this service may outlive
 * the activity when the user or the system disposes of the activity. In that case the user may
 * restart {@link LiftActivity} later to yet again access the sessions.
 * <p/>
 * In order to keep both terminal sessions and spawned processes (who may outlive the terminal sessions) alive as long
 * as wanted by the user this service is a foreground service, {@link Service#startForeground(int, Notification)}.
 * <p/>
 * Optionally may hold a wake and a wifi lock, in which case that is shown in the notification - see
 * {@link #buildNotification()}.
 */
public final class LiftService extends Service implements AppShell.AppShellClient, LiftSession.LiftSessionClient {

    /** This service is only bound from inside the same process and never uses IPC. */
    class LocalBinder extends Binder {
        public final LiftService service = LiftService.this;
    }

    private final IBinder mBinder = new LocalBinder();

    private final Handler mHandler = new Handler();


    /** The full implementation of the {@link TerminalSessionClient} interface to be used by {@link TerminalSession}
     * that holds activity references for activity related functions.
     * Note that the service may often outlive the activity, so need to clear this reference.
     */
    private LiftTerminalSessionActivityClient mLiftTerminalSessionActivityClient;

    /** The basic implementation of the {@link TerminalSessionClient} interface to be used by {@link TerminalSession}
     * that does not hold activity references and only a service reference.
     */
    private final LiftTerminalSessionServiceClient mLiftTerminalSessionServiceClient = new LiftTerminalSessionServiceClient(this);

    /**
     * Lift app shared properties manager, loaded from lift.properties
     */
    private LiftAppSharedProperties mProperties;

    /**
     * Lift app shell manager
     */
    private LiftShellManager mShellManager;

    /** The wake lock and wifi lock are always acquired and released together. */
    private PowerManager.WakeLock mWakeLock;
    private WifiManager.WifiLock mWifiLock;

    /** If the user has executed the {@link LIFT_SERVICE#ACTION_STOP_SERVICE} intent. */
    boolean mWantsToStop = false;

    private static final String LOG_TAG = "LiftService";

    @Override
    public void onCreate() {
        Logger.logVerbose(LOG_TAG, "onCreate");

        // Get Lift app SharedProperties without loading from disk since LiftApplication handles
        // load and LiftActivity handles reloads
        mProperties = LiftAppSharedProperties.getProperties();

        mShellManager = LiftShellManager.getShellManager();

        runStartForeground();

        SystemEventReceiver.registerPackageUpdateEvents(this);
    }

    @SuppressLint("Wakelock")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.logDebug(LOG_TAG, "onStartCommand");

        // Run again in case service is already started and onCreate() is not called
        runStartForeground();

        String action = null;
        if (intent != null) {
            Logger.logVerboseExtended(LOG_TAG, "Intent Received:\n" + IntentUtils.getIntentString(intent));
            action = intent.getAction();
        }

        if (action != null) {
            switch (action) {
                case LIFT_SERVICE.ACTION_STOP_SERVICE:
                    Logger.logDebug(LOG_TAG, "ACTION_STOP_SERVICE intent received");
                    actionStopService();
                    break;
                case LIFT_SERVICE.ACTION_WAKE_LOCK:
                    Logger.logDebug(LOG_TAG, "ACTION_WAKE_LOCK intent received");
                    actionAcquireWakeLock();
                    break;
                case LIFT_SERVICE.ACTION_WAKE_UNLOCK:
                    Logger.logDebug(LOG_TAG, "ACTION_WAKE_UNLOCK intent received");
                    actionReleaseWakeLock(true);
                    break;
                case LIFT_SERVICE.ACTION_SERVICE_EXECUTE:
                    Logger.logDebug(LOG_TAG, "ACTION_SERVICE_EXECUTE intent received");
                    actionServiceExecute(intent);
                    break;
                default:
                    Logger.logError(LOG_TAG, "Invalid action: \"" + action + "\"");
                    break;
            }
        }

        // If this service really do get killed, there is no point restarting it automatically - let the user do on next
        // start of {@link Term):
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Logger.logVerbose(LOG_TAG, "onDestroy");

        LiftShellUtils.clearLiftTMPDIR(true);

        actionReleaseWakeLock(false);
        if (!mWantsToStop)
            killAllLiftExecutionCommands();

        LiftShellManager.onAppExit(this);

        SystemEventReceiver.unregisterPackageUpdateEvents(this);

        runStopForeground();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Logger.logVerbose(LOG_TAG, "onBind");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Logger.logVerbose(LOG_TAG, "onUnbind");

        // Since we cannot rely on {@link LiftActivity.onDestroy()} to always complete,
        // we unset clients here as well if it failed, so that we do not leave service and session
        // clients with references to the activity.
        if (mLiftTerminalSessionActivityClient != null)
            unsetLiftTerminalSessionClient();
        return false;
    }

    /** Make service run in foreground mode. */
    private void runStartForeground() {
        setupNotificationChannel();
        startForeground(LiftConstants.LIFT_APP_NOTIFICATION_ID, buildNotification());
    }

    /** Make service leave foreground mode. */
    private void runStopForeground() {
        stopForeground(true);
    }

    /** Request to stop service. */
    private void requestStopService() {
        Logger.logDebug(LOG_TAG, "Requesting to stop service");
        runStopForeground();
        stopSelf();
    }

    /** Process action to stop service. */
    private void actionStopService() {
        mWantsToStop = true;
        killAllLiftExecutionCommands();
        requestStopService();
    }

    /** Kill all LiftSessions and LiftTasks by sending SIGKILL to their processes.
     *
     * For LiftSessions, all sessions will be killed, whether user manually exited Lift or if
     * onDestroy() was directly called because of unintended shutdown. The processing of results
     * will only be done if user manually exited lift or if the session was started by a plugin
     * which **expects** the result back via a pending intent.
     *
     * For LiftTasks, only tasks that were started by a plugin which **expects** the result
     * back via a pending intent will be killed, whether user manually exited Lift or if
     * onDestroy() was directly called because of unintended shutdown. The processing of results
     * will always be done for the tasks that are killed. The remaining processes will keep on
     * running until the lift app process is killed by android, like by OOM, so we let them run
     * as long as they can.
     *
     * Some plugin execution commands may not have been processed and added to mLiftSessions and
     * mLiftTasks lists before the service is killed, so we maintain a separate
     * mPendingPluginExecutionCommands list for those, so that we can notify the pending intent
     * creators that execution was cancelled.
     *
     * Note that if user didn't manually exit Lift and if onDestroy() was directly called because
     * of unintended shutdown, like android deciding to kill the service, then there will be no
     * guarantee that onDestroy() will be allowed to finish and lift app process may be killed before
     * it has finished. This means that in those cases some results may not be sent back to their
     * creators for plugin commands but we still try to process whatever results can be processed
     * despite the unreliable behaviour of onDestroy().
     *
     * Note that if don't kill the processes started by plugins which **expect** the result back
     * and notify their creators that they have been killed, then they may get stuck waiting for
     * the results forever like in case of commands started by Lift:Tasker or RUN_COMMAND intent,
     * since once LiftService has been killed, no result will be sent back. They may still get
     * stuck if lift app process gets killed, so for this case reasonable timeout values should
     * be used, like in Tasker for the Lift:Tasker actions.
     *
     * We make copies of each list since items are removed inside the loop.
     */
    private synchronized void killAllLiftExecutionCommands() {
        boolean processResult;

        Logger.logDebug(LOG_TAG, "Killing LiftSessions=" + mShellManager.mLiftSessions.size() +
            ", LiftTasks=" + mShellManager.mLiftTasks.size() +
            ", PendingPluginExecutionCommands=" + mShellManager.mPendingPluginExecutionCommands.size());

        List<LiftSession> liftSessions = new ArrayList<>(mShellManager.mLiftSessions);
        List<AppShell> liftTasks = new ArrayList<>(mShellManager.mLiftTasks);
        List<ExecutionCommand> pendingPluginExecutionCommands = new ArrayList<>(mShellManager.mPendingPluginExecutionCommands);

        for (int i = 0; i < liftSessions.size(); i++) {
            ExecutionCommand executionCommand = liftSessions.get(i).getExecutionCommand();
            processResult = mWantsToStop || executionCommand.isPluginExecutionCommandWithPendingResult();
            liftSessions.get(i).killIfExecuting(this, processResult);
            if (!processResult)
                mShellManager.mLiftSessions.remove(liftSessions.get(i));
        }


        for (int i = 0; i < liftTasks.size(); i++) {
            ExecutionCommand executionCommand = liftTasks.get(i).getExecutionCommand();
            if (executionCommand.isPluginExecutionCommandWithPendingResult())
                liftTasks.get(i).killIfExecuting(this, true);
            else
                mShellManager.mLiftTasks.remove(liftTasks.get(i));
        }

        for (int i = 0; i < pendingPluginExecutionCommands.size(); i++) {
            ExecutionCommand executionCommand = pendingPluginExecutionCommands.get(i);
            if (!executionCommand.shouldNotProcessResults() && executionCommand.isPluginExecutionCommandWithPendingResult()) {
                if (executionCommand.setStateFailed(Errno.ERRNO_CANCELLED.getCode(), this.getString(com.lift.shared.R.string.error_execution_cancelled))) {
                    LiftPluginUtils.processPluginExecutionCommandResult(this, LOG_TAG, executionCommand);
                }
            }
        }
    }



    /** Process action to acquire Power and Wi-Fi WakeLocks. */
    @SuppressLint({"WakelockTimeout", "BatteryLife"})
    private void actionAcquireWakeLock() {
        if (mWakeLock != null) {
            Logger.logDebug(LOG_TAG, "Ignoring acquiring WakeLocks since they are already held");
            return;
        }

        Logger.logDebug(LOG_TAG, "Acquiring WakeLocks");

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LiftConstants.LIFT_APP_NAME.toLowerCase() + ":service-wakelock");
        mWakeLock.acquire();

        // http://tools.android.com/tech-docs/lint-in-studio-2-3#TOC-WifiManager-Leak
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, LiftConstants.LIFT_APP_NAME.toLowerCase());
        mWifiLock.acquire();

        if (!PermissionUtils.checkIfBatteryOptimizationsDisabled(this)) {
            PermissionUtils.requestDisableBatteryOptimizations(this);
        }

        updateNotification();

        Logger.logDebug(LOG_TAG, "WakeLocks acquired successfully");

    }

    /** Process action to release Power and Wi-Fi WakeLocks. */
    private void actionReleaseWakeLock(boolean updateNotification) {
        if (mWakeLock == null && mWifiLock == null) {
            Logger.logDebug(LOG_TAG, "Ignoring releasing WakeLocks since none are already held");
            return;
        }

        Logger.logDebug(LOG_TAG, "Releasing WakeLocks");

        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }

        if (mWifiLock != null) {
            mWifiLock.release();
            mWifiLock = null;
        }

        if (updateNotification)
            updateNotification();

        Logger.logDebug(LOG_TAG, "WakeLocks released successfully");
    }

    /** Process {@link LIFT_SERVICE#ACTION_SERVICE_EXECUTE} intent to execute a shell command in
     * a foreground LiftSession or in a background LiftTask. */
    private void actionServiceExecute(Intent intent) {
        if (intent == null) {
            Logger.logError(LOG_TAG, "Ignoring null intent to actionServiceExecute");
            return;
        }

        ExecutionCommand executionCommand = new ExecutionCommand(LiftShellManager.getNextShellId());

        executionCommand.executableUri = intent.getData();
        executionCommand.isPluginExecutionCommand = true;

        // If EXTRA_RUNNER is passed, use that, otherwise check EXTRA_BACKGROUND and default to Runner.TERMINAL_SESSION
        executionCommand.runner = IntentUtils.getStringExtraIfSet(intent, LIFT_SERVICE.EXTRA_RUNNER,
            (intent.getBooleanExtra(LIFT_SERVICE.EXTRA_BACKGROUND, false) ? Runner.APP_SHELL.getName() : Runner.TERMINAL_SESSION.getName()));
        if (Runner.runnerOf(executionCommand.runner) == null) {
            String errmsg = this.getString(R.string.error_lift_service_invalid_execution_command_runner, executionCommand.runner);
            executionCommand.setStateFailed(Errno.ERRNO_FAILED.getCode(), errmsg);
            LiftPluginUtils.processPluginExecutionCommandError(this, LOG_TAG, executionCommand, false);
            return;
        }

        if (executionCommand.executableUri != null) {
            Logger.logVerbose(LOG_TAG, "uri: \"" + executionCommand.executableUri + "\", path: \"" + executionCommand.executableUri.getPath() + "\", fragment: \"" + executionCommand.executableUri.getFragment() + "\"");

            // Get full path including fragment (anything after last "#")
            executionCommand.executable = UriUtils.getUriFilePathWithFragment(executionCommand.executableUri);
            executionCommand.arguments = IntentUtils.getStringArrayExtraIfSet(intent, LIFT_SERVICE.EXTRA_ARGUMENTS, null);
            if (Runner.APP_SHELL.equalsRunner(executionCommand.runner))
                executionCommand.stdin = IntentUtils.getStringExtraIfSet(intent, LIFT_SERVICE.EXTRA_STDIN, null);
            executionCommand.backgroundCustomLogLevel = IntentUtils.getIntegerExtraIfSet(intent, LIFT_SERVICE.EXTRA_BACKGROUND_CUSTOM_LOG_LEVEL, null);
        }

        executionCommand.workingDirectory = IntentUtils.getStringExtraIfSet(intent, LIFT_SERVICE.EXTRA_WORKDIR, null);
        executionCommand.isFailsafe = intent.getBooleanExtra(LIFT_ACTIVITY.EXTRA_FAILSAFE_SESSION, false);
        executionCommand.sessionAction = intent.getStringExtra(LIFT_SERVICE.EXTRA_SESSION_ACTION);
        executionCommand.shellName = IntentUtils.getStringExtraIfSet(intent, LIFT_SERVICE.EXTRA_SHELL_NAME, null);
        executionCommand.shellCreateMode = IntentUtils.getStringExtraIfSet(intent, LIFT_SERVICE.EXTRA_SHELL_CREATE_MODE, null);
        executionCommand.commandLabel = IntentUtils.getStringExtraIfSet(intent, LIFT_SERVICE.EXTRA_COMMAND_LABEL, "Execution Intent Command");
        executionCommand.commandDescription = IntentUtils.getStringExtraIfSet(intent, LIFT_SERVICE.EXTRA_COMMAND_DESCRIPTION, null);
        executionCommand.commandHelp = IntentUtils.getStringExtraIfSet(intent, LIFT_SERVICE.EXTRA_COMMAND_HELP, null);
        executionCommand.pluginAPIHelp = IntentUtils.getStringExtraIfSet(intent, LIFT_SERVICE.EXTRA_PLUGIN_API_HELP, null);
        executionCommand.resultConfig.resultPendingIntent = intent.getParcelableExtra(LIFT_SERVICE.EXTRA_PENDING_INTENT);
        executionCommand.resultConfig.resultDirectoryPath = IntentUtils.getStringExtraIfSet(intent, LIFT_SERVICE.EXTRA_RESULT_DIRECTORY, null);
        if (executionCommand.resultConfig.resultDirectoryPath != null) {
            executionCommand.resultConfig.resultSingleFile = intent.getBooleanExtra(LIFT_SERVICE.EXTRA_RESULT_SINGLE_FILE, false);
            executionCommand.resultConfig.resultFileBasename = IntentUtils.getStringExtraIfSet(intent, LIFT_SERVICE.EXTRA_RESULT_FILE_BASENAME, null);
            executionCommand.resultConfig.resultFileOutputFormat = IntentUtils.getStringExtraIfSet(intent, LIFT_SERVICE.EXTRA_RESULT_FILE_OUTPUT_FORMAT, null);
            executionCommand.resultConfig.resultFileErrorFormat = IntentUtils.getStringExtraIfSet(intent, LIFT_SERVICE.EXTRA_RESULT_FILE_ERROR_FORMAT, null);
            executionCommand.resultConfig.resultFilesSuffix = IntentUtils.getStringExtraIfSet(intent, LIFT_SERVICE.EXTRA_RESULT_FILES_SUFFIX, null);
        }

        if (executionCommand.shellCreateMode == null)
            executionCommand.shellCreateMode = ShellCreateMode.ALWAYS.getMode();

        // Add the execution command to pending plugin execution commands list
        mShellManager.mPendingPluginExecutionCommands.add(executionCommand);

        if (Runner.APP_SHELL.equalsRunner(executionCommand.runner))
            executeLiftTaskCommand(executionCommand);
        else if (Runner.TERMINAL_SESSION.equalsRunner(executionCommand.runner))
            executeLiftSessionCommand(executionCommand);
        else {
            String errmsg = getString(R.string.error_lift_service_unsupported_execution_command_runner, executionCommand.runner);
            executionCommand.setStateFailed(Errno.ERRNO_FAILED.getCode(), errmsg);
            LiftPluginUtils.processPluginExecutionCommandError(this, LOG_TAG, executionCommand, false);
        }
    }





    /** Execute a shell command in background LiftTask. */
    private void executeLiftTaskCommand(ExecutionCommand executionCommand) {
        if (executionCommand == null) return;

        Logger.logDebug(LOG_TAG, "Executing background \"" + executionCommand.getCommandIdAndLabelLogString() + "\" LiftTask command");

        // Transform executable path to shell/session name, e.g. "/bin/do-something.sh" => "do-something.sh".
        if (executionCommand.shellName == null && executionCommand.executable != null)
            executionCommand.shellName = ShellUtils.getExecutableBasename(executionCommand.executable);

        AppShell newLiftTask = null;
        ShellCreateMode shellCreateMode = processShellCreateMode(executionCommand);
        if (shellCreateMode == null) return;
        if (ShellCreateMode.NO_SHELL_WITH_NAME.equals(shellCreateMode)) {
            newLiftTask = getLiftTaskForShellName(executionCommand.shellName);
            if (newLiftTask != null)
                Logger.logVerbose(LOG_TAG, "Existing LiftTask with \"" + executionCommand.shellName + "\" shell name found for shell create mode \"" + shellCreateMode.getMode() + "\"");
            else
                Logger.logVerbose(LOG_TAG, "No existing LiftTask with \"" + executionCommand.shellName + "\" shell name found for shell create mode \"" + shellCreateMode.getMode() + "\"");
        }

        if (newLiftTask == null)
            newLiftTask = createLiftTask(executionCommand);
    }

    /** Create a LiftTask. */
    @Nullable
    public AppShell createLiftTask(String executablePath, String[] arguments, String stdin, String workingDirectory) {
        return createLiftTask(new ExecutionCommand(LiftShellManager.getNextShellId(), executablePath,
            arguments, stdin, workingDirectory, Runner.APP_SHELL.getName(), false));
    }

    /** Create a LiftTask. */
    @Nullable
    public synchronized AppShell createLiftTask(ExecutionCommand executionCommand) {
        if (executionCommand == null) return null;

        Logger.logDebug(LOG_TAG, "Creating \"" + executionCommand.getCommandIdAndLabelLogString() + "\" LiftTask");

        if (!Runner.APP_SHELL.equalsRunner(executionCommand.runner)) {
            Logger.logDebug(LOG_TAG, "Ignoring wrong runner \"" + executionCommand.runner + "\" command passed to createLiftTask()");
            return null;
        }

        executionCommand.setShellCommandShellEnvironment = true;

        if (Logger.getLogLevel() >= Logger.LOG_LEVEL_VERBOSE)
            Logger.logVerboseExtended(LOG_TAG, executionCommand.toString());

        AppShell newLiftTask = AppShell.execute(this, executionCommand, this,
            new LiftShellEnvironment(), null,false);
        if (newLiftTask == null) {
            Logger.logError(LOG_TAG, "Failed to execute new LiftTask command for:\n" + executionCommand.getCommandIdAndLabelLogString());
            // If the execution command was started for a plugin, then process the error
            if (executionCommand.isPluginExecutionCommand)
                LiftPluginUtils.processPluginExecutionCommandError(this, LOG_TAG, executionCommand, false);
            else {
                Logger.logError(LOG_TAG, "Set log level to debug or higher to see error in logs");
                Logger.logErrorPrivateExtended(LOG_TAG, executionCommand.toString());
            }
            return null;
        }

        mShellManager.mLiftTasks.add(newLiftTask);

        // Remove the execution command from the pending plugin execution commands list since it has
        // now been processed
        if (executionCommand.isPluginExecutionCommand)
            mShellManager.mPendingPluginExecutionCommands.remove(executionCommand);

        updateNotification();

        return newLiftTask;
    }

    /** Callback received when a LiftTask finishes. */
    @Override
    public void onAppShellExited(final AppShell liftTask) {
        mHandler.post(() -> {
            if (liftTask != null) {
                ExecutionCommand executionCommand = liftTask.getExecutionCommand();

                Logger.logVerbose(LOG_TAG, "The onLiftTaskExited() callback called for \"" + executionCommand.getCommandIdAndLabelLogString() + "\" LiftTask command");

                // If the execution command was started for a plugin, then process the results
                if (executionCommand != null && executionCommand.isPluginExecutionCommand)
                    LiftPluginUtils.processPluginExecutionCommandResult(this, LOG_TAG, executionCommand);

                mShellManager.mLiftTasks.remove(liftTask);
            }

            updateNotification();
        });
    }





    /** Execute a shell command in a foreground {@link LiftSession}. */
    private void executeLiftSessionCommand(ExecutionCommand executionCommand) {
        if (executionCommand == null) return;

        Logger.logDebug(LOG_TAG, "Executing foreground \"" + executionCommand.getCommandIdAndLabelLogString() + "\" LiftSession command");

        // Transform executable path to shell/session name, e.g. "/bin/do-something.sh" => "do-something.sh".
        if (executionCommand.shellName == null && executionCommand.executable != null)
            executionCommand.shellName = ShellUtils.getExecutableBasename(executionCommand.executable);

        LiftSession newLiftSession = null;
        ShellCreateMode shellCreateMode = processShellCreateMode(executionCommand);
        if (shellCreateMode == null) return;
        if (ShellCreateMode.NO_SHELL_WITH_NAME.equals(shellCreateMode)) {
            newLiftSession = getLiftSessionForShellName(executionCommand.shellName);
            if (newLiftSession != null)
                Logger.logVerbose(LOG_TAG, "Existing LiftSession with \"" + executionCommand.shellName + "\" shell name found for shell create mode \"" + shellCreateMode.getMode() + "\"");
            else
                Logger.logVerbose(LOG_TAG, "No existing LiftSession with \"" + executionCommand.shellName + "\" shell name found for shell create mode \"" + shellCreateMode.getMode() + "\"");
        }

        if (newLiftSession == null)
            newLiftSession = createLiftSession(executionCommand);
        if (newLiftSession == null) return;

        handleSessionAction(DataUtils.getIntFromString(executionCommand.sessionAction,
            LIFT_SERVICE.VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY),
            newLiftSession.getTerminalSession());
    }

    /**
     * Create a {@link LiftSession}.
     * Currently called by {@link LiftTerminalSessionActivityClient#addNewSession(boolean, String)} to add a new {@link LiftSession}.
     */
    @Nullable
    public LiftSession createLiftSession(String executablePath, String[] arguments, String stdin,
                                             String workingDirectory, boolean isFailSafe, String sessionName) {
        ExecutionCommand executionCommand = new ExecutionCommand(LiftShellManager.getNextShellId(),
            executablePath, arguments, stdin, workingDirectory, Runner.TERMINAL_SESSION.getName(), isFailSafe);
        executionCommand.shellName = sessionName;
        return createLiftSession(executionCommand);
    }

    /** Create a {@link LiftSession}. */
    @Nullable
    public synchronized LiftSession createLiftSession(ExecutionCommand executionCommand) {
        if (executionCommand == null) return null;

        Logger.logDebug(LOG_TAG, "Creating \"" + executionCommand.getCommandIdAndLabelLogString() + "\" LiftSession");

        if (!Runner.TERMINAL_SESSION.equalsRunner(executionCommand.runner)) {
            Logger.logDebug(LOG_TAG, "Ignoring wrong runner \"" + executionCommand.runner + "\" command passed to createLiftSession()");
            return null;
        }

        executionCommand.setShellCommandShellEnvironment = true;
        executionCommand.terminalTranscriptRows = mProperties.getTerminalTranscriptRows();

        if (Logger.getLogLevel() >= Logger.LOG_LEVEL_VERBOSE)
            Logger.logVerboseExtended(LOG_TAG, executionCommand.toString());

        // If the execution command was started for a plugin, only then will the stdout be set
        // Otherwise if command was manually started by the user like by adding a new terminal session,
        // then no need to set stdout
        LiftSession newLiftSession = LiftSession.execute(this, executionCommand, getLiftTerminalSessionClient(),
            this, new LiftShellEnvironment(), null, executionCommand.isPluginExecutionCommand);
        if (newLiftSession == null) {
            Logger.logError(LOG_TAG, "Failed to execute new LiftSession command for:\n" + executionCommand.getCommandIdAndLabelLogString());
            // If the execution command was started for a plugin, then process the error
            if (executionCommand.isPluginExecutionCommand)
                LiftPluginUtils.processPluginExecutionCommandError(this, LOG_TAG, executionCommand, false);
            else {
                Logger.logError(LOG_TAG, "Set log level to debug or higher to see error in logs");
                Logger.logErrorPrivateExtended(LOG_TAG, executionCommand.toString());
            }
            return null;
        }

        mShellManager.mLiftSessions.add(newLiftSession);

        // Remove the execution command from the pending plugin execution commands list since it has
        // now been processed
        if (executionCommand.isPluginExecutionCommand)
            mShellManager.mPendingPluginExecutionCommands.remove(executionCommand);

        // Notify {@link LiftSessionsListViewController} that sessions list has been updated if
        // activity in is foreground
        if (mLiftTerminalSessionActivityClient != null)
            mLiftTerminalSessionActivityClient.liftSessionListNotifyUpdated();

        updateNotification();

        // No need to recreate the activity since it likely just started and theme should already have applied
        LiftActivity.updateLiftActivityStyling(this, false);

        return newLiftSession;
    }

    /** Remove a LiftSession. */
    public synchronized int removeLiftSession(TerminalSession sessionToRemove) {
        int index = getIndexOfSession(sessionToRemove);

        if (index >= 0)
            mShellManager.mLiftSessions.get(index).finish();

        return index;
    }

    /** Callback received when a {@link LiftSession} finishes. */
    @Override
    public void onLiftSessionExited(final LiftSession liftSession) {
        if (liftSession != null) {
            ExecutionCommand executionCommand = liftSession.getExecutionCommand();

            Logger.logVerbose(LOG_TAG, "The onLiftSessionExited() callback called for \"" + executionCommand.getCommandIdAndLabelLogString() + "\" LiftSession command");

            // If the execution command was started for a plugin, then process the results
            if (executionCommand != null && executionCommand.isPluginExecutionCommand)
                LiftPluginUtils.processPluginExecutionCommandResult(this, LOG_TAG, executionCommand);

            mShellManager.mLiftSessions.remove(liftSession);

            // Notify {@link LiftSessionsListViewController} that sessions list has been updated if
            // activity in is foreground
            if (mLiftTerminalSessionActivityClient != null)
                mLiftTerminalSessionActivityClient.liftSessionListNotifyUpdated();
        }

        updateNotification();
    }





    private ShellCreateMode processShellCreateMode(@NonNull ExecutionCommand executionCommand) {
        if (ShellCreateMode.ALWAYS.equalsMode(executionCommand.shellCreateMode))
            return ShellCreateMode.ALWAYS; // Default
        else if (ShellCreateMode.NO_SHELL_WITH_NAME.equalsMode(executionCommand.shellCreateMode))
            if (DataUtils.isNullOrEmpty(executionCommand.shellName)) {
                LiftPluginUtils.setAndProcessPluginExecutionCommandError(this, LOG_TAG, executionCommand, false,
                    getString(R.string.error_lift_service_execution_command_shell_name_unset, executionCommand.shellCreateMode));
                return null;
            } else {
               return ShellCreateMode.NO_SHELL_WITH_NAME;
            }
        else {
            LiftPluginUtils.setAndProcessPluginExecutionCommandError(this, LOG_TAG, executionCommand, false,
                getString(R.string.error_lift_service_unsupported_execution_command_shell_create_mode, executionCommand.shellCreateMode));
            return null;
        }
    }

    /** Process session action for new session. */
    private void handleSessionAction(int sessionAction, TerminalSession newTerminalSession) {
        Logger.logDebug(LOG_TAG, "Processing sessionAction \"" + sessionAction + "\" for session \"" + newTerminalSession.mSessionName + "\"");

        switch (sessionAction) {
            case LIFT_SERVICE.VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY:
                setCurrentStoredTerminalSession(newTerminalSession);
                if (mLiftTerminalSessionActivityClient != null)
                    mLiftTerminalSessionActivityClient.setCurrentSession(newTerminalSession);
                startLiftActivity();
                break;
            case LIFT_SERVICE.VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_OPEN_ACTIVITY:
                if (getLiftSessionsSize() == 1)
                    setCurrentStoredTerminalSession(newTerminalSession);
                startLiftActivity();
                break;
            case LIFT_SERVICE.VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_DONT_OPEN_ACTIVITY:
                setCurrentStoredTerminalSession(newTerminalSession);
                if (mLiftTerminalSessionActivityClient != null)
                    mLiftTerminalSessionActivityClient.setCurrentSession(newTerminalSession);
                break;
            case LIFT_SERVICE.VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_DONT_OPEN_ACTIVITY:
                if (getLiftSessionsSize() == 1)
                    setCurrentStoredTerminalSession(newTerminalSession);
                break;
            default:
                Logger.logError(LOG_TAG, "Invalid sessionAction: \"" + sessionAction + "\". Force using default sessionAction.");
                handleSessionAction(LIFT_SERVICE.VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY, newTerminalSession);
                break;
        }
    }

    /** Launch the {@link }LiftActivity} to bring it to foreground. */
    private void startLiftActivity() {
        // For android >= 10, apps require Display over other apps permission to start foreground activities
        // from background (services). If it is not granted, then LiftSessions that are started will
        // show in Lift notification but will not run until user manually clicks the notification.
        if (PermissionUtils.validateDisplayOverOtherAppsPermissionForPostAndroid10(this, true)) {
            LiftActivity.startLiftActivity(this);
        } else {
            LiftAppSharedPreferences preferences = LiftAppSharedPreferences.build(this);
            if (preferences == null) return;
            if (preferences.arePluginErrorNotificationsEnabled(false))
                Logger.showToast(this, this.getString(R.string.error_display_over_other_apps_permission_not_granted_to_start_terminal), true);
        }
    }





    /** If {@link LiftActivity} has not bound to the {@link LiftService} yet or is destroyed, then
     * interface functions requiring the activity should not be available to the terminal sessions,
     * so we just return the {@link #mLiftTerminalSessionServiceClient}. Once {@link LiftActivity} bind
     * callback is received, it should call {@link #setLiftTerminalSessionClient} to set the
     * {@link LiftService#mLiftTerminalSessionActivityClient} so that further terminal sessions are directly
     * passed the {@link LiftTerminalSessionActivityClient} object which fully implements the
     * {@link TerminalSessionClient} interface.
     *
     * @return Returns the {@link LiftTerminalSessionActivityClient} if {@link LiftActivity} has bound with
     * {@link LiftService}, otherwise {@link LiftTerminalSessionServiceClient}.
     */
    public synchronized LiftTerminalSessionClientBase getLiftTerminalSessionClient() {
        if (mLiftTerminalSessionActivityClient != null)
            return mLiftTerminalSessionActivityClient;
        else
            return mLiftTerminalSessionServiceClient;
    }

    /** This should be called when {@link LiftActivity#onServiceConnected} is called to set the
     * {@link LiftService#mLiftTerminalSessionActivityClient} variable and update the {@link TerminalSession}
     * and {@link TerminalEmulator} clients in case they were passed {@link LiftTerminalSessionServiceClient}
     * earlier.
     *
     * @param liftTerminalSessionActivityClient The {@link LiftTerminalSessionActivityClient} object that fully
     * implements the {@link TerminalSessionClient} interface.
     */
    public synchronized void setLiftTerminalSessionClient(LiftTerminalSessionActivityClient liftTerminalSessionActivityClient) {
        mLiftTerminalSessionActivityClient = liftTerminalSessionActivityClient;

        for (int i = 0; i < mShellManager.mLiftSessions.size(); i++)
            mShellManager.mLiftSessions.get(i).getTerminalSession().updateTerminalSessionClient(mLiftTerminalSessionActivityClient);
    }

    /** This should be called when {@link LiftActivity} has been destroyed and in {@link #onUnbind(Intent)}
     * so that the {@link LiftService} and {@link TerminalSession} and {@link TerminalEmulator}
     * clients do not hold an activity references.
     */
    public synchronized void unsetLiftTerminalSessionClient() {
        for (int i = 0; i < mShellManager.mLiftSessions.size(); i++)
            mShellManager.mLiftSessions.get(i).getTerminalSession().updateTerminalSessionClient(mLiftTerminalSessionServiceClient);

        mLiftTerminalSessionActivityClient = null;
    }





    private Notification buildNotification() {
        Resources res = getResources();

        // Set pending intent to be launched when notification is clicked
        Intent notificationIntent = LiftActivity.newInstance(this);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);


        // Set notification text
        int sessionCount = getLiftSessionsSize();
        int taskCount = mShellManager.mLiftTasks.size();
        String notificationText = sessionCount + " session" + (sessionCount == 1 ? "" : "s");
        if (taskCount > 0) {
            notificationText += ", " + taskCount + " task" + (taskCount == 1 ? "" : "s");
        }

        final boolean wakeLockHeld = mWakeLock != null;
        if (wakeLockHeld) notificationText += " (wake lock held)";


        // Set notification priority
        // If holding a wake or wifi lock consider the notification of high priority since it's using power,
        // otherwise use a low priority
        int priority = (wakeLockHeld) ? Notification.PRIORITY_HIGH : Notification.PRIORITY_LOW;


        // Build the notification
        Notification.Builder builder =  NotificationUtils.geNotificationBuilder(this,
            LiftConstants.LIFT_APP_NOTIFICATION_CHANNEL_ID, priority,
            LiftConstants.LIFT_APP_NAME, notificationText, null,
            contentIntent, null, NotificationUtils.NOTIFICATION_MODE_SILENT);
        if (builder == null)  return null;

        // No need to show a timestamp:
        builder.setShowWhen(false);

        // Set notification icon
        builder.setSmallIcon(R.drawable.ic_service_notification);

        // Set background color for small notification icon
        builder.setColor(0xFF607D8B);

        // LiftSessions are always ongoing
        builder.setOngoing(true);


        // Set Exit button action
        Intent exitIntent = new Intent(this, LiftService.class).setAction(LIFT_SERVICE.ACTION_STOP_SERVICE);
        builder.addAction(android.R.drawable.ic_delete, res.getString(R.string.notification_action_exit), PendingIntent.getService(this, 0, exitIntent, 0));


        // Set Wakelock button actions
        String newWakeAction = wakeLockHeld ? LIFT_SERVICE.ACTION_WAKE_UNLOCK : LIFT_SERVICE.ACTION_WAKE_LOCK;
        Intent toggleWakeLockIntent = new Intent(this, LiftService.class).setAction(newWakeAction);
        String actionTitle = res.getString(wakeLockHeld ? R.string.notification_action_wake_unlock : R.string.notification_action_wake_lock);
        int actionIcon = wakeLockHeld ? android.R.drawable.ic_lock_idle_lock : android.R.drawable.ic_lock_lock;
        builder.addAction(actionIcon, actionTitle, PendingIntent.getService(this, 0, toggleWakeLockIntent, 0));


        return builder.build();
    }

    private void setupNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationUtils.setupNotificationChannel(this, LiftConstants.LIFT_APP_NOTIFICATION_CHANNEL_ID,
            LiftConstants.LIFT_APP_NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
    }

    /** Update the shown foreground service notification after making any changes that affect it. */
    private synchronized void updateNotification() {
        if (mWakeLock == null && mShellManager.mLiftSessions.isEmpty() && mShellManager.mLiftTasks.isEmpty()) {
            // Exit if we are updating after the user disabled all locks with no sessions or tasks running.
            requestStopService();
        } else {
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(LiftConstants.LIFT_APP_NOTIFICATION_ID, buildNotification());
        }
    }





    private void setCurrentStoredTerminalSession(TerminalSession terminalSession) {
        if (terminalSession == null) return;
        // Make the newly created session the current one to be displayed
        LiftAppSharedPreferences preferences = LiftAppSharedPreferences.build(this);
        if (preferences == null) return;
        preferences.setCurrentSession(terminalSession.mHandle);
    }

    public synchronized boolean isLiftSessionsEmpty() {
        return mShellManager.mLiftSessions.isEmpty();
    }

    public synchronized int getLiftSessionsSize() {
        return mShellManager.mLiftSessions.size();
    }

    public synchronized List<LiftSession> getLiftSessions() {
        return mShellManager.mLiftSessions;
    }

    @Nullable
    public synchronized LiftSession getLiftSession(int index) {
        if (index >= 0 && index < mShellManager.mLiftSessions.size())
            return mShellManager.mLiftSessions.get(index);
        else
            return null;
    }

    @Nullable
    public synchronized LiftSession getLiftSessionForTerminalSession(TerminalSession terminalSession) {
        if (terminalSession == null) return null;

        for (int i = 0; i < mShellManager.mLiftSessions.size(); i++) {
            if (mShellManager.mLiftSessions.get(i).getTerminalSession().equals(terminalSession))
                return mShellManager.mLiftSessions.get(i);
        }

        return null;
    }

    public synchronized LiftSession getLastLiftSession() {
        return mShellManager.mLiftSessions.isEmpty() ? null : mShellManager.mLiftSessions.get(mShellManager.mLiftSessions.size() - 1);
    }

    public synchronized int getIndexOfSession(TerminalSession terminalSession) {
        if (terminalSession == null) return -1;

        for (int i = 0; i < mShellManager.mLiftSessions.size(); i++) {
            if (mShellManager.mLiftSessions.get(i).getTerminalSession().equals(terminalSession))
                return i;
        }
        return -1;
    }

    public synchronized TerminalSession getTerminalSessionForHandle(String sessionHandle) {
        TerminalSession terminalSession;
        for (int i = 0, len = mShellManager.mLiftSessions.size(); i < len; i++) {
            terminalSession = mShellManager.mLiftSessions.get(i).getTerminalSession();
            if (terminalSession.mHandle.equals(sessionHandle))
                return terminalSession;
        }
        return null;
    }

    public synchronized AppShell getLiftTaskForShellName(String name) {
        if (DataUtils.isNullOrEmpty(name)) return null;
        AppShell appShell;
        for (int i = 0, len = mShellManager.mLiftTasks.size(); i < len; i++) {
            appShell = mShellManager.mLiftTasks.get(i);
            String shellName = appShell.getExecutionCommand().shellName;
            if (shellName != null && shellName.equals(name))
                return appShell;
        }
        return null;
    }

    public synchronized LiftSession getLiftSessionForShellName(String name) {
        if (DataUtils.isNullOrEmpty(name)) return null;
        LiftSession liftSession;
        for (int i = 0, len = mShellManager.mLiftSessions.size(); i < len; i++) {
            liftSession = mShellManager.mLiftSessions.get(i);
            String shellName = liftSession.getExecutionCommand().shellName;
            if (shellName != null && shellName.equals(name))
                return liftSession;
        }
        return null;
    }



    public boolean wantsToStop() {
        return mWantsToStop;
    }

}
