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
        // implemented in Task 2.5
        throw new UnsupportedOperationException("message not yet wired");
    }
}
