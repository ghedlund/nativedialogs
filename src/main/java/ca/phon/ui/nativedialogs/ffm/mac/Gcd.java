package ca.phon.ui.nativedialogs.ffm.mac;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static java.lang.foreign.ValueLayout.*;

/**
 * Runs work on the AppKit main thread via {@code dispatch_*_f} (the
 * function-pointer GCD variants — no Objective-C block ABI needed).
 *
 * <p>{@code dispatch_get_main_queue()} is a macro expanding to the address of
 * the global {@code _dispatch_main_q}; we look that symbol up directly. The
 * work function is a {@code void(void*)} upcall stub with the {@link Runnable}
 * bound in, so the context pointer is unused.
 *
 * <p>NOTE: dispatching to the main queue only completes when the main thread is
 * draining it (i.e. a running Cocoa main run loop, as in an AWT/Swing app). In a
 * context with no running main run loop, {@link #onMainSync} would block forever.
 */
public final class Gcd {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final Arena GLOBAL = Arena.ofShared();
    private static final SymbolLookup LIBSYSTEM =
        SymbolLookup.libraryLookup("/usr/lib/libSystem.B.dylib", GLOBAL);

    private static final MemorySegment MAIN_QUEUE =
        LIBSYSTEM.find("_dispatch_main_q").orElseThrow(
            () -> new IllegalStateException("_dispatch_main_q not found"));

    private static final MethodHandle DISPATCH_SYNC_F = LINKER.downcallHandle(
        LIBSYSTEM.find("dispatch_sync_f").orElseThrow(),
        FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, ADDRESS));
    private static final MethodHandle DISPATCH_ASYNC_F = LINKER.downcallHandle(
        LIBSYSTEM.find("dispatch_async_f").orElseThrow(),
        FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, ADDRESS));

    private static final FunctionDescriptor WORK_DESC = FunctionDescriptor.ofVoid(ADDRESS);
    private static final MethodHandle INVOKE_RUNNABLE;
    static {
        try {
            INVOKE_RUNNABLE = MethodHandles.lookup().findStatic(
                Gcd.class, "invokeRunnable",
                MethodType.methodType(void.class, Runnable.class, MemorySegment.class));
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Gcd() {}

    @SuppressWarnings("unused")
    private static void invokeRunnable(Runnable r, MemorySegment ignoredContext) {
        r.run();
    }

    private static MemorySegment stubFor(Runnable work, Arena arena) {
        MethodHandle bound = INVOKE_RUNNABLE.bindTo(work);
        return LINKER.upcallStub(bound, WORK_DESC, arena);
    }

    /** Run on the main thread and block the caller until it finishes. */
    public static void onMainSync(Runnable work) {
        if (isMainThread()) { work.run(); return; }
        try (Arena a = Arena.ofConfined()) {
            MemorySegment stub = stubFor(work, a);
            DISPATCH_SYNC_F.invokeExact(MAIN_QUEUE, MemorySegment.NULL, stub);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /** Run on the main thread without blocking the caller. */
    public static void onMainAsync(Runnable work) {
        if (isMainThread()) { work.run(); return; }
        Arena a = Arena.ofShared();
        Runnable wrapped = () -> {
            try { work.run(); } finally { a.close(); }
        };
        try {
            MemorySegment stub = stubFor(wrapped, a);
            DISPATCH_ASYNC_F.invokeExact(MAIN_QUEUE, MemorySegment.NULL, stub);
        } catch (Throwable t) {
            a.close();
            throw new RuntimeException(t);
        }
    }

    private static boolean isMainThread() {
        return ObjC.sendBoolRet(ObjC.cls("NSThread"), "isMainThread");
    }
}
