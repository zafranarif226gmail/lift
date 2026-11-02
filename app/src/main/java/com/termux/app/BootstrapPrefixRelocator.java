package com.lift.app;

import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructStat;

import com.lift.shared.lift.LiftConstants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
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
        return path.replace(TERMUX_APP_DATA_PATH, LIFT_APP_DATA_PATH_ALIAS);
    }

    /**
     * Copy a bootstrap file, rewriting the fixed-length app data path in scripts and binary data without
     * loading the complete file into memory or changing ELF file offsets.
     */
    static void copyWithLiftPaths(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE + TERMUX_APP_DATA_PATH_BYTES.length - 1];
        int pendingLength = 0;
        int bytesRead;

        while ((bytesRead = input.read(buffer, pendingLength, BUFFER_SIZE)) != -1) {
            if (bytesRead == 0) continue;

            int totalLength = pendingLength + bytesRead;
            int searchableLength = totalLength - TERMUX_APP_DATA_PATH_BYTES.length + 1;
            int position = 0;

            while (position < searchableLength) {
                int match = indexOfAppDataPath(buffer, position, searchableLength);
                if (match == -1) {
                    int safeLength = searchableLength - position;
                    output.write(buffer, position, safeLength);
                    position += safeLength;
                } else {
                    output.write(buffer, position, match - position);
                    output.write(LIFT_APP_DATA_PATH_ALIAS_BYTES);
                    position = match + TERMUX_APP_DATA_PATH_BYTES.length;
                }
            }

            pendingLength = totalLength - position;
            System.arraycopy(buffer, position, buffer, 0, pendingLength);
        }

        // At most TERMUX_APP_DATA_PATH_BYTES.length - 1 bytes remain, so no complete path can be here.
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
    }

    static boolean requiresExecutePermission(String entryName) {
        return entryName.startsWith("bin/") || entryName.startsWith("libexec") ||
            entryName.startsWith("lib/apt/apt-helper") || entryName.startsWith("lib/apt/methods");
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
                    int match = indexOfAppDataPath(buffer, position, searchableLength);
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

    private static int indexOfAppDataPath(byte[] buffer, int fromIndex, int toIndex) {
        for (int index = fromIndex; index < toIndex; index++) {
            if (buffer[index] != TERMUX_APP_DATA_PATH_BYTES[0]) continue;

            int offset = 1;
            while (offset < TERMUX_APP_DATA_PATH_BYTES.length &&
                buffer[index + offset] == TERMUX_APP_DATA_PATH_BYTES[offset]) {
                offset++;
            }
            if (offset == TERMUX_APP_DATA_PATH_BYTES.length) return index;
        }
        return -1;
    }
}
