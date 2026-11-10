package com.limelight.binding.input;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class ClipboardTyperTest {
    private final List<Runnable> pending = new ArrayList<>();
    private final List<String> sent = new ArrayList<>();
    private boolean ready = true;
    private final ClipboardTyper typer = new ClipboardTyper(new ClipboardTyper.Output() {
        public boolean ready() { return ready; }
        public void send(ClipboardText.Part part) { sent.add(part.text); }
        public void progress(int sent, int total) { }
        public void later(Runnable task, long delay) {
            assertEquals(10, delay); pending.add(task);
        }
    });
    @Test public void userInputCancelsUnsentChunksEvenIfCallbackAlreadyQueued() {
        typer.start(ClipboardText.parse("가".repeat(30)));
        assertEquals(1, sent.size());
        typer.cancel(); pending.remove(0).run();
        assertEquals(1, sent.size());
    }
    @Test public void lostCaptureStopsQueueWithoutSending() {
        typer.start(ClipboardText.parse("가".repeat(30)));
        ready = false; pending.remove(0).run();
        assertEquals(1, sent.size());
    }
    @Test public void newRequestInvalidatesOldGeneration() {
        typer.start(ClipboardText.parse("가".repeat(30)));
        typer.start(ClipboardText.parse("new"));
        pending.remove(0).run();
        assertEquals(List.of("가".repeat(10), "new"), sent);
    }
}
