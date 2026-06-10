package ca.phon.ui.nativedialogs.ffm.win;

import java.awt.Font;
import java.lang.foreign.*;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.*;

/**
 * comdlg32 {@code ChooseFontW}. CHOOSEFONTW is declared with {@link MemoryLayout}
 * (x64 size 104). LOGFONTW (92 bytes) is accessed by explicit field offsets.
 */
public final class ChooseFont {

    // LOGFONTW field offsets (x64; size 92)
    private static final long LOGFONT_SIZE = 92;
    private static final long LF_HEIGHT = 0;     // LONG
    private static final long LF_WEIGHT = 16;    // LONG
    private static final long LF_ITALIC = 20;    // BYTE
    private static final long LF_FACENAME = 28;  // WCHAR[32]

    // CHOOSEFONTW layout (x64; size 104). Note the hDC field at offset 16 that the
    // initial draft omitted — lpLogFont is at 24, iPointSize 32, Flags 36.
    private static final MemoryLayout CF = MemoryLayout.structLayout(
        JAVA_INT.withName("lStructSize"),   // 0
        MemoryLayout.paddingLayout(4),
        ADDRESS.withName("hwndOwner"),       // 8
        ADDRESS.withName("hDC"),             // 16
        ADDRESS.withName("lpLogFont"),       // 24
        JAVA_INT.withName("iPointSize"),     // 32
        JAVA_INT.withName("Flags"),          // 36
        JAVA_INT.withName("rgbColors"),      // 40
        MemoryLayout.paddingLayout(4),
        ADDRESS.withName("lCustData"),       // 48
        ADDRESS.withName("lpfnHook"),        // 56
        ADDRESS.withName("lpTemplateName"),  // 64
        ADDRESS.withName("hInstance"),       // 72
        ADDRESS.withName("lpszStyle"),       // 80
        JAVA_SHORT.withName("nFontType"),    // 88
        MemoryLayout.paddingLayout(2),
        JAVA_INT.withName("nSizeMin"),       // 92
        JAVA_INT.withName("nSizeMax"),       // 96
        MemoryLayout.paddingLayout(4));      // tail pad -> 104

    private static long off(String name) {
        return CF.byteOffset(PathElement.groupElement(name));
    }

    private static final int CF_SCREENFONTS = 0x00000001;
    private static final int CF_INITTOLOGFONTSTRUCT = 0x00000040;
    private static final int FW_NORMAL = 400, FW_BOLD = 700;

    private static final MethodHandle CHOOSE_FONT = Linker.nativeLinker().downcallHandle(
        Win32.COMDLG32.find("ChooseFontW").orElseThrow(),
        FunctionDescriptor.of(JAVA_INT, ADDRESS));

    private ChooseFont() {}

    /** @return the chosen Font, or null if cancelled. */
    public static Font show(Font initial) {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment lf = a.allocate(LOGFONT_SIZE);
            int pt = (initial != null) ? initial.getSize() : 12;
            // lfHeight: negative = character height; -round(pt * 96/72) at 96 dpi.
            lf.set(JAVA_INT, LF_HEIGHT, -(int) Math.round(pt * 96.0 / 72.0));
            lf.set(JAVA_INT, LF_WEIGHT, (initial != null && initial.isBold()) ? FW_BOLD : FW_NORMAL);
            lf.set(JAVA_BYTE, LF_ITALIC, (byte) ((initial != null && initial.isItalic()) ? 1 : 0));
            String face = (initial != null) ? initial.getFamily() : "Segoe UI";
            putFaceName(lf, face);

            MemorySegment cf = a.allocate(CF);
            cf.set(JAVA_INT, off("lStructSize"), (int) CF.byteSize());
            cf.set(ADDRESS, off("lpLogFont"), lf);
            cf.set(JAVA_INT, off("Flags"), CF_SCREENFONTS | CF_INITTOLOGFONTSTRUCT);

            int ok = (int) CHOOSE_FONT.invokeExact(cf);
            if (ok == 0) return null;

            int pointSizeTenths = cf.get(JAVA_INT, off("iPointSize")); // 1/10 pt
            int size;
            if (pointSizeTenths > 0) {
                size = Math.max(1, Math.round(pointSizeTenths / 10f));
            } else {
                int h = lf.get(JAVA_INT, LF_HEIGHT);
                size = Math.max(1, Math.round(Math.abs(h) * 72f / 96f));
            }
            int weight = lf.get(JAVA_INT, LF_WEIGHT);
            boolean bold = weight >= FW_BOLD;
            boolean italic = lf.get(JAVA_BYTE, LF_ITALIC) != 0;
            String chosenFace = readFaceName(lf);
            int style = (bold ? Font.BOLD : 0) | (italic ? Font.ITALIC : 0);
            return new Font(chosenFace, style, size);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static void putFaceName(MemorySegment lf, String face) {
        char[] chars = face.toCharArray();
        int n = Math.min(chars.length, 31);
        for (int i = 0; i < n; i++) lf.set(JAVA_CHAR, LF_FACENAME + i * 2L, chars[i]);
        lf.set(JAVA_CHAR, LF_FACENAME + n * 2L, '\0');
    }

    private static String readFaceName(MemorySegment lf) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            char c = lf.get(JAVA_CHAR, LF_FACENAME + i * 2L);
            if (c == 0) break;
            sb.append(c);
        }
        return sb.toString();
    }
}
