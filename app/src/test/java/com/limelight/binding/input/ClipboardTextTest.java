package com.limelight.binding.input;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class ClipboardTextTest {
    @Test public void elevenKoreanCharactersSplitAtByteBoundary() {
        ClipboardText plan = ClipboardText.parse("가".repeat(11));
        assertEquals(2, plan.parts.size());
        assertEquals("가".repeat(10), plan.parts.get(0).text);
        assertEquals("가", plan.parts.get(1).text);
    }
    @Test public void mixedTextPreservesEveryCharacterWithin32Bytes() {
        String text = "abc가나다라마바".repeat(20);
        StringBuilder result = new StringBuilder();
        for (ClipboardText.Part part : ClipboardText.parse(text).parts) {
            assertTrue(part.text.getBytes(StandardCharsets.UTF_8).length <= 32);
            result.append(part.text);
        }
        assertEquals(text, result.toString());
    }
    @Test public void newlineAndTabBecomeExplicitKeysAndCrLfIsOneEnter() {
        ClipboardText plan = ClipboardText.parse("a\r\nb\tc\rd\ne");
        assertTrue(plan.multiline);
        assertEquals(9, plan.parts.size());
        assertEquals(13, plan.parts.get(1).key);
        assertEquals(9, plan.parts.get(3).key);
    }
    @Test public void unsupportedTextRejectsWholeRequest() {
        for (String text : new String[]{"", "x\0", "x\uD83D\uDE00", "\uD800", "\uDC00", "\u0007", "a".repeat(2001)}) {
            assertThrows(IllegalArgumentException.class, () -> ClipboardText.parse(text));
        }
        assertEquals(2000, ClipboardText.parse("가".repeat(2000)).codePoints);
    }
    @Test public void tabSeparatedCellsRequireConfirmation() {
        assertTrue(ClipboardText.parse("first\tsecond").multiline);
    }
}
