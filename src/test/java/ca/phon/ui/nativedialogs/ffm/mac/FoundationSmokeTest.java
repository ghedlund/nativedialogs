package ca.phon.ui.nativedialogs.ffm.mac;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import java.lang.foreign.MemorySegment;
import static org.junit.jupiter.api.Assertions.*;

@EnabledOnOs(OS.MAC)
class FoundationSmokeTest {

    @Test
    void resolvesNSObjectClass() {
        MemorySegment cls = ObjC.cls("NSObject");
        assertNotEquals(0L, cls.address(), "NSObject class must resolve");
    }

    @Test
    void resolvesNSOpenPanelClass() {
        MemorySegment cls = ObjC.cls("NSOpenPanel");
        assertNotEquals(0L, cls.address(), "AppKit must be loaded for NSOpenPanel");
    }
}
