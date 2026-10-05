package ca.phon.ui.nativedialogs.ffm.mac;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

@EnabledOnOs(OS.MAC)
class GcdTest {

    @Test
    void asyncWorkRunsOnMainThread() throws Exception {
        final AtomicBoolean onMainThread = new AtomicBoolean();
        final CountDownLatch ran = new CountDownLatch(1);

        Gcd.onMainAsync(() -> {
            onMainThread.set(ObjC.sendBoolRet(ObjC.cls("NSThread"), "isMainThread"));
            ran.countDown();
        });

        assertTrue(ran.await(5, TimeUnit.SECONDS), "submitted work must run");
        assertTrue(onMainThread.get(), "submitted work must run on the main thread");
    }

    /**
     * Asynchronous work must not bring the JVM down when a garbage collection or a
     * stack walk happens while that work is finishing. All blocking dialogs requested
     * on the event dispatch thread run through this path.
     */
    @Test
    void asyncWorkSurvivesCollectionsAndStackWalks() throws Exception {
        final String javaBin = ProcessHandle.current().info().command().orElseThrow();
        final File output = File.createTempFile("gcd-async-stress", ".log");
        output.deleteOnExit();
        final Process stress = new ProcessBuilder(
                javaBin, "--enable-native-access=ALL-UNNAMED",
                "-XX:ErrorFile=" + output.getAbsolutePath() + ".hs_err",
                "-cp", System.getProperty("java.class.path"),
                GcdAsyncStress.class.getName(), "3000")
            .redirectErrorStream(true)
            .redirectOutput(output)
            .start();

        assertTrue(stress.waitFor(60, TimeUnit.SECONDS), "stress run must finish");
        assertEquals(0, stress.exitValue(),
            "JVM running asynchronous work must exit normally; output:\n"
                + Files.readString(output.toPath()));
    }
}
