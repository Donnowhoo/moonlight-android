package com.limelight.binding.input;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class FingerGesturePolicyTest {
    private final List<String> events = new ArrayList<>();
    private final FingerGesturePolicy policy = new FingerGesturePolicy(new FingerGesturePolicy.Output() {
        public void singleDown(float x, float y, long time) { events.add("singleDown"); }
        public void singleMove(float x, float y, long time) { events.add("singleMove"); }
        public void singleUp(float x, float y, long time) { events.add("singleUp"); }
        public void cancelSingle() { events.add("cancelSingle"); }
        public void beginDrag(int button, float x, float y) { events.add("down" + button); }
        public void moveDrag(float x, float y, float dx, float dy) { events.add("move"); }
        public void endDrag(int button, boolean complete) { events.add("up" + button); }
        public void scroll(float dy) { events.add("scroll"); }
        public void rightClick(float x, float y) { events.add("right"); }
        public void keyboard() { events.add("keyboard"); }
    }, 1);
    private void down(int id, float x, float y, long t) { policy.down(id, x, y, t); }
    private void move(long t, float... points) {
        int n = points.length / 3; int[] ids = new int[n]; float[] x = new float[n], y = new float[n];
        for (int i=0;i<n;i++) { ids[i]=(int)points[i*3]; x[i]=points[i*3+1]; y[i]=points[i*3+2]; }
        policy.move(ids,x,y,t);
    }
    @Test public void stationaryAnchorMayWaitLongerThan650msBeforeSecondContact() {
        down(0,0,0,0);policy.decide(2000);
        assertTrue(events.isEmpty());
        down(1,40,0,2100);move(2200,0,0,0,1,60,0);
        assertEquals(List.of("down1","move"),events);
    }
    @Test public void bothFingersMovingVerticallyScrollWithoutLeftClick() {
        down(5,0,0,0); down(9,40,0,20); move(50,5,0,12,9,40,13);
        assertEquals(List.of("scroll"), events);
        move(60,5,0,12,9,60,30);
        assertFalse(events.contains("down1"));
    }
    @Test public void stationaryAnchorCanBeEitherPointer() {
        for (boolean firstMoves : new boolean[]{true,false}) {
            events.clear(); policy.cancel(); events.clear();
            down(5,0,0,0); down(9,40,0,20);
            move(100,5,firstMoves?20:0,0,9,firstMoves?40:60,0);
            assertEquals(List.of("down1","move"), events);
            policy.up(5,100); assertEquals("up1",events.get(events.size()-1));
        }
    }
    @Test public void ambiguousOppositeMotionCannotFlipIntoDrag() {
        down(0,0,0,0); down(1,40,0,10); move(30,0,0,10,1,40,-10);
        assertTrue(events.isEmpty());
        move(100,0,0,35,1,40,-35); policy.up(1,180); policy.up(0,190);
        assertTrue(events.isEmpty());
    }
    @Test public void anchorAndTwoTapsProduceExactlyOneRightClick() {
        down(0,0,0,0); down(1,40,0,20); policy.up(1,90);
        down(7,42,0,170); policy.up(7,230); policy.up(0,250);
        assertEquals(List.of("right"),events);
    }
    @Test public void secondPointerMayBeTheAnchor() {
        down(0,0,0,0); down(1,40,0,20); policy.up(0,90);
        down(7,1,0,170); policy.up(7,230); policy.up(1,250);
        assertEquals(List.of("right"),events);
    }
    @Test public void movingAnchorCancelsRightClickCandidate() {
        down(0,0,0,0); down(1,40,0,20); policy.up(1,90);
        move(100,0,8,0); down(7,40,0,170); policy.up(7,230); policy.up(0,250);
        assertTrue(events.isEmpty());
    }
    @Test public void thirdContactReleasesLeftBeforeMiddleAndLiftReleasesMiddle() {
        down(0,0,0,0); down(1,40,0,20); move(100,0,0,0,1,60,0);
        down(2,100,0,150); move(180,0,20,0,1,80,0,2,120,0);
        policy.up(1,200); policy.up(2,210); policy.up(0,220);
        assertEquals(List.of("down1","move","up1","down2","move","up2"), events);
        assertFalse(events.contains("keyboard"));
    }
    @Test public void stationaryThreeFingerTapIsKeyboardOnly() {
        down(0,0,0,0); down(1,40,0,20); down(2,80,0,40);
        policy.up(2,100); policy.up(1,120); policy.up(0,140);
        assertEquals(List.of("keyboard"),events);
    }
    @Test public void cancellationReleasesDragOnceAndStopsFurtherMotion() {
        down(0,0,0,0); down(1,40,0,20); move(100,0,0,0,1,60,0);
        policy.cancel(); policy.cancel(); move(190,0,0,0,1,80,0);
        assertEquals(List.of("down1","move","up1"),events);
    }
    @Test public void liftCoordinatesNeverStartDrag() {
        down(0,0,0,0);down(1,40,0,20);
        policy.lifting(new int[]{0,1},new float[]{0,60},new float[]{0,0},120);
        policy.up(1,120);policy.up(0,140);
        assertTrue(events.isEmpty());
    }
    @Test public void cancellationReleasesMiddleAndNeverOpensKeyboard() {
        down(0,0,0,0);down(1,40,0,20);down(2,80,0,40);
        move(100,0,20,0,1,60,0,2,100,0);policy.cancel();policy.cancel();
        assertEquals(List.of("down2","move","up2"),events);
    }
    @Test public void gracePeriodLetsBothFingersBecomeScroll() {
        down(0,0,0,0);down(1,40,0,20);
        move(40,0,0,14,1,40,0);
        assertTrue(events.isEmpty());
        move(60,0,0,18,1,40,12);
        assertEquals(List.of("scroll"),events);
    }
    @Test public void lateSecondTapAndLongTapDoNotRightClick() {
        down(0,0,0,0);down(1,40,0,20);policy.up(1,90);
        down(2,40,0,500);policy.up(2,550);policy.up(0,580);
        assertTrue(events.isEmpty());
    }
}
