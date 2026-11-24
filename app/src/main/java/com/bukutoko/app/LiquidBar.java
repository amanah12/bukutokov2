package com.bukutoko.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Region;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.Gravity;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Navbar kaca: kapsul tembus pandang berkilau, dengan penanda tab yang bergerak seperti cairan
 * (tepi depan melesat, tepi belakang menyusul, lalu menyatu).
 */
public class LiquidBar extends LinearLayout {
    public interface Pick {
        void on(int i);
    }

    private final Th th;
    private final float dn;
    private final TextView[] tabs;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rf = new RectF();
    private final Path body = new Path();
    private final float sh; // ruang bayangan di tepi

    private float iL, iR, indA;
    private int sel = -1;
    private ValueAnimator aL, aR, aA;
    private boolean laidOut;

    public LiquidBar(Context c, Th th, float dn, String[] labels, Pick pick) {
        super(c);
        this.th = th;
        this.dn = dn;
        this.sh = 8 * dn;
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setWillNotDraw(false);
        setClipChildren(false);
        int pad = Math.round(sh + 6 * dn);
        setPadding(pad, Math.round(sh * 0.4f + 6 * dn), pad, Math.round(sh + 6 * dn));
        tabs = new TextView[labels.length];
        for (int i = 0; i < labels.length; i++) {
            final int k = i;
            TextView t = new TextView(c);
            t.setText(labels[i]);
            t.setTextSize(12);
            t.setTypeface(th.font);
            t.setGravity(Gravity.CENTER);
            t.setSingleLine(true);
            t.setPadding(Math.round(2 * dn), Math.round(12 * dn), Math.round(2 * dn), Math.round(12 * dn));
            t.setTextColor(th.cardInk);
            t.setOnClickListener(v -> pick.on(k));
            addView(t, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
            tabs[i] = t;
        }
    }

    private int onInk() {
        return Color.luminance(th.c1) > 0.45f ? 0xFF111111 : 0xFFFFFFFF;
    }

    private static int a(int clr, int al) {
        return (clr & 0x00FFFFFF) | (Math.max(0, Math.min(255, al)) << 24);
    }

    /** Pilih tab i (-1 = tidak ada, mis. di layar turunan). */
    public void select(int i, boolean animate) {
        sel = i;
        for (int k = 0; k < tabs.length; k++) tabs[k].setTextColor(k == i ? onInk() : th.cardInk);
        if (aA != null) aA.cancel();
        if (i < 0) {
            fadeTo(0f, animate);
            return;
        }
        if (!laidOut || getWidth() == 0) {
            indA = 1f;
            return; // posisi dipasang di onLayout
        }
        float tl = tabs[i].getLeft() + 2 * dn, tr = tabs[i].getRight() - 2 * dn;
        if (!animate || indA < 0.05f) {
            cancelEdges();
            iL = tl;
            iR = tr;
            fadeTo(1f, animate);
            invalidate();
            return;
        }
        boolean right = tl > iL;
        Interpolator slow = new DecelerateInterpolator(1.4f);
        Interpolator fast = new OvershootInterpolator(1.5f);
        cancelEdges();
        aL = ValueAnimator.ofFloat(iL, tl);
        aL.setDuration(right ? 440 : 270);
        aL.setInterpolator(right ? slow : fast);
        aL.addUpdateListener(v -> {
            iL = (Float) v.getAnimatedValue();
            invalidate();
        });
        aR = ValueAnimator.ofFloat(iR, tr);
        aR.setDuration(right ? 270 : 440);
        aR.setInterpolator(right ? fast : slow);
        aR.addUpdateListener(v -> {
            iR = (Float) v.getAnimatedValue();
            invalidate();
        });
        aL.start();
        aR.start();
        fadeTo(1f, true);
    }

    private void cancelEdges() {
        if (aL != null) aL.cancel();
        if (aR != null) aR.cancel();
        aL = null;
        aR = null;
    }

    private void fadeTo(float to, boolean animate) {
        if (aA != null) aA.cancel();
        if (!animate) {
            indA = to;
            invalidate();
            return;
        }
        aA = ValueAnimator.ofFloat(indA, to);
        aA.setDuration(220);
        aA.addUpdateListener(v -> {
            indA = (Float) v.getAnimatedValue();
            invalidate();
        });
        aA.start();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        laidOut = true;
        boolean moving = aL != null && aL.isRunning();
        if (sel >= 0 && !moving) {
            iL = tabs[sel].getLeft() + 2 * dn;
            iR = tabs[sel].getRight() - 2 * dn;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelEdges();
        if (aA != null) aA.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas cv) {
        super.onDraw(cv);
        float bl = sh, bt = sh * 0.4f, br = getWidth() - sh, bb = getHeight() - sh;
        float h = bb - bt, r = h / 2f;
        rf.set(bl, bt, br, bb);
        body.reset();
        body.addRoundRect(rf, r, r, Path.Direction.CW);

        // bayangan lembut, hanya di luar badan kaca
        cv.save();
        cv.clipPath(body, Region.Op.DIFFERENCE);
        p.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 6; i++) {
            float g = i * 1.5f * dn;
            p.setColor(a(0xFF000000, th.dark ? 0x26 - i * 4 : 0x1C - i * 3));
            rf.set(bl - g * 0.5f, bt + 2 * dn - g * 0.1f + 2 * dn, br + g * 0.5f, bb + 2 * dn + g);
            cv.drawRoundRect(rf, r + g * 0.5f, r + g * 0.5f, p);
        }
        cv.restore();

        // badan kaca tembus pandang
        rf.set(bl, bt, br, bb);
        p.setStyle(Paint.Style.FILL);
        p.setShader(null);
        p.setColor(a(th.card, th.dark ? 0xB0 : 0xA8));
        cv.drawRoundRect(rf, r, r, p);

        cv.save();
        cv.clipPath(body);
        // kilau atas
        p.setShader(new LinearGradient(0, bt, 0, bt + h * 0.6f, a(0xFFFFFFFF, th.dark ? 0x30 : 0x80), a(0xFFFFFFFF, 0x00), Shader.TileMode.CLAMP));
        cv.drawRect(bl, bt, br, bt + h * 0.6f, p);
        // pantulan tipis di tepi bawah
        p.setShader(new LinearGradient(0, bb - h * 0.3f, 0, bb, a(0xFFFFFFFF, 0x00), a(0xFFFFFFFF, th.dark ? 0x18 : 0x40), Shader.TileMode.CLAMP));
        cv.drawRect(bl, bb - h * 0.3f, br, bb, p);
        p.setShader(null);
        cv.restore();

        // tepi kaca
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1f, 1.1f * dn));
        p.setColor(a(0xFFFFFFFF, th.dark ? 0x44 : 0xC0));
        rf.set(bl + 0.5f * dn, bt + 0.5f * dn, br - 0.5f * dn, bb - 0.5f * dn);
        cv.drawRoundRect(rf, r, r, p);

        drawIndicator(cv, bt, bb);
    }

    private void drawIndicator(Canvas cv, float bt, float bb) {
        if (indA <= 0.01f || iR <= iL) return;
        float inset = 5 * dn;
        float t = bt + inset, b = bb - inset;
        float rest = (tabs.length > 0 && sel >= 0) ? (tabs[sel].getWidth() - 4 * dn) : (iR - iL);
        float stretch = Math.abs((iR - iL) - rest);
        float squash = Math.min(stretch * 0.14f, 7 * dn);
        t += squash * 0.5f;
        b -= squash * 0.5f;
        float rr = (b - t) / 2f;

        // cahaya warna di bawah penanda
        p.setStyle(Paint.Style.FILL);
        p.setShader(null);
        p.setColor(a(th.c1, Math.round(0x40 * indA)));
        rf.set(iL + 2 * dn, t + 3 * dn, iR - 2 * dn, b + 3 * dn);
        cv.drawRoundRect(rf, rr, rr, p);

        // isi cairan
        rf.set(iL, t, iR, b);
        p.setColor(a(th.c1, Math.round(0xEA * indA)));
        cv.drawRoundRect(rf, rr, rr, p);

        // kilau dalam
        cv.save();
        Path pp = new Path();
        pp.addRoundRect(rf, rr, rr, Path.Direction.CW);
        cv.clipPath(pp);
        p.setShader(new LinearGradient(0, t, 0, t + (b - t) * 0.65f, a(0xFFFFFFFF, Math.round(0x90 * indA)), a(0xFFFFFFFF, 0x00), Shader.TileMode.CLAMP));
        cv.drawRect(iL, t, iR, t + (b - t) * 0.65f, p);
        p.setShader(null);
        cv.restore();

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1f, dn));
        p.setColor(a(0xFFFFFFFF, Math.round(0x90 * indA)));
        cv.drawRoundRect(rf, rr, rr, p);
    }
}
