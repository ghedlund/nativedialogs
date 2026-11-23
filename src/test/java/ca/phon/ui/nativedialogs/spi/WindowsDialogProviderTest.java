package ca.phon.ui.nativedialogs.spi;

import java.awt.GraphicsEnvironment;
import java.lang.foreign.MemorySegment;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import ca.phon.ui.nativedialogs.MessageDialogProperties;
import ca.phon.ui.nativedialogs.NativeDialogEvent;
import ca.phon.ui.nativedialogs.OpenDialogProperties;
import ca.phon.ui.nativedialogs.ffm.win.OwnerWindow;
import ca.phon.ui.nativedialogs.ffm.win.TestUser32;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@EnabledOnOs(OS.WINDOWS)
class WindowsDialogProviderTest {

    @Test
    void asyncMessageFallsBackWhenTaskDialogFails() throws InterruptedException {
        WindowsDialogProvider provider = new WindowsDialogProvider((title, header, content, buttons, owner) -> {
            throw new IllegalStateException("TaskDialogIndirect failed");
        });
        MessageDialogProperties props = new MessageDialogProperties();
        props.setRunAsync(true);
        props.setOptions(MessageDialogProperties.yesNoCancelOptions);
        props.setListener(e -> fail("a failed dialog must not report a result"));

        CountDownLatch fellBack = new CountDownLatch(1);
        provider.showMessage(props, fellBack::countDown);

        assertTrue(fellBack.await(5, TimeUnit.SECONDS), "fallback not called");
    }

    private static NativeDialogEvent showMessageWithOwnerFinder(WindowsDialogProvider.OwnerFinder finder,
                                                                AtomicLong seenOwner) {
        WindowsDialogProvider provider = new WindowsDialogProvider((title, header, content, buttons, owner) -> {
            seenOwner.set(owner.address());
            return 1;
        }, finder);
        MessageDialogProperties props = new MessageDialogProperties();
        props.setRunAsync(false);
        props.setOptions(MessageDialogProperties.yesNoCancelOptions);
        AtomicReference<NativeDialogEvent> result = new AtomicReference<>();
        props.setListener(result::set);
        provider.showMessage(props, () -> fail("must not fall back"));
        return result.get();
    }

    @Test
    void messageHasNoOwnerWhenOwnerLookupFails() {
        AtomicLong seen = new AtomicLong(-1);
        NativeDialogEvent evt = showMessageWithOwnerFinder(parent -> {
            throw new IllegalStateException("lookup failed");
        }, seen);
        assertEquals(0L, seen.get());
        assertEquals(1, evt.getDialogResult());
    }

    @Test
    void messageHasNoOwnerWhenTheHandleIsNotAWindow() {
        AtomicLong seen = new AtomicLong(-1);
        showMessageWithOwnerFinder(parent -> 0x7FFF_FFF0L, seen);
        assertEquals(0L, seen.get());
    }

    @Test
    void openDialogIsOwnedByTheParentWindow() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        JFrame[] f = new JFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            f[0] = new JFrame("open-owner-" + UUID.randomUUID());
            f[0].setBounds(150, 120, 900, 650);
            f[0].setVisible(true);
        });
        JFrame frame = f[0];
        try {
            long ownerHwnd = OwnerWindow.handleOf(frame);
            assertNotEquals(0L, ownerHwnd);

            String title = "open-" + UUID.randomUUID();
            OpenDialogProperties props = new OpenDialogProperties();
            props.setRunAsync(true);
            props.setTitle(title);
            props.setParentWindow(frame);
            CountDownLatch closed = new CountDownLatch(1);
            AtomicReference<NativeDialogEvent> result = new AtomicReference<>();
            props.setListener(e -> { result.set(e); closed.countDown(); });

            new WindowsDialogProvider().showOpen(props);

            long dialog = 0L;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (dialog == 0L && System.nanoTime() < deadline) {
                dialog = TestUser32.findByTitle(title);
                if (dialog == 0L) Thread.sleep(50);
            }
            assertNotEquals(0L, dialog, "open dialog did not appear");
            long owner = TestUser32.owner(dialog);
            TestUser32.postClose(dialog);

            assertTrue(closed.await(10, TimeUnit.SECONDS), "open dialog did not close");
            assertEquals(ownerHwnd, owner, "open dialog not owned by the frame");
            assertEquals(NativeDialogEvent.CANCEL_OPTION, result.get().getDialogResult());
        } finally {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
    }
}
