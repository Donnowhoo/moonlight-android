package com.limelight.binding.input;

import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import com.limelight.binding.input.touch.TouchContext;
import com.limelight.nvstream.NvConnection;
import com.limelight.preferences.PreferenceConfiguration;

/** Finger-only Android adapter. Native pen/eraser events never enter this controller. */
public final class FingerGestureController {
    public interface Host { boolean canSend(); void keyboard(); }
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final FingerGesturePolicy policy;
    private final Host host;
    private final TouchContext[] legacy;
    private float scrollRemainder, remainderX, remainderY;
    private boolean waitForNewContact;
    private final Runnable decision = new Runnable() {
        public void run() { if(host.canSend()) policy.decide(android.os.SystemClock.uptimeMillis()); }
    };

    public FingerGestureController(NvConnection conn, ShortcutController shortcuts, View stream,
                                   TouchContext[] legacy, PreferenceConfiguration config, Host host) {
        this.host=host; this.legacy=legacy;
        final float density=stream.getResources().getDisplayMetrics().density;
        policy=new FingerGesturePolicy(new FingerGesturePolicy.Output() {
            public void singleDown(float x,float y,long time) {
                legacy[0].setPointerCount(1); legacy[0].touchDownEvent((int)x,(int)y,time,true);
            }
            public void singleMove(float x,float y,long time) { legacy[0].touchMoveEvent((int)x,(int)y,time); }
            public void singleUp(float x,float y,long time) { legacy[0].touchUpEvent((int)x,(int)y,time); }
            public void cancelSingle() { cancelLegacy(); }
            private void position(float x,float y) {
                if(config.touchscreenTrackpad)return;
                conn.sendMousePosition((short)Math.max(0,Math.min(stream.getWidth(),x)),
                        (short)Math.max(0,Math.min(stream.getHeight(),y)),
                        (short)stream.getWidth(),(short)stream.getHeight());
            }
            public void beginDrag(int button,float x,float y) {
                cancelLegacy(); remainderX=remainderY=0;
                position(x,y); shortcuts.mouse("gesture",(byte)button,true,false);
            }
            public void moveDrag(float x,float y,float dx,float dy) {
                if(!config.touchscreenTrackpad) { position(x,y); return; }
                remainderX+=dx*1280/Math.max(1,stream.getWidth());
                remainderY+=dy*720/Math.max(1,stream.getHeight());
                int mx=(int)remainderX,my=(int)remainderY; remainderX-=mx;remainderY-=my;
                if(config.absoluteMouseMode) conn.sendMouseMoveAsMousePosition((short)mx,(short)my,
                        (short)stream.getWidth(),(short)stream.getHeight());
                else conn.sendMouseMove((short)mx,(short)my);
            }
            public void endDrag(int button,boolean complete) { shortcuts.mouse("gesture",(byte)button,false,complete); }
            public void scroll(float dy) {
                // 48 dp equals one 120-unit wheel detent; preserve fractional high-resolution units.
                scrollRemainder+=dy*120f/(48f*density);
                int units=(int)scrollRemainder;scrollRemainder-=units;
                while(units!=0) {
                    int part=Math.max(Short.MIN_VALUE,Math.min(Short.MAX_VALUE,units));
                    conn.sendMouseHighResScroll((short)part);units-=part;
                }
            }
            public void rightClick(float x,float y) { position(x,y); shortcuts.gestureRightClick(); }
            public void keyboard() { shortcuts.releaseAll(); host.keyboard(); }
        },density);
    }
    private void cancelLegacy() { for(TouchContext context:legacy)context.cancelForGestureTransfer(); }
    public boolean handle(MotionEvent event,float offsetX,float offsetY) {
        if(!host.canSend()) { cancel(); return true; }
        int action=event.getActionMasked(),index=event.getActionIndex();
        if(action==MotionEvent.ACTION_CANCEL) { cancel(); return true; }
        if(action==MotionEvent.ACTION_DOWN)waitForNewContact=false;
        else if(waitForNewContact)return true;
        // Update every existing pointer atomically before contact changes.
        int count=event.getPointerCount(); int[] ids=new int[count]; float[] x=new float[count],y=new float[count];
        for(int i=0;i<count;i++) { ids[i]=event.getPointerId(i);x[i]=event.getX(i)+offsetX;y[i]=event.getY(i)+offsetY; }
        if(action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_POINTER_DOWN) {
            if(action==MotionEvent.ACTION_DOWN) {
                scrollRemainder=remainderX=remainderY=0;
            }
            policy.move(ids,x,y,event.getEventTime());
            if(action==MotionEvent.ACTION_POINTER_DOWN) cancelLegacy();
            policy.down(ids[index],x[index],y[index],event.getEventTime());
            if(count==2) { handler.removeCallbacks(decision);handler.postDelayed(decision,80); }
        } else if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_POINTER_UP) {
            handler.removeCallbacks(decision);
            policy.lifting(ids,x,y,event.getEventTime());policy.up(ids[index],event.getEventTime());
            if(action==MotionEvent.ACTION_UP)handler.removeCallbacksAndMessages(null);
        } else if(action==MotionEvent.ACTION_MOVE) {
            for(int h=0;h<event.getHistorySize();h++) {
                for(int i=0;i<count;i++) { x[i]=event.getHistoricalX(i,h)+offsetX;y[i]=event.getHistoricalY(i,h)+offsetY; }
                policy.move(ids,x,y,event.getHistoricalEventTime(h));
            }
            for(int i=0;i<count;i++) { x[i]=event.getX(i)+offsetX;y[i]=event.getY(i)+offsetY; }
            policy.move(ids,x,y,event.getEventTime());
        }
        return true;
    }
    public void cancel() {
        waitForNewContact=true;handler.removeCallbacksAndMessages(null);policy.cancel();cancelLegacy();
        scrollRemainder=remainderX=remainderY=0;
    }
}
