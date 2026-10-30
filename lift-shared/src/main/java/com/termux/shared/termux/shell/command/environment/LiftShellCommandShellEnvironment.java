package com.lift.shared.lift.shell.command.environment;

import android.content.Context;

import androidx.annotation.NonNull;

import com.lift.shared.shell.command.ExecutionCommand;
import com.lift.shared.shell.command.environment.ShellCommandShellEnvironment;
import com.lift.shared.shell.command.environment.ShellEnvironmentUtils;
import com.lift.shared.lift.settings.preferences.LiftAppSharedPreferences;
import com.lift.shared.lift.shell.LiftShellManager;

import java.util.HashMap;

/**
 * Environment for Lift {@link ExecutionCommand}.
 */
public class LiftShellCommandShellEnvironment extends ShellCommandShellEnvironment {

    /** Get shell environment containing info for Lift {@link ExecutionCommand}. */
    @NonNull
    @Override
    public HashMap<String, String> getEnvironment(@NonNull Context currentPackageContext,
                                                  @NonNull ExecutionCommand executionCommand) {
        HashMap<String, String> environment = super.getEnvironment(currentPackageContext, executionCommand);

        LiftAppSharedPreferences preferences = LiftAppSharedPreferences.build(currentPackageContext);
        if (preferences == null) return environment;

        if (ExecutionCommand.Runner.APP_SHELL.equalsRunner(executionCommand.runner)) {
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_SHELL_CMD__APP_SHELL_NUMBER_SINCE_BOOT,
                String.valueOf(preferences.getAndIncrementAppShellNumberSinceBoot()));
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_SHELL_CMD__APP_SHELL_NUMBER_SINCE_APP_START,
                String.valueOf(LiftShellManager.getAndIncrementAppShellNumberSinceAppStart()));

        } else if (ExecutionCommand.Runner.TERMINAL_SESSION.equalsRunner(executionCommand.runner)) {
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_SHELL_CMD__TERMINAL_SESSION_NUMBER_SINCE_BOOT,
                String.valueOf(preferences.getAndIncrementTerminalSessionNumberSinceBoot()));
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_SHELL_CMD__TERMINAL_SESSION_NUMBER_SINCE_APP_START,
                String.valueOf(LiftShellManager.getAndIncrementTerminalSessionNumberSinceAppStart()));
        } else {
            return environment;
        }

        return environment;
    }

}
