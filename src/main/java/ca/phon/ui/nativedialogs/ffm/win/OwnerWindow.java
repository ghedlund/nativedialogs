package ca.phon.ui.nativedialogs.ffm.win;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Window;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.foreign.ValueLayout.*;

/**
 * Finds the Win32 window handle ({@code HWND}) of a {@link java.awt.Window}, so a
 * native dialog can be owned by it: centred over it and modal to it.
 *
 * <p>AWT does not expose the handle. JAWT does, but its entry points take a
 * {@code JNIEnv*} and a JNI reference to the window, which FFM cannot produce, and
 * {@code WComponentPeer.getHWnd} needs {@code --add-opens}. Instead this class lists
 * the top-level AWT windows of this process ({@code FindWindowExW} by AWT window class,
 * {@code GetWindowThreadProcessId}) and picks the one whose title matches the Java
 * window. If several match, it takes the foreground window when the Java window is
 * active, otherwise the one whose bounds are closest.
 *
 * <p>Every method returns {@code 0} (no owner) instead of throwing. No window messages
 * are sent: the title is read with {@code InternalGetWindowText}, so this cannot wait on
 * the AWT toolkit thread.
 */
public final class OwnerWindow {

    private static final Logger LOGGER = Logger.getLogger(OwnerWindow.class.getName());

    /** Window classes AWT registers for top-level windows (awt_Frame/awt_Dialog/awt_Window.cpp). */
    private static final List<String> AWT_CLASSES = List.of("SunAwtFrame", "SunAwtDialog", "SunAwtWindow");

    private static final int MAX_TITLE = 512;

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup USER32 = SymbolLookup.libraryLookup("user32.dll", Arena.global());

    private static MethodHandle user32(String name, FunctionDescriptor desc) {
        return LINKER.downcallHandle(USER32.find(name).orElseThrow(), desc);
    }

    private static final MethodHandle FIND_WINDOW_EX = user32("FindWindowExW",
        FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));
    private static final MethodHandle GET_WINDOW_THREAD_PROCESS_ID = user32("GetWindowThreadProcessId",
        FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
    private static final MethodHandle IS_WINDOW = user32("IsWindow",
        FunctionDescriptor.of(JAVA_INT, ADDRESS));
    private static final MethodHandle IS_WINDOW_VISIBLE = user32("IsWindowVisible",
        FunctionDescriptor.of(JAVA_INT, ADDRESS));
    private static final MethodHandle INTERNAL_GET_WINDOW_TEXT = user32("InternalGetWindowText",
        FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT));
    private static final MethodHandle GET_WINDOW_RECT = user32("GetWindowRect",
        FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
    private static final MethodHandle GET_FOREGROUND_WINDOW = user32("GetForegroundWindow",
        FunctionDescriptor.of(ADDRESS));

    private OwnerWindow() {}

    /**
     * The window a dialog should belong to: {@code parent} if it is showing, otherwise
     * the active window, otherwise {@code null}.
     */
    public static Window resolve(Window parent) {
        try {
            if (parent != null && parent.isShowing()) return parent;
            Window active = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
            return (active != null && active.isShowing()) ? active : null;
        } catch (RuntimeException e) {
            LOGGER.log(Level.FINE, "Could not resolve the dialog owner", e);
            return null;
        }
    }

    /** {@link #handleOf(Window)} of {@link #resolve(Window)}. */
    public static long ownerFor(Window parent) {
        return handleOf(resolve(parent));
    }

    /** The HWND of a showing window, or {@code 0} if it cannot be found. */
    public static long handleOf(Window window) {
        if (window == null) return 0L;
        try {
            if (!window.isShowing()) return 0L;
            String title = titleOf(window);
            Rectangle expected = deviceBounds(window);

            List<MemorySegment> matches = new ArrayList<>();
            try (Arena a = Arena.ofConfined()) {
                long pid = ProcessHandle.current().pid();
                MemorySegment pidBuf = a.allocate(JAVA_INT);
                MemorySegment textBuf = a.allocate(JAVA_CHAR, MAX_TITLE);
                for (String cls : AWT_CLASSES) {
                    MemorySegment clsName = WinStr.wide(a, cls);
                    MemorySegment hwnd = MemorySegment.NULL;
                    while (true) {
                        hwnd = (MemorySegment) FIND_WINDOW_EX.invokeExact(
                            MemorySegment.NULL, hwnd, clsName, MemorySegment.NULL);
                        if (hwnd.address() == 0L) break;
                        int unusedTid = (int) GET_WINDOW_THREAD_PROCESS_ID.invokeExact(hwnd, pidBuf);
                        if (Integer.toUnsignedLong(pidBuf.get(JAVA_INT, 0)) != pid) continue;
                        if ((int) IS_WINDOW_VISIBLE.invokeExact(hwnd) == 0) continue;
                        int len = (int) INTERNAL_GET_WINDOW_TEXT.invokeExact(hwnd, textBuf, MAX_TITLE);
                        String text = len > 0 ? new String(textBuf.toArray(JAVA_CHAR), 0, len) : "";
                        if (text.equals(title)) matches.add(hwnd);
                    }
                }
                if (matches.isEmpty()) return 0L;
                if (matches.size() == 1) return matches.get(0).address();

                if (window.isActive()) {
                    long fg = ((MemorySegment) GET_FOREGROUND_WINDOW.invokeExact()).address();
                    for (MemorySegment m : matches) {
                        if (m.address() == fg) return fg;
                    }
                }

                MemorySegment rect = a.allocate(JAVA_INT, 4);
                long best = 0L;
                long bestScore = Long.MAX_VALUE;
                for (MemorySegment m : matches) {
                    if ((int) GET_WINDOW_RECT.invokeExact(m, rect) == 0) continue;
                    int l = rect.getAtIndex(JAVA_INT, 0), t = rect.getAtIndex(JAVA_INT, 1);
                    int r = rect.getAtIndex(JAVA_INT, 2), b = rect.getAtIndex(JAVA_INT, 3);
                    long score = Math.abs(l - expected.x) + Math.abs(t - expected.y)
                        + Math.abs((r - l) - expected.width) + Math.abs((b - t) - expected.height);
                    if (score < bestScore) { bestScore = score; best = m.address(); }
                }
                return best;
            }
        } catch (Throwable t) {
            LOGGER.log(Level.FINE, "Could not find the window handle of the dialog owner", t);
            return 0L;
        }
    }

    /** {@code hwnd} as a segment if it is still a window, otherwise {@code NULL}. */
    public static MemorySegment validOrNull(long hwnd) {
        if (hwnd == 0L) return MemorySegment.NULL;
        try {
            MemorySegment h = MemorySegment.ofAddress(hwnd);
            return ((int) IS_WINDOW.invokeExact(h) != 0) ? h : MemorySegment.NULL;
        } catch (Throwable t) {
            return MemorySegment.NULL;
        }
    }

    private static String titleOf(Window w) {
        String t = null;
        if (w instanceof Frame f) t = f.getTitle();
        else if (w instanceof Dialog d) t = d.getTitle();
        return t != null ? t : "";
    }

    /**
     * Approximate bounds of the window in device pixels, as {@code GetWindowRect}
     * reports them. Used only to choose between windows with the same title.
     */
    private static Rectangle deviceBounds(Window w) {
        Rectangle b = new Rectangle(w.getLocationOnScreen(), w.getSize());
        GraphicsConfiguration gc = w.getGraphicsConfiguration();
        if (gc == null) return b;
        double sx = gc.getDefaultTransform().getScaleX();
        double sy = gc.getDefaultTransform().getScaleY();
        Rectangle screen = gc.getBounds();
        return new Rectangle(
            (int) Math.round(screen.x + (b.x - screen.x) * sx),
            (int) Math.round(screen.y + (b.y - screen.y) * sy),
            (int) Math.round(b.width * sx),
            (int) Math.round(b.height * sy));
    }
}
