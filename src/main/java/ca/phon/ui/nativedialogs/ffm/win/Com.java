package ca.phon.ui.nativedialogs.ffm.win;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.*;

/**
 * Invokes COM interface methods by vtable index.
 *
 * <p>A COM interface pointer points to an object whose first field is a pointer
 * to its vtable: an array of function pointers. Method {@code i} is at
 * {@code vtable[i]}; every method takes the interface pointer as its first
 * ("this") argument and (for the ones we use) returns an {@code HRESULT} int.
 */
public final class Com {

    private static final Linker LINKER = Linker.nativeLinker();

    private Com() {}

    private static MemorySegment methodPtr(MemorySegment iface, int index) {
        MemorySegment vtbl = iface.reinterpret(ADDRESS.byteSize()).get(ADDRESS, 0)
            .reinterpret(ADDRESS.byteSize() * (index + 1));
        return vtbl.getAtIndex(ADDRESS, index);
    }

    private static MethodHandle handle(MemorySegment fn, FunctionDescriptor desc) {
        return LINKER.downcallHandle(fn, desc);
    }

    /** Call an HRESULT-returning method whose only args (after this) are pointers. */
    public static int callHr(MemorySegment iface, int index, MemorySegment... ptrArgs) {
        MemoryLayout[] argLayouts = new MemoryLayout[ptrArgs.length + 1];
        argLayouts[0] = ADDRESS;
        for (int i = 0; i < ptrArgs.length; i++) argLayouts[i + 1] = ADDRESS;
        FunctionDescriptor desc = FunctionDescriptor.of(JAVA_INT, argLayouts);
        MethodHandle h = handle(methodPtr(iface, index), desc);
        Object[] args = new Object[ptrArgs.length + 1];
        args[0] = iface;
        System.arraycopy(ptrArgs, 0, args, 1, ptrArgs.length);
        try { return (int) h.invokeWithArguments(args); }
        catch (Throwable t) { throw new RuntimeException(t); }
    }

    /** Call an HRESULT method taking a single int after this (e.g. SetOptions). */
    public static int callHrInt(MemorySegment iface, int index, int value) {
        FunctionDescriptor desc = FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT);
        MethodHandle h = handle(methodPtr(iface, index), desc);
        try { return (int) h.invokeExact(iface, value); }
        catch (Throwable t) { throw new RuntimeException(t); }
    }

    /** Call an HRESULT method: (this, int, ptr) — e.g. SetFileTypes(count, specs), GetItemAt(i, ppsi), GetDisplayName(sigdn, ppsz). */
    public static int callHrIntPtr(MemorySegment iface, int index, int value, MemorySegment ptr) {
        FunctionDescriptor desc = FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS);
        MethodHandle h = handle(methodPtr(iface, index), desc);
        try { return (int) h.invokeExact(iface, value, ptr); }
        catch (Throwable t) { throw new RuntimeException(t); }
    }

    /** Call Release (vtable index 2), returning the new ref count. Safe on NULL. */
    public static int release(MemorySegment iface) {
        if (iface == null || iface.address() == 0L) return 0;
        FunctionDescriptor desc = FunctionDescriptor.of(JAVA_INT, ADDRESS);
        MethodHandle h = handle(methodPtr(iface, 2), desc);
        try { return (int) h.invokeExact(iface); }
        catch (Throwable t) { throw new RuntimeException(t); }
    }
}
