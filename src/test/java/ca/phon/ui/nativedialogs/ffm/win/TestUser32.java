package ca.phon.ui.nativedialogs.ffm.win;

import java.awt.Rectangle;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.*;

/** user32 calls the Windows tests use to inspect and close real dialogs. */
public final class TestUser32 {

    public static final int GW_OWNER = 4;
    public static final int WM_CLOSE = 0x0010;

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup USER32 = SymbolLookup.libraryLookup("user32.dll", Arena.global());

    private static MethodHandle h(String name, FunctionDescriptor d) {
        return LINKER.downcallHandle(USER32.find(name).orElseThrow(), d);
    }

    private static final MethodHandle GET_WINDOW = h("GetWindow", FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT));
    private static final MethodHandle GET_WINDOW_RECT = h("GetWindowRect", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
    private static final MethodHandle FIND_WINDOW = h("FindWindowW", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));
    private static final MethodHandle POST_MESSAGE = h("PostMessageW",
        FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_LONG, JAVA_LONG));
    private static final MethodHandle INTERNAL_GET_WINDOW_TEXT = h("InternalGetWindowText",
        FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT));

    private TestUser32() {}

    public static long owner(long hwnd) {
        try {
            return ((MemorySegment) GET_WINDOW.invokeExact(MemorySegment.ofAddress(hwnd), GW_OWNER)).address();
        } catch (Throwable t) { throw new RuntimeException(t); }
    }

    /** Window rectangle in device pixels as x, y, width, height. */
    public static Rectangle rect(long hwnd) {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment r = a.allocate(JAVA_INT, 4);
            int ok = (int) GET_WINDOW_RECT.invokeExact(MemorySegment.ofAddress(hwnd), r);
            if (ok == 0) throw new IllegalStateException("GetWindowRect failed");
            int l = r.getAtIndex(JAVA_INT, 0), t = r.getAtIndex(JAVA_INT, 1);
            return new Rectangle(l, t, r.getAtIndex(JAVA_INT, 2) - l, r.getAtIndex(JAVA_INT, 3) - t);
        } catch (RuntimeException e) { throw e; }
        catch (Throwable t) { throw new RuntimeException(t); }
    }

    /** Top-level window with this exact title, or 0. */
    public static long findByTitle(String title) {
        try (Arena a = Arena.ofConfined()) {
            return ((MemorySegment) FIND_WINDOW.invokeExact(MemorySegment.NULL, WinStr.wide(a, title))).address();
        } catch (Throwable t) { throw new RuntimeException(t); }
    }

    public static String text(long hwnd) {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment buf = a.allocate(JAVA_CHAR, 512);
            int len = (int) INTERNAL_GET_WINDOW_TEXT.invokeExact(MemorySegment.ofAddress(hwnd), buf, 512);
            return new String(buf.toArray(JAVA_CHAR), 0, Math.max(len, 0));
        } catch (Throwable t) { throw new RuntimeException(t); }
    }

    public static void postClose(long hwnd) {
        try {
            int unused = (int) POST_MESSAGE.invokeExact(MemorySegment.ofAddress(hwnd), WM_CLOSE, 0L, 0L);
        } catch (Throwable t) { throw new RuntimeException(t); }
    }
}
