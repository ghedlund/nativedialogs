package ca.phon.ui.nativedialogs.ffm.win;

import java.lang.foreign.*;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.*;

/**
 * comdlg32 {@code ChooseColorW}. CHOOSECOLORW is 72 bytes on x64; layout declared
 * with {@link MemoryLayout} so offsets/size are computed by FFM.
 */
public final class ChooseColor {

    private static final int CC_RGBINIT = 0x00000001;
    private static final int CC_FULLOPEN = 0x00000002;

    private static final MemoryLayout CC = MemoryLayout.structLayout(
        JAVA_INT.withName("lStructSize"),
        MemoryLayout.paddingLayout(4),
        ADDRESS.withName("hwndOwner"),
        ADDRESS.withName("hInstance"),
        JAVA_INT.withName("rgbResult"),
        MemoryLayout.paddingLayout(4),
        ADDRESS.withName("lpCustColors"),
        JAVA_INT.withName("Flags"),
        MemoryLayout.paddingLayout(4),
        ADDRESS.withName("lCustData"),
        ADDRESS.withName("lpfnHook"),
        ADDRESS.withName("lpTemplateName"));

    private static long off(String name) {
        return CC.byteOffset(PathElement.groupElement(name));
    }

    private static final MethodHandle CHOOSE_COLOR = Linker.nativeLinker().downcallHandle(
        Win32.COMDLG32.find("ChooseColorW").orElseThrow(),
        FunctionDescriptor.of(JAVA_INT, ADDRESS));

    private ChooseColor() {}

    /** @return 0xRRGGBB chosen colour, or -1 if cancelled. */
    public static int show(int initialRgb) {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment custom = a.allocate(JAVA_INT.byteSize() * 16); // 16 custom slots; must be non-null
            MemorySegment cc = a.allocate(CC);
            cc.set(JAVA_INT, off("lStructSize"), (int) CC.byteSize());
            cc.set(JAVA_INT, off("rgbResult"), toColorRef(initialRgb)); // COLORREF is 0x00BBGGRR
            cc.set(ADDRESS, off("lpCustColors"), custom);
            cc.set(JAVA_INT, off("Flags"), CC_RGBINIT | CC_FULLOPEN);

            int ok = (int) CHOOSE_COLOR.invokeExact(cc);
            if (ok == 0) return -1; // cancelled
            return fromColorRef(cc.get(JAVA_INT, off("rgbResult")));
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static int toColorRef(int rgb) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        return (b << 16) | (g << 8) | r;
    }
    private static int fromColorRef(int cr) {
        int r = cr & 0xFF, g = (cr >> 8) & 0xFF, b = (cr >> 16) & 0xFF;
        return (r << 16) | (g << 8) | b;
    }
}
