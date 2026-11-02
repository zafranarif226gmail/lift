package com.lift.app;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;

public class BootstrapPrefixRelocatorTest {

    private static final String TERMUX_APP_DATA_PATH = "/data/data/com.termux";

    @Test
    public void rewritesLoginInterpreterToLiftPrefixWithoutChangingLength() {
        String termuxLogin = "#!/data/data/com.termux/files/usr/bin/sh";
        String liftLogin = BootstrapPrefixRelocator.rewritePath(termuxLogin);

        Assert.assertEquals("#!/data/data/com.lift/./files/usr/bin/sh", liftLogin);
        Assert.assertEquals(termuxLogin.length(), liftLogin.length());
        Assert.assertEquals("/data/data/com.lift/./cache/apt/pkgcache.bin",
            BootstrapPrefixRelocator.rewritePath("/data/data/com.termux/cache/apt/pkgcache.bin"));
        Assert.assertEquals("/data/data/com.lift/./files/usr/share/termux-keyring/keyring.gpg",
            BootstrapPrefixRelocator.rewritePath(
                "/data/data/com.termux/files/usr/share/termux-keyring/keyring.gpg"));
        Assert.assertEquals("/data/data/com.lift/files/usr/bin/sh",
            Paths.get(liftLogin.substring(2)).normalize().toString());
    }

    @Test
    public void rewritesBinaryStreamsAcrossBufferBoundariesAndPreservesOtherBytes() throws Exception {
        byte[] sourcePrefix = TERMUX_APP_DATA_PATH.getBytes(StandardCharsets.US_ASCII);
        byte[] liftPrefix = BootstrapPrefixRelocator.rewritePath(TERMUX_APP_DATA_PATH).getBytes(StandardCharsets.US_ASCII);
        byte[] leadingBytes = new byte[8192 - 4];
        Arrays.fill(leadingBytes, (byte) 0xa5);
        byte[] trailingBytes = new byte[] { 0, 1, (byte) 0xff };
        byte[] incompletePrefix = Arrays.copyOf(sourcePrefix, sourcePrefix.length - 1);

        ByteArrayOutputStream inputData = new ByteArrayOutputStream();
        inputData.write(leadingBytes);
        inputData.write(sourcePrefix);
        inputData.write(trailingBytes);
        inputData.write(sourcePrefix);
        inputData.write(incompletePrefix);

        ByteArrayOutputStream expected = new ByteArrayOutputStream();
        expected.write(leadingBytes);
        expected.write(liftPrefix);
        expected.write(trailingBytes);
        expected.write(liftPrefix);
        expected.write(incompletePrefix);

        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        BootstrapPrefixRelocator.copyWithLiftPaths(
            new ByteArrayInputStream(inputData.toByteArray()), actual);

        Assert.assertArrayEquals(expected.toByteArray(), actual.toByteArray());
    }

    @Test
    public void migratesExistingFilesInPlaceAndPreservesExecutablePermission() throws Exception {
        byte[] sourcePath = TERMUX_APP_DATA_PATH.getBytes(StandardCharsets.US_ASCII);
        byte[] liftPath = BootstrapPrefixRelocator.rewritePath(TERMUX_APP_DATA_PATH)
            .getBytes(StandardCharsets.US_ASCII);
        byte[] leadingBytes = new byte[8192 - 4];
        Arrays.fill(leadingBytes, (byte) 0x5a);

        ByteArrayOutputStream expected = new ByteArrayOutputStream();
        expected.write(leadingBytes);
        expected.write(liftPath);
        expected.write(new byte[] { 0, (byte) 0xff });
        expected.write(liftPath);

        File file = File.createTempFile("lift-prefix-migration", ".bin");
        file.deleteOnExit();
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(leadingBytes);
            output.write(sourcePath);
            output.write(new byte[] { 0, (byte) 0xff });
            output.write(sourcePath);
        }
        Assert.assertTrue(file.setExecutable(true, true));
        long originalLength = file.length();
        java.nio.file.attribute.UserPrincipal originalOwner = Files.getOwner(file.toPath());

        BootstrapPrefixRelocator.rewriteRegularFile(file);

        Assert.assertArrayEquals(expected.toByteArray(),
            java.nio.file.Files.readAllBytes(file.toPath()));
        Assert.assertEquals(originalLength, file.length());
        Assert.assertTrue(file.canExecute());
        Assert.assertEquals(originalOwner, Files.getOwner(file.toPath()));
    }

    @Test
    public void inaccessibleTermuxLoginInterpreterReturnsPermissionDeniedAndRelocationStarts() throws Exception {
        Path temporaryDirectory = Files.createTempDirectory("lift-login-startup");
        Path termuxAppData = temporaryDirectory.resolve("com.termux");
        Path liftAppData = temporaryDirectory.resolve("com.lift");
        Path termuxShell = termuxAppData.resolve("files/usr/bin/sh");
        Path liftShell = liftAppData.resolve("files/usr/bin/sh");
        Path originalLogin = temporaryDirectory.resolve("login");
        Path relocatedLogin = temporaryDirectory.resolve("login-relocated");

        try {
            Files.createDirectories(termuxShell.getParent());
            Files.createDirectories(liftShell.getParent());
            Path systemShell = getSystemShell();
            Files.createSymbolicLink(termuxShell, systemShell);
            Files.createSymbolicLink(liftShell, systemShell);

            String originalScript = "#!" + termuxShell + "\nexit 0\n";
            Files.write(originalLogin, originalScript.getBytes(StandardCharsets.US_ASCII));
            setOwnerExecutable(originalLogin);
            Assert.assertTrue(originalLogin.toFile().canExecute());
            Assert.assertEquals(Files.getOwner(temporaryDirectory), Files.getOwner(originalLogin));

            // This models another app's private data directory: the script can be executed,
            // but its shebang interpreter cannot be traversed, so execve returns EACCES.
            Files.setPosixFilePermissions(termuxAppData, EnumSet.noneOf(PosixFilePermission.class));
            try {
                int exitCode = new ProcessBuilder(originalLogin.toString()).start().waitFor();
                Assert.fail("Expected execve to reject the inaccessible interpreter, got exit " + exitCode);
            } catch (IOException e) {
                Assert.assertTrue(e.toString(), e.toString().toLowerCase(Locale.ROOT).contains("permission denied"));
            } finally {
                Files.setPosixFilePermissions(termuxAppData, EnumSet.of(
                    PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE));
            }

            String targetAppDataAlias = liftAppData + "/.";
            Assert.assertEquals(termuxAppData.toString().length(), targetAppDataAlias.length());
            ByteArrayOutputStream relocatedScript = new ByteArrayOutputStream();
            BootstrapPrefixRelocator.copyWithPaths(
                new ByteArrayInputStream(originalScript.getBytes(StandardCharsets.US_ASCII)), relocatedScript,
                termuxAppData.toString(), targetAppDataAlias);
            Files.write(relocatedLogin, relocatedScript.toByteArray());
            setOwnerExecutable(relocatedLogin);

            Assert.assertTrue(new String(Files.readAllBytes(relocatedLogin), StandardCharsets.US_ASCII)
                .startsWith("#!" + targetAppDataAlias + "/files/usr/bin/sh\n"));
            Assert.assertEquals(Files.getOwner(temporaryDirectory), Files.getOwner(relocatedLogin));
            Assert.assertEquals(0, new ProcessBuilder(relocatedLogin.toString()).start().waitFor());
        } finally {
            // Restore traversal before deleting the fixture if an assertion interrupts the test.
            if (Files.exists(termuxAppData, LinkOption.NOFOLLOW_LINKS)) {
                Files.setPosixFilePermissions(termuxAppData, EnumSet.of(
                    PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE));
            }
            deleteRecursively(temporaryDirectory);
        }
    }

    @Test
    public void recognizesBootstrapExecutablesThatNeedPermissionsRestored() {
        Assert.assertTrue(BootstrapPrefixRelocator.requiresExecutePermission("bin/login"));
        Assert.assertTrue(BootstrapPrefixRelocator.requiresExecutePermission("libexec/coreutils/cat"));
        Assert.assertTrue(BootstrapPrefixRelocator.requiresExecutePermission("lib/apt/methods/http"));
        Assert.assertFalse(BootstrapPrefixRelocator.requiresExecutePermission("lib/libc.so"));
    }

    private static Path getSystemShell() {
        Path linuxShell = Paths.get("/bin/sh");
        if (Files.isExecutable(linuxShell)) return linuxShell;

        Path androidShell = Paths.get("/system/bin/sh");
        if (Files.isExecutable(androidShell)) return androidShell;

        throw new AssertionError("Unable to find a system shell for the startup regression test");
    }

    private static void setOwnerExecutable(Path file) throws IOException {
        Files.setPosixFilePermissions(file, EnumSet.of(PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return;
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            try (DirectoryStream<Path> children = Files.newDirectoryStream(path)) {
                for (Path child : children) deleteRecursively(child);
            }
        }
        Files.delete(path);
    }
}
