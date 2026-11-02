package com.lift.app;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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

        BootstrapPrefixRelocator.rewriteRegularFile(file);

        Assert.assertArrayEquals(expected.toByteArray(),
            java.nio.file.Files.readAllBytes(file.toPath()));
        Assert.assertEquals(originalLength, file.length());
        Assert.assertTrue(file.canExecute());
    }

    @Test
    public void recognizesBootstrapExecutablesThatNeedPermissionsRestored() {
        Assert.assertTrue(BootstrapPrefixRelocator.requiresExecutePermission("bin/login"));
        Assert.assertTrue(BootstrapPrefixRelocator.requiresExecutePermission("libexec/coreutils/cat"));
        Assert.assertTrue(BootstrapPrefixRelocator.requiresExecutePermission("lib/apt/methods/http"));
        Assert.assertFalse(BootstrapPrefixRelocator.requiresExecutePermission("lib/libc.so"));
    }
}
