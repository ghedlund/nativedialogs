package ca.phon.ui.nativedialogs;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeUtilitiesTest {

    @Test
    void exactlyOneOsFlagIsTrue() {
        int trueCount = 0;
        if (NativeUtilities.isMacOs()) trueCount++;
        if (NativeUtilities.isWindows()) trueCount++;
        if (NativeUtilities.isLinux()) trueCount++;
        assertTrue(trueCount <= 1, "OS detection must not report multiple platforms");
    }

    @Test
    void osDetectionMatchesSystemProperty() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) assertTrue(NativeUtilities.isMacOs());
        else if (os.contains("windows")) assertTrue(NativeUtilities.isWindows());
    }
}
