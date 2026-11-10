package com.limelight.binding.input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Validates the complete request before producing JNI-safe UTF-8 packets. No clipboard access. */
public final class ClipboardText {
    public static final class Part {
        public final String text;
        public final int key;
        private Part(String text, int key) { this.text = text; this.key = key; }
    }
    public final List<Part> parts;
    public final int codePoints;
    public final boolean multiline;

    private ClipboardText(List<Part> parts, int count, boolean multiline) {
        this.parts = Collections.unmodifiableList(parts);
        this.codePoints = count;
        this.multiline = multiline;
    }

    public static ClipboardText parse(String text) {
        if (text == null || text.isEmpty() || text.codePointCount(0, text.length()) > 2000) {
            throw new IllegalArgumentException("length");
        }
        List<Part> parts = new ArrayList<>();
        StringBuilder chunk = new StringBuilder();
        int bytes = 0;
        boolean multiline = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isSurrogate(c) || (Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t') ||
                    c == '\uFFFE' || c == '\uFFFF') {
                throw new IllegalArgumentException("unsupported");
            }
            if (c == '\n' || c == '\r' || c == '\t') {
                if (chunk.length() != 0) { parts.add(new Part(chunk.toString(), 0)); chunk.setLength(0); }
                bytes = 0;
                parts.add(new Part(null, c == '\t' ? 0x09 : 0x0D));
                // Tab also moves between ERP fields and needs the same explicit confirmation.
                multiline = true;
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                continue;
            }
            int size = c <= 0x7F ? 1 : c <= 0x7FF ? 2 : 3;
            if (bytes + size > 32) {
                parts.add(new Part(chunk.toString(), 0)); chunk.setLength(0); bytes = 0;
            }
            chunk.append(c); bytes += size;
        }
        if (chunk.length() != 0) parts.add(new Part(chunk.toString(), 0));
        return new ClipboardText(parts, text.length(), multiline);
    }
}
