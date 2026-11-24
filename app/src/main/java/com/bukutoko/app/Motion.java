package com.bukutoko.app;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Gerak kartu pada isi layar:
 * - reveal: tiap kartu muncul satu per satu (masuk dari bawah) saat layar dibuka dan saat digulir ke bawah
 * - tilt: kartu di tepi atas/bawah layar sedikit miring seperti lembaran
 * - sway: kartu bergoyang mengikuti hembusan angin
 */
public class Motion {
    private final SmoothScroll sv;
    private final ViewGroup content;
    private final float dn;
    private final List<View> pending = new ArrayList<>();
    private boolean tiltOn;

    public Motion(SmoothScroll sv, ViewGroup content, float dn) {
        this.sv = sv;
        this.content = content;
        this.dn = dn;
    }

    // ------------------------------------------------------------------ posisi
    private int topIn(View v) {
        int y = 0;
        View c = v;
        while (c != null && c != sv) {
            y += c.getTop();
            ViewParent p = c.getParent();
            c = p instanceof View ? (View) p : null;
        }
        return y;
    }

    private void normal(View v) {
        v.animate().cancel();
        v.setAlpha(1f);
        v.setTranslationY(0f);
        v.setScaleX(1f);
        v.setScaleY(1f);
    }

    private void hide(View v) {
        v.animate().cancel();
        v.setAlpha(0f);
        v.setTranslationY(34f * dn);
        v.setScaleX(0.94f);
        v.setScaleY(0.94f);
    }

    private void play(View v, long delay) {
        v.animate().cancel();
        v.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                .setStartDelay(delay).setDuration(440)
                .setInterpolator(new DecelerateInterpolator(1.8f)).start();
    }

    // ------------------------------------------------------------------ muncul satu per satu
    /** Lupakan kartu yang menunggu (dipanggil sebelum isi dibangun ulang). */
    public void clear() {
        pending.clear();
    }

    /** Tampilkan semua kartu seperti biasa. */
    public void showAll() {
        pending.clear();
        for (int i = 0; i < content.getChildCount(); i++) normal(content.getChildAt(i));
    }

    /** Dipanggil setelah isi layar selesai dibangun. */
    public void reveal(boolean on) {
        pending.clear();
        showAll();
        if (!on) return;
        final ViewTreeObserver vo = content.getViewTreeObserver();
        vo.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                ViewTreeObserver o = content.getViewTreeObserver();
                if (o.isAlive()) o.removeOnPreDrawListener(this);
                arm();
                return true;
            }
        });
    }

    private void arm() {
        int vb = sv.getScrollY() + sv.getHeight();
        int k = 0;
        for (int i = 0; i < content.getChildCount(); i++) {
            View c = content.getChildAt(i);
            if (c.getVisibility() == View.GONE) continue;
            hide(c);
            if (topIn(c) < vb) {
                play(c, 40L + 65L * Math.min(k, 9));
                k++;
            } else pending.add(c);
        }
    }

    /** Dipanggil saat digulir atau tata letak berubah: munculkan kartu yang sudah masuk layar. */
    public void check() {
        if (pending.isEmpty()) return;
        int vb = sv.getScrollY() + sv.getHeight() - Math.round(18 * dn);
        int k = 0;
        Iterator<View> it = pending.iterator();
        while (it.hasNext()) {
            View v = it.next();
            if (v.getParent() != content) {
                it.remove();
                continue;
            }
            if (topIn(v) < vb) {
                play(v, 70L * Math.min(k, 6));
                k++;
                it.remove();
            }
        }
    }

    // ------------------------------------------------------------------ miring di tepi layar
    public void tilt(boolean on) {
        tiltOn = on;
        int vh = sv.getHeight();
        int sy = sv.getScrollY();
        for (int i = 0; i < content.getChildCount(); i++) {
            View c = content.getChildAt(i);
            if (!on || vh <= 0 || c.getHeight() <= 0) {
                if (c.getRotationX() != 0f) c.setRotationX(0f);
                continue;
            }
            float center = topIn(c) + c.getHeight() / 2f - sy;
            float rel = (center - vh / 2f) / (vh / 2f);
            float e = (Math.abs(rel) - 0.5f) / 0.5f;
            if (e < 0f) e = 0f;
            if (e > 1.4f) e = 1.4f;
            float rot = (rel < 0 ? 1f : -1f) * e * 7f;
            c.setCameraDistance(9000f * dn);
            c.setPivotY(rel < 0 ? c.getHeight() : 0f);
            c.setRotationX(rot);
        }
    }

    // ------------------------------------------------------------------ bergoyang karena angin
    public void sway(float wind, boolean on) {
        for (int i = 0; i < content.getChildCount(); i++) {
            View c = content.getChildAt(i);
            if (!on) {
                if (c.getTranslationX() != 0f) c.setTranslationX(0f);
                if (c.getRotation() != 0f) c.setRotation(0f);
                continue;
            }
            float k = 1f - 0.07f * Math.min(i, 8);
            c.setTranslationX(wind * 7f * dn * k);
            c.setRotation(wind * 0.7f * k);
        }
    }
}
