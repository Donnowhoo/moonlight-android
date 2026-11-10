package com.limelight.binding.input;

import java.util.LinkedHashMap;
import java.util.Map;

/** Finger-only arbitration. Coordinates are pixels; density converts the documented dp thresholds. */
public final class FingerGesturePolicy {
    public interface Output {
        void singleDown(float x, float y, long time);
        void singleMove(float x, float y, long time);
        void singleUp(float x, float y, long time);
        void cancelSingle();
        void beginDrag(int button, float x, float y);
        void moveDrag(float x, float y, float dx, float dy);
        void endDrag(int button, boolean completes);
        void scroll(float dy);
        void rightClick(float x, float y);
        void keyboard();
    }
    private enum Mode { IDLE, ONE, SINGLE, TWO, SCROLL, LEFT, WAIT_TAP, SECOND_TAP, THREE, MIDDLE, THREE_END, SUPPRESSED }
    private static final class Point {
        float x, y, startX, startY, maxTravel;
        final long time;
        Point(float x, float y, long time) { this.x=this.startX=x; this.y=this.startY=y; this.time=time; }
        void move(float x, float y) {
            this.x=x; this.y=y; maxTravel=Math.max(maxTravel, distance(x-startX,y-startY));
        }
        void rebase() { startX=x; startY=y; maxTravel=0; }
    }
    private final Map<Integer, Point> points = new LinkedHashMap<>();
    private final Output output;
    private final float density;
    private Mode mode = Mode.IDLE;
    private int movingId, anchorId;
    private float lastX, lastY, tapX, tapY, anchorX, anchorY;
    private long firstTapUp, threeStarted, twoStarted;
    private boolean threeTap;

    public FingerGesturePolicy(Output output, float density) { this.output=output; this.density=Math.max(0.1f,density); }
    private static float distance(float x, float y) { return (float)Math.hypot(x,y); }
    private float dp(float value) { return value*density; }
    private Point first() { return points.values().iterator().next(); }
    private float center(boolean x) {
        float total=0; for (Point p:points.values()) total+=x?p.x:p.y;
        return total/points.size();
    }
    private void endDrag(boolean complete) {
        if (mode==Mode.LEFT) output.endDrag(1,complete);
        if (mode==Mode.MIDDLE) output.endDrag(2,complete);
    }
    public void down(int id, float x, float y, long time) {
        if (points.containsKey(id)) return;
        points.put(id,new Point(x,y,time));
        if (points.size()==1) { mode=Mode.ONE; return; }
        if (points.size()>3) { endDrag(false); if(mode==Mode.SINGLE)output.cancelSingle(); mode=Mode.SUPPRESSED; return; }
        if (mode==Mode.SUPPRESSED || mode==Mode.THREE_END) return;
        if (points.size()==3) {
            endDrag(false); if(mode==Mode.SINGLE)output.cancelSingle();
            mode=Mode.THREE; threeStarted=time; threeTap=true;
            for(Point p:points.values())p.rebase();
            lastX=center(true); lastY=center(false); return;
        }
        if (mode==Mode.WAIT_TAP) {
            if(time-firstTapUp<=300 && distance(x-tapX,y-tapY)<=dp(24) && anchorStill()) {
                mode=Mode.SECOND_TAP; movingId=id;
            } else mode=Mode.SUPPRESSED;
            return;
        }
        if(mode==Mode.SINGLE)output.cancelSingle();
        mode=Mode.TWO;
        twoStarted=time;
        for(Point p:points.values())p.rebase();
        lastY=center(false);
    }
    private boolean anchorStill() {
        Point anchor=points.get(anchorId);
        return anchor!=null && distance(anchor.x-anchorX,anchor.y-anchorY)<=dp(6);
    }
    /** One batched sample is essential: never classify while only one pointer has been updated. */
    public void move(int[] ids, float[] x, float[] y, long time) {
        for(int i=0;i<ids.length;i++) { Point p=points.get(ids[i]); if(p!=null)p.move(x[i],y[i]); }
        if(points.isEmpty())return;
        if(mode==Mode.ONE) {
            Point p=first();
            if(p.maxTravel>dp(8)) {
                mode=Mode.SINGLE; output.singleDown(p.startX,p.startY,p.time); output.singleMove(p.x,p.y,time);
            }
        } else if(mode==Mode.SINGLE) {
            Point p=first(); output.singleMove(p.x,p.y,time);
        } else if(mode==Mode.TWO) {
            Point[] pair=points.values().toArray(new Point[0]);
            Point a=pair[0], b=pair[1];
            float ay=a.y-a.startY, by=b.y-b.startY;
            if(Math.abs(ay)>=dp(8) && Math.abs(by)>=dp(8) && ay*by>0 &&
                    Math.abs(a.x-a.startX)<=Math.abs(ay)*1.5f && Math.abs(b.x-b.startX)<=Math.abs(by)*1.5f) {
                mode=Mode.SCROLL; output.scroll(center(false)-lastY); lastY=center(false);
            } else {
                Point moving=null;
                // Allow the second finger to start moving before interpreting the first as an anchor.
                if(time-twoStarted>=80 && a.maxTravel<=dp(6) && b.maxTravel>=dp(12))moving=b;
                else if(time-twoStarted>=80 && b.maxTravel<=dp(6) && a.maxTravel>=dp(12))moving=a;
                if(moving!=null) {
                    for(Map.Entry<Integer,Point> entry:points.entrySet())if(entry.getValue()==moving)movingId=entry.getKey();
                    mode=Mode.LEFT; lastX=moving.startX; lastY=moving.startY;
                    output.beginDrag(1,lastX,lastY); dragMove(moving.x,moving.y);
                } else if(time-twoStarted>=80 && (a.maxTravel>dp(28) || b.maxTravel>dp(28))) mode=Mode.SUPPRESSED;
            }
        } else if(mode==Mode.SCROLL) {
            output.scroll(center(false)-lastY); lastY=center(false);
        } else if(mode==Mode.LEFT) {
            Point p=points.get(movingId); if(p!=null)dragMove(p.x,p.y);
        } else if(mode==Mode.WAIT_TAP || mode==Mode.SECOND_TAP) {
            Point tapping=points.get(movingId);
            if(!anchorStill() || (mode==Mode.SECOND_TAP && tapping!=null && tapping.maxTravel>dp(6)))mode=Mode.SUPPRESSED;
        } else if(mode==Mode.THREE) {
            for(Point p:points.values())if(p.maxTravel>dp(8))threeTap=false;
            float cx=center(true), cy=center(false);
            if(distance(cx-lastX,cy-lastY)>=dp(12)) {
                mode=Mode.MIDDLE; threeTap=false; output.beginDrag(2,lastX,lastY); dragMove(cx,cy);
            }
        } else if(mode==Mode.MIDDLE) dragMove(center(true),center(false));
        else if(mode==Mode.THREE_END) {
            for(Point p:points.values())if(p.maxTravel>dp(8))threeTap=false;
        }
    }
    /** Update final coordinates without allowing an Up event to start a new drag or scroll. */
    public void lifting(int[] ids, float[] x, float[] y, long time) {
        for(int i=0;i<ids.length;i++) { Point p=points.get(ids[i]); if(p!=null)p.move(x[i],y[i]); }
        if(points.isEmpty())return;
        if(mode==Mode.SINGLE) { Point p=first(); output.singleMove(p.x,p.y,time); }
        else if(mode==Mode.LEFT) { Point p=points.get(movingId); if(p!=null)dragMove(p.x,p.y); }
        else if(mode==Mode.MIDDLE)dragMove(center(true),center(false));
        else if(mode==Mode.THREE || mode==Mode.THREE_END) {
            for(Point p:points.values())if(p.maxTravel>dp(8))threeTap=false;
        } else if((mode==Mode.WAIT_TAP || mode==Mode.SECOND_TAP) && !anchorStill())mode=Mode.SUPPRESSED;
    }
    private void dragMove(float x,float y) {
        output.moveDrag(x,y,x-lastX,y-lastY); lastX=x; lastY=y;
    }
    public void decide(long time) { move(new int[0],new float[0],new float[0],time); }
    public void up(int id,long time) {
        Point p=points.get(id); if(p==null)return;
        if(mode==Mode.ONE) {
            output.singleDown(p.startX,p.startY,p.time); output.singleUp(p.x,p.y,time);
        } else if(mode==Mode.SINGLE) output.singleUp(p.x,p.y,time);
        else if(mode==Mode.LEFT || mode==Mode.MIDDLE) { endDrag(true); mode=Mode.SUPPRESSED; }
        else if(mode==Mode.SCROLL)mode=Mode.SUPPRESSED;
        else if(mode==Mode.TWO) {
            boolean stationary=true;
            for(Point point:points.values())if(point.maxTravel>dp(6))stationary=false;
            if(stationary && time-p.time<=220) {
                for(int other:points.keySet())if(other!=id)anchorId=other;
                Point anchor=points.get(anchorId); anchorX=anchor.x; anchorY=anchor.y;
                tapX=p.x; tapY=p.y; firstTapUp=time; mode=Mode.WAIT_TAP;
            } else mode=Mode.SUPPRESSED;
        } else if(mode==Mode.SECOND_TAP) {
            if(id==movingId && time-p.time<=220 && anchorStill() && p.maxTravel<=dp(6))output.rightClick(tapX,tapY);
            mode=Mode.SUPPRESSED;
        } else if(mode==Mode.WAIT_TAP)mode=Mode.SUPPRESSED;
        else if(mode==Mode.THREE)mode=Mode.THREE_END;
        points.remove(id);
        if(points.isEmpty()) {
            if(mode==Mode.THREE_END && threeTap && time-threeStarted<=300)output.keyboard();
            mode=Mode.IDLE;
        }
    }
    public void cancel() {
        endDrag(false); if(mode==Mode.SINGLE)output.cancelSingle();
        points.clear(); mode=Mode.IDLE; threeTap=false;
    }
}
