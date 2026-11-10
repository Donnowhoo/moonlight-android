package com.limelight.binding.input;

/** Paced, cancellable queue. A generation invalidates even callbacks already dequeued. */
public final class ClipboardTyper {
    public interface Output {
        boolean ready();
        void send(ClipboardText.Part part);
        void progress(int sent, int total);
        void later(Runnable task, long delayMs);
    }
    private final Output output;
    private int generation;
    public ClipboardTyper(Output output) { this.output = output; }
    public void cancel() { generation++; output.progress(0, 0); }
    public void start(ClipboardText text) {
        cancel();
        send(text, 0, generation);
    }
    private void send(ClipboardText text, int index, int token) {
        if (token != generation) return;
        if (!output.ready()) { cancel(); return; }
        output.send(text.parts.get(index));
        output.progress(index + 1, text.parts.size());
        if (index + 1 < text.parts.size()) {
            output.later(() -> send(text, index + 1, token), 10);
        }
    }
}
