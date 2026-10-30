package com.lift.app.api.file;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Patterns;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.lift.R;
import com.lift.shared.android.PackageUtils;
import com.lift.shared.data.DataUtils;
import com.lift.shared.data.IntentUtils;
import com.lift.shared.net.uri.UriUtils;
import com.lift.shared.interact.MessageDialogUtils;
import com.lift.shared.net.uri.UriScheme;
import com.lift.shared.lift.interact.TextInputDialogUtils;
import com.lift.shared.lift.LiftConstants;
import com.lift.shared.lift.LiftConstants.LIFT_APP;
import com.lift.shared.lift.LiftConstants.LIFT_APP.LIFT_SERVICE;
import com.lift.app.LiftService;
import com.lift.shared.logger.Logger;
import com.lift.shared.lift.settings.properties.LiftAppSharedProperties;
import com.lift.shared.lift.settings.properties.LiftPropertyConstants;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class FileReceiverActivity extends AppCompatActivity {

    static final String LIFT_RECEIVEDIR = LiftConstants.LIFT_FILES_DIR_PATH + "/home/downloads";
    static final String EDITOR_PROGRAM = LiftConstants.LIFT_HOME_DIR_PATH + "/bin/lift-file-editor";
    static final String URL_OPENER_PROGRAM = LiftConstants.LIFT_HOME_DIR_PATH + "/bin/lift-url-opener";

    /**
     * If the activity should be finished when the name input dialog is dismissed. This is disabled
     * before showing an error dialog, since the act of showing the error dialog will cause the
     * name input dialog to be implicitly dismissed, and we do not want to finish the activity directly
     * when showing the error dialog.
     */
    boolean mFinishOnDismissNameDialog = true;

    private static final String API_TAG = LiftConstants.LIFT_APP_NAME + "FileReceiver";

    private static final String LOG_TAG = "FileReceiverActivity";

    static boolean isSharedTextAnUrl(String sharedText) {
        if (sharedText == null || sharedText.isEmpty()) return false;

        return Patterns.WEB_URL.matcher(sharedText).matches()
            || Pattern.matches("magnet:\\?xt=urn:btih:.*?", sharedText);
    }

    @Override
    protected void onResume() {
        super.onResume();

        final Intent intent = getIntent();
        final String action = intent.getAction();
        final String type = intent.getType();
        final String scheme = intent.getScheme();

        Logger.logVerbose(LOG_TAG, "Intent Received:\n" + IntentUtils.getIntentString(intent));

        final String sharedTitle = IntentUtils.getStringExtraIfSet(intent, Intent.EXTRA_TITLE, null);

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            final String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            final Uri sharedUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);

            if (sharedUri != null) {
                handleContentUri(sharedUri, sharedTitle);
            } else if (sharedText != null) {
                if (isSharedTextAnUrl(sharedText)) {
                    handleUrlAndFinish(sharedText);
                } else {
                    String subject = IntentUtils.getStringExtraIfSet(intent, Intent.EXTRA_SUBJECT, null);
                    if (subject == null) subject = sharedTitle;
                    if (subject != null) subject += ".txt";
                    promptNameAndSave(new ByteArrayInputStream(sharedText.getBytes(StandardCharsets.UTF_8)), subject);
                }
            } else {
                showErrorDialogAndQuit("Send action without content - nothing to save.");
            }
        } else {
            Uri dataUri = intent.getData();

            if (dataUri == null) {
                showErrorDialogAndQuit("Data uri not passed.");
                return;
            }

            if (UriScheme.SCHEME_CONTENT.equals(scheme)) {
                handleContentUri(dataUri, sharedTitle);
            } else if (UriScheme.SCHEME_FILE.equals(scheme)) {
                Logger.logVerbose(LOG_TAG, "uri: \"" + dataUri + "\", path: \"" + dataUri.getPath() + "\", fragment: \"" + dataUri.getFragment() + "\"");

                // Get full path including fragment (anything after last "#")
                String path = UriUtils.getUriFilePathWithFragment(dataUri);
                if (DataUtils.isNullOrEmpty(path)) {
                    showErrorDialogAndQuit("File path from data uri is null, empty or invalid.");
                    return;
                }

                File file = new File(path);
                try {
                    FileInputStream in = new FileInputStream(file);
                    promptNameAndSave(in, file.getName());
                } catch (FileNotFoundException e) {
                    showErrorDialogAndQuit("Cannot open file: " + e.getMessage() + ".");
                }
            } else {
                showErrorDialogAndQuit("Unable to receive any file or URL.");
            }
        }
    }

    void showErrorDialogAndQuit(String message) {
        mFinishOnDismissNameDialog = false;
        MessageDialogUtils.showMessage(this,
            API_TAG, message,
            null, (dialog, which) -> finish(),
            null, null,
            dialog -> finish());
    }

    void handleContentUri(@NonNull final Uri uri, String subjectFromIntent) {
        try {
            Logger.logVerbose(LOG_TAG, "uri: \"" + uri + "\", path: \"" + uri.getPath() + "\", fragment: \"" + uri.getFragment() + "\"");

            String attachmentFileName = null;

            String[] projection = new String[]{OpenableColumns.DISPLAY_NAME};
            try (Cursor c = getContentResolver().query(uri, projection, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    final int fileNameColumnId = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (fileNameColumnId >= 0) attachmentFileName = c.getString(fileNameColumnId);
                }
            }

            if (attachmentFileName == null) attachmentFileName = subjectFromIntent;
            if (attachmentFileName == null) attachmentFileName = UriUtils.getUriFileBasename(uri, true);

            InputStream in = getContentResolver().openInputStream(uri);
            promptNameAndSave(in, attachmentFileName);
        } catch (Exception e) {
            showErrorDialogAndQuit("Unable to handle shared content:\n\n" + e.getMessage());
            Logger.logStackTraceWithMessage(LOG_TAG, "handleContentUri(uri=" + uri + ") failed", e);
        }
    }

    void promptNameAndSave(final InputStream in, final String attachmentFileName) {
        TextInputDialogUtils.textInput(this, R.string.title_file_received, attachmentFileName,
            R.string.action_file_received_edit, text -> {
                File outFile = saveStreamWithName(in, text);
                if (outFile == null) return;

                final File editorProgramFile = new File(EDITOR_PROGRAM);
                if (!editorProgramFile.isFile()) {
                    showErrorDialogAndQuit("The following file does not exist:\n$HOME/bin/lift-file-editor\n\n"
                        + "Create this file as a script or a symlink - it will be called with the received file as only argument.");
                    return;
                }

                // Do this for the user if necessary:
                //noinspection ResultOfMethodCallIgnored
                editorProgramFile.setExecutable(true);

                final Uri scriptUri = UriUtils.getFileUri(EDITOR_PROGRAM);

                Intent executeIntent = new Intent(LIFT_SERVICE.ACTION_SERVICE_EXECUTE, scriptUri);
                executeIntent.setClass(FileReceiverActivity.this, LiftService.class);
                executeIntent.putExtra(LIFT_SERVICE.EXTRA_ARGUMENTS, new String[]{outFile.getAbsolutePath()});
                startService(executeIntent);
                finish();
            },
            R.string.action_file_received_open_directory, text -> {
                if (saveStreamWithName(in, text) == null) return;

                Intent executeIntent = new Intent(LIFT_SERVICE.ACTION_SERVICE_EXECUTE);
                executeIntent.putExtra(LIFT_SERVICE.EXTRA_WORKDIR, LIFT_RECEIVEDIR);
                executeIntent.setClass(FileReceiverActivity.this, LiftService.class);
                startService(executeIntent);
                finish();
            },
            android.R.string.cancel, text -> finish(), dialog -> {
                if (mFinishOnDismissNameDialog) finish();
            });
    }

    public File saveStreamWithName(InputStream in, String attachmentFileName) {
        File receiveDir = new File(LIFT_RECEIVEDIR);

        if (DataUtils.isNullOrEmpty(attachmentFileName)) {
            showErrorDialogAndQuit("File name cannot be null or empty");
            return null;
        }

        if (!receiveDir.isDirectory() && !receiveDir.mkdirs()) {
            showErrorDialogAndQuit("Cannot create directory: " + receiveDir.getAbsolutePath());
            return null;
        }

        try {
            final File outFile = new File(receiveDir, attachmentFileName);
            try (FileOutputStream f = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[4096];
                int readBytes;
                while ((readBytes = in.read(buffer)) > 0) {
                    f.write(buffer, 0, readBytes);
                }
            }
            return outFile;
        } catch (IOException e) {
            showErrorDialogAndQuit("Error saving file:\n\n" + e);
            Logger.logStackTraceWithMessage(LOG_TAG, "Error saving file", e);
            return null;
        }
    }

    void handleUrlAndFinish(final String url) {
        final File urlOpenerProgramFile = new File(URL_OPENER_PROGRAM);
        if (!urlOpenerProgramFile.isFile()) {
            showErrorDialogAndQuit("The following file does not exist:\n$HOME/bin/lift-url-opener\n\n"
                + "Create this file as a script or a symlink - it will be called with the shared URL as the first argument.");
            return;
        }

        // Do this for the user if necessary:
        //noinspection ResultOfMethodCallIgnored
        urlOpenerProgramFile.setExecutable(true);

        final Uri urlOpenerProgramUri = UriUtils.getFileUri(URL_OPENER_PROGRAM);

        Intent executeIntent = new Intent(LIFT_SERVICE.ACTION_SERVICE_EXECUTE, urlOpenerProgramUri);
        executeIntent.setClass(FileReceiverActivity.this, LiftService.class);
        executeIntent.putExtra(LIFT_SERVICE.EXTRA_ARGUMENTS, new String[]{url});
        startService(executeIntent);
        finish();
    }

    /**
     * Update {@link LIFT_APP#FILE_SHARE_RECEIVER_ACTIVITY_CLASS_NAME} component state depending on
     * {@link LiftPropertyConstants#KEY_DISABLE_FILE_SHARE_RECEIVER} value and
     * {@link LIFT_APP#FILE_VIEW_RECEIVER_ACTIVITY_CLASS_NAME} component state depending on
     * {@link LiftPropertyConstants#KEY_DISABLE_FILE_VIEW_RECEIVER} value.
     */
    public static void updateFileReceiverActivityComponentsState(@NonNull Context context) {
        new Thread() {
            @Override
            public void run() {
                LiftAppSharedProperties properties = LiftAppSharedProperties.getProperties();

                String errmsg;
                boolean state;

                state = !properties.isFileShareReceiverDisabled();
                Logger.logVerbose(LOG_TAG, "Setting " + LIFT_APP.FILE_SHARE_RECEIVER_ACTIVITY_CLASS_NAME + " component state to " + state);
                errmsg = PackageUtils.setComponentState(context,LiftConstants.LIFT_PACKAGE_NAME,
                    LIFT_APP.FILE_SHARE_RECEIVER_ACTIVITY_CLASS_NAME,
                    state, null, false, false);
                if (errmsg != null)
                    Logger.logError(LOG_TAG, errmsg);

                state = !properties.isFileViewReceiverDisabled();
                Logger.logVerbose(LOG_TAG, "Setting " + LIFT_APP.FILE_VIEW_RECEIVER_ACTIVITY_CLASS_NAME + " component state to " + state);
                errmsg = PackageUtils.setComponentState(context,LiftConstants.LIFT_PACKAGE_NAME,
                    LIFT_APP.FILE_VIEW_RECEIVER_ACTIVITY_CLASS_NAME,
                    state, null, false, false);
                if (errmsg != null)
                    Logger.logError(LOG_TAG, errmsg);

            }
        }.start();
    }

}
