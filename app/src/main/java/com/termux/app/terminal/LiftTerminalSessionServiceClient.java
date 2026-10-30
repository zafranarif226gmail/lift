package com.lift.app.terminal;

import android.app.Service;

import androidx.annotation.NonNull;

import com.lift.app.LiftService;
import com.lift.shared.lift.shell.command.runner.terminal.LiftSession;
import com.lift.shared.lift.terminal.LiftTerminalSessionClientBase;
import com.lift.terminal.TerminalSession;
import com.lift.terminal.TerminalSessionClient;

/** The {@link TerminalSessionClient} implementation that may require a {@link Service} for its interface methods. */
public class LiftTerminalSessionServiceClient extends LiftTerminalSessionClientBase {

    private static final String LOG_TAG = "LiftTerminalSessionServiceClient";

    private final LiftService mService;

    public LiftTerminalSessionServiceClient(LiftService service) {
        this.mService = service;
    }

    @Override
    public void setTerminalShellPid(@NonNull TerminalSession terminalSession, int pid) {
        LiftSession liftSession = mService.getLiftSessionForTerminalSession(terminalSession);
        if (liftSession != null)
            liftSession.getExecutionCommand().mPid = pid;
    }

}
