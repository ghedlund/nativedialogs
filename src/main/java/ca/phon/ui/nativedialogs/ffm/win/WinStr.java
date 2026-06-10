package ca.phon.ui.nativedialogs.ffm.win;

import java.lang.foreign.*;
import java.nio.charset.StandardCharsets;

import static java.lang.foreign.ValueLayout.*;

/** UTF-16LE (wide) string helpers for the Win32 W APIs. */
public final class WinStr {

    private WinStr() {}

    /** Java String -> null-terminated UTF-16LE buffer. */
    public static MemorySegment wide(Arena arena, String s) {
        byte[] utf16 = s.getBytes(StandardCharsets.UTF_16LE);
        MemorySegment seg = arena.allocate(utf16.length + 2L);
        MemorySegment.copy(utf16, 0, seg, JAVA_BYTE, 0, utf16.length);
        seg.set(JAVA_BYTE, utf16.length, (byte) 0);
        seg.set(JAVA_BYTE, utf16.length + 1, (byte) 0);
        return seg;
    }

    /** Pointer to a null-terminated UTF-16LE string -> Java String. */
    public static String fromWide(MemorySegment ptr) {
        if (ptr.address() == 0L) return null;
        MemorySegment p = ptr.reinterpret(Long.MAX_VALUE);
        StringBuilder sb = new StringBuilder();
        for (long i = 0; ; i += 2) {
            char c = p.get(JAVA_CHAR, i);
            if (c == 0) break;
            sb.append(c);
        }
        return sb.toString();
    }
}
