package ca.phon.ui.nativedialogs.ffm.mac;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.*;

/**
 * Minimal Objective-C runtime bridge over libobjc using FFM.
 *
 * Loads Foundation and AppKit so that objc_getClass resolves the Cocoa classes
 * we need. Each objc_msgSend variant is bound with a fixed FunctionDescriptor;
 * this is valid on arm64/x86_64 macOS for the integer/pointer/BOOL signatures
 * used here.
 */
public final class ObjC {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final Arena GLOBAL = Arena.ofShared();

    static {
        SymbolLookup.libraryLookup(
            "/System/Library/Frameworks/Foundation.framework/Foundation", GLOBAL);
        SymbolLookup.libraryLookup(
            "/System/Library/Frameworks/AppKit.framework/AppKit", GLOBAL);
    }

    private static final SymbolLookup OBJC =
        SymbolLookup.libraryLookup("/usr/lib/libobjc.A.dylib", GLOBAL);

    private static MethodHandle h(String name, FunctionDescriptor desc) {
        return LINKER.downcallHandle(OBJC.find(name).orElseThrow(
            () -> new IllegalStateException("symbol not found: " + name)), desc);
    }

    private static final MethodHandle OBJC_GET_CLASS =
        h("objc_getClass", FunctionDescriptor.of(ADDRESS, ADDRESS));
    private static final MethodHandle SEL_REGISTER_NAME =
        h("sel_registerName", FunctionDescriptor.of(ADDRESS, ADDRESS));

    private static final MethodHandle MSG_id =
        h("objc_msgSend", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));
    private static final MethodHandle MSG_id_id =
        h("objc_msgSend", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS));
    private static final MethodHandle MSG_void =
        h("objc_msgSend", FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
    private static final MethodHandle MSG_void_id =
        h("objc_msgSend", FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, ADDRESS));
    private static final MethodHandle MSG_void_bool =
        h("objc_msgSend", FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, JAVA_BOOLEAN));
    private static final MethodHandle MSG_long =
        h("objc_msgSend", FunctionDescriptor.of(JAVA_LONG, ADDRESS, ADDRESS));
    private static final MethodHandle MSG_id_id_long =
        h("objc_msgSend", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_LONG));
    private static final MethodHandle MSG_bool =
        h("objc_msgSend", FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, ADDRESS));
    private static final MethodHandle MSG_id_long =
        h("objc_msgSend", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_LONG));
    private static final MethodHandle MSG_void_long =
        h("objc_msgSend", FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, JAVA_LONG));

    private ObjC() {}

    public static MemorySegment cls(String name) {
        try (Arena a = Arena.ofConfined()) {
            return (MemorySegment) OBJC_GET_CLASS.invokeExact(a.allocateFrom(name));
        } catch (Throwable t) { throw new RuntimeException(t); }
    }

    public static MemorySegment sel(String name) {
        try (Arena a = Arena.ofConfined()) {
            return (MemorySegment) SEL_REGISTER_NAME.invokeExact(a.allocateFrom(name));
        } catch (Throwable t) { throw new RuntimeException(t); }
    }

    public static MemorySegment send(MemorySegment recv, String sel) {
        try { return (MemorySegment) MSG_id.invokeExact(recv, sel(sel)); }
        catch (Throwable t) { throw new RuntimeException(t); }
    }

    public static MemorySegment send(MemorySegment recv, String sel, MemorySegment arg) {
        try { return (MemorySegment) MSG_id_id.invokeExact(recv, sel(sel), arg); }
        catch (Throwable t) { throw new RuntimeException(t); }
    }

    public static void sendVoid(MemorySegment recv, String sel) {
        try { MSG_void.invokeExact(recv, sel(sel)); }
        catch (Throwable t) { throw new RuntimeException(t); }
    }

    public static void sendVoid(MemorySegment recv, String sel, MemorySegment arg) {
        try { MSG_void_id.invokeExact(recv, sel(sel), arg); }
        catch (Throwable t) { throw new RuntimeException(t); }
    }

    public static void sendBool(MemorySegment recv, String sel, boolean value) {
        try { MSG_void_bool.invokeExact(recv, sel(sel), value); }
        catch (Throwable t) { throw new RuntimeException(t); }
    }

    public static long sendLong(MemorySegment recv, String sel) {
        try { return (long) MSG_long.invokeExact(recv, sel(sel)); }
        catch (Throwable t) { throw new RuntimeException(t); }
    }

    public static MemorySegment sendObjLong(MemorySegment recv, String sel, MemorySegment arg, long n) {
        try { return (MemorySegment) MSG_id_id_long.invokeExact(recv, sel(sel), arg, n); }
        catch (Throwable t) { throw new RuntimeException(t); }
    }

    public static boolean sendBoolRet(MemorySegment recv, String sel) {
        try { return (boolean) MSG_bool.invokeExact(recv, sel(sel)); }
        catch (Throwable t) { throw new RuntimeException(t); }
    }

    public static MemorySegment sendIdLong(MemorySegment recv, String sel, long n) {
        try { return (MemorySegment) MSG_id_long.invokeExact(recv, sel(sel), n); }
        catch (Throwable t) { throw new RuntimeException(t); }
    }

    public static void sendLongArg(MemorySegment recv, String sel, long value) {
        try { MSG_void_long.invokeExact(recv, sel(sel), value); }
        catch (Throwable t) { throw new RuntimeException(t); }
    }
}
