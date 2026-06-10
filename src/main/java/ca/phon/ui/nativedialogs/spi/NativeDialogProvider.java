package ca.phon.ui.nativedialogs.spi;

import ca.phon.ui.nativedialogs.*;

/**
 * A platform backend that shows dialogs natively. Each {@code show*} method must
 * deliver its result by calling {@code props.getListener().nativeDialogEvent(...)}.
 * The {@code supports*} methods let the facade fall back to Swing when a backend
 * does not implement a given dialog.
 */
public interface NativeDialogProvider {

    boolean supportsOpen();
    boolean supportsSave();
    boolean supportsMessage();
    boolean supportsFont();
    boolean supportsColor();

    void showOpen(OpenDialogProperties props);
    void showSave(SaveDialogProperties props);
    void showMessage(MessageDialogProperties props);
    void showFont(FontDialogProperties props);
    void showColor(ColorDialogProperties props);
}
