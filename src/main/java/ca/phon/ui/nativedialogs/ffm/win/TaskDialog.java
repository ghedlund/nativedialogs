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
 * the overall size are computed by FFM (x64 ABI), avoiding hand-counted offsets.
 * TASKDIALOGCONFIG is 176 bytes on x64.
 */
public final class TaskDialog {

    // TASKDIALOG_BUTTON { int nButtonID; PCWSTR pszButtonText; } — 16 bytes on x64.
    private static final MemoryLayout BUTTON = MemoryLayout.structLayout(
        JAVA_INT.withName("nButtonID"),
        MemoryLayout.paddingLayout(4),
        ADDRESS.withName("pszButtonText"));

    private static final MemoryLayout TDC = MemoryLayout.structLayout(
        JAVA_INT.withName("cbSize"),
        MemoryLayout.paddingLayout(4),
        ADDRESS.withName("hwndParent"),
        ADDRESS.withName("hInstance"),
        JAVA_INT.withName("dwFlags"),
        JAVA_INT.withName("dwCommonButtons"),
        ADDRESS.withName("pszWindowTitle"),
        ADDRESS.withName("pszMainIcon"),
        ADDRESS.withName("pszMainInstruction"),
        ADDRESS.withName("pszContent"),
        JAVA_INT.withName("cButtons"),
        MemoryLayout.paddingLayout(4),
        ADDRESS.withName("pButtons"),
        JAVA_INT.withName("nDefaultButton"),
        JAVA_INT.withName("cRadioButtons"),
        ADDRESS.withName("pRadioButtons"),
        JAVA_INT.withName("nDefaultRadioButton"),
        MemoryLayout.paddingLayout(4),
        ADDRESS.withName("pszVerificationText"),
        ADDRESS.withName("pszExpandedInformation"),
        ADDRESS.withName("pszExpandedControlText"),
        ADDRESS.withName("pszCollapsedControlText"),
        ADDRESS.withName("hFooterIcon"),
        ADDRESS.withName("pszFooter"),
        ADDRESS.withName("pfCallback"),
        ADDRESS.withName("lpCallbackData"),
        JAVA_INT.withName("cxWidth"),
        MemoryLayout.paddingLayout(4));

    private static long off(String name) {
        return TDC.byteOffset(PathElement.groupElement(name));
    }
    private static long btnOff(String name) {
        return BUTTON.byteOffset(PathElement.groupElement(name));
    }

    private static final int TDF_ALLOW_DIALOG_CANCELLATION = 0x0008;
    private static final int FIRST_ID = 1000;

    private static final MethodHandle TASK_DIALOG_INDIRECT = Linker.nativeLinker().downcallHandle(
        Win32.COMCTL32.find("TaskDialogIndirect").orElseThrow(),
        FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS));

    private TaskDialog() {}

    /**
     * @return index into {@code buttons} of the chosen button, or -1 if cancelled.
     */
    public static int show(String title, String header, String content, List<String> buttons) {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment cfg = a.allocate(TDC);
            cfg.set(JAVA_INT, off("cbSize"), (int) TDC.byteSize());
            cfg.set(JAVA_INT, off("dwFlags"), TDF_ALLOW_DIALOG_CANCELLATION);
            if (title != null) cfg.set(ADDRESS, off("pszWindowTitle"), WinStr.wide(a, title));
            if (header != null) cfg.set(ADDRESS, off("pszMainInstruction"), WinStr.wide(a, header));
            if (content != null) cfg.set(ADDRESS, off("pszContent"), WinStr.wide(a, content));

            MemorySegment btnArray = a.allocate(BUTTON, buttons.size());
            for (int i = 0; i < buttons.size(); i++) {
                long base = i * BUTTON.byteSize();
                btnArray.set(JAVA_INT, base + btnOff("nButtonID"), FIRST_ID + i);
                btnArray.set(ADDRESS, base + btnOff("pszButtonText"), WinStr.wide(a, buttons.get(i)));
            }
            cfg.set(JAVA_INT, off("cButtons"), buttons.size());
            cfg.set(ADDRESS, off("pButtons"), btnArray);
            cfg.set(JAVA_INT, off("nDefaultButton"), FIRST_ID);

            MemorySegment pressed = a.allocate(JAVA_INT);
            int hr = (int) TASK_DIALOG_INDIRECT.invokeExact(
                cfg, pressed, MemorySegment.NULL, MemorySegment.NULL);
            if (hr != Win32.S_OK) return -1;
            int id = pressed.get(JAVA_INT, 0);
            int idx = id - FIRST_ID;
            return (idx >= 0 && idx < buttons.size()) ? idx : -1;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
