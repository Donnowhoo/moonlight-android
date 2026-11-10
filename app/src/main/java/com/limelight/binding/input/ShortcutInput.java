package com.limelight.binding.input;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Packet ownership for F3. All calls are serialized on the Android UI thread. */
public final class ShortcutInput {
    public interface Sink {
        void key(int vk, boolean down, byte modifiers, byte flags);
        void mouse(int button, boolean down);
    }

    private final Sink sink;
    private final Map<Integer, Set<String>> keys = new HashMap<>();
    private final Map<Integer, Set<String>> buttons = new HashMap<>();
    private final Map<Integer, Integer> modes = new HashMap<>();
    private final Map<Integer, Byte> keyFlags = new HashMap<>();
    private boolean enabled = true;
    private boolean nativePenDown;

    public ShortcutInput(Sink sink) { this.sink = sink; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int mode(int vk) { return modes.getOrDefault(vk, 0); }

    private static int mask(int vk) {
        switch (vk) {
            case 0x10: case 0xA0: case 0xA1: return 1;
            case 0x11: case 0xA2: case 0xA3: return 2;
            case 0x12: case 0xA4: case 0xA5: return 4;
            case 0x5B: case 0x5C: return 8;
            default: return 0;
        }
    }

    public byte modifiers() {
        int value = 0;
        for (Map.Entry<Integer, Set<String>> entry : keys.entrySet()) {
            if (!entry.getValue().isEmpty()) value |= mask(entry.getKey());
        }
        return (byte)value;
    }

    private void key(String owner, int vk, boolean down, byte extra, byte flags) {
        if (!enabled) return;
        Set<String> owners = keys.computeIfAbsent(vk, k -> new HashSet<>());
        boolean wasDown = !owners.isEmpty();
        if (down) owners.add(owner); else owners.remove(owner);
        if (wasDown != !owners.isEmpty()) {
            if (down) keyFlags.put(vk, flags);
            sink.key(vk, down, (byte)(modifiers() | extra), down ? flags : keyFlags.getOrDefault(vk, flags));
        }
    }

    /** Physical ownership includes device ID and exact left/right translated VK. */
    public void physical(String device, int vk, boolean down, byte modifiers, byte flags) {
        boolean owned = keys.containsKey(vk) && keys.get(vk).contains("physical:" + device);
        key("physical:" + device, vk, down, modifiers, flags);
        if (!down && owned && mask(vk) == 0) complete();
    }

    public void cycle(int vk) {
        if (!enabled) return;
        int mode = (mode(vk) + 1) % 3;
        modes.put(vk, mode);
        key("sticky", vk, mode != 0, (byte)0, (byte)0);
    }

    public void tap(int vk) {
        if (!enabled) return;
        key("tap", vk, true, (byte)0, (byte)0);
        key("tap", vk, false, (byte)0, (byte)0);
        complete();
    }

    public void chord(int modifier, int vk) {
        if (!enabled) return;
        key("chord", modifier, true, (byte)0, (byte)0);
        tap(vk);
        key("chord", modifier, false, (byte)0, (byte)0);
    }

    public void altTab() {
        if (!enabled) return;
        key("switcher", 0xA4, true, (byte)0, (byte)0);
        tap(0x09);
    }
    public void endAltTab() { key("switcher", 0xA4, false, (byte)0, (byte)0); }

    /** Completion is attached to the actual remote Up, never Android ACTION_UP. */
    public void mouse(String owner, int button, boolean down, boolean completes) {
        if (!enabled) return;
        Set<String> owners = buttons.computeIfAbsent(button, k -> new HashSet<>());
        boolean wasDown = !owners.isEmpty();
        if (down) owners.add(owner); else owners.remove(owner);
        if (wasDown != !owners.isEmpty()) sink.mouse(button, down);
        if (!down && wasDown && owners.isEmpty() && completes) complete();
    }

    public void complete() {
        if (nativePenDown) return;
        // A second input must not release Shift halfway through a drag.
        for (Set<String> owners : buttons.values()) if (!owners.isEmpty()) return;
        for (int vk : new int[]{0xA2, 0xA0, 0xA4}) {
            if (mode(vk) == 1) {
                modes.put(vk, 0);
                key("sticky", vk, false, (byte)0, (byte)0);
            }
        }
    }

    /** Only releases synthetic owners. Never lifts a physically held key or pen button. */
    public void releaseAll() {
        for (Map.Entry<Integer, Set<String>> entry : buttons.entrySet()) {
            boolean wasDown = !entry.getValue().isEmpty();
            entry.getValue().remove("bar");
            if (enabled && wasDown && entry.getValue().isEmpty()) sink.mouse(entry.getKey(), false);
        }
        modes.clear();
        for (int vk : new HashSet<>(keys.keySet())) {
            for (String owner : new HashSet<>(keys.get(vk))) {
                if (!owner.startsWith("physical:")) key(owner, vk, false, (byte)0, (byte)0);
            }
        }
        if (!enabled) {
            for (Set<String> owners : keys.values()) owners.removeIf(owner -> !owner.startsWith("physical:"));
        }
    }

    /** Focus/capture boundary: Android can lose physical Up events while another window owns focus. */
    public void releaseForFocusLoss() {
        releaseAll();
        for (Map.Entry<Integer, Set<String>> entry : buttons.entrySet()) {
            if (enabled && !entry.getValue().isEmpty()) sink.mouse(entry.getKey(), false);
        }
        buttons.clear();
        for (int vk : new HashSet<>(keys.keySet())) {
            boolean down = !keys.get(vk).isEmpty();
            keys.get(vk).clear();
            if (enabled && down) sink.key(vk, false, modifiers(), keyFlags.getOrDefault(vk, (byte)0));
        }
        keys.clear(); keyFlags.clear(); nativePenDown = false;
    }
    public void nativePen(boolean down) {
        nativePenDown = down;
        if (!down) complete();
    }
}
