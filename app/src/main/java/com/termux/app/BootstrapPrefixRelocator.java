package com.lift.app;

import android.os.Process;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructStat;

import com.lift.shared.lift.LiftConstants;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

/**
 * Adapts paths in the upstream Termux bootstrap to Lift's private app directory while extracting it.
 *
 * The replacement is the same length as the Termux app data path so paths embedded in ELF files
 * remain at their recorded offsets. The "/." component resolves to Lift's app data directory.
 */
final class BootstrapPrefixRelocator {

    private static final int BUFFER_SIZE = 8192;
    private static final String TERMUX_APP_DATA_PATH = "/data/data/com.termux";
    private static final String LIFT_APP_DATA_PATH_ALIAS = LiftConstants.LIFT_INTERNAL_PRIVATE_APP_DATA_DIR_PATH + "/.";
    static final String LIFT_WELCOME_MESSAGE = "Welcome to lift!\n\n" +
        "Working with packages:\n\n" +
        "- Search:  pkg search <query>\n" +
        "- Install: pkg install <package>\n" +
        "- Upgrade: pkg upgrade\n\n" +
        "For fixing any repository issues,\n" +
        "try 'lift-change-repo' command.\n";
    private static final String BOOTSTRAP_SECOND_STAGE_LOG = "${PREFIX}/var/log/lift-bootstrap-second-stage.log";
    private static final String BOOTSTRAP_SECOND_STAGE_SCRIPT = LIFT_APP_DATA_PATH_ALIAS +
        "/files/usr/etc/termux/termux-bootstrap/second-stage/termux-bootstrap-second-stage.sh";
    private static final String BOOTSTRAP_SECOND_STAGE_FALLBACK = "etc/profile.d/01-termux-bootstrap-second-stage-fallback.sh";
    private static final byte[] TERMUX_APP_DATA_PATH_BYTES = TERMUX_APP_DATA_PATH.getBytes(StandardCharsets.US_ASCII);
    private static final byte[] LIFT_APP_DATA_PATH_ALIAS_BYTES = LIFT_APP_DATA_PATH_ALIAS.getBytes(StandardCharsets.US_ASCII);
    private static final File PREFIX_RELOCATION_MARKER = new File(
        LiftConstants.LIFT_FILES_DIR_PATH, ".lift-bootstrap-prefix-relocated-v1");

    static {
        if (TERMUX_APP_DATA_PATH_BYTES.length != LIFT_APP_DATA_PATH_ALIAS_BYTES.length) {
            throw new IllegalStateException("Lift app data path alias must preserve the upstream path length");
        }
    }

    private BootstrapPrefixRelocator() { }

    static String rewritePath(String path) {
        return rewritePath(path, TERMUX_APP_DATA_PATH, LIFT_APP_DATA_PATH_ALIAS);
    }

    static String rewritePath(String path, String sourceAppDataPath, String targetAppDataPathAlias) {
        if (sourceAppDataPath.length() != targetAppDataPathAlias.length()) {
            throw new IllegalArgumentException("Bootstrap app data path replacement must preserve path length");
        }
        return path.replace(sourceAppDataPath, targetAppDataPathAlias);
    }

    /**
     * Copy a bootstrap file, rewriting the fixed-length app data path in scripts and binary data without
     * loading the complete file into memory or changing ELF file offsets.
     */
    static void copyWithLiftPaths(InputStream input, OutputStream output) throws IOException {
        copyWithPaths(input, output, TERMUX_APP_DATA_PATH, LIFT_APP_DATA_PATH_ALIAS);
    }

    /** Transform only startup files whose contents are intentionally Lift-specific. */
    static void copyBootstrapEntryWithLiftPaths(String entryName, InputStream input, OutputStream output) throws IOException {
        if ("etc/motd".equals(entryName)) {
            output.write(LIFT_WELCOME_MESSAGE.getBytes(StandardCharsets.UTF_8));
        } else if (BOOTSTRAP_SECOND_STAGE_FALLBACK.equals(entryName)) {
            String script = new String(readStream(input), StandardCharsets.UTF_8);
            String relocatedScript = rewritePath(script);
            String quietScript = quietSuccessfulBootstrapOutput(relocatedScript);
            output.write(quietScript.getBytes(StandardCharsets.UTF_8));
        } else {
            copyWithLiftPaths(input, output);
        }
    }

    static void copyWithPaths(InputStream input, OutputStream output, String sourceAppDataPath,
                              String targetAppDataPathAlias) throws IOException {
        byte[] sourceAppDataPathBytes = sourceAppDataPath.getBytes(StandardCharsets.US_ASCII);
        byte[] targetAppDataPathAliasBytes = targetAppDataPathAlias.getBytes(StandardCharsets.US_ASCII);
        if (sourceAppDataPathBytes.length != targetAppDataPathAliasBytes.length) {
            throw new IOException("Bootstrap app data path replacement must preserve path length");
        }

        byte[] buffer = new byte[BUFFER_SIZE + sourceAppDataPathBytes.length - 1];
        int pendingLength = 0;
        int bytesRead;

        while ((bytesRead = input.read(buffer, pendingLength, BUFFER_SIZE)) != -1) {
            if (bytesRead == 0) continue;

            int totalLength = pendingLength + bytesRead;
            int searchableLength = totalLength - sourceAppDataPathBytes.length + 1;
            int position = 0;

            while (position < searchableLength) {
                int match = indexOfAppDataPath(buffer, position, searchableLength, sourceAppDataPathBytes);
                if (match == -1) {
                    int safeLength = searchableLength - position;
                    output.write(buffer, position, safeLength);
                    position += safeLength;
                } else {
                    output.write(buffer, position, match - position);
                    output.write(targetAppDataPathAliasBytes);
                    position = match + sourceAppDataPathBytes.length;
                }
            }

            pendingLength = totalLength - position;
            System.arraycopy(buffer, position, buffer, 0, pendingLength);
        }

        // At most sourceAppDataPathBytes.length - 1 bytes remain, so no complete path can be here.
        if (pendingLength > 0) output.write(buffer, 0, pendingLength);
    }

    static boolean isExistingPrefixRelocated() {
        return PREFIX_RELOCATION_MARKER.isFile();
    }

    static void markPrefixRelocated() throws IOException {
        try (FileOutputStream markerStream = new FileOutputStream(PREFIX_RELOCATION_MARKER)) {
            markerStream.write("1\n".getBytes(StandardCharsets.US_ASCII));
        }
    }

    /** Rewrite paths in an already-installed prefix without replacing its files or permissions. */
    static void relocateExistingPrefix(File prefixDirectory) throws IOException {
        rewriteDirectory(prefixDirectory);
        ensureBootstrapFallbackQuiet(prefixDirectory);
    }

    static boolean requiresExecutePermission(String entryName) {
        return entryName.startsWith("bin/") || entryName.startsWith("libexec") ||
            entryName.startsWith("lib/apt/apt-helper") || entryName.startsWith("lib/apt/methods");
    }

    /** Rewrite the known bootstrap second-stage launcher to retain successful output in app-private logs. */
    static String quietSuccessfulBootstrapOutput(String script) throws IOException {
        String oldBlock = "\t\techo \"Starting fallback run of termux bootstrap second stage\"\n" +
            "\t\tchmod +x \"" + BOOTSTRAP_SECOND_STAGE_SCRIPT + "\" || exit $?\n" +
            "\t\t\"" + BOOTSTRAP_SECOND_STAGE_SCRIPT + "\" || exit $?\n";
        String newBlock = "\t\tbootstrap_log=\"" + BOOTSTRAP_SECOND_STAGE_LOG + "\"\n" +
            "\t\tmkdir -p \"${PREFIX}/var/log\" || exit $?\n" +
            "\t\tif {\n" +
            "\t\t\tprintf '%s\\n' 'Starting fallback run of termux bootstrap second stage'\n" +
            "\t\t\tchmod +x \"" + BOOTSTRAP_SECOND_STAGE_SCRIPT + "\" &&\n" +
            "\t\t\t\"" + BOOTSTRAP_SECOND_STAGE_SCRIPT + "\"\n" +
            "\t\t} > \"$bootstrap_log\"; then\n" +
            "\t\t\t:\n" +
            "\t\telse\n" +
            "\t\t\tbootstrap_status=$?\n" +
            "\t\t\tcat \"$bootstrap_log\" >&2\n" +
            "\t\t\texit \"$bootstrap_status\"\n" +
            "\t\tfi\n";
        if (script.contains("bootstrap_log=\"" + BOOTSTRAP_SECOND_STAGE_LOG + "\"")) return script;
        if (!script.contains("Starting fallback run of termux bootstrap second stage")) return script;
        if (!script.contains(oldBlock)) {
            throw new IOException("Bootstrap fallback script no longer matches the reviewed second-stage launcher");
        }
        return script.replace(oldBlock, newBlock);
    }

    /** Update only the fallback startup script if it remains in an existing prefix. */
    static void ensureBootstrapFallbackQuiet(File prefixDirectory) throws IOException {
        File fallback = new File(prefixDirectory, BOOTSTRAP_SECOND_STAGE_FALLBACK);
        StructStat originalStat;
        try {
            originalStat = Os.lstat(fallback.getAbsolutePath());
        } catch (ErrnoException e) {
            if (e.errno == OsConstants.ENOENT) return;
            throw new IOException("Unable to inspect bootstrap fallback script: " + fallback, e);
        }
        if ((originalStat.st_mode & OsConstants.S_IFMT) != OsConstants.S_IFREG ||
            originalStat.st_uid != Process.myUid()) {
            throw new IOException("Bootstrap fallback script is not a regular file owned by the Lift app UID: " + fallback);
        }

        byte[] originalBytes = readFile(fallback);
        String originalScript = new String(originalBytes, StandardCharsets.UTF_8);
        String updatedScript = quietSuccessfulBootstrapOutput(rewritePath(originalScript));
        byte[] updatedBytes = updatedScript.getBytes(StandardCharsets.UTF_8);
        if (Arrays.equals(originalBytes, updatedBytes)) return;

        String temporaryPath = new File(fallback.getParentFile(),
            ".lift-bootstrap-output-" + UUID.randomUUID()).getAbsolutePath();
        try {
            try (FileOutputStream output = new FileOutputStream(temporaryPath)) {
                output.write(updatedBytes);
            }
            Os.chmod(temporaryPath, (int) (originalStat.st_mode & 0777));
            Os.rename(temporaryPath, fallback.getAbsolutePath());
        } catch (ErrnoException e) {
            try {
                Os.remove(temporaryPath);
            } catch (ErrnoException ignored) { }
            throw new IOException("Unable to update bootstrap fallback output handling: " + fallback, e);
        } catch (IOException e) {
            try {
                Os.remove(temporaryPath);
            } catch (ErrnoException ignored) { }
            throw e;
        }
    }

    /** Ensure the login script uses the app UID's private, owner-executable file. */
    static void ensureLoginExecutable(File prefixDirectory) throws IOException {
        File login = new File(prefixDirectory, "bin/login");
        try {
            StructStat before = Os.lstat(login.getAbsolutePath());
            if ((before.st_mode & OsConstants.S_IFMT) != OsConstants.S_IFREG) {
                throw new IOException("Bootstrap login is not a regular file: " + login);
            }
            if (before.st_uid != Process.myUid()) {
                throw new IOException("Bootstrap login is not owned by the Lift app UID: " + login);
            }
            if ((before.st_mode & 0700) != 0700 || (before.st_mode & 0077) != 0) {
                Os.chmod(login.getAbsolutePath(), 0700);
            }
            StructStat after = Os.stat(login.getAbsolutePath());
            if (after.st_uid != Process.myUid() || (after.st_mode & 0700) != 0700 || (after.st_mode & 0077) != 0) {
                throw new IOException("Bootstrap login owner or executable permissions are invalid: " + login);
            }
        } catch (ErrnoException e) {
            throw new IOException("Unable to validate bootstrap login ownership and permissions: " + login, e);
        }
    }

    /** Install the requested exact MOTD, without rewriting it on every app start. */
    static void ensureLiftWelcomeMessage(File prefixDirectory) throws IOException {
        File motd = new File(prefixDirectory, "etc/motd");
        byte[] expected = LIFT_WELCOME_MESSAGE.getBytes(StandardCharsets.UTF_8);
        try {
            StructStat stat = Os.lstat(motd.getAbsolutePath());
            int fileType = (int) (stat.st_mode & OsConstants.S_IFMT);
            if (fileType == OsConstants.S_IFREG) {
                if (stat.st_uid != Process.myUid()) {
                    throw new IOException("Bootstrap MOTD is not owned by the Lift app UID: " + motd);
                }
                if (Arrays.equals(readFile(motd), expected)) return;
            } else if (fileType == OsConstants.S_IFLNK && stat.st_uid == Process.myUid()) {
                // Replace a dangling or user-created symlink itself; never follow it into user data.
                Os.remove(motd.getAbsolutePath());
            } else {
                throw new IOException("Bootstrap MOTD is not a regular app-owned file: " + motd);
            }
        } catch (ErrnoException e) {
            if (e.errno != OsConstants.ENOENT) {
                throw new IOException("Unable to inspect bootstrap MOTD: " + motd, e);
            }
        }

        File parent = motd.getParentFile();
        if (parent == null || (!parent.isDirectory() && !parent.mkdirs())) {
            throw new IOException("Unable to create bootstrap MOTD directory: " + parent);
        }
        try (FileOutputStream output = new FileOutputStream(motd)) {
            output.write(expected);
        }
        try {
            Os.chmod(motd.getAbsolutePath(), 0600);
        } catch (ErrnoException e) {
            throw new IOException("Unable to set bootstrap MOTD permissions: " + motd, e);
        }
    }

    private static byte[] readFile(File file) throws IOException {
        try (FileInputStream input = new FileInputStream(file)) {
            return readStream(input);
        }
    }

    private static byte[] readStream(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
        return output.toByteArray();
    }

    private static void rewriteDirectory(File directory) throws IOException {
        File[] children = directory.listFiles();
        if (children == null) throw new IOException("Unable to list bootstrap directory: " + directory);

        for (File child : children) {
            StructStat stat;
            try {
                stat = Os.lstat(child.getAbsolutePath());
            } catch (ErrnoException e) {
                throw new IOException("Unable to inspect bootstrap path: " + child, e);
            }

            int fileType = (int) (stat.st_mode & OsConstants.S_IFMT);
            if (fileType == OsConstants.S_IFDIR) {
                rewriteDirectory(child);
            } else if (fileType == OsConstants.S_IFLNK) {
                rewriteSymlink(child);
            } else if (fileType == OsConstants.S_IFREG) {
                rewriteRegularFile(child);
            }
        }
    }

    private static void rewriteSymlink(File symlink) throws IOException {
        String target;
        try {
            target = Os.readlink(symlink.getAbsolutePath());
        } catch (ErrnoException e) {
            throw new IOException("Unable to read bootstrap symlink: " + symlink, e);
        }

        String rewrittenTarget = rewritePath(target);
        if (target.equals(rewrittenTarget)) return;

        String temporaryPath = new File(symlink.getParentFile(),
            ".lift-prefix-relocation-" + UUID.randomUUID()).getAbsolutePath();
        try {
            Os.symlink(rewrittenTarget, temporaryPath);
            Os.rename(temporaryPath, symlink.getAbsolutePath());
        } catch (ErrnoException e) {
            try {
                Os.remove(temporaryPath);
            } catch (ErrnoException ignored) { }
            throw new IOException("Unable to update bootstrap symlink: " + symlink, e);
        }
    }

    static void rewriteRegularFile(File file) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE + TERMUX_APP_DATA_PATH_BYTES.length - 1];
        int pendingLength = 0;
        long bufferStart = 0;

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) {
            while (true) {
                long readStart = randomAccessFile.getFilePointer();
                int bytesRead = randomAccessFile.read(buffer, pendingLength, BUFFER_SIZE);
                if (bytesRead == -1) break;
                if (bytesRead == 0) continue;

                int totalLength = pendingLength + bytesRead;
                int searchableLength = totalLength - TERMUX_APP_DATA_PATH_BYTES.length + 1;
                int position = 0;

                while (position < searchableLength) {
                    int match = indexOfAppDataPath(buffer, position, searchableLength, TERMUX_APP_DATA_PATH_BYTES);
                    if (match == -1) {
                        position = searchableLength;
                    } else {
                        randomAccessFile.seek(bufferStart + match);
                        randomAccessFile.write(LIFT_APP_DATA_PATH_ALIAS_BYTES);
                        position = match + TERMUX_APP_DATA_PATH_BYTES.length;
                    }
                }

                pendingLength = totalLength - position;
                System.arraycopy(buffer, position, buffer, 0, pendingLength);
                long resumeOffset = readStart + bytesRead;
                bufferStart = resumeOffset - pendingLength;
                randomAccessFile.seek(resumeOffset);
            }
        }
    }

    private static int indexOfAppDataPath(byte[] buffer, int fromIndex, int toIndex, byte[] pathBytes) {
        for (int index = fromIndex; index < toIndex; index++) {
            if (buffer[index] != pathBytes[0]) continue;

            int offset = 1;
            while (offset < pathBytes.length && buffer[index + offset] == pathBytes[offset]) {
                offset++;
            }
            if (offset == pathBytes.length) return index;
        }
        return -1;
    }
}
