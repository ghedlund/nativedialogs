package ca.phon.ui.nativedialogs.ffm.win;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@EnabledOnOs(OS.WINDOWS)
class OwnerWindowTest {

    private final List<JFrame> frames = new ArrayList<>();

    @BeforeEach
    void requireDisplay() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
    }

    @AfterEach
    void disposeFrames() throws Exception {
        SwingUtilities.invokeAndWait(() -> frames.forEach(JFrame::dispose));
    }

    /** Shows a frame and waits until it is showing. */
    static JFrame showFrame(String title, Rectangle bounds) throws Exception {
        JFrame[] f = new JFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            f[0] = new JFrame(title);
            f[0].setBounds(bounds);
            f[0].setVisible(true);
        });
        return f[0];
    }

    private JFrame frame(String title, Rectangle bounds) throws Exception {
        JFrame f = showFrame(title, bounds);
        frames.add(f);
        return f;
    }

    @Test
    void findsTheHandleOfAShownFrame() throws Exception {
        String title = "owner-" + UUID.randomUUID();
        JFrame f = frame(title, new Rectangle(100, 100, 400, 300));

        long hwnd = OwnerWindow.handleOf(f);

        assertNotEquals(0L, hwnd);
        assertEquals(title, TestUser32.text(hwnd));
        assertEquals(TestUser32.findByTitle(title), hwnd);
    }

    @Test
    void picksTheFrameWithMatchingBoundsWhenTitlesAreEqual() throws Exception {
        String title = "twin-" + UUID.randomUUID();
        JFrame small = frame(title, new Rectangle(100, 100, 300, 200));
        JFrame large = frame(title, new Rectangle(200, 150, 600, 450));

        long hSmall = OwnerWindow.handleOf(small);
        long hLarge = OwnerWindow.handleOf(large);

        assertNotEquals(0L, hSmall);
        assertNotEquals(0L, hLarge);
        assertNotEquals(hSmall, hLarge);
        assertTrue(TestUser32.rect(hSmall).width < TestUser32.rect(hLarge).width,
            "small frame resolved to the large frame's window");
    }

    @Test
    void noHandleWithoutAShowingWindow() throws Exception {
        assertEquals(0L, OwnerWindow.handleOf(null));
        JFrame[] hidden = new JFrame[1];
        SwingUtilities.invokeAndWait(() -> hidden[0] = new JFrame("never shown"));
        assertEquals(0L, OwnerWindow.handleOf(hidden[0]));
        SwingUtilities.invokeAndWait(hidden[0]::dispose);
    }

    @Test
    void resolvePrefersTheShowingParent() throws Exception {
        JFrame f = frame("parent-" + UUID.randomUUID(), new Rectangle(100, 100, 300, 200));
        assertSame(f, OwnerWindow.resolve(f));
    }

    @Test
    void invalidHandleBecomesNull() {
        assertEquals(0L, OwnerWindow.validOrNull(0L).address());
        // a handle value no window uses
        assertEquals(0L, OwnerWindow.validOrNull(0x7FFF_FFF0L).address());
    }
}
