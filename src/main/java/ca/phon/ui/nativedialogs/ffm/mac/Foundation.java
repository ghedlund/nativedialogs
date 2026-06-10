package ca.phon.ui.nativedialogs.ffm.mac;

import java.lang.foreign.*;
import java.util.List;

import static java.lang.foreign.ValueLayout.*;

/** NSString / NSURL / NSArray helpers built on {@link ObjC}. */
public final class Foundation {

    private Foundation() {}

    /** Java String -> NSString* (autoreleased). */
    public static MemorySegment nsString(Arena arena, String s) {
        MemorySegment utf8 = arena.allocateFrom(s);
        return ObjC.send(ObjC.cls("NSString"), "stringWithUTF8String:", utf8);
    }

    /** NSString* -> Java String. Reads the UTF8String C pointer. */
    public static String toJavaString(MemorySegment nsString) {
        if (nsString.address() == 0L) return null;
        MemorySegment cstr = ObjC.send(nsString, "UTF8String");
        if (cstr.address() == 0L) return null;
        return cstr.reinterpret(Long.MAX_VALUE).getString(0);
    }

    /** file path -> NSURL* (fileURLWithPath:). */
    public static MemorySegment fileUrl(Arena arena, String path) {
        return ObjC.send(ObjC.cls("NSURL"), "fileURLWithPath:", nsString(arena, path));
    }

    /** NSURL* -> file path Java String ([url path]). */
    public static String urlPath(MemorySegment url) {
        return toJavaString(ObjC.send(url, "path"));
    }

    /** List<String> -> NSArray<NSString>* via arrayWithObjects:count:. */
    public static MemorySegment nsStringArray(Arena arena, List<String> items) {
        MemorySegment buf = arena.allocate(ADDRESS.byteSize() * items.size());
        for (int i = 0; i < items.size(); i++) {
            buf.setAtIndex(ADDRESS, i, nsString(arena, items.get(i)));
        }
        return ObjC.sendObjLong(ObjC.cls("NSArray"), "arrayWithObjects:count:", buf, items.size());
    }
}
