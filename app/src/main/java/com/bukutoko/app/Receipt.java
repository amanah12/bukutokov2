package com.bukutoko.app;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Gambar struk (setara canvas di versi web) + versi teks. */
public class Receipt {
    private static final int W = 480, P = 28;

    public static JSONObject cell(String t, String a, Object b, int s) {
        JSONObject o = new JSONObject();
        Store.put(o, "t", t);
        if (a != null) Store.put(o, "a", a);
        if (b != null) Store.put(o, "b", b);
        if (s > 0) Store.put(o, "s", s);
        return o;
    }

    public static JSONObject c(String a, int size, boolean bold) {
        return cell("c", a, bold ? (Object) 1 : null, size);
    }

    public static JSONObject kv(String a, String b) {
        return cell("kv", a, b, 0);
    }

    public static JSONObject lr(String a, String b) {
        return cell("lr", a, b, 0);
    }

    public static JSONObject tx(String a) {
        return cell("tx", a, null, 0);
    }

    public static JSONObject sep() {
        return cell("sep", null, null, 0);
    }

    public static JSONObject big(String b) {
        return cell("big", null, b, 0);
    }

    public static JSONObject logo() {
        return cell("logo", null, null, 0);
    }

    private static List<String> wrap(Paint p, String text, float mw) {
        List<String> out = new ArrayList<>();
        for (String par : text.split("\n", -1)) {
            String ln = "";
            for (String w0 : par.split(" ", -1)) {
                String w = w0;
                while (p.measureText(w) > mw) {
                    int k = w.length();
                    while (k > 1 && p.measureText(w.substring(0, k)) > mw) k--;
                    if (!ln.isEmpty()) {
                        out.add(ln);
                        ln = "";
                    }
                    out.add(w.substring(0, k));
                    w = w.substring(k);
                }
                String t2 = ln.isEmpty() ? w : ln + " " + w;
                if (!ln.isEmpty() && p.measureText(t2) > mw) {
                    out.add(ln);
                    ln = w;
                } else ln = t2;
            }
            out.add(ln);
        }
        return out;
    }

    private static class Item {
        JSONObject c;
        float y, h;
        List<String> ls;
        int size;
        boolean bold;
    }

    private static void txt(Canvas cv, Paint p, String s, float x, float yTop) {
        cv.drawText(s, x, yTop - p.ascent(), p);
    }

    public static Bitmap render(JSONArray M, Bitmap logo) {
        return render(M, logo, null);
    }

    public static Bitmap render(JSONArray M, Bitmap logo, Bitmap qr) {
        Typeface reg = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL);
        Typeface bld = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        List<Item> items = new ArrayList<>();
        float y = P;
        for (JSONObject c : Store.list(M)) {
            Item it = new Item();
            it.c = c;
            it.y = y;
            String t = Store.str(c, "t");
            switch (t) {
                case "logo":
                    it.h = logo != null ? 92 : 0;
                    break;
                case "c": {
                    int s = c.has("s") ? (int) Store.num(c, "s") : 20;
                    if (s <= 0) s = 20;
                    it.size = s;
                    it.bold = c.has("b") && Store.num(c, "b") == 1;
                    p.setTypeface(it.bold ? bld : reg);
                    p.setTextSize(s);
                    it.ls = wrap(p, Store.str(c, "a"), W - 2 * P);
                    it.h = it.ls.size() * Math.round(s * 1.5f) + 4;
                    break;
                }
                case "sep":
                    it.h = 24;
                    break;
                case "kv":
                    p.setTypeface(bld);
                    p.setTextSize(20);
                    it.ls = wrap(p, Store.str(c, "b"), W - P - (P + 150));
                    it.h = it.ls.size() * 30 + 2;
                    break;
                case "tx":
                    p.setTypeface(bld);
                    p.setTextSize(20);
                    it.ls = wrap(p, Store.str(c, "a"), W - 2 * P);
                    it.h = it.ls.size() * 28;
                    break;
                case "lr":
                    it.h = 30;
                    break;
                case "big":
                    it.h = 54;
                    break;
                case "qr":
                    it.h = qr != null ? 250 : 0;
                    break;
                default:
                    it.h = 0;
            }
            y += it.h;
            items.add(it);
        }
        int H = Math.round(y + P);
        Bitmap bm = Bitmap.createBitmap(W * 2, H * 2, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bm);
        cv.scale(2, 2);
        cv.drawColor(Color.WHITE);
        int ink = 0xFF111111;
        for (Item it : items) {
            JSONObject c = it.c;
            String t = Store.str(c, "t");
            p.setColor(ink);
            p.setTextAlign(Paint.Align.LEFT);
            p.setStyle(Paint.Style.FILL);
            switch (t) {
                case "logo":
                    if (logo != null) {
                        int m = Math.min(logo.getWidth(), logo.getHeight());
                        Rect src = new Rect((logo.getWidth() - m) / 2, (logo.getHeight() - m) / 2,
                                (logo.getWidth() - m) / 2 + m, (logo.getHeight() - m) / 2 + m);
                        RectF dst = new RectF(W / 2f - 40, it.y + 4, W / 2f + 40, it.y + 84);
                        cv.drawBitmap(logo, src, dst, p);
                    }
                    break;
                case "c": {
                    p.setTypeface(it.bold ? bld : reg);
                    p.setTextSize(it.size);
                    p.setTextAlign(Paint.Align.CENTER);
                    int lh = Math.round(it.size * 1.5f);
                    for (int i = 0; i < it.ls.size(); i++) txt(cv, p, it.ls.get(i), W / 2f, it.y + 2 + i * lh);
                    break;
                }
                case "sep":
                    for (float x = P; x < W - P; x += 10) cv.drawRect(x, it.y + 11, x + 5, it.y + 13, p);
                    break;
                case "kv":
                    p.setTypeface(reg);
                    p.setTextSize(18);
                    p.setColor(0xFF555555);
                    txt(cv, p, Store.str(c, "a"), P, it.y + 3);
                    p.setColor(ink);
                    p.setTypeface(bld);
                    p.setTextSize(20);
                    for (int i = 0; i < it.ls.size(); i++) txt(cv, p, it.ls.get(i), P + 150, it.y + i * 30);
                    break;
                case "tx":
                    p.setTypeface(bld);
                    p.setTextSize(20);
                    for (int i = 0; i < it.ls.size(); i++) txt(cv, p, it.ls.get(i), P, it.y + i * 28);
                    break;
                case "lr":
                    p.setTypeface(reg);
                    p.setTextSize(20);
                    txt(cv, p, Store.str(c, "a"), P, it.y + 3);
                    p.setTypeface(bld);
                    p.setTextAlign(Paint.Align.RIGHT);
                    txt(cv, p, Store.str(c, "b"), W - P, it.y + 3);
                    break;
                case "big":
                    p.setTypeface(bld);
                    p.setTextSize(24);
                    txt(cv, p, "TOTAL", P, it.y + 12);
                    p.setTextSize(30);
                    p.setTextAlign(Paint.Align.RIGHT);
                    txt(cv, p, Store.str(c, "b"), W - P, it.y + 9);
                    break;
                case "qr":
                    if (qr != null) {
                        float k = Math.min(200f / qr.getWidth(), 200f / qr.getHeight());
                        float qw = qr.getWidth() * k, qh = qr.getHeight() * k;
                        cv.drawBitmap(qr, null, new RectF(W / 2f - qw / 2, it.y + 4, W / 2f + qw / 2, it.y + 4 + qh), p);
                        p.setTypeface(reg);
                        p.setTextSize(16);
                        p.setColor(ink);
                        p.setTextAlign(Paint.Align.CENTER);
                        txt(cv, p, "Scan untuk bayar (QRIS)", W / 2f, it.y + 216);
                    }
                    break;
                default:
                    break;
            }
        }
        return bm;
    }

    public static String text(JSONArray M) {
        StringBuilder sb = new StringBuilder();
        for (JSONObject c : Store.list(M)) {
            String t = Store.str(c, "t"), s = null;
            switch (t) {
                case "c":
                case "tx":
                    s = Store.str(c, "a");
                    break;
                case "kv":
                    s = Store.str(c, "a") + ": " + Store.str(c, "b");
                    break;
                case "lr":
                    s = Store.str(c, "a") + "  " + Store.str(c, "b");
                    break;
                case "big":
                    s = "TOTAL  " + Store.str(c, "b");
                    break;
                case "sep":
                    s = "--------------------";
                    break;
                default:
                    break;
            }
            if (s != null && !s.isEmpty()) {
                if (sb.length() > 0) sb.append('\n');
                sb.append(s);
            }
        }
        return sb.toString();
    }
}
