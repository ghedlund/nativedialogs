package ca.phon.ui.nativedialogs.spi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import ca.phon.ui.nativedialogs.NativeUtilities;
import static org.junit.jupiter.api.Assertions.*;

class NativeDialogProvidersTest {

    @Test
    void linuxHasNoProvider() {
        if (NativeUtilities.isLinux()) {
            assertNull(NativeDialogProviders.get(),
                "Linux must have no native provider (Swing fallback)");
        }
    }

    @Test
    void getIsStable() {
        assertSame(NativeDialogProviders.get(), NativeDialogProviders.get(),
            "provider selection must be cached");
    }

    /**
     * Runtime validation (not just compile) that the macOS provider initialises:
     * its constructor resolves NSOpenPanel via FFM/AppKit. Does NOT open any dialog.
     */
    @Test
    @EnabledOnOs(OS.MAC)
    void macProviderInitialisesAndSupportsExpectedDialogs() {
        NativeDialogProvider p = NativeDialogProviders.get();
        assertNotNull(p, "macOS must select a native provider (FFM/AppKit init)");
        assertTrue(p.supportsOpen(), "macOS supports native open");
        assertTrue(p.supportsSave(), "macOS supports native save");
        assertTrue(p.supportsMessage(), "macOS supports native message");
        assertFalse(p.supportsFont(), "macOS font falls back to Swing");
        assertFalse(p.supportsColor(), "macOS colour falls back to Swing");
    }
}
