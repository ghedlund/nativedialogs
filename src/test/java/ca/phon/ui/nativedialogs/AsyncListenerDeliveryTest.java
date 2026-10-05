package ca.phon.ui.nativedialogs;

import org.junit.jupiter.api.Test;
import java.awt.EventQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Results of asynchronous dialogs are delivered by whichever thread ran the dialog:
 * on macOS a native callback on the AppKit thread, on Windows a COM thread. The
 * application's listener must not run there.
 */
class AsyncListenerDeliveryTest {

    private static final NativeDialogEvent OK =
        new NativeDialogEvent(NativeDialogEvent.OK_OPTION, "/media/session.wav");

    private static NativeDialogProperties asyncRequest(NativeDialogListener listener) {
        final OpenDialogProperties props = new OpenDialogProperties();
        props.setRunAsync(true);
        props.setListener(listener);
        NativeDialogs.prepareListener(props);
        return props;
    }

    @Test
    void asyncListenerRunsOnEventDispatchThread() throws Exception {
        final AtomicReference<NativeDialogEvent> received = new AtomicReference<>();
        final AtomicBoolean onEventThread = new AtomicBoolean();
        final CountDownLatch called = new CountDownLatch(1);
        final NativeDialogProperties props = asyncRequest(evt -> {
            received.set(evt);
            onEventThread.set(EventQueue.isDispatchThread());
            called.countDown();
        });

        props.getListener().nativeDialogEvent(OK);

        assertTrue(called.await(5, TimeUnit.SECONDS), "listener must be called");
        assertTrue(onEventThread.get(), "listener must be called on the event dispatch thread");
        assertSame(OK, received.get());
    }

    /**
     * An exception that leaves a native callback ends the JVM, so a listener that
     * fails must not fail the thread that delivers the result.
     */
    @Test
    void failingAsyncListenerDoesNotFailTheDeliveringThread() throws Exception {
        final IllegalStateException failure = new IllegalStateException("listener failed");
        final NativeDialogProperties props = asyncRequest(evt -> { throw failure; });
        final AtomicReference<Throwable> reported = new AtomicReference<>();
        final CountDownLatch handled = new CountDownLatch(1);
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            reported.set(error);
            handled.countDown();
        });
        try {
            assertDoesNotThrow(() -> props.getListener().nativeDialogEvent(OK),
                "the delivering thread must not see the listener's exception");
            assertTrue(handled.await(5, TimeUnit.SECONDS),
                "the failure must still be reported, on the event dispatch thread");
            assertSame(failure, reported.get());
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previous);
        }
    }

    /** The Swing dialogs report on the event dispatch thread; their listeners run at once. */
    @Test
    void asyncListenerOnEventDispatchThreadRunsBeforeDeliveryReturns() throws Exception {
        final AtomicBoolean ranBeforeReturn = new AtomicBoolean();
        final AtomicBoolean called = new AtomicBoolean();
        final NativeDialogProperties props = asyncRequest(evt -> called.set(true));

        EventQueue.invokeAndWait(() -> {
            props.getListener().nativeDialogEvent(OK);
            ranBeforeReturn.set(called.get());
        });

        assertTrue(ranBeforeReturn.get(),
            "a result delivered on the event dispatch thread must reach the listener immediately");
    }

    /** A blocking caller is waiting for the result; it must not be queued behind other events. */
    @Test
    void blockingRequestReceivesResultOnDeliveringThread() {
        final OpenDialogProperties props = new OpenDialogProperties();
        props.setRunAsync(false);
        NativeDialogs.prepareListener(props);

        props.getListener().nativeDialogEvent(OK);

        assertSame(OK, ((NativeDialogs.MessageWaitListener) props.getListener()).getEvent());
    }

    /** Properties objects are reused; each request must not add another layer of forwarding. */
    @Test
    void preparingAgainKeepsTheSameListener() {
        final NativeDialogProperties props = asyncRequest(evt -> {});
        final NativeDialogListener first = props.getListener();

        NativeDialogs.prepareListener(props);

        assertSame(first, props.getListener());
    }
}
