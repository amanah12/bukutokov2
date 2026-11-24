package com.bukutoko.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;
import android.widget.ScrollView;

/** ScrollView dengan lemparan lebih panjang, pelapor posisi gulir, dan gulir halus ke posisi tertentu. */
public class SmoothScroll extends ScrollView {
    public interface OnScroll {
        void on(int y, int oldY);
    }

    private OnScroll cb;
    private ValueAnimator anim;

    public SmoothScroll(Context c) {
        super(c);
        setOverScrollMode(OVER_SCROLL_ALWAYS);
        setFillViewport(true);
        setClipChildren(false);
        setClipToPadding(false);
    }

    public void setOnScroll(OnScroll l) {
        cb = l;
    }

    @Override
    public void fling(int velocityY) {
        cancelAnim();
        super.fling((int) (velocityY * 1.15f));
    }

    @Override
    protected void onScrollChanged(int l, int t, int ol, int ot) {
        super.onScrollChanged(l, t, ol, ot);
        if (cb != null) cb.on(t, ot);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        if (cb != null) cb.on(getScrollY(), getScrollY());
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) cancelAnim();
        return super.onInterceptTouchEvent(e);
    }

    private void cancelAnim() {
        if (anim != null) {
            anim.cancel();
            anim = null;
        }
    }

    public void smoothTop() {
        smoothTo(0);
    }

    /** Gulir ke y dengan perlambatan lembut. */
    public void smoothTo(int y) {
        cancelAnim();
        int child = getChildCount() > 0 ? getChildAt(0).getHeight() : 0;
        int max = Math.max(0, child - getHeight());
        final int to = Math.max(0, Math.min(y, max));
        final int from = getScrollY();
        if (from == to) return;
        long d = Math.min(650, 260 + Math.abs(to - from) / 4);
        anim = ValueAnimator.ofInt(from, to);
        anim.setDuration(d);
        anim.setInterpolator(new DecelerateInterpolator(2f));
        anim.addUpdateListener(a -> scrollTo(0, (Integer) a.getAnimatedValue()));
        anim.start();
    }
}
