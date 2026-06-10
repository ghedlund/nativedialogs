package ca.phon.ui.nativedialogs.ffm.win;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.*;

/** Win32 constants, GUIDs, library lookups, and shared downcall handles. */
public final class Win32 {

    public static final int S_OK = 0;
    public static final int CLSCTX_INPROC_SERVER = 0x1;
    public static final int COINIT_APARTMENTTHREADED = 0x2;
    public static final int SIGDN_FILESYSPATH = 0x80058000;

    public static final int FOS_OVERWRITEPROMPT  = 0x00000002;
    public static final int FOS_PICKFOLDERS      = 0x00000020;
    public static final int FOS_FORCEFILESYSTEM  = 0x00000040;
    public static final int FOS_ALLOWMULTISELECT = 0x00000200;
    public static final int FOS_PATHMUSTEXIST    = 0x00000800;
    public static final int FOS_FILEMUSTEXIST    = 0x00001000;
    public static final int FOS_FORCESHOWHIDDEN  = 0x10000000;

    private static final Linker LINKER = Linker.nativeLinker();
    private static final Arena GLOBAL = Arena.ofShared();

    private static final SymbolLookup OLE32   = SymbolLookup.libraryLookup("ole32.dll", GLOBAL);
    private static final SymbolLookup SHELL32 = SymbolLookup.libraryLookup("shell32.dll", GLOBAL);
    static final SymbolLookup COMDLG32 = SymbolLookup.libraryLookup("comdlg32.dll", GLOBAL);
    static final SymbolLookup COMCTL32 = SymbolLookup.libraryLookup("comctl32.dll", GLOBAL);

    public static final MethodHandle CoInitializeEx = LINKER.downcallHandle(
        OLE32.find("CoInitializeEx").orElseThrow(),
        FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
    public static final MethodHandle CoUninitialize = LINKER.downcallHandle(
        OLE32.find("CoUninitialize").orElseThrow(), FunctionDescriptor.ofVoid());
    public static final MethodHandle CoCreateInstance = LINKER.downcallHandle(
        OLE32.find("CoCreateInstance").orElseThrow(),
        FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS));
    public static final MethodHandle CoTaskMemFree = LINKER.downcallHandle(
        OLE32.find("CoTaskMemFree").orElseThrow(), FunctionDescriptor.ofVoid(ADDRESS));
    public static final MethodHandle SHCreateItemFromParsingName = LINKER.downcallHandle(
        SHELL32.find("SHCreateItemFromParsingName").orElseThrow(),
        FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS));

    private Win32() {}

    /** Build a 16-byte GUID from its canonical fields (little-endian platform). */
    public static MemorySegment guid(Arena a, int d1, short d2, short d3, long d4Hi8Bytes) {
        MemorySegment g = a.allocate(16);
        g.set(JAVA_INT, 0, d1);
        g.set(JAVA_SHORT, 4, d2);
        g.set(JAVA_SHORT, 6, d3);
        for (int i = 0; i < 8; i++) {
            g.set(JAVA_BYTE, 8 + i, (byte) ((d4Hi8Bytes >>> (56 - 8 * i)) & 0xFF));
        }
        return g;
    }

    public static MemorySegment CLSID_FileOpenDialog(Arena a) {
        return guid(a, 0xDC1C5A9C, (short) 0xE88A, (short) 0x4DDE, 0xA5A160F82A20AEF7L);
    }
    public static MemorySegment CLSID_FileSaveDialog(Arena a) {
        return guid(a, 0xC0B4E2F3, (short) 0xBA21, (short) 0x4773, 0x8DBA335EC946EB8BL);
    }
    public static MemorySegment IID_IFileOpenDialog(Arena a) {
        return guid(a, 0xD57C7288, (short) 0xD4AD, (short) 0x4768, 0xBE029D969532D960L);
    }
    public static MemorySegment IID_IFileSaveDialog(Arena a) {
        return guid(a, 0x84BCCD23, (short) 0x5FDE, (short) 0x4CDB, 0xAEA4AF64B83D78ABL);
    }
    public static MemorySegment IID_IShellItem(Arena a) {
        return guid(a, 0x43826D1E, (short) 0xE718, (short) 0x42EE, 0xBC55A1E261C37BFEL);
    }
}
