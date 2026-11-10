package com.limelight.binding.input;

import java.util.HashSet;
import java.util.Set;

/** Tracks touch-owned buttons so a cancelled 100 ms callback cannot complete a later click. */
public final class TouchMouseState {
    public interface Sink { void button(byte button, boolean down, boolean completes); }
    private final Sink sink;
    private final Set<Byte> down = new HashSet<>();
    private int generation;
    private final java.util.Map<Byte, Integer> presses = new java.util.HashMap<>();
    public TouchMouseState(Sink sink) { this.sink = sink; }
    public void down(byte button) {
        // A new tap before the previous 100 ms Up is a new click, not another owner of that click.
        if (down.contains(button)) up(button, true);
        presses.put(button, ++generation);
        down.add(button); sink.button(button, true, false);
    }
    public void up(byte button, boolean completes) {
        if (down.remove(button)) sink.button(button, false, completes);
    }
    public Runnable delayedUp(byte button) {
        Integer token = presses.get(button);
        return () -> { if (token != null && token.equals(presses.get(button))) up(button, true); };
    }
    public void cancel() {
        generation++;
        presses.clear();
        for (byte button : new HashSet<>(down)) up(button, false);
    }
}
