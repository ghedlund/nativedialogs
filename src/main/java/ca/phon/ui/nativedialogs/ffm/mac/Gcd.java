package ca.phon.ui.nativedialogs.ffm.mac;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.foreign.ValueLayout.*;

/**
 * Runs work on the AppKit main thread via {@code dispatch_*_f} (the
 * function-pointer GCD variants — no Objective-C block ABI needed).
 *
 * <p>{@code dispatch_get_main_queue()} is a macro expanding to the address of
 * the global {@code _dispatch_main_q}; we look that symbol up directly. The
 * work function is a {@code void(void*)} upcall stub. Synchronous work binds the
 * {@link Runnable} into a stub of its own and ignores the context pointer;
 * asynchronous work shares one stub and identifies the {@link Runnable} by the
 * context pointer.
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

    /** Work submitted with {@link #onMainAsync}, keyed by the context value given to GCD. */
    private static final Map<Long, Runnable> PENDING = new ConcurrentHashMap<>();
    private static final AtomicLong NEXT_KEY = new AtomicLong(1);

    /**
     * The one stub that runs all asynchronous work; never freed. A stub per call would
     * have to be freed by the call itself, from inside the stub, and the JVM crashes
     * if a garbage collection walks that thread's stack before the stub returns.
     */
    private static final MemorySegment RUN_PENDING_STUB;

    static {
        try {
            INVOKE_RUNNABLE = MethodHandles.lookup().findStatic(
                Gcd.class, "invokeRunnable",
                MethodType.methodType(void.class, Runnable.class, MemorySegment.class));
            RUN_PENDING_STUB = LINKER.upcallStub(
                MethodHandles.lookup().findStatic(
                    Gcd.class, "runPending",
                    MethodType.methodType(void.class, MemorySegment.class)),
                WORK_DESC, GLOBAL);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Gcd() {}

    @SuppressWarnings("unused")
    private static void invokeRunnable(Runnable r, MemorySegment ignoredContext) {
        r.run();
    }

    @SuppressWarnings("unused")
    private static void runPending(MemorySegment context) {
        Runnable work = PENDING.remove(context.address());
        if (work != null) work.run();
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
        long key = NEXT_KEY.getAndIncrement();
        PENDING.put(key, work);
        try {
            DISPATCH_ASYNC_F.invokeExact(MAIN_QUEUE, MemorySegment.ofAddress(key), RUN_PENDING_STUB);
        } catch (Throwable t) {
            PENDING.remove(key);
            throw new RuntimeException(t);
        }
    }

    private static boolean isMainThread() {
        return ObjC.sendBoolRet(ObjC.cls("NSThread"), "isMainThread");
    }
}
