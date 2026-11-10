package com.limelight.binding.input.touch;

public interface TouchContext {
    void setShortcutController(com.limelight.binding.input.ShortcutController controller);
    int getActionIndex();
    void setPointerCount(int pointerCount);
    boolean touchDownEvent(int eventX, int eventY, long eventTime, boolean isNewFinger);
    boolean touchMoveEvent(int eventX, int eventY, long eventTime);
    void touchUpEvent(int eventX, int eventY, long eventTime);
    void cancelTouch();
    default void cancelForGestureTransfer() { cancelTouch(); }
    boolean isCancelled();
}
