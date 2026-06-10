package ca.phon.ui.nativedialogs.spi;
import ca.phon.ui.nativedialogs.*;
final class MacDialogProvider implements NativeDialogProvider {
    MacDialogProvider() { throw new UnsupportedOperationException("not yet implemented"); }
    public boolean supportsOpen() { return false; }
    public boolean supportsSave() { return false; }
    public boolean supportsMessage() { return false; }
    public boolean supportsFont() { return false; }
    public boolean supportsColor() { return false; }
    public void showOpen(OpenDialogProperties p) {}
    public void showSave(SaveDialogProperties p) {}
    public void showMessage(MessageDialogProperties p) {}
    public void showFont(FontDialogProperties p) {}
    public void showColor(ColorDialogProperties p) {}
}
