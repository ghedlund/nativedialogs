package ca.phon.ui.nativedialogs.spi;

import org.junit.jupiter.api.Test;
import java.awt.EventQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import ca.phon.ui.nativedialogs.OpenDialogProperties;
import static org.junit.jupiter.api.Assertions.*;

class MacDialogProviderDispatchTest {

    /**
     * The event dispatch thread must never wait for the AppKit thread to run a
     * dialog. AppKit calls that need the event dispatch thread (accessibility
     * queries, input method callbacks) would then never be answered: the dialog
     * would not open and the application would freeze.
     */
    @Test
    void blockingRequestOnEventDispatchThreadDoesNotWaitForAppKit() throws Exception {
        final OpenDialogProperties props = new OpenDialogProperties();
        props.setRunAsync(false);
        final AtomicBoolean waits = new AtomicBoolean(true);

        EventQueue.invokeAndWait(() -> waits.set(MacDialogProvider.callerWaitsForAppKit(props)));

        assertFalse(waits.get(),
            "a blocking dialog requested on the event dispatch thread must be started without waiting");
    }

    @Test
    void blockingRequestOnOtherThreadsWaitsForAppKit() {
        final OpenDialogProperties props = new OpenDialogProperties();
        props.setRunAsync(false);

        assertTrue(MacDialogProvider.callerWaitsForAppKit(props),
            "a blocking dialog requested off the event dispatch thread waits for the dialog");
    }

    @Test
    void asyncRequestNeverWaitsForAppKit() {
        final OpenDialogProperties props = new OpenDialogProperties();
        props.setRunAsync(true);

        assertFalse(MacDialogProvider.callerWaitsForAppKit(props),
            "an asynchronous dialog must return to the caller immediately");
    }
}
