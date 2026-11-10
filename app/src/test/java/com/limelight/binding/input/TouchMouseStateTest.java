package com.limelight.binding.input;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class TouchMouseStateTest {
    @Test public void rapidSecondTapClosesFirstPressAndInvalidatesItsTimer() {
        List<String> events = new ArrayList<>();
        TouchMouseState state = new TouchMouseState((button, down, complete) -> events.add(down + ":" + complete));
        state.down((byte)1); Runnable first = state.delayedUp((byte)1);
        state.down((byte)1); Runnable second = state.delayedUp((byte)1);
        first.run();
        assertEquals(List.of("true:false", "false:true", "true:false"), events);
        second.run();
        assertEquals("false:true", events.get(3));
    }
    @Test public void delayedUpCompletesOnlyWhenActuallySent() {
        List<String> events = new ArrayList<>();
        TouchMouseState state = new TouchMouseState((button, down, complete) -> events.add(down + ":" + complete));
        state.down((byte)1);
        Runnable delayed = state.delayedUp((byte)1);
        assertEquals(List.of("true:false"), events);
        delayed.run();
        assertEquals(List.of("true:false", "false:true"), events);
    }
    @Test public void cancellationInvalidatesOldUpBeforeNewGesture() {
        List<String> events = new ArrayList<>();
        TouchMouseState state = new TouchMouseState((button, down, complete) -> events.add(down + ":" + complete));
        state.down((byte)1); Runnable oldUp = state.delayedUp((byte)1);
        state.cancel(); state.down((byte)1); oldUp.run();
        assertEquals(List.of("true:false", "false:false", "true:false"), events);
        state.up((byte)1, true);
        assertEquals("false:true", events.get(3));
    }
    @Test public void longPressTransitionDoesNotCompleteAtIntermediateLeftUp() {
        List<Boolean> completions = new ArrayList<>();
        TouchMouseState state = new TouchMouseState((button, down, complete) -> { if (!down) completions.add(complete); });
        state.down((byte)1); state.up((byte)1, false); state.down((byte)3); state.up((byte)3, true);
        assertEquals(List.of(false, true), completions);
    }
}
