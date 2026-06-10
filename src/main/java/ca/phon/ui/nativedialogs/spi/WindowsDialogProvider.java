package ca.phon.ui.nativedialogs.spi;

import java.lang.foreign.*;
import java.util.ArrayList;
import java.util.List;

import ca.phon.ui.nativedialogs.*;
import ca.phon.ui.nativedialogs.ffm.win.*;

import static java.lang.foreign.ValueLayout.*;
import static ca.phon.ui.nativedialogs.ffm.win.Win32.*;

/** Windows native dialogs via the Common Item Dialog (COM) and comdlg32. */
final class WindowsDialogProvider implements NativeDialogProvider {

    // COM vtable indices (0-based; 0/1/2 = QueryInterface/AddRef/Release)
    private static final int SHOW = 3, SET_FILE_TYPES = 4, SET_OPTIONS = 9, GET_OPTIONS = 10,
        SET_DEFAULT_FOLDER = 11, SET_FILE_NAME = 15, SET_TITLE = 17, SET_OK_LABEL = 18,
        SET_FILE_NAME_LABEL = 19, GET_RESULT = 20, SET_DEFAULT_EXTENSION = 22, GET_RESULTS = 27;
    private static final int SI_GET_DISPLAY_NAME = 5;
    private static final int SIA_GET_COUNT = 7, SIA_GET_ITEM_AT = 8;

    WindowsDialogProvider() {
        // fail fast if the Win32 libraries are unavailable (forces Win32 class init)
        Win32.CoUninitialize.toString();
    }

    @Override public boolean supportsOpen() { return true; }
    @Override public boolean supportsSave() { return true; }
    @Override public boolean supportsMessage() { return true; }
    @Override public boolean supportsFont() { return true; }
    @Override public boolean supportsColor() { return true; }

    @Override
    public void showOpen(OpenDialogProperties props) {
        runOnComThread(props.isRunAsync(), () -> runFileDialog(props, true));
    }

    @Override
    public void showSave(SaveDialogProperties props) {
        runOnComThread(props.isRunAsync(), () -> runFileDialog(props, false));
    }

    private void runOnComThread(boolean async, Runnable work) {
        Runnable sta = () -> {
            try {
                int hr = (int) CoInitializeEx.invokeExact(MemorySegment.NULL, COINIT_APARTMENTTHREADED);
                try {
                    work.run();
                } finally {
                    CoUninitialize.invokeExact();
                }
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        };
        if (async) {
            Thread th = new Thread(sta, "native-dialog-com");
            th.setDaemon(true);
            th.start();
        } else {
            sta.run();
        }
    }

    private void runFileDialog(NativeDialogProperties props, boolean open) {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment ppv = a.allocate(ADDRESS);
            MemorySegment clsid = open ? CLSID_FileOpenDialog(a) : CLSID_FileSaveDialog(a);
            MemorySegment iid = open ? IID_IFileOpenDialog(a) : IID_IFileSaveDialog(a);

            int hr = (int) CoCreateInstance.invokeExact(
                clsid, MemorySegment.NULL, CLSCTX_INPROC_SERVER, iid, ppv);
            if (hr != S_OK) { deliverCancel(props); return; }
            MemorySegment dlg = ppv.get(ADDRESS, 0);

            try {
                applyFileOptions(a, dlg, props, open);
                int showHr = Com.callHr(dlg, SHOW, MemorySegment.NULL);
                if (showHr != S_OK) { deliverCancel(props); return; }

                if (open && isMulti(props)) {
                    deliverMulti(a, dlg, props);
                } else {
                    deliverSingle(a, dlg, props);
                }
            } finally {
                Com.release(dlg);
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private boolean isMulti(NativeDialogProperties props) {
        return props instanceof OpenDialogProperties o && o.isAllowMultipleSelection();
    }

    private void applyFileOptions(Arena a, MemorySegment dlg, NativeDialogProperties props, boolean open) {
        // OpenDialogProperties extends SaveDialogProperties, so the shared file getters
        // are reachable via a single SaveDialogProperties reference for both dialogs.
        SaveDialogProperties s = (SaveDialogProperties) props;

        MemorySegment optBuf = a.allocate(JAVA_INT);
        Com.callHr(dlg, GET_OPTIONS, optBuf);
        int opts = optBuf.get(JAVA_INT, 0) | FOS_FORCEFILESYSTEM;
        if (s.isShowHidden()) opts |= FOS_FORCESHOWHIDDEN;
        if (open) {
            opts |= FOS_FILEMUSTEXIST | FOS_PATHMUSTEXIST;
            OpenDialogProperties o = (OpenDialogProperties) props;
            if (o.isCanChooseDirectories() && !o.isCanChooseFiles()) opts |= FOS_PICKFOLDERS;
            if (o.isAllowMultipleSelection()) opts |= FOS_ALLOWMULTISELECT;
        } else {
            opts |= FOS_OVERWRITEPROMPT | FOS_PATHMUSTEXIST;
        }
        Com.callHrInt(dlg, SET_OPTIONS, opts);

        if (s.getTitle() != null) Com.callHr(dlg, SET_TITLE, WinStr.wide(a, s.getTitle()));
        if (s.getPrompt() != null) Com.callHr(dlg, SET_OK_LABEL, WinStr.wide(a, s.getPrompt()));
        if (s.getNameFieldLabel() != null) Com.callHr(dlg, SET_FILE_NAME_LABEL, WinStr.wide(a, s.getNameFieldLabel()));
        if (s.getInitialFile() != null) Com.callHr(dlg, SET_FILE_NAME, WinStr.wide(a, s.getInitialFile()));

        FileFilter filter = s.getFileFilter();
        if (filter != null) {
            applyFilter(a, dlg, filter);
            String ext = filter.getDefaultExtension();
            if (ext != null && !ext.isEmpty())
                Com.callHr(dlg, SET_DEFAULT_EXTENSION, WinStr.wide(a, ext));
        }

        String initialFolder = s.getInitialFolder();
        if (initialFolder != null) {
            MemorySegment psiPP = a.allocate(ADDRESS);
            try {
                int shr = (int) SHCreateItemFromParsingName.invokeExact(
                    WinStr.wide(a, initialFolder), MemorySegment.NULL, IID_IShellItem(a), psiPP);
                if (shr == S_OK) {
                    MemorySegment psi = psiPP.get(ADDRESS, 0);
                    Com.callHr(dlg, SET_DEFAULT_FOLDER, psi);
                    Com.release(psi);
                }
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }

    private void applyFilter(Arena a, MemorySegment dlg, FileFilter filter) {
        // COMDLG_FILTERSPEC[2] = { {desc, "*.ext;*.ext2"}, {"All Files","*.*"} }
        List<String> exts = new ArrayList<>(filter.getAllExtensions());
        StringBuilder spec = new StringBuilder();
        for (int i = 0; i < exts.size(); i++) {
            if (i > 0) spec.append(';');
            spec.append("*.").append(exts.get(i));
        }
        String desc = filter.getDescription() != null ? filter.getDescription() : "Files";

        MemorySegment specs = a.allocate(ADDRESS.byteSize() * 2 * 2); // 2 specs * 2 ptrs each
        specs.setAtIndex(ADDRESS, 0, WinStr.wide(a, desc));
        specs.setAtIndex(ADDRESS, 1, WinStr.wide(a, spec.toString()));
        specs.setAtIndex(ADDRESS, 2, WinStr.wide(a, "All Files"));
        specs.setAtIndex(ADDRESS, 3, WinStr.wide(a, "*.*"));
        Com.callHrIntPtr(dlg, SET_FILE_TYPES, 2, specs);
    }

    private void deliverSingle(Arena a, MemorySegment dlg, NativeDialogProperties props) {
        MemorySegment psiPP = a.allocate(ADDRESS);
        int hr = Com.callHr(dlg, GET_RESULT, psiPP);
        if (hr != S_OK) { deliverCancel(props); return; }
        MemorySegment psi = psiPP.get(ADDRESS, 0);
        String path = displayName(a, psi);
        Com.release(psi);
        props.getListener().nativeDialogEvent(new NativeDialogEvent(NativeDialogEvent.OK_OPTION, path));
    }

    private void deliverMulti(Arena a, MemorySegment dlg, NativeDialogProperties props) {
        MemorySegment arrPP = a.allocate(ADDRESS);
        int hr = Com.callHr(dlg, GET_RESULTS, arrPP);
        if (hr != S_OK) { deliverCancel(props); return; }
        MemorySegment arr = arrPP.get(ADDRESS, 0);
        MemorySegment countBuf = a.allocate(JAVA_INT);
        Com.callHr(arr, SIA_GET_COUNT, countBuf);
        int count = countBuf.get(JAVA_INT, 0);
        List<String> paths = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            MemorySegment itemPP = a.allocate(ADDRESS);
            Com.callHrIntPtr(arr, SIA_GET_ITEM_AT, i, itemPP);
            MemorySegment psi = itemPP.get(ADDRESS, 0);
            paths.add(displayName(a, psi));
            Com.release(psi);
        }
        Com.release(arr);
        props.getListener().nativeDialogEvent(
            new NativeDialogEvent(NativeDialogEvent.OK_OPTION, paths.toArray(new String[0])));
    }

    private String displayName(Arena a, MemorySegment psi) {
        MemorySegment namePP = a.allocate(ADDRESS);
        // GetDisplayName(SIGDN_FILESYSPATH, &pszName) — (this, int, ptr)
        Com.callHrIntPtr(psi, SI_GET_DISPLAY_NAME, SIGDN_FILESYSPATH, namePP);
        MemorySegment pszName = namePP.get(ADDRESS, 0);
        String path = WinStr.fromWide(pszName);
        try { CoTaskMemFree.invokeExact(pszName); }
        catch (Throwable t) { throw new RuntimeException(t); }
        return path;
    }

    private void deliverCancel(NativeDialogProperties props) {
        props.getListener().nativeDialogEvent(new NativeDialogEvent(NativeDialogEvent.CANCEL_OPTION, null));
    }

    @Override public void showMessage(MessageDialogProperties p) { throw new UnsupportedOperationException(); }
    @Override public void showFont(FontDialogProperties p) { throw new UnsupportedOperationException(); }
    @Override public void showColor(ColorDialogProperties p) { throw new UnsupportedOperationException(); }
}
