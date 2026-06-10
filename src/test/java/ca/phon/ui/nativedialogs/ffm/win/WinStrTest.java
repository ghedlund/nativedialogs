package ca.phon.ui.nativedialogs.ffm.win;

import org.junit.jupiter.api.Test;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import static org.junit.jupiter.api.Assertions.*;

class WinStrTest {
    @Test
    void roundTrip() {
        try (Arena a = Arena.ofConfined()) {
            MemorySegment w = WinStr.wide(a, "C:/héllo/世界.txt");
            assertEquals("C:/héllo/世界.txt", WinStr.fromWide(w));
        }
    }
}
