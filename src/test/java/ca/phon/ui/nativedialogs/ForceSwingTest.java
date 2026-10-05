package ca.phon.ui.nativedialogs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import static org.junit.jupiter.api.Assertions.*;

class ForceSwingTest {

    /**
     * The documented escape hatch: with the system property set, no dialog may use
     * the native backend, so an installation where native dialogs misbehave can be
     * switched to the Swing dialogs without a new build.
     */
    @Test
    void forceSwingPropertyDisablesNativeProvider() {
        System.setProperty(NativeDialogs.FORCE_SWING_PROP, "true");
        try {
            assertNull(NativeDialogs.provider(),
                "no native provider may be used while " + NativeDialogs.FORCE_SWING_PROP + " is true");
        } finally {
            System.clearProperty(NativeDialogs.FORCE_SWING_PROP);
        }
    }

    @Test
    @EnabledOnOs({OS.MAC, OS.WINDOWS})
    void nativeProviderUsedWhenPropertyIsNotSet() {
        System.clearProperty(NativeDialogs.FORCE_SWING_PROP);

        assertNotNull(NativeDialogs.provider(), "native dialogs are the default on this platform");
    }
}
