package ca.phon.ui.nativedialogs.ffm.win;

import java.lang.foreign.*;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.invoke.MethodHandle;
import java.util.List;

import static java.lang.foreign.ValueLayout.*;

/**
 * Wraps comctl32 {@code TaskDialogIndirect} with a custom button set.
 * Returns the 0-based index of the chosen custom button, or -1 if dismissed.
 *
 * <p>Struct layouts are declared with {@link MemoryLayout} so field offsets and
 * the overall size are computed by FFM, avoiding hand-counted offsets. commctrl.h
 * declares both structs with 1-byte packing ({@code pshpack1.h}), so the layouts
 * have no padding and use unaligned members: TASKDIALOGCONFIG is 160 bytes on x64
 * and TASKDIALOG_BUTTON is 12. Windows rejects any other size with E_INVALIDARG.
 */
public final class TaskDialog {

    private static final ValueLayout.OfInt INT = JAVA_INT_UNALIGNED;
    private static final AddressLayout PTR = ADDRESS_UNALIGNED;

    // TASKDIALOG_BUTTON { int nButtonID; PCWSTR pszButtonText; }
    static final MemoryLayout BUTTON = MemoryLayout.structLayout(
        INT.withName("nButtonID"),
        PTR.withName("pszButtonText"));

    static final MemoryLayout TDC = MemoryLayout.structLayout(
        INT.withName("cbSize"),
        PTR.withName("hwndParent"),
        PTR.withName("hInstance"),
        INT.withName("dwFlags"),
        INT.withName("dwCommonButtons"),
        PTR.withName("pszWindowTitle"),
        PTR.withName("pszMainIcon"),
        PTR.withName("pszMainInstruction"),
        PTR.withName("pszContent"),
        INT.withName("cButtons"),
        PTR.withName("pButtons"),
        INT.withName("nDefaultButton"),
        INT.withName("cRadioButtons"),
        PTR.withName("pRadioButtons"),
        INT.withName("nDefaultRadioButton"),
        PTR.withName("pszVerificationText"),
        PTR.withName("pszExpandedInformation"),
        PTR.withName("pszExpandedControlText"),
        PTR.withName("pszCollapsedControlText"),
        PTR.withName("hFooterIcon"),
        PTR.withName("pszFooter"),
        PTR.withName("pfCallback"),
        PTR.withName("lpCallbackData"),
        INT.withName("cxWidth"));

    private static long off(String name) {
        return TDC.byteOffset(PathElement.groupElement(name));
    }
    private static long btnOff(String name) {
        return BUTTON.byteOffset(PathElement.groupElement(name));
    }

    private static final int TDF_ALLOW_DIALOG_CANCELLATION = 0x0008;
    private static final int TDF_POSITION_RELATIVE_TO_WINDOW = 0x1000;
    private static final int FIRST_ID = 1000;

    private static final MethodHandle TASK_DIALOG_INDIRECT = Linker.nativeLinker().downcallHandle(
        Win32.COMCTL32.find("TaskDialogIndirect").orElseThrow(),
        FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS));

    private TaskDialog() {}

    /**
     * Shows the dialog with no owner window.
     *
     * @return index into {@code buttons} of the chosen button, or -1 if cancelled.
     * @throws IllegalStateException if Windows cannot show the dialog
     */
    public static int show(String title, String header, String content, List<String> buttons) {
        return show(title, header, content, buttons, MemorySegment.NULL);
    }

    /**
     * Shows the dialog owned by {@code owner}: centred over it and modal to it.
     *
     * @param owner HWND of the owner window, or {@code NULL} for none
     * @return index into {@code buttons} of the chosen button, or -1 if cancelled.
     * @throws IllegalStateException if Windows cannot show the dialog
     */
    public static int show(String title, String header, String content, List<String> buttons, MemorySegment owner) {
        return show(title, header, content, buttons, owner, MemorySegment.NULL);
    }

    /** Fills a TASKDIALOGCONFIG; strings and buttons are allocated in {@code a}. */
    static MemorySegment config(Arena a, String title, String header, String content, List<String> buttons,
                                MemorySegment owner, MemorySegment callback) {
        MemorySegment cfg = a.allocate(TDC);
        cfg.set(INT, off("cbSize"), (int) TDC.byteSize());
        int flags = TDF_ALLOW_DIALOG_CANCELLATION;
        if (owner.address() != 0L) {
            cfg.set(PTR, off("hwndParent"), owner);
            // without this flag Windows centres the dialog on the monitor, not the owner
            flags |= TDF_POSITION_RELATIVE_TO_WINDOW;
        }
        cfg.set(INT, off("dwFlags"), flags);
        if (title != null) cfg.set(PTR, off("pszWindowTitle"), WinStr.wide(a, title));
        if (header != null) cfg.set(PTR, off("pszMainInstruction"), WinStr.wide(a, header));
        if (content != null) cfg.set(PTR, off("pszContent"), WinStr.wide(a, content));

        MemorySegment btnArray = a.allocate(BUTTON, buttons.size());
        for (int i = 0; i < buttons.size(); i++) {
            long base = i * BUTTON.byteSize();
            btnArray.set(INT, base + btnOff("nButtonID"), FIRST_ID + i);
            btnArray.set(PTR, base + btnOff("pszButtonText"), WinStr.wide(a, buttons.get(i)));
        }
        cfg.set(INT, off("cButtons"), buttons.size());
        cfg.set(PTR, off("pButtons"), btnArray);
        cfg.set(INT, off("nDefaultButton"), FIRST_ID);
        cfg.set(PTR, off("pfCallback"), callback);
        return cfg;
    }

    static int show(String title, String header, String content, List<String> buttons,
                    MemorySegment owner, MemorySegment callback) {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment cfg = config(a, title, header, content, buttons, owner, callback);

            MemorySegment pressed = a.allocate(JAVA_INT);
            int hr = (int) TASK_DIALOG_INDIRECT.invokeExact(
                cfg, pressed, MemorySegment.NULL, MemorySegment.NULL);
            if (hr != Win32.S_OK) {
                throw new IllegalStateException(String.format("TaskDialogIndirect failed: hr=0x%08X", hr));
            }
            int id = pressed.get(JAVA_INT, 0);
            int idx = id - FIRST_ID;
            return (idx >= 0 && idx < buttons.size()) ? idx : -1;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
