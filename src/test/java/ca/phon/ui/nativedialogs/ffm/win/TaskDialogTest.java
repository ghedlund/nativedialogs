package ca.phon.ui.nativedialogs.ffm.win;

import java.lang.foreign.*;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static java.lang.foreign.ValueLayout.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledOnOs(OS.WINDOWS)
class TaskDialogTest {

    private static long tdc(String field) {
        return TaskDialog.TDC.byteOffset(PathElement.groupElement(field));
    }

    /** commctrl.h declares the task dialog structs with 1-byte packing (pshpack1.h). */
    @Test
    void configMatchesPackedWindowsLayout() {
        assertEquals(160, TaskDialog.TDC.byteSize());
        assertEquals(4, tdc("hwndParent"));
        assertEquals(20, tdc("dwFlags"));
        assertEquals(28, tdc("pszWindowTitle"));
        assertEquals(60, tdc("cButtons"));
        assertEquals(64, tdc("pButtons"));
        assertEquals(72, tdc("nDefaultButton"));
        assertEquals(140, tdc("pfCallback"));
        assertEquals(156, tdc("cxWidth"));
    }

    @Test
    void buttonMatchesPackedWindowsLayout() {
        assertEquals(12, TaskDialog.BUTTON.byteSize());
        assertEquals(4, TaskDialog.BUTTON.byteOffset(PathElement.groupElement("pszButtonText")));
    }

    // --- real dialog, clicked by its own callback as soon as it is created ---

    private static final int TDN_CREATED = 0;
    private static final int TDM_CLICK_BUTTON = 0x0400 + 102;
    private static final int SECOND_BUTTON_ID = 1001;

    private static final MethodHandle SEND_MESSAGE = Linker.nativeLinker().downcallHandle(
        SymbolLookup.libraryLookup("user32.dll", Arena.global()).find("SendMessageW").orElseThrow(),
        FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_INT, JAVA_LONG, JAVA_LONG));

    static int clickSecondButtonOnCreate(MemorySegment hwnd, int msg, long wParam, long lParam, long refData) {
        if (msg == TDN_CREATED) {
            try {
                long unused = (long) SEND_MESSAGE.invokeExact(hwnd, TDM_CLICK_BUTTON, (long) SECOND_BUTTON_ID, 0L);
            } catch (Throwable t) {
                // an exception leaving an upcall ends the JVM
            }
        }
        return 0;
    }

    @Test
    void showReturnsTheClickedButton() throws Throwable {
        MethodHandle target = MethodHandles.lookup().findStatic(TaskDialogTest.class, "clickSecondButtonOnCreate",
            MethodType.methodType(int.class, MemorySegment.class, int.class, long.class, long.class, long.class));
        try (Arena a = Arena.ofShared()) {
            MemorySegment callback = Linker.nativeLinker().upcallStub(target,
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_LONG, JAVA_LONG, JAVA_LONG), a);
            int idx = assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
                TaskDialog.show("Test", "Header", "Closes itself", List.of("Yes", "No", "Cancel"), callback));
            assertEquals(1, idx);
        }
    }
}
