package ca.phon.ui.nativedialogs.spi;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import ca.phon.ui.nativedialogs.MessageDialogProperties;

import static org.junit.jupiter.api.Assertions.*;

@EnabledOnOs(OS.WINDOWS)
class WindowsDialogProviderTest {

    @Test
    void asyncMessageFallsBackWhenTaskDialogFails() throws InterruptedException {
        WindowsDialogProvider provider = new WindowsDialogProvider((title, header, content, buttons) -> {
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
}
