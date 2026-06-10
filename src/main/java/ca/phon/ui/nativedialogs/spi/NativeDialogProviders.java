package ca.phon.ui.nativedialogs.spi;

import java.util.logging.Level;
import java.util.logging.Logger;

import ca.phon.ui.nativedialogs.NativeUtilities;

/**
 * Chooses the native dialog provider for the current OS, once, lazily.
 * Returns {@code null} when no native backend is available or it fails to
 * initialise; the facade then uses Swing.
 */
public final class NativeDialogProviders {

    private static final Logger LOGGER = Logger.getLogger(NativeDialogProviders.class.getName());

    private static final NativeDialogProvider PROVIDER = select();

    private NativeDialogProviders() {}

    public static NativeDialogProvider get() {
        return PROVIDER;
    }

    private static NativeDialogProvider select() {
        try {
            if (NativeUtilities.isMacOs()) {
                return new MacDialogProvider();
            } else if (NativeUtilities.isWindows()) {
                return new WindowsDialogProvider();
            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Native dialog provider unavailable; using Swing fallback", t);
        }
        return null;
    }
}
