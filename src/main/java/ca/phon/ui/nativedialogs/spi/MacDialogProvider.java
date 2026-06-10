package ca.phon.ui.nativedialogs.spi;

import java.lang.foreign.*;
import java.util.ArrayList;
import java.util.List;

import ca.phon.ui.nativedialogs.*;
import ca.phon.ui.nativedialogs.ffm.mac.*;

/**
 * macOS native dialogs via Cocoa (FFM, app-modal). Open/save/message native;
 * font/colour unsupported (facade falls back to Swing).
 */
final class MacDialogProvider implements NativeDialogProvider {

    private static final long NS_MODAL_RESPONSE_OK = 1L;     // NSModalResponseOK
    private static final long NS_ALERT_FIRST_BUTTON = 1000L;     // NSAlertFirstButtonReturn
    private static final int NS_WARNING_ALERT_STYLE = 0;         // NSAlertStyleWarning
    private static final long NS_CONTROL_STATE_ON = 1L;          // NSControlStateValueOn

    MacDialogProvider() {
        // Force class init so construction fails fast if FFM/AppKit is unavailable.
        ObjC.cls("NSOpenPanel");
    }

    @Override public boolean supportsOpen() { return true; }
    @Override public boolean supportsSave() { return true; }
    @Override public boolean supportsMessage() { return true; }
    @Override public boolean supportsFont() { return false; }
    @Override public boolean supportsColor() { return false; }

    @Override public void showFont(FontDialogProperties p) { throw new UnsupportedOperationException(); }
    @Override public void showColor(ColorDialogProperties p) { throw new UnsupportedOperationException(); }

    @Override
    public void showOpen(OpenDialogProperties props) {
        Runnable work = () -> runOpen(props);
        if (props.isRunAsync()) Gcd.onMainAsync(work); else Gcd.onMainSync(work);
    }

    private void runOpen(OpenDialogProperties props) {
        try (Arena a = Arena.ofShared()) {
            MemorySegment panel = ObjC.send(ObjC.cls("NSOpenPanel"), "openPanel");
            ObjC.sendBool(panel, "setCanCreateDirectories:", props.isCanCreateDirectories());
            ObjC.sendBool(panel, "setShowsHiddenFiles:", props.isShowHidden());
            ObjC.sendBool(panel, "setCanChooseFiles:", props.isCanChooseFiles());
            ObjC.sendBool(panel, "setCanChooseDirectories:", props.isCanChooseDirectories());
            ObjC.sendBool(panel, "setAllowsMultipleSelection:", props.isAllowMultipleSelection());

            applyCommon(a, panel, props.getTitle(), props.getPrompt(),
                props.getNameFieldLabel(), props.getMessage(),
                props.getInitialFolder(), props.getInitialFile(), props.getFileFilter());

            long response = ObjC.sendLong(panel, "runModal");
            NativeDialogEvent evt;
            if (response == NS_MODAL_RESPONSE_OK) {
                if (props.isAllowMultipleSelection()) {
                    MemorySegment urls = ObjC.send(panel, "URLs");
                    long count = ObjC.sendLong(urls, "count");
                    List<String> paths = new ArrayList<>();
                    for (long i = 0; i < count; i++) {
                        MemorySegment url = ObjC.sendIdLong(urls, "objectAtIndex:", i);
                        paths.add(Foundation.urlPath(url));
                    }
                    evt = new NativeDialogEvent(NativeDialogEvent.OK_OPTION, paths.toArray(new String[0]));
                } else {
                    MemorySegment url = ObjC.send(panel, "URL");
                    evt = new NativeDialogEvent(NativeDialogEvent.OK_OPTION, Foundation.urlPath(url));
                }
            } else {
                evt = new NativeDialogEvent(NativeDialogEvent.CANCEL_OPTION, null);
            }
            props.getListener().nativeDialogEvent(evt);
        }
    }

    @Override
    public void showSave(SaveDialogProperties props) {
        Runnable work = () -> runSave(props);
        if (props.isRunAsync()) Gcd.onMainAsync(work); else Gcd.onMainSync(work);
    }

    private void runSave(SaveDialogProperties props) {
        try (Arena a = Arena.ofShared()) {
            MemorySegment panel = ObjC.send(ObjC.cls("NSSavePanel"), "savePanel");
            ObjC.sendBool(panel, "setCanCreateDirectories:", props.isCanCreateDirectories());
            ObjC.sendBool(panel, "setShowsHiddenFiles:", props.isShowHidden());

            applyCommon(a, panel, props.getTitle(), props.getPrompt(),
                props.getNameFieldLabel(), props.getMessage(),
                props.getInitialFolder(), props.getInitialFile(), props.getFileFilter());

            long response = ObjC.sendLong(panel, "runModal");
            NativeDialogEvent evt;
            if (response == NS_MODAL_RESPONSE_OK) {
                MemorySegment url = ObjC.send(panel, "URL");
                evt = new NativeDialogEvent(NativeDialogEvent.OK_OPTION, Foundation.urlPath(url));
            } else {
                evt = new NativeDialogEvent(NativeDialogEvent.CANCEL_OPTION, null);
            }
            props.getListener().nativeDialogEvent(evt);
        }
    }

    private void applyCommon(Arena a, MemorySegment panel, String title, String prompt,
            String nameFieldLabel, String message, String initialFolder,
            String initialFile, FileFilter filter) {
        if (title != null) ObjC.sendVoid(panel, "setTitle:", Foundation.nsString(a, title));
        if (prompt != null) ObjC.sendVoid(panel, "setPrompt:", Foundation.nsString(a, prompt));
        if (nameFieldLabel != null) ObjC.sendVoid(panel, "setNameFieldLabel:", Foundation.nsString(a, nameFieldLabel));
        if (message != null) ObjC.sendVoid(panel, "setMessage:", Foundation.nsString(a, message));
        if (initialFolder != null) ObjC.sendVoid(panel, "setDirectoryURL:", Foundation.fileUrl(a, initialFolder));
        if (initialFile != null) ObjC.sendVoid(panel, "setNameFieldStringValue:", Foundation.nsString(a, initialFile));
        if (filter != null && filter.getAllExtensions() != null && !filter.getAllExtensions().isEmpty()) {
            ObjC.sendVoid(panel, "setAllowedFileTypes:",
                Foundation.nsStringArray(a, new ArrayList<>(filter.getAllExtensions())));
        }
    }

    @Override
    public void showMessage(MessageDialogProperties props) {
        Runnable work = () -> runMessage(props);
        if (props.isRunAsync()) Gcd.onMainAsync(work); else Gcd.onMainSync(work);
    }

    private void runMessage(MessageDialogProperties props) {
        try (Arena a = Arena.ofShared()) {
            MemorySegment alert = ObjC.send(
                ObjC.send(ObjC.cls("NSAlert"), "alloc"), "init");
            ObjC.sendLongArg(alert, "setAlertStyle:", NS_WARNING_ALERT_STYLE);
            if (props.getHeader() != null)
                ObjC.sendVoid(alert, "setMessageText:", Foundation.nsString(a, props.getHeader()));
            if (props.getMessage() != null)
                ObjC.sendVoid(alert, "setInformativeText:", Foundation.nsString(a, props.getMessage()));

            boolean suppression = props.isShowSuppressionBox();
            ObjC.sendBool(alert, "setShowsSuppressionButton:", suppression);
            if (suppression && props.getSuppressionMessage() != null) {
                MemorySegment btn = ObjC.send(alert, "suppressionButton");
                ObjC.sendVoid(btn, "setTitle:", Foundation.nsString(a, props.getSuppressionMessage()));
            }

            String[] options = props.getOptions();
            if (options == null || options.length == 0) options = new String[] { "Ok" };
            for (String opt : options) {
                ObjC.send(alert, "addButtonWithTitle:", Foundation.nsString(a, opt));
            }

            long alertResult = ObjC.sendLong(alert, "runModal");
            int resultCode = (int) (alertResult - NS_ALERT_FIRST_BUTTON);

            Boolean suppressed = null;
            if (suppression) {
                MemorySegment btn = ObjC.send(alert, "suppressionButton");
                long state = ObjC.sendLong(btn, "state");
                suppressed = (state == NS_CONTROL_STATE_ON);
            }
            props.getListener().nativeDialogEvent(new NativeDialogEvent(resultCode, suppressed));
        }
    }
}
