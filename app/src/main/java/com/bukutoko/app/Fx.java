package com.bukutoko.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Lapisan efek di atas seluruh layar: salju, hujan, hembusan angin, daun, dan air mengalir di kaca.
 * Tidak menangkap sentuhan. Animasi hanya berjalan kalau ada efek yang menyala.
 */
public class Fx extends View {
    private final float dn;
    private final Random rnd = new Random();
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean snow, rain, wind, leaf, gust, glass, light;
    private int level = 2;

    /** Hembusan angin saat ini, kira-kira -0.7 .. 1.0 (kanan positif). */
    public float windX;
    private float t;
    private long last;
    private boolean running;
    private Runnable tickListener;

    private static class Flake {
        float x, y, r, vy, ph;
    }

    private static class Streak {
        float x, y, len, vy;
    }

    private static class Leaf {
        float x, y, s, rot, vr, ph, vph, vy;
        int clr;
    }

    private static class Wisp {
        float x, y, len, v, amp, ph, w;
    }

    private static class Drop {
        float x, y, y0, r, tr, vy, ph, age, wait;
        boolean slide;
    }

    private final List<Flake> flakes = new ArrayList<>();
    private final List<Streak> streaks = new ArrayList<>();
    private final List<Leaf> leaves = new ArrayList<>();
    private final List<Wisp> wisps = new ArrayList<>();
    private final List<Drop> drops = new ArrayList<>();
    private final Path leafPath = new Path();
    private static final int[] LEAF_CLR = {0xFF8BC34A, 0xFFFFB300, 0xFFE57339, 0xFF66A94B, 0xFFD4A017, 0xFFC9572E};

    public Fx(Context c, float dn) {
        super(c);
        this.dn = dn;
        setWillNotDraw(false);
        setClickable(false);
        setFocusable(false);
        leafPath.moveTo(0, -1);
        leafPath.quadTo(0.95f, -0.15f, 0, 1);
        leafPath.quadTo(-0.95f, -0.15f, 0, -1);
        leafPath.close();
    }

    public void setTick(Runnable r) {
        tickListener = r;
    }

    public void configure(boolean snow, boolean rain, boolean wind, boolean leaf, boolean gust, boolean glass, int level, boolean lightBg) {
        this.snow = snow;
        this.rain = rain;
        this.wind = wind;
        this.leaf = leaf;
        this.gust = gust;
        this.glass = glass;
        this.level = level < 1 ? 1 : Math.min(level, 3);
        this.light = lightBg;
        populate();
        boolean any = snow || rain || wind || leaf || gust || glass;
        setVisibility(any ? View.VISIBLE : View.GONE);
        if (any) start();
        else stop();
        invalidate();
    }

    private int n(int a, int b, int c) {
        return level == 1 ? a : level == 3 ? c : b;
    }

    private void populate() {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        flakes.clear();
        streaks.clear();
        leaves.clear();
        wisps.clear();
        drops.clear();
        if (snow) {
            for (int i = 0, m = n(40, 80, 150); i < m; i++) {
                Flake f = new Flake();
                f.r = (1.2f + rnd.nextFloat() * 2.8f) * dn;
                f.x = rnd.nextFloat() * w;
                f.y = rnd.nextFloat() * h;
                f.vy = (22 + rnd.nextFloat() * 40) * dn * (0.55f + f.r / (4 * dn));
                f.ph = rnd.nextFloat() * 6.28f;
                flakes.add(f);
            }
        }
        if (rain) {
            for (int i = 0, m = n(50, 100, 180); i < m; i++) {
                Streak s = new Streak();
                s.x = rnd.nextFloat() * (w + 80 * dn);
                s.y = rnd.nextFloat() * h;
                s.len = (12 + rnd.nextFloat() * 16) * dn;
                s.vy = (650 + rnd.nextFloat() * 400) * dn;
                streaks.add(s);
            }
        }
        if (leaf) {
            for (int i = 0, m = n(6, 12, 22); i < m; i++) leaves.add(newLeaf(w, h, true));
        }
        if (wind) {
            for (int i = 0, m = n(2, 4, 7); i < m; i++) {
                Wisp ws = new Wisp();
                resetWisp(ws, w, h, true);
                wisps.add(ws);
            }
        }
        if (glass) {
            for (int i = 0, m = n(14, 26, 40); i < m; i++) {
                Drop d = new Drop();
                resetDrop(d, w, h, true);
                drops.add(d);
            }
        }
    }

    private Leaf newLeaf(int w, int h, boolean any) {
        Leaf l = new Leaf();
        l.s = (7 + rnd.nextFloat() * 8) * dn;
        l.x = any ? rnd.nextFloat() * w : -30 * dn;
        l.y = any ? rnd.nextFloat() * h : rnd.nextFloat() * h * 0.7f;
        l.rot = rnd.nextFloat() * 360;
        l.vr = (rnd.nextFloat() - 0.5f) * 240;
        l.ph = rnd.nextFloat() * 6.28f;
        l.vph = 1.5f + rnd.nextFloat() * 2.5f;
        l.vy = (22 + rnd.nextFloat() * 30) * dn;
        l.clr = LEAF_CLR[rnd.nextInt(LEAF_CLR.length)];
        return l;
    }

    private void resetWisp(Wisp ws, int w, int h, boolean any) {
        ws.len = (180 + rnd.nextFloat() * 160) * dn;
        ws.v = (480 + rnd.nextFloat() * 420) * dn;
        ws.amp = (6 + rnd.nextFloat() * 14) * dn;
        ws.ph = rnd.nextFloat() * 6.28f;
        ws.w = 0.012f + rnd.nextFloat() * 0.01f;
        ws.y = h * (0.08f + rnd.nextFloat() * 0.8f);
        ws.x = any ? -ws.len - rnd.nextFloat() * w * 1.5f : -ws.len - rnd.nextFloat() * w * 0.8f;
    }

    private void resetDrop(Drop d, int w, int h, boolean any) {
        d.x = rnd.nextFloat() * w;
        d.y = rnd.nextFloat() * h * 0.9f;
        d.y0 = d.y;
        d.tr = (1.4f + rnd.nextFloat() * rnd.nextFloat() * 5.5f) * dn;
        d.r = any ? d.tr * (0.4f + rnd.nextFloat() * 0.6f) : d.tr * 0.35f;
        d.vy = (18 + rnd.nextFloat() * 55) * dn;
        d.ph = rnd.nextFloat() * 6.28f;
        d.age = 0;
        d.wait = 1 + rnd.nextFloat() * 9;
        d.slide = false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        populate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (snow || rain || wind || leaf || gust || glass) start();
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onWindowVisibilityChanged(int v) {
        super.onWindowVisibilityChanged(v);
        if (v == View.VISIBLE) {
            if (snow || rain || wind || leaf || gust || glass) start();
        } else stop();
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            long now = System.nanoTime();
            float dt = last == 0 ? 0.016f : (now - last) / 1e9f;
            last = now;
            if (dt > 0.05f) dt = 0.05f;
            update(dt);
            if (tickListener != null) tickListener.run();
            invalidate();
            postOnAnimation(this);
        }
    };

    private void start() {
        if (running || !isAttachedToWindow()) return;
        running = true;
        last = 0;
        postOnAnimation(tick);
    }

    private void stop() {
        running = false;
        removeCallbacks(tick);
        windX = 0;
        if (tickListener != null) tickListener.run();
    }

    private void update(float dt) {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        t += dt;
        boolean breezy = wind || gust || leaf;
        float base = (float) (Math.sin(t * 0.45) * 0.55 + Math.sin(t * 1.1 + 1.3) * 0.3 + 0.15);
        windX = breezy ? base : base * 0.2f;
        for (Flake f : flakes) {
            f.y += f.vy * dt;
            f.x += ((float) Math.sin(t * 1.2 + f.ph) * 14 * dn + windX * 70 * dn) * dt;
            if (f.y > h + 8 * dn) {
                f.y = -8 * dn;
                f.x = rnd.nextFloat() * w;
            }
            if (f.x > w + 10 * dn) f.x = -10 * dn;
            else if (f.x < -10 * dn) f.x = w + 10 * dn;
        }
        for (Streak s : streaks) {
            s.y += s.vy * dt;
            s.x += (60 + windX * 160) * dn * dt;
            if (s.y > h + 30 * dn) {
                s.y = -30 * dn;
                s.x = rnd.nextFloat() * (w + 80 * dn) - 40 * dn;
            }
            if (s.x > w + 40 * dn) s.x = -40 * dn;
            else if (s.x < -40 * dn) s.x = w + 40 * dn;
        }
        for (Leaf l : leaves) {
            l.x += (35 + windX * 150) * dn * dt;
            l.y += (l.vy + (float) Math.sin(t * 1.7 + l.ph) * 30 * dn) * dt;
            l.rot += l.vr * dt * (0.6f + Math.abs(windX));
            l.ph += l.vph * dt;
            if (l.x > w + 30 * dn || l.y > h + 30 * dn || l.x < -60 * dn) {
                Leaf n = newLeaf(w, h, false);
                l.x = n.x;
                l.y = n.y;
                l.clr = n.clr;
                l.s = n.s;
            }
        }
        for (Wisp ws : wisps) {
            ws.x += ws.v * (0.6f + Math.max(0, windX)) * dt;
            if (ws.x - ws.len > w) resetWisp(ws, w, h, false);
        }
        for (Drop d : drops) {
            d.age += dt;
            if (!d.slide) {
                if (d.r < d.tr) d.r += (d.tr - d.r) * Math.min(1f, dt * 1.2f);
                else d.tr = Math.min(7 * dn, d.tr + 0.25f * dn * dt);
                if (d.age > d.wait && d.tr > 2.8f * dn) {
                    d.slide = true;
                    d.y0 = d.y;
                } else if (d.age > 16) resetDrop(d, w, h, false);
            } else {
                float k = (float) Math.sin(t * 2.3 + d.ph);
                float sp = d.vy * (0.15f + 0.85f * Math.max(0f, k)) * (0.6f + d.r / (6 * dn));
                d.y += sp * dt;
                d.r = Math.max(d.r * (1 - 0.02f * dt), 1.6f * dn);
                if (d.y > h + 20 * dn || d.y - d.y0 > h * 0.65f && rnd.nextFloat() < 0.01f) resetDrop(d, w, h, false);
            }
        }
    }

    @Override
    protected void onDraw(Canvas cv) {
        super.onDraw(cv);
        int w = getWidth(), h = getHeight();
        if (glass) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(light ? 0x0A7F93A8 : 0x0CFFFFFF);
            cv.drawRect(0, 0, w, h, p);
            drawDrops(cv);
        }
        if (wind) drawWisps(cv);
        if (snow) {
            p.setStyle(Paint.Style.FILL);
            for (Flake f : flakes) {
                if (light) {
                    p.setColor(0xB8E8F1FA);
                    cv.drawCircle(f.x, f.y, f.r, p);
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(Math.max(1f, dn * 0.8f));
                    p.setColor(0x669FB6D2);
                    cv.drawCircle(f.x, f.y, f.r, p);
                    p.setStyle(Paint.Style.FILL);
                } else {
                    p.setColor(0xD8FFFFFF);
                    cv.drawCircle(f.x, f.y, f.r, p);
                }
            }
        }
        if (rain) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeWidth(1.4f * dn);
            p.setColor(light ? 0x907FA8D8 : 0x88CFE6FF);
            for (Streak s : streaks) {
                float vx = (60 + windX * 160) * dn, vy = s.vy;
                float m = (float) Math.hypot(vx, vy);
                cv.drawLine(s.x, s.y, s.x - vx / m * s.len, s.y - vy / m * s.len, p);
            }
            p.setStrokeCap(Paint.Cap.BUTT);
        }
        if (leaf) {
            for (Leaf l : leaves) {
                cv.save();
                cv.translate(l.x, l.y);
                cv.rotate(l.rot);
                cv.scale(l.s * 0.62f, l.s * (0.3f + 0.7f * Math.abs((float) Math.cos(l.ph))));
                p.setStyle(Paint.Style.FILL);
                p.setColor(l.clr);
                cv.drawPath(leafPath, p);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(0.12f);
                p.setColor(0x66000000);
                cv.drawLine(0, -0.9f, 0, 0.9f, p);
                cv.restore();
            }
        }
    }

    private void drawWisps(Canvas cv) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(1.6f * dn);
        int base = light ? 0x5A7A8FA6 : 0x66FFFFFF;
        int baseA = (base >>> 24);
        int rgb = base & 0x00FFFFFF;
        for (Wisp ws : wisps) {
            int N = 16;
            float px = 0, py = 0;
            for (int i = 0; i <= N; i++) {
                float f = i / (float) N;
                float x = ws.x - ws.len * (1 - f);
                float y = ws.y + (float) Math.sin(x * ws.w + ws.ph + t * 2) * ws.amp * (0.4f + f);
                if (i > 0) {
                    float a = f * f * (1 - 0.15f * f);
                    p.setColor(((int) (baseA * a) << 24) | rgb);
                    cv.drawLine(px, py, x, y, p);
                }
                px = x;
                py = y;
            }
        }
        p.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawDrops(Canvas cv) {
        for (Drop d : drops) {
            float fade = Math.min(1f, d.age * 1.5f);
            if (d.slide) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeCap(Paint.Cap.ROUND);
                p.setStrokeWidth(Math.max(1f, d.r * 0.55f));
                p.setColor(light ? 0x1F5B7391 : 0x26FFFFFF);
                cv.drawLine(d.x, d.y0, d.x, d.y - d.r * 0.3f, p);
                p.setStrokeCap(Paint.Cap.BUTT);
            }
            float ry = d.slide ? d.r * 1.3f : d.r;
            p.setStyle(Paint.Style.FILL);
            p.setColor(((int) (0x2C * fade) << 24) | 0xFFFFFF);
            cv.drawOval(d.x - d.r, d.y - ry, d.x + d.r, d.y + ry, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Math.max(1f, dn * 0.9f));
            p.setColor(light ? ((int) (0x66 * fade) << 24) | 0x3A506B : ((int) (0x66 * fade) << 24) | 0xFFFFFF);
            cv.drawOval(d.x - d.r, d.y - ry, d.x + d.r, d.y + ry, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(((int) (0xC8 * fade) << 24) | 0xFFFFFF);
            cv.drawCircle(d.x - d.r * 0.32f, d.y - ry * 0.38f, Math.max(0.8f * dn, d.r * 0.26f), p);
        }
    }
}
