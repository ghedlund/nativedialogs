package ca.phon.ui.nativedialogs.ffm.win;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import static org.junit.jupiter.api.Assertions.*;

@EnabledOnOs(OS.WINDOWS)
class Win32SmokeTest {
    @Test
    void comInitializesAndUninitializes() throws Throwable {
        int hr = (int) Win32.CoInitializeEx.invokeExact(
            java.lang.foreign.MemorySegment.NULL, Win32.COINIT_APARTMENTTHREADED);
        assertTrue(hr == 0 || hr == 1, "CoInitializeEx returned 0x" + Integer.toHexString(hr));
        Win32.CoUninitialize.invokeExact();
    }
}
