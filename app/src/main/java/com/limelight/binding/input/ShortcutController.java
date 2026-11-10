package com.limelight.binding.input;

import android.os.Handler;
import android.os.Looper;
import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.input.KeyboardPacket;
import com.limelight.nvstream.input.MouseButtonPacket;

/** Android scheduling and transport adapter; never changes Game's local modifier flags. */
public final class ShortcutController {
    public interface Host {
        boolean canSend();
        void changed();
        void progress(int sent, int total);
        void cancelTouchInput();
    }
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Host host;
    private final ShortcutInput input;
    private final ClipboardTyper typer;
    private int repeatGeneration;
    private final Runnable altUp;
    private final Runnable rightUp;

    public ShortcutController(NvConnection connection, Host host) {
        this.host = host;
        input = new ShortcutInput(new ShortcutInput.Sink() {
            public void key(int vk, boolean down, byte modifiers, byte flags) {
                if (host.canSend()) connection.sendKeyboardInput((short)(0x8000 | vk),
                        down ? KeyboardPacket.KEY_DOWN : KeyboardPacket.KEY_UP, modifiers, flags);
            }
            public void mouse(int button, boolean down) {
                if (!host.canSend()) return;
                if (down) connection.sendMouseButtonDown((byte)button);
                else connection.sendMouseButtonUp((byte)button);
            }
        });
        typer = new ClipboardTyper(new ClipboardTyper.Output() {
            public boolean ready() { return host.canSend() && input.modifiers() == 0; }
            public void send(ClipboardText.Part part) {
                if (part.key != 0) input.tap(part.key); else connection.sendUtf8Text(part.text);
            }
            public void progress(int sent, int total) { host.progress(sent, total); }
            public void later(Runnable task, long delay) { handler.postDelayed(task, delay); }
        });
        altUp = () -> { input.endAltTab(); host.changed(); };
        rightUp = () -> { input.mouse("bar", MouseButtonPacket.BUTTON_RIGHT, false, true); host.changed(); };
    }

    private boolean ready() { input.setEnabled(host.canSend()); return host.canSend(); }
    public int mode(int key) { return input.mode(key); }
    public byte modifiers() { return input.modifiers(); }
    public void userInput() { typer.cancel(); }
    public void overlayTouch() { host.cancelTouchInput(); stopRepeat(); }
    private void barInput() { host.cancelTouchInput(); userInput(); }
    public void cycle(int key) { barInput(); if (ready()) input.cycle(key); host.changed(); }
    public void tap(int key) { barInput(); if (ready()) input.tap(key); host.changed(); }
    public void chord(int modifier, int key) { barInput(); if (ready()) input.chord(modifier, key); host.changed(); }
    public void altTab() {
        barInput(); if (!ready()) return;
        input.altTab(); handler.removeCallbacks(altUp); handler.postDelayed(altUp, 2000); host.changed();
    }
    public void rightClick() {
        barInput(); gestureRightClick();
    }
    public void gestureRightClick() {
        userInput(); if (!ready()) return;
        input.mouse("bar", MouseButtonPacket.BUTTON_RIGHT, true, false);
        handler.removeCallbacks(rightUp); handler.postDelayed(rightUp, 100);
    }
    public void repeat(int key) {
        stopRepeat(); tap(key);
        final int token = repeatGeneration;
        handler.postDelayed(new Runnable() {
            public void run() {
                if (token != repeatGeneration || !ready()) return;
                input.tap(key); host.changed(); handler.postDelayed(this, 70);
            }
        }, 400);
    }
    public void stopRepeat() { repeatGeneration++; }
    public void physical(String device, short key, boolean down, byte modifiers, byte flags) {
        userInput(); if (ready()) input.physical(device, key & 0xFF, down, modifiers, flags);
        host.changed();
    }
    public void mouse(String owner, byte button, boolean down, boolean completes) {
        if (ready()) input.mouse(owner, button, down, completes);
        host.changed();
    }
    public void complete() { if (ready()) input.complete(); host.changed(); }
    public void nativePen(boolean down) { if (ready()) input.nativePen(down); host.changed(); }
    public boolean type(ClipboardText text) {
        if (!ready() || input.modifiers() != 0) return false;
        typer.start(text); return true;
    }
    public void releaseAll() {
        host.cancelTouchInput();
        typer.cancel(); stopRepeat(); handler.removeCallbacksAndMessages(null);
        // Caller invokes this before changing grabbedInput/connected or stopping the transport.
        input.setEnabled(host.canSend()); input.releaseAll(); host.changed();
    }
    public void releaseForFocusLoss() {
        releaseAll(); input.releaseForFocusLoss(); host.changed();
    }
}
