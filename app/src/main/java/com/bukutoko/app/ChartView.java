package com.bukutoko.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import java.util.Locale;

/** Grafik masuk/keluar (balok atau garis), setara SVG di versi web (kanvas 320x190). */
public class ChartView extends View {
    private final String[] labels;
    private final double[] ins, outs;
    private final int c1, c2, ink;
    private final boolean line;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    public ChartView(Context c, String[] labels, double[] ins, double[] outs, int c1, int c2, int ink, boolean line) {
        super(c);
        this.labels = labels;
        this.ins = ins;
        this.outs = outs;
        this.c1 = c1;
        this.c2 = c2;
        this.ink = ink;
        this.line = line;
    }

    @Override
    protected void onMeasure(int w, int h) {
        int ww = MeasureSpec.getSize(w);
        setMeasuredDimension(ww, Math.round(ww * 190f / 320f));
    }

    private static String sh(double n) {
        if (n >= 1e6) {
            String s = String.format(Locale.US, "%.1f", n / 1e6);
            if (s.endsWith(".0")) s = s.substring(0, s.length() - 2);
            return s + "jt";
        }
        if (n >= 1e3) return Math.round(n / 1e3) + "rb";
        return String.valueOf(Math.round(n));
    }

    @Override
    protected void onDraw(Canvas cv) {
        int sl = labels.length;
        if (sl == 0 || getWidth() == 0) return;
        final float W = 320f, H = 190f, L = 6f, T = 16f, Bm = 24f;
        float pw = W - L - 6f, ph = H - T - Bm, gw = pw / sl;
        double mx = 1;
        for (int i = 0; i < sl; i++) mx = Math.max(mx, Math.max(ins[i], outs[i]));

        cv.save();
        float s = getWidth() / W;
        cv.scale(s, s);

        p.setStyle(Paint.Style.FILL);
        p.setColor(ink);
        p.setTextSize(9f);
        p.setTextAlign(Paint.Align.LEFT);
        cv.drawText("maks " + sh(mx), L, 10f, p);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2f);
        p.setColor(ink);
        cv.drawLine(L, T + ph, W - 6f, T + ph, p);

        if (line) {
            for (int f = 0; f < 2; f++) {
                double[] d = f == 0 ? ins : outs;
                int col = f == 0 ? c1 : c2;
                Path path = new Path();
                for (int j = 0; j < sl; j++) {
                    float x = L + gw * j + gw / 2f, y = T + ph - (float) (d[j] / mx * ph);
                    if (j == 0) path.moveTo(x, y);
                    else path.lineTo(x, y);
                }
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeJoin(Paint.Join.ROUND);
                p.setColor(ink);
                p.setStrokeWidth(6f);
                cv.drawPath(path, p);
                p.setColor(col);
                p.setStrokeWidth(3f);
                cv.drawPath(path, p);
                for (int j = 0; j < sl; j++) {
                    float x = L + gw * j + gw / 2f, y = T + ph - (float) (d[j] / mx * ph);
                    p.setStyle(Paint.Style.FILL);
                    p.setColor(col);
                    cv.drawRect(x - 4f, y - 4f, x + 4f, y + 4f, p);
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(2f);
                    p.setColor(ink);
                    cv.drawRect(x - 4f, y - 4f, x + 4f, y + 4f, p);
                }
            }
        } else {
            float bw = Math.min(22f, gw / 2f - 2f);
            for (int j = 0; j < sl; j++) {
                for (int f = 0; f < 2; f++) {
                    double v = f == 0 ? ins[j] : outs[j];
                    float h = (float) (v / mx * ph);
                    float x = L + gw * j + gw / 2f + (f == 0 ? -bw : 0f);
                    float y = T + ph - h;
                    float hh = Math.max(h, v > 0 ? 2f : 0f);
                    p.setStyle(Paint.Style.FILL);
                    p.setColor(f == 0 ? c1 : c2);
                    cv.drawRect(x, y, x + bw, y + hh, p);
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(2f);
                    p.setColor(ink);
                    cv.drawRect(x, y, x + bw, y + hh, p);
                }
            }
        }

        int st = (int) Math.ceil(sl / 6.0);
        p.setStyle(Paint.Style.FILL);
        p.setColor(ink);
        p.setTextSize(9f);
        p.setTextAlign(Paint.Align.CENTER);
        for (int j = 0; j < sl; j += st) cv.drawText(labels[j], L + gw * j + gw / 2f, H - 8f, p);
        cv.restore();
    }
}
