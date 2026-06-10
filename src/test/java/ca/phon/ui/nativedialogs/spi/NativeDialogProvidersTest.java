package ca.phon.ui.nativedialogs.spi;

import org.junit.jupiter.api.Test;
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
}
