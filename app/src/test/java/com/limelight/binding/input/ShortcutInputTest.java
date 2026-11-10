package com.limelight.binding.input;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class ShortcutInputTest {
    private final List<String> events = new ArrayList<>();
    private final ShortcutInput input = new ShortcutInput(new ShortcutInput.Sink() {
        public void key(int vk, boolean down, byte modifiers, byte flags) {
            events.add(vk + (down ? "+" : "-"));
        }
        public void mouse(int button, boolean down) { events.add("m" + button + (down ? "+" : "-")); }
    });

    @Test public void copyUsesRealModifierPackets() {
        input.chord(0xA2, 0x43);
        assertEquals(List.of("162+", "67+", "67-", "162-"), events);
    }
    @Test public void physicalOwnerSurvivesBarRelease() {
        input.physical("keyboard1", 0xA2, true, (byte)2, (byte)0);
        input.cycle(0xA2);
        input.chord(0xA2, 0x43);
        assertEquals(List.of("162+", "67+", "67-"), events);
        input.physical("keyboard1", 0xA2, false, (byte)0, (byte)0);
        assertEquals("162-", events.get(events.size() - 1));
    }
    @Test public void oneShotEndsAfterMouseUpNotDown() {
        input.cycle(0xA0);
        input.mouse("touch0", 1, true, false);
        assertEquals(1, input.mode(0xA0));
        input.mouse("touch0", 1, false, true);
        assertEquals(List.of("160+", "m1+", "m1-", "160-"), events);
    }
    @Test public void lockedModifierSurvivesKeyAndReleaseIsIdempotent() {
        input.cycle(0xA0); input.cycle(0xA0); input.tap(0x25);
        assertEquals(2, input.mode(0xA0));
        input.releaseAll(); input.releaseAll();
        assertEquals(List.of("160+", "37+", "37-", "160-"), events);
    }
    @Test public void altTabOwnershipDoesNotReleaseLockedAlt() {
        input.cycle(0xA4); input.cycle(0xA4);
        input.altTab(); input.altTab(); input.endAltTab();
        assertEquals(2, input.mode(0xA4));
        assertFalse(events.contains("164-"));
    }
    @Test public void separatePhysicalDevicesKeepModifierHeld() {
        input.physical("one", 0xA2, true, (byte)2, (byte)0);
        input.physical("two", 0xA2, true, (byte)2, (byte)0);
        input.physical("one", 0xA2, false, (byte)0, (byte)0);
        assertEquals(List.of("162+"), events);
        input.physical("two", 0xA2, false, (byte)0, (byte)0);
        assertEquals(List.of("162+", "162-"), events);
    }
    @Test public void rightClickOwnershipDoesNotReleasePhysicalMouse() {
        input.mouse("physical", 3, true, false);
        input.mouse("bar", 3, true, false);
        input.mouse("bar", 3, false, true);
        assertEquals(List.of("m3+"), events);
        input.releaseAll();
        assertEquals(List.of("m3+"), events);
    }
    @Test public void disabledGateSendsNothingAndClearsSyntheticState() {
        input.cycle(0xA2); input.releaseAll(); input.setEnabled(false);
        events.clear(); input.cycle(0xA2); input.tap(0x43); input.altTab();
        input.mouse("bar", 3, true, false);
        assertTrue(events.isEmpty());
        assertEquals(0, input.mode(0xA2));
    }
    @Test public void focusLossAllowsPhysicalDownAgainWithoutReceivingOldUp() {
        input.physical("one", 0xA3, true, (byte)2, (byte)1);
        input.releaseForFocusLoss();
        input.physical("one", 0xA3, true, (byte)2, (byte)1);
        input.physical("one", 0xA3, false, (byte)0, (byte)1);
        assertEquals(List.of("163+", "163-", "163+", "163-"), events);
    }
    @Test public void leftAndRightModifiersHaveIndependentPackets() {
        input.physical("one", 0xA3, true, (byte)2, (byte)0);
        input.chord(0xA2, 0x43);
        assertEquals(List.of("163+", "162+", "67+", "67-", "162-"), events);
        assertEquals(2, input.modifiers());
    }
}
