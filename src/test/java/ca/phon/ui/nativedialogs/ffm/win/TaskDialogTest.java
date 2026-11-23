package ca.phon.ui.nativedialogs.ffm.win;

import java.lang.foreign.*;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static java.lang.foreign.ValueLayout.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

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
                TaskDialog.show("Test", "Header", "Closes itself", List.of("Yes", "No", "Cancel"),
                    MemorySegment.NULL, callback));
            assertEquals(1, idx);
        }
    }

    // --- owner window ---

    private static final int TDF_POSITION_RELATIVE_TO_WINDOW = 0x1000;

    @Test
    void configSetsTheOwnerWhenGiven() {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment owner = MemorySegment.ofAddress(0x1234L);
            MemorySegment cfg = TaskDialog.config(a, "t", "h", "c", List.of("Ok"), owner, MemorySegment.NULL);
            assertEquals(0x1234L, cfg.get(ADDRESS_UNALIGNED, tdc("hwndParent")).address());
            assertNotEquals(0, cfg.get(JAVA_INT_UNALIGNED, tdc("dwFlags")) & TDF_POSITION_RELATIVE_TO_WINDOW);
        }
    }

    @Test
    void configHasNoOwnerByDefault() {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment cfg = TaskDialog.config(a, "t", "h", "c", List.of("Ok"), MemorySegment.NULL, MemorySegment.NULL);
            assertEquals(0L, cfg.get(ADDRESS_UNALIGNED, tdc("hwndParent")).address());
            assertEquals(0, cfg.get(JAVA_INT_UNALIGNED, tdc("dwFlags")) & TDF_POSITION_RELATIVE_TO_WINDOW);
        }
    }

    private static volatile long createdOwner;
    private static volatile Rectangle createdRect;

    /** Records the dialog's owner and position, then clicks its first button. */
    static int recordAndClose(MemorySegment hwnd, int msg, long wParam, long lParam, long refData) {
        if (msg == TDN_CREATED) {
            try {
                createdOwner = TestUser32.owner(hwnd.address());
                createdRect = TestUser32.rect(hwnd.address());
                long unused = (long) SEND_MESSAGE.invokeExact(hwnd, TDM_CLICK_BUTTON, 1000L, 0L);
            } catch (Throwable t) {
                // an exception leaving an upcall ends the JVM
            }
        }
        return 0;
    }

    /** Shows an owned task dialog with {@code runner} and checks it opens centred over its owner. */
    private void assertOwnedAndCentred(java.util.function.Consumer<Runnable> runner) throws Throwable {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        JFrame frame = OwnerWindowTest.showFrame("td-owner-" + UUID.randomUUID(), new Rectangle(150, 120, 900, 650));
        try {
            long owner = OwnerWindow.handleOf(frame);
            assertNotEquals(0L, owner);
            createdOwner = 0L;
            createdRect = null;

            MethodHandle target = MethodHandles.lookup().findStatic(TaskDialogTest.class, "recordAndClose",
                MethodType.methodType(int.class, MemorySegment.class, int.class, long.class, long.class, long.class));
            try (Arena a = Arena.ofShared()) {
                MemorySegment callback = Linker.nativeLinker().upcallStub(target,
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_LONG, JAVA_LONG, JAVA_LONG), a);
                int[] idx = { -2 };
                assertTimeoutPreemptively(Duration.ofSeconds(10), () -> runner.accept(() ->
                    idx[0] = TaskDialog.show("Owned", "Header", "Closes itself", List.of("Ok", "Cancel"),
                        MemorySegment.ofAddress(owner), callback)));
                assertEquals(0, idx[0]);
            }

            assertEquals(owner, createdOwner, "dialog not owned by the frame");
            Rectangle o = TestUser32.rect(owner);
            Rectangle d = createdRect;
            assertNotNull(d);
            // Windows centres it horizontally and places it a little above the vertical centre
            assertTrue(Math.abs(o.getCenterX() - d.getCenterX()) <= 10
                    && Math.abs(o.getCenterY() - d.getCenterY()) <= o.height / 10.0,
                "dialog " + d + " not centred over owner " + o);
        } finally {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
    }

    /** The asynchronous case: the dialog runs on a thread other than the owner's (AWT toolkit) thread. */
    @Test
    void ownedDialogOnAnotherThreadIsCentredOverItsOwner() throws Throwable {
        assertOwnedAndCentred(Runnable::run);
    }

    /** The blocking case on the event dispatch thread: the provider shows the dialog there. */
    @Test
    void ownedDialogOnTheEventDispatchThreadIsCentredOverItsOwner() throws Throwable {
        assertOwnedAndCentred(r -> {
            try {
                SwingUtilities.invokeAndWait(r);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
