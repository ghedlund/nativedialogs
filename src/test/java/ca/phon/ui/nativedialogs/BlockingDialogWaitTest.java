package ca.phon.ui.nativedialogs;

import org.junit.jupiter.api.Test;
import java.awt.EventQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class BlockingDialogWaitTest {

    /**
     * While a blocking dialog is open the native side may need an answer from the
     * event dispatch thread before it can deliver a result (on macOS: accessibility
     * queries and input method callbacks arrive this way). If the event dispatch
     * thread stops dispatching events while it waits, both sides wait forever.
     */
    @Test
    void waitOnEventDispatchThreadKeepsDispatchingEvents() throws Exception {
        final AtomicReference<NativeDialogEvent> received = new AtomicReference<>();
        final CountDownLatch returned = new CountDownLatch(1);

        EventQueue.invokeLater(() -> {
            final NativeDialogs.MessageWaitListener waiter = new NativeDialogs.MessageWaitListener();
            final Thread nativeSide = new Thread(() -> {
                try {
                    // needs the event dispatch thread before the dialog can finish
                    EventQueue.invokeAndWait(() -> {});
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
                waiter.nativeDialogEvent(
                    new NativeDialogEvent(NativeDialogEvent.OK_OPTION, "/media/session.wav"));
            }, "native-side");
            nativeSide.setDaemon(true);
            nativeSide.start();

            waiter.waitLoop();
            received.set(waiter.getEvent());
            returned.countDown();
        });

        assertTrue(returned.await(5, TimeUnit.SECONDS),
            "blocking wait must return once the native side delivers its result");
        assertEquals(NativeDialogEvent.OK_OPTION, received.get().getDialogResult());
        assertEquals("/media/session.wav", received.get().getDialogData());
    }

    /** A result delivered before the wait starts must not leave the caller waiting. */
    @Test
    void waitOnEventDispatchThreadReturnsWhenResultAlreadyDelivered() throws Exception {
        final AtomicReference<NativeDialogEvent> received = new AtomicReference<>();
        final CountDownLatch returned = new CountDownLatch(1);

        EventQueue.invokeLater(() -> {
            final NativeDialogs.MessageWaitListener waiter = new NativeDialogs.MessageWaitListener();
            waiter.nativeDialogEvent(new NativeDialogEvent(NativeDialogEvent.CANCEL_OPTION, null));
            waiter.waitLoop();
            received.set(waiter.getEvent());
            returned.countDown();
        });

        assertTrue(returned.await(5, TimeUnit.SECONDS),
            "wait must return at once when the result was delivered first");
        assertEquals(NativeDialogEvent.CANCEL_OPTION, received.get().getDialogResult());
    }

    /** Callers on other threads keep blocking until the result arrives. */
    @Test
    void waitOnOtherThreadsBlocksUntilResultDelivered() throws Exception {
        final NativeDialogs.MessageWaitListener waiter = new NativeDialogs.MessageWaitListener();
        final CountDownLatch returned = new CountDownLatch(1);
        final Thread caller = new Thread(() -> {
            waiter.waitLoop();
            returned.countDown();
        }, "blocking-caller");
        caller.setDaemon(true);
        caller.start();

        assertFalse(returned.await(300, TimeUnit.MILLISECONDS),
            "wait must not return before a result is delivered");
        waiter.nativeDialogEvent(new NativeDialogEvent(NativeDialogEvent.OK_OPTION, "x"));
        assertTrue(returned.await(5, TimeUnit.SECONDS),
            "wait must return once the result is delivered");
    }
}
