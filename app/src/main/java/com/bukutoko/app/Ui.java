package com.bukutoko.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.InputType;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Pembangun tampilan (tanpa XML) + gaya tema. */
public class Ui {
    public static final int NORMAL = 0, C1 = 1, C2 = 2, PLAIN = 3, DIALOG = 4;
    public static final int P = 0, W = 1, R = 2;
    public static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT, WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;
    public static final int INK_TEXT = InputType.TYPE_CLASS_TEXT;
    public static final int INK_NAME = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS;
    public static final int INK_NUM = InputType.TYPE_CLASS_NUMBER;

    public final Context c;
    public final Th th;
    public final float dn;

    public Ui(Context c, Th th) {
        this.c = c;
        this.th = th;
        this.dn = c.getResources().getDisplayMetrics().density;
    }

    public int dp(float v) {
        return Math.round(v * dn);
    }

    public int screenW() {
        DisplayMetrics m = c.getResources().getDisplayMetrics();
        return m.widthPixels;
    }

    public void toast(String s) {
        Toast.makeText(c, s, Toast.LENGTH_LONG).show();
    }

    public void confirm(String msg, Runnable yes) {
        new AlertDialog.Builder(c).setMessage(msg)
                .setPositiveButton("OK", (d, w) -> yes.run())
                .setNegativeButton("Batal", null).show();
    }

    // ------------------------------------------------------------------ drawable
    public static class Box extends Drawable {
        public int fill, g1, g2, border, sdClr;
        public boolean grad, dash, topOnly, shadowOn = true, gloss, sketch, wide;
        public float r, bw, sx, sy, dn = 1;
        public float[] cr;
        public String shape = "";
        public int tint, hole;
        public float ins, padL, padX, padB; // dp, tambahan ruang untuk bentuk dekoratif
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        public float ex() {
            return wide ? 0 : sdClr != 0 ? sx * dn : 0;
        }

        public float ey() {
            return sdClr != 0 ? sy * dn : 0;
        }

        public int extL() {
            return Math.round((ins + padL + padX) * dn);
        }

        public int extR() {
            return Math.round((ins + padX) * dn);
        }

        public int extT() {
            return Math.round(ins * dn);
        }

        public int extB() {
            return Math.round((ins + padB) * dn);
        }

        public Box setShape(String s) {
            shape = s == null ? "" : s;
            ins = 0;
            padL = 0;
            padX = 0;
            padB = 0;
            switch (shape) {
                case "solatip": ins = 14; break;
                case "daun": ins = 10; padX = 2; break;
                case "sobek": ins = 8; padX = 2; break;
                case "lipat": ins = 6; break;
                case "kertas": ins = 6; padL = 34; break;
                case "memo": ins = 6; break;
                case "jahit": ins = 4; padX = 2; break;
                case "hexagon": padX = 14; break;
                case "lancip": padX = 8; break;
                case "miring": padX = 10; break;
                case "acak": ins = 4; padX = 6; break;
                case "lendir": padB = 14; break;
                case "cat": ins = 4; padX = 12; break;
                case "tape": padX = 6; break;
                default: break;
            }
            return this;
        }

        private void shape(RectF rc) {
            path.reset();
            if (cr != null) {
                float m = Math.min(rc.width(), rc.height()) / 2f;
                float[] rr = new float[8];
                for (int i = 0; i < 4; i++) {
                    float v = Math.min(cr[i] * dn, m);
                    rr[i * 2] = v;
                    rr[i * 2 + 1] = v;
                }
                path.addRoundRect(rc, rr, Path.Direction.CW);
                return;
            }
            float rr = Math.min(r * dn, Math.min(rc.width(), rc.height()) / 2f);
            if (rr <= 0) path.addRect(rc, Path.Direction.CW);
            else if (topOnly) path.addRoundRect(rc, new float[]{rr, rr, rr, rr, 0, 0, 0, 0}, Path.Direction.CW);
            else path.addRoundRect(rc, rr, rr, Path.Direction.CW);
        }

        private static int dark(int c, float f) {
            return Th.mix(c, 0xFF000000, f);
        }

        private void gloss(Canvas cv, RectF body) {
            cv.save();
            cv.clipPath(path);
            p.setStyle(Paint.Style.FILL);
            p.setShader(new LinearGradient(0, body.top, 0, body.bottom,
                    new int[]{0x45FFFFFF, 0x10FFFFFF, 0x00000000, 0x1C000000},
                    new float[]{0f, 0.48f, 0.52f, 1f}, Shader.TileMode.CLAMP));
            p.setColor(0xFF000000);
            cv.drawRect(body, p);
            p.setShader(null);
            cv.restore();
        }

        @Override
        public void draw(Canvas cv) {
            Rect b = getBounds();
            if (!shape.isEmpty()) {
                drawShape(cv, b);
                return;
            }
            if (sketch) {
                drawSketchCached(cv, b);
                return;
            }
            if (wide) {
                drawFlatWide(cv, b);
                return;
            }
            float ex = ex(), ey = ey();
            RectF body = new RectF(b.left, b.top, b.right - ex, b.bottom - ey);
            if (body.width() <= 0 || body.height() <= 0) return;
            if (sdClr != 0 && shadowOn && (ex > 0 || ey > 0)) {
                RectF sh = new RectF(body);
                sh.offset(ex, ey);
                // bayangan hanya di luar badan, supaya isi tembus pandang tidak jadi gelap
                cv.save();
                shape(body);
                cv.clipPath(path, android.graphics.Region.Op.DIFFERENCE);
                shape(sh);
                p.setShader(null);
                p.setPathEffect(null);
                p.setStyle(Paint.Style.FILL);
                p.setColor(sdClr);
                cv.drawPath(path, p);
                cv.restore();
            }
            shape(body);
            p.setPathEffect(null);
            p.setStyle(Paint.Style.FILL);
            if (grad) {
                p.setShader(new LinearGradient(body.left, body.top, body.right, body.bottom, g1, g2, Shader.TileMode.CLAMP));
                p.setColor(0xFF000000);
                cv.drawPath(path, p);
                p.setShader(null);
            } else if (fill != 0) {
                p.setColor(fill);
                cv.drawPath(path, p);
            }
            if (gloss && (fill != 0 || grad)) gloss(cv, body);
            if (bw > 0 && border != 0) {
                float w = bw * dn;
                RectF in = new RectF(body);
                in.inset(w / 2f, w / 2f);
                if (in.width() <= 0 || in.height() <= 0) in = new RectF(body);
                shape(in);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(w);
                p.setColor(border);
                if (dash) p.setPathEffect(new DashPathEffect(new float[]{dn * 7, dn * 5}, 0));
                cv.drawPath(path, p);
                p.setPathEffect(null);
            }
        }


        // ---------------------------------------------------------- gaya coretan tangan
        /** Garis tangan: titik di sepanjang [src] digeser acak halus ke samping, lalu dihaluskan. */
        private Path wobble(Path src, java.util.Random rnd, float amp, float step, float startFrac, float span) {
            android.graphics.PathMeasure pm = new android.graphics.PathMeasure(src, true);
            float len = pm.getLength();
            Path out = new Path();
            if (len <= 0) return out;
            int cnt = Math.max(4, Math.round(len * span / step));
            float[] px = new float[cnt + 1], py = new float[cnt + 1];
            float[] pos = new float[2], tan = new float[2];
            float noise = 0;
            for (int i = 0; i <= cnt; i++) {
                float dist = (startFrac * len + len * span * i / cnt) % len;
                pm.getPosTan(dist, pos, tan);
                noise = noise * 0.55f + (rnd.nextFloat() - 0.5f) * 2f * amp;
                px[i] = pos[0] - tan[1] * noise;
                py[i] = pos[1] + tan[0] * noise;
            }
            out.moveTo(px[0], py[0]);
            for (int i = 1; i <= cnt; i++) out.quadTo(px[i - 1], py[i - 1], (px[i - 1] + px[i]) / 2f, (py[i - 1] + py[i]) / 2f);
            out.lineTo(px[cnt], py[cnt]);
            return out;
        }

        private Path skOut, skOut2, skFill;
        private float skW = -1, skH = -1, skL = -1, skT = -1, skAngle;
        private final long skSeed = (long) (Math.random() * 1e9);

        private void drawSketch(Canvas cv, Rect b) {
            float ex = ex(), ey = ey(), pad = 2.5f * dn;
            RectF body = new RectF(b.left + pad, b.top + pad, b.right - ex - pad, b.bottom - ey - pad);
            if (body.width() <= 4 * dn || body.height() <= 4 * dn) return;
            if (skOut == null || skW != body.width() || skH != body.height() || skL != b.left || skT != b.top) {
                java.util.Random rnd = new java.util.Random(skSeed);
                shape(body);
                Path base = new Path(path);
                float amp = 1.25f * dn;
                skOut = wobble(base, rnd, amp, 13 * dn, rnd.nextFloat(), 1.04f);
                skOut2 = wobble(base, rnd, amp * 1.15f, 17 * dn, rnd.nextFloat(), 0.93f);
                skFill = wobble(base, rnd, amp * 1.3f, 15 * dn, rnd.nextFloat(), 1f);
                skFill.close();
                skAngle = (rnd.nextFloat() - 0.5f) * 0.7f;
                skW = body.width();
                skH = body.height();
                skL = b.left;
                skT = b.top;
            }
            cv.save();
            cv.rotate(skAngle, body.centerX(), body.centerY());
            p.setShader(null);
            p.setPathEffect(null);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
            if (sdClr != 0 && shadowOn && (ex > 0 || ey > 0)) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(Math.max(1f, bw * dn * 0.8f));
                p.setColor(Th.alpha(sdClr, 0x70));
                cv.save();
                cv.translate(ex, ey);
                cv.drawPath(skOut2, p);
                cv.restore();
                p.setStrokeWidth(1.1f * dn);
                p.setColor(Th.alpha(sdClr, 0x60));
                float rr = Math.min(r * dn, Math.min(body.width(), body.height()) / 2f);
                for (float x = body.left + rr + 4 * dn; x < body.right - rr; x += 9 * dn) cv.drawLine(x, body.bottom, x + ex, body.bottom + ey, p);
                for (float y = body.top + rr + 4 * dn; y < body.bottom - rr; y += 9 * dn) cv.drawLine(body.right, y, body.right + ex, y + ey, p);
            }
            p.setStyle(Paint.Style.FILL);
            cv.save();
            cv.translate(0.9f * dn, 0.9f * dn);
            if (grad) {
                p.setShader(new LinearGradient(body.left, body.top, body.right, body.bottom, g1, g2, Shader.TileMode.CLAMP));
                p.setColor(0xFF000000);
                cv.drawPath(skFill, p);
                p.setShader(null);
            } else if (fill != 0) {
                p.setColor(fill);
                cv.drawPath(skFill, p);
            }
            cv.restore();
            if (bw > 0 && border != 0) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(bw * dn);
                p.setColor(border);
                cv.drawPath(skOut, p);
                p.setStrokeWidth(Math.max(1f, bw * dn * 0.65f));
                p.setColor(Th.alpha(border, 0xB4));
                cv.drawPath(skOut2, p);
            }
            p.setStyle(Paint.Style.FILL);
            cv.restore();
        }


        /** Kartu memanjang untuk tema biasa: persegi penuh, garis hanya di atas dan bawah. */
        private void drawFlatWide(Canvas cv, Rect b) {
            float ey = ey(), w = bw * dn;
            RectF body = new RectF(b.left, b.top, b.right, b.bottom - ey);
            if (body.height() <= 0) return;
            p.setPathEffect(null);
            p.setShader(null);
            p.setStyle(Paint.Style.FILL);
            if (sdClr != 0 && shadowOn && ey > 0) {
                p.setColor(sdClr);
                cv.drawRect(b.left, body.bottom, b.right, b.bottom, p);
            }
            if (grad) {
                p.setShader(new LinearGradient(body.left, body.top, body.right, body.bottom, g1, g2, Shader.TileMode.CLAMP));
                p.setColor(0xFF000000);
                cv.drawRect(body, p);
                p.setShader(null);
            } else if (fill != 0) {
                p.setColor(fill);
                cv.drawRect(body, p);
            }
            if (w > 0 && border != 0) {
                p.setColor(border);
                cv.drawRect(b.left, body.top, b.right, body.top + w, p);
                cv.drawRect(b.left, body.bottom - w, b.right, body.bottom, p);
            }
        }

        // kartu memanjang: hanya garis atas dan bawah (tanpa garis tepi kiri-kanan)
        private Path wTopA, wTopB, wBotA, wBotB, wFill;
        private float wW = -1, wH = -1;

        private float[][] wobLine(float x0, float x1, float y, java.util.Random rnd, float amp, float step) {
            int n = Math.max(4, Math.round((x1 - x0) / step));
            float[] px = new float[n + 1], py = new float[n + 1];
            float noise = 0;
            for (int i = 0; i <= n; i++) {
                noise = noise * 0.55f + (rnd.nextFloat() - 0.5f) * 2f * amp;
                px[i] = x0 + (x1 - x0) * i / n;
                py[i] = y + noise;
            }
            return new float[][]{px, py};
        }

        private void smoothInto(Path out, float[][] pts, boolean reverse, boolean move) {
            float[] px = pts[0], py = pts[1];
            int n = px.length, s = reverse ? n - 1 : 0, d = reverse ? -1 : 1;
            if (move) out.moveTo(px[s], py[s]);
            else out.lineTo(px[s], py[s]);
            for (int k = 1; k < n; k++) {
                int i = s + d * k, j = i - d;
                out.quadTo(px[j], py[j], (px[j] + px[i]) / 2f, (py[j] + py[i]) / 2f);
            }
            out.lineTo(px[s + d * (n - 1)], py[s + d * (n - 1)]);
        }

        private void drawWide(Canvas cv, Rect b) {
            float ey = ey(), pad = 2.5f * dn;
            float top = b.top + pad, bot = b.bottom - ey - pad;
            if (bot - top <= 4 * dn || b.width() <= 0) return;
            if (wTopA == null || wW != b.width() || wH != b.height()) {
                java.util.Random rnd = new java.util.Random(skSeed);
                float amp = 1.25f * dn, x0 = b.left - 2 * dn, x1 = b.right + 2 * dn;
                wTopA = new Path();
                smoothInto(wTopA, wobLine(x0, x1, top, rnd, amp, 13 * dn), false, true);
                wTopB = new Path();
                smoothInto(wTopB, wobLine(x0, x1, top + 0.6f * dn, rnd, amp * 1.15f, 17 * dn), false, true);
                wBotA = new Path();
                smoothInto(wBotA, wobLine(x0, x1, bot, rnd, amp, 13 * dn), false, true);
                wBotB = new Path();
                smoothInto(wBotB, wobLine(x0, x1, bot + 0.6f * dn, rnd, amp * 1.15f, 17 * dn), false, true);
                wFill = new Path();
                smoothInto(wFill, wobLine(x0, x1, top, rnd, amp * 1.3f, 15 * dn), false, true);
                smoothInto(wFill, wobLine(x0, x1, bot, rnd, amp * 1.3f, 15 * dn), true, false);
                wFill.close();
                wW = b.width();
                wH = b.height();
            }
            p.setShader(null);
            p.setPathEffect(null);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
            if (sdClr != 0 && shadowOn && ey > 0) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(Math.max(1f, bw * dn * 0.8f));
                p.setColor(Th.alpha(sdClr, 0x70));
                cv.save();
                cv.translate(0, ey);
                cv.drawPath(wBotB, p);
                cv.restore();
                p.setStrokeWidth(1.1f * dn);
                p.setColor(Th.alpha(sdClr, 0x60));
                for (float x = b.left + 6 * dn; x < b.right; x += 9 * dn) cv.drawLine(x, bot, x + ey * 0.8f, bot + ey, p);
            }
            p.setStyle(Paint.Style.FILL);
            cv.save();
            cv.translate(0, 0.9f * dn);
            if (grad) {
                p.setShader(new LinearGradient(b.left, top, b.right, bot, g1, g2, Shader.TileMode.CLAMP));
                p.setColor(0xFF000000);
                cv.drawPath(wFill, p);
                p.setShader(null);
            } else if (fill != 0) {
                p.setColor(fill);
                cv.drawPath(wFill, p);
            }
            cv.restore();
            if (bw > 0 && border != 0) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(bw * dn);
                p.setColor(border);
                cv.drawPath(wTopA, p);
                cv.drawPath(wBotA, p);
                p.setStrokeWidth(Math.max(1f, bw * dn * 0.65f));
                p.setColor(Th.alpha(border, 0xB4));
                cv.drawPath(wTopB, p);
                cv.drawPath(wBotB, p);
            }
            p.setStyle(Paint.Style.FILL);
        }

        // Gambar coretan dirender sekali ke bitmap lalu tinggal ditempel (gulir tetap ringan).
        private android.graphics.Bitmap skBmp;
        private boolean skShadowKey;

        private void drawSketchCached(Canvas cv, Rect b) {
            int w = b.width(), h = b.height();
            if (w <= 0 || h <= 0) return;
            if (w > 2600 || h > 2600) {
                if (wide) drawWide(cv, b);
                else drawSketch(cv, b);
                return;
            }
            if (skBmp == null || skBmp.getWidth() != w || skBmp.getHeight() != h || skShadowKey != shadowOn) {
                skBmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888);
                Canvas bc = new Canvas(skBmp);
                Rect z = new Rect(0, 0, w, h);
                if (wide) drawWide(bc, z);
                else drawSketch(bc, z);
                skShadowKey = shadowOn;
            }
            cv.drawBitmap(skBmp, b.left, b.top, null);
        }

        // ---------------------------------------------------------- bentuk dekoratif
        private void poly(float... xy) {
            path.moveTo(xy[0], xy[1]);
            for (int i = 2; i < xy.length; i += 2) path.lineTo(xy[i], xy[i + 1]);
            path.close();
        }

        /** Tepi kertas sobek: atas dan bawah bergerigi (acak tetap). grow>0 = melebar keluar. */
        private void jag(RectF b, float grow) {
            path.reset();
            java.util.Random rnd = new java.util.Random(7);
            int n = Math.max(3, Math.round(b.width() / (9 * dn)));
            float dx = b.width() / n, amp = 5 * dn;
            float[] to = new float[n + 1], bo = new float[n + 1], jx = new float[n + 1];
            for (int i = 0; i <= n; i++) {
                to[i] = rnd.nextFloat() * amp + (rnd.nextInt(6) == 0 ? 3 * dn : 0);
                bo[i] = rnd.nextFloat() * amp + (rnd.nextInt(6) == 0 ? 3 * dn : 0);
                jx[i] = (rnd.nextFloat() - 0.5f) * dx * 0.7f;
            }
            path.moveTo(b.left - grow, b.top + to[0] - grow);
            for (int i = 1; i < n; i++) path.lineTo(b.left + dx * i + jx[i], b.top + to[i] - grow);
            path.lineTo(b.right + grow, b.top + to[n] - grow);
            path.lineTo(b.right + grow, b.bottom - bo[n] + grow);
            for (int i = n - 1; i > 0; i--) path.lineTo(b.left + dx * i + jx[i], b.bottom - bo[i] + grow);
            path.lineTo(b.left - grow, b.bottom - bo[0] + grow);
            path.close();
        }

        private void leaf(RectF b) {
            float d = dn, R = Math.min(36 * d, Math.min(b.width(), b.height()) / 2f);
            path.reset();
            path.moveTo(b.left + R, b.top);
            path.lineTo(b.right - 22 * d, b.top);
            path.quadTo(b.right - 5 * d, b.top + 1 * d, b.right + 6 * d, b.top - 7 * d);
            path.quadTo(b.right - 1 * d, b.top + 6 * d, b.right, b.top + 22 * d);
            path.lineTo(b.right, b.bottom - R);
            path.quadTo(b.right, b.bottom, b.right - R, b.bottom);
            path.lineTo(b.left + 22 * d, b.bottom);
            path.quadTo(b.left + 5 * d, b.bottom - 1 * d, b.left - 6 * d, b.bottom + 7 * d);
            path.quadTo(b.left + 1 * d, b.bottom - 6 * d, b.left, b.bottom - 22 * d);
            path.lineTo(b.left, b.top + R);
            path.quadTo(b.left, b.top, b.left + R, b.top);
            path.close();
        }

        private void buildPath(RectF b) {
            path.reset();
            float w = b.width(), h = b.height(), d = dn;
            switch (shape) {
                case "hexagon": {
                    float k = Math.min(20 * d, w / 4);
                    poly(b.left + k, b.top, b.right - k, b.top, b.right, b.centerY(), b.right - k, b.bottom, b.left + k, b.bottom, b.left, b.centerY());
                    break;
                }
                case "lancip": {
                    float k = Math.min(24 * d, w / 3);
                    poly(b.left, b.top, b.right - k, b.top, b.right, b.centerY(), b.right - k, b.bottom, b.left, b.bottom);
                    break;
                }
                case "miring": {
                    float k = Math.min(16 * d, w / 4);
                    poly(b.left + k, b.top, b.right, b.top, b.right - k, b.bottom, b.left, b.bottom);
                    break;
                }
                case "acak":
                    poly(b.left, b.top + 7 * d, b.left + .18f * w, b.top, b.left + .43f * w, b.top + 6 * d, b.left + .70f * w, b.top,
                            b.right, b.top + 9 * d, b.left + .99f * w, b.top + .62f * h, b.right, b.bottom - 7 * d,
                            b.left + .76f * w, b.bottom, b.left + .48f * w, b.bottom - 6 * d, b.left + .22f * w, b.bottom,
                            b.left, b.bottom - 9 * d, b.left + .01f * w, b.top + .40f * h);
                    break;
                case "cat":
                    path.addRoundRect(b, new float[]{.44f * w, .34f * h, .56f * w, .38f * h, .52f * w, .62f * h, .48f * w, .66f * h}, Path.Direction.CW);
                    break;
                case "lendir": {
                    float dh = 14 * d;
                    RectF top = new RectF(b.left, b.top, b.right, b.bottom - dh);
                    path.addRoundRect(top, new float[]{18 * d, 18 * d, 18 * d, 18 * d, 0, 0, 0, 0}, Path.Direction.CW);
                    float[][] dr = {{.10f, 18}, {.28f, 12}, {.52f, 22}, {.74f, 14}, {.91f, 17}};
                    for (float[] q : dr) {
                        Path o = new Path();
                        float cx = b.left + q[0] * w;
                        o.addOval(new RectF(cx - 9 * d, top.bottom - 10 * d, cx + 9 * d, top.bottom - 10 * d + q[1] * d), Path.Direction.CW);
                        path.op(o, Path.Op.UNION);
                    }
                    break;
                }
                case "sobek":
                    jag(b, 0);
                    break;
                case "daun":
                    leaf(b);
                    break;
                case "lipat": {
                    float k = Math.min(28 * d, Math.min(w, h) / 2);
                    poly(b.left, b.top, b.right - k, b.top, b.right, b.top + k, b.right, b.bottom, b.left, b.bottom);
                    break;
                }
                case "tape": {
                    float t = Math.min(2.4f * d, h / 6);
                    path.moveTo(b.left, b.top);
                    path.lineTo(b.right, b.top);
                    boolean in = true;
                    for (float y = b.top + t; y < b.bottom; y += t) {
                        path.lineTo(in ? b.right - t : b.right, y);
                        in = !in;
                    }
                    path.lineTo(b.right, b.bottom);
                    path.lineTo(b.left, b.bottom);
                    in = true;
                    for (float y = b.bottom - t; y > b.top; y -= t) {
                        path.lineTo(in ? b.left + t : b.left, y);
                        in = !in;
                    }
                    path.close();
                    break;
                }
                case "kaca":
                    path.addRoundRect(b, 24 * d, 24 * d, Path.Direction.CW);
                    break;
                case "jahit":
                    path.addRoundRect(b, Math.min(r > 0 ? r * d : 14 * d, h / 2), Math.min(r > 0 ? r * d : 14 * d, h / 2), Path.Direction.CW);
                    break;
                default: // solatip, kertas, memo
                    path.addRoundRect(b, 3 * d, 3 * d, Path.Direction.CW);
            }
        }

        /** Bayangan lembut berlapis (tanpa blur, aman di hardware canvas). */
        private void softShadow(Canvas cv, Path pth, float dy, float dxo, int n, int a) {
            p.setShader(null);
            p.setPathEffect(null);
            p.setStyle(Paint.Style.FILL_AND_STROKE);
            p.setStrokeJoin(Paint.Join.ROUND);
            for (int i = n; i >= 1; i--) {
                p.setStrokeWidth(i * 1.7f * dn);
                p.setColor(a << 24);
                cv.save();
                cv.translate(dxo * dn, dy * dn);
                cv.drawPath(pth, p);
                cv.restore();
            }
            p.setStrokeWidth(0);
            p.setStyle(Paint.Style.FILL);
        }

        private void tape(Canvas cv, float cx, float cy, float deg) {
            float hw = 27 * dn, hh = 9 * dn, t = 2.2f * dn;
            Path tp = new Path();
            tp.moveTo(-hw, -hh);
            tp.lineTo(hw, -hh);
            boolean in = true;
            for (float y = -hh + t; y < hh; y += t) {
                tp.lineTo(in ? hw - t : hw, y);
                in = !in;
            }
            tp.lineTo(hw, hh);
            tp.lineTo(-hw, hh);
            in = true;
            for (float y = hh - t; y > -hh; y -= t) {
                tp.lineTo(in ? -hw + t : -hw, y);
                in = !in;
            }
            tp.close();
            cv.save();
            cv.translate(cx, cy);
            cv.rotate(deg);
            p.setStyle(Paint.Style.FILL);
            p.setShader(null);
            p.setColor(0x26000000);
            cv.save();
            cv.translate(0, 1.6f * dn);
            cv.drawPath(tp, p);
            cv.restore();
            int tc = Th.alpha(Th.mix(tint == 0 ? 0xFFFFD966 : tint, 0xFFFFFFFF, 0.30f), 0xC4);
            p.setColor(tc);
            cv.drawPath(tp, p);
            p.setColor(0x58FFFFFF);
            cv.drawRect(-hw + 3 * dn, -hh * 0.55f, hw - 3 * dn, -hh * 0.12f, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Math.max(1f, dn * 0.8f));
            p.setColor(0x22000000);
            cv.drawPath(tp, p);
            p.setStyle(Paint.Style.FILL);
            cv.restore();
        }

        private void veins(Canvas cv, RectF b) {
            float x0 = b.left + 6 * dn, y0 = b.bottom - 6 * dn, x1 = b.right - 6 * dn, y1 = b.top + 6 * dn;
            float ux = x1 - x0, uy = y1 - y0, len = (float) Math.hypot(ux, uy);
            ux /= len;
            uy /= len;
            float nx = -uy, ny = ux;
            int vc = tint == 0 ? 0xFF5AA24A : tint;
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeWidth(1.3f * dn);
            p.setColor(Th.alpha(vc, 0x34));
            int n = Math.max(5, Math.min(14, Math.round(len / (34 * dn))));
            for (int i = 1; i < n; i++) {
                float t = i / (float) n;
                float px = x0 + (x1 - x0) * t, py = y0 + (y1 - y0) * t;
                float l = Math.min(b.width(), b.height()) * 0.30f * (float) Math.sin(Math.PI * t) + 10 * dn;
                cv.drawLine(px, py, px + (ux * 0.9f + nx) * l, py + (uy * 0.9f + ny) * l, p);
                cv.drawLine(px, py, px + (ux * 0.9f - nx) * l, py + (uy * 0.9f - ny) * l, p);
            }
            p.setStrokeWidth(2.2f * dn);
            p.setColor(Th.alpha(vc, 0x5A));
            cv.drawLine(x0 - 6 * dn, y0 + 6 * dn, x1, y1, p);
            p.setStrokeCap(Paint.Cap.BUTT);
            p.setStyle(Paint.Style.FILL);
        }

        private void drawShape(Canvas cv, Rect bd) {
            float in = ins * dn;
            RectF body = new RectF(bd.left + in, bd.top + in, bd.right - in, bd.bottom - in);
            if (body.width() <= 0 || body.height() <= 0) return;
            float d = dn;
            boolean small = shape.equals("tape") || shape.equals("jahit") && body.height() < 80 * d;
            int fc = fill != 0 ? fill : 0xFFFFFFFF;

            if (shape.equals("sobek")) {
                jag(body, 2.6f * d);
                Path outer = new Path(path);
                jag(body, 0);
                Path inner = new Path(path);
                softShadow(cv, outer, 3, 0, 5, 0x0C);
                p.setStyle(Paint.Style.FILL);
                p.setShader(null);
                p.setColor(Th.mix(fc, 0xFFFFFFFF, 0.78f));
                cv.drawPath(outer, p);
                p.setShader(new LinearGradient(0, body.top, 0, body.bottom, Th.mix(fc, 0xFFFFFFFF, 0.06f), dark(fc, 0.07f), Shader.TileMode.CLAMP));
                p.setColor(0xFF000000);
                cv.drawPath(inner, p);
                p.setShader(null);
                cv.save();
                cv.clipPath(inner);
                p.setShader(new LinearGradient(0, body.bottom - 16 * d, 0, body.bottom, 0x00000000, 0x26000000, Shader.TileMode.CLAMP));
                cv.drawRect(body.left, body.bottom - 16 * d, body.right, body.bottom, p);
                p.setShader(new LinearGradient(0, body.top, 0, body.top + 12 * d, 0x1A000000, 0x00000000, Shader.TileMode.CLAMP));
                cv.drawRect(body.left, body.top, body.right, body.top + 12 * d, p);
                p.setShader(null);
                cv.restore();
                return;
            }

            buildPath(body);
            Path sp = new Path(path);
            if (shape.equals("memo")) softShadow(cv, sp, 4, 1.5f, 5, 0x10);
            else if (!small) softShadow(cv, sp, shape.equals("kaca") ? 5 : 3.5f, 0, 5, 0x0E);

            // isi
            p.setStyle(Paint.Style.FILL);
            if (grad) {
                p.setShader(new LinearGradient(body.left, body.top, body.right, body.bottom, g1, g2, Shader.TileMode.CLAMP));
                p.setColor(0xFF000000);
            } else if (shape.equals("daun")) {
                p.setShader(new LinearGradient(body.left, body.bottom, body.right, body.top,
                        Th.mix(fc, tint == 0 ? 0xFF5AA24A : tint, 0.22f), fc, Shader.TileMode.CLAMP));
                p.setColor(0xFF000000);
            } else if (shape.equals("kaca")) {
                p.setShader(null);
                p.setColor(fc);
            } else if (small) {
                p.setShader(null);
                p.setColor(fc);
            } else {
                p.setShader(new LinearGradient(0, body.top, 0, body.bottom, Th.mix(fc, 0xFFFFFFFF, 0.05f), dark(fc, 0.04f), Shader.TileMode.CLAMP));
                p.setColor(0xFF000000);
            }
            cv.drawPath(sp, p);
            p.setShader(null);

            cv.save();
            cv.clipPath(sp);
            switch (shape) {
                case "kertas": {
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(Math.max(1f, d * 1.1f));
                    p.setColor(0x405B8DEF);
                    for (float y = body.top + 30 * d; y < body.bottom - 6 * d; y += 22 * d)
                        cv.drawLine(body.left + 3 * d, y, body.right - 3 * d, y, p);
                    p.setStrokeWidth(1.5f * d);
                    p.setColor(0x70E0605A);
                    cv.drawLine(body.left + 32 * d, body.top, body.left + 32 * d, body.bottom, p);
                    p.setStyle(Paint.Style.FILL);
                    int hc = hole != 0 ? hole : 0xFFDDDDDD;
                    float[] fy = {0.2f, 0.5f, 0.8f};
                    for (float f : fy) {
                        float cx = body.left + 14 * d, cy = body.top + body.height() * f;
                        p.setColor(0x30000000);
                        cv.drawCircle(cx, cy + 1.2f * d, 6.2f * d, p);
                        p.setColor(hc);
                        cv.drawCircle(cx, cy, 5.6f * d, p);
                    }
                    p.setShader(new LinearGradient(body.left, 0, body.left + 8 * d, 0, 0x1E000000, 0x00000000, Shader.TileMode.CLAMP));
                    cv.drawRect(body.left, body.top, body.left + 8 * d, body.bottom, p);
                    p.setShader(null);
                    break;
                }
                case "memo": {
                    p.setShader(new LinearGradient(0, body.top, 0, body.top + 18 * d, 0x22000000, 0x00000000, Shader.TileMode.CLAMP));
                    cv.drawRect(body.left, body.top, body.right, body.top + 18 * d, p);
                    p.setShader(new LinearGradient(0, body.bottom - 16 * d, 0, body.bottom, 0x00000000, 0x2A000000, Shader.TileMode.CLAMP));
                    cv.drawRect(body.left, body.bottom - 16 * d, body.right, body.bottom, p);
                    p.setShader(null);
                    p.setColor(0x20000000);
                    Path c = new Path();
                    c.moveTo(body.right - 30 * d, body.bottom);
                    c.lineTo(body.right, body.bottom - 30 * d);
                    c.lineTo(body.right, body.bottom);
                    c.close();
                    cv.drawPath(c, p);
                    break;
                }
                case "daun":
                    veins(cv, body);
                    break;
                case "kaca": {
                    p.setShader(new LinearGradient(body.left, body.top, body.right, body.bottom,
                            new int[]{0x66FFFFFF, 0x14FFFFFF, 0x00FFFFFF, 0x22FFFFFF}, new float[]{0f, 0.35f, 0.7f, 1f}, Shader.TileMode.CLAMP));
                    cv.drawRect(body, p);
                    p.setShader(null);
                    break;
                }
                case "lipat": {
                    float k = Math.min(28 * d, Math.min(body.width(), body.height()) / 2);
                    Path fl = new Path();
                    fl.moveTo(body.right - k, body.top);
                    fl.lineTo(body.right, body.top + k);
                    fl.lineTo(body.right - k, body.top + k);
                    fl.close();
                    cv.restore();
                    cv.save();
                    p.setStyle(Paint.Style.FILL);
                    p.setColor(0x30000000);
                    cv.save();
                    cv.translate(-1.8f * d, 2.2f * d);
                    cv.drawPath(fl, p);
                    cv.restore();
                    p.setShader(new LinearGradient(body.right - k, body.top + k, body.right, body.top,
                            dark(fc, 0.22f), Th.mix(fc, 0xFFFFFFFF, 0.25f), Shader.TileMode.CLAMP));
                    p.setColor(0xFF000000);
                    cv.drawPath(fl, p);
                    p.setShader(null);
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(Math.max(1f, d));
                    p.setColor(0x26000000);
                    cv.drawLine(body.right - k, body.top, body.right, body.top + k, p);
                    p.setStyle(Paint.Style.FILL);
                    break;
                }
                default:
                    break;
            }
            cv.restore();

            if (gloss && small) gloss(cv, body);
            if (shape.equals("jahit")) {
                RectF st = new RectF(body);
                float gap = (small ? 3 : 5) * d;
                st.inset(gap, gap);
                Path sw = new Path();
                float rr = Math.max(2 * d, Math.min(small ? 9 * d : 10 * d, st.height() / 2));
                sw.addRoundRect(st, rr, rr, Path.Direction.CW);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(1.7f * d);
                p.setColor(tint != 0 ? tint : 0x99000000);
                p.setPathEffect(new DashPathEffect(new float[]{6 * d, 4 * d}, 0));
                cv.drawPath(sw, p);
                p.setPathEffect(null);
                p.setStyle(Paint.Style.FILL);
            }
            if (shape.equals("solatip")) {
                tape(cv, body.left + 14 * d, body.top + 6 * d, -38);
                tape(cv, body.right - 14 * d, body.top + 6 * d, 38);
            }
            if (shape.equals("kaca") && bw > 0 && border != 0) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(bw * d);
                p.setColor(border);
                cv.drawPath(sp, p);
                p.setStyle(Paint.Style.FILL);
            }
        }

        @Override
        public void setAlpha(int a) {
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    /** Latar halaman: warna + kisi / garis buku + cahaya radial + gambar latar. */
    public static class PageBg extends Drawable {
        private final Th th;
        private final float dn;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        public PageBg(Th th, float dn) {
            this.th = th;
            this.dn = dn;
        }

        @Override
        public void draw(Canvas cv) {
            Rect b = getBounds();
            cv.drawColor(th.bg);
            if (th.bgBmp != null) {
                Bitmap bm = th.bgBmp;
                float sc = Math.max(b.width() / (float) bm.getWidth(), b.height() / (float) bm.getHeight());
                float sw = b.width() / sc, sh = b.height() / sc;
                Rect src = new Rect(Math.round((bm.getWidth() - sw) / 2f), Math.round((bm.getHeight() - sh) / 2f),
                        Math.round((bm.getWidth() + sw) / 2f), Math.round((bm.getHeight() + sh) / 2f));
                cv.drawBitmap(bm, src, b, p);
                p.setStyle(Paint.Style.FILL);
                p.setColor(Th.alpha(th.bg, 0x99));
                cv.drawRect(b, p);
            }
            if (th.grid != 0) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(Math.max(1f, dn));
                p.setColor(th.grid);
                float step = 22 * dn;
                for (float x = b.left; x < b.right; x += step) cv.drawLine(x, b.top, x, b.bottom, p);
                for (float y = b.top; y < b.bottom; y += step) cv.drawLine(b.left, y, b.right, y, p);
            }
            if (th.lined) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(Math.max(1f, dn));
                p.setColor(0x668FB0D4);
                float step = 32 * dn;
                for (float y = b.top + step - dn; y < b.bottom; y += step) cv.drawLine(b.left, y, b.right, y, p);
                p.setStrokeWidth(2 * dn);
                p.setColor(0x88D9534F);
                cv.drawLine(b.left + 31 * dn, b.top, b.left + 31 * dn, b.bottom, p);
            }
            p.setStyle(Paint.Style.FILL);
            for (int i = 0; i < th.glowClr.length; i++) {
                float cx = b.left + b.width() * th.glowX[i], cy = b.top + b.height() * th.glowY[i];
                float rad = Math.max(b.width(), b.height()) * th.glowR[i];
                if (rad <= 0) continue;
                p.setShader(new RadialGradient(cx, cy, rad, th.glowClr[i], th.glowClr[i] & 0x00FFFFFF, Shader.TileMode.CLAMP));
                cv.drawRect(b, p);
                p.setShader(null);
            }
        }

        @Override
        public void setAlpha(int a) {
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.OPAQUE;
        }
    }

    // ------------------------------------------------------------------ view
    public static class Card extends LinearLayout {
        public final int kind;
        public int bleed; // >0: kartu memanjang sampai tepi layar (px per sisi)

        public Card(Context c, int kind) {
            super(c);
            this.kind = kind;
            setOrientation(VERTICAL);
        }
    }

    public int kindOf(View v) {
        android.view.ViewParent p = v.getParent();
        while (p instanceof View) {
            if (p instanceof Card) return ((Card) p).kind;
            p = p.getParent();
        }
        return PLAIN;
    }

    private ViewGroup.LayoutParams defLp(ViewGroup p) {
        if (p instanceof LinearLayout && ((LinearLayout) p).getOrientation() == LinearLayout.HORIZONTAL)
            return new LinearLayout.LayoutParams(WRAP, WRAP);
        return new LinearLayout.LayoutParams(MATCH, WRAP);
    }

    public <T extends View> T add(ViewGroup p, T v) {
        p.addView(v, defLp(p));
        return v;
    }

    public static LinearLayout.LayoutParams lpOf(View v) {
        ViewGroup.LayoutParams l = v.getLayoutParams();
        if (l instanceof LinearLayout.LayoutParams) return (LinearLayout.LayoutParams) l;
        LinearLayout.LayoutParams n = l == null ? new LinearLayout.LayoutParams(WRAP, WRAP) : new LinearLayout.LayoutParams(l);
        v.setLayoutParams(n);
        return n;
    }

    public <T extends View> T wt(T v, float w) {
        LinearLayout.LayoutParams l = lpOf(v);
        l.width = 0;
        l.weight = w;
        v.setLayoutParams(l);
        return v;
    }

    public <T extends View> T mar(T v, float l, float t, float r, float b) {
        LinearLayout.LayoutParams lp = lpOf(v);
        lp.setMargins(dp(l), dp(t), dp(r), dp(b));
        v.setLayoutParams(lp);
        return v;
    }

    public <T extends View> T mt(T v, float t) {
        LinearLayout.LayoutParams lp = lpOf(v);
        lp.topMargin = dp(t);
        v.setLayoutParams(lp);
        return v;
    }

    public <T extends View> T size(T v, float w, float h) {
        LinearLayout.LayoutParams lp = lpOf(v);
        lp.width = w < 0 ? (int) w : dp(w);
        lp.height = h < 0 ? (int) h : dp(h);
        v.setLayoutParams(lp);
        return v;
    }

    public LinearLayout col(ViewGroup p) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        add(p, l);
        return l;
    }

    public LinearLayout row(ViewGroup p) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        p.addView(l, new LinearLayout.LayoutParams(MATCH, WRAP));
        return l;
    }

    public View spacer(ViewGroup p, float h) {
        View v = new View(c);
        p.addView(v, new LinearLayout.LayoutParams(MATCH, dp(h)));
        return v;
    }

    public Box boxFor(int kind) {
        Box b = new Box();
        b.dn = dn;
        b.r = th.cardR;
        b.bw = th.cardBw;
        b.border = th.cardBorder;
        b.dash = th.cardDash;
        b.sx = th.cardSx;
        b.sy = th.cardSy;
        b.sdClr = th.cardSdClr;
        b.cr = th.cardCr;
        if (kind == C1) b.fill = th.c1;
        else if (kind == C2) b.fill = th.c2;
        else if (kind == DIALOG) {
            b.fill = th.card;
            if (b.sdClr == 0 && th.shape.isEmpty()) b.sdClr = 0x55000000;
            if (b.sx == 0 && b.sy == 0) b.sy = 6;
        } else if (th.cardGrad) {
            b.grad = true;
            b.g1 = th.cardG1;
            b.g2 = th.cardG2;
        } else b.fill = th.card;
        b.sketch = th.sketch;
        if (!th.shape.isEmpty()) {
            b.setShape(th.shape);
            b.tint = th.shape.equals("jahit") ? Th.alpha(th.ink, 0x99) : th.tint;
            b.hole = th.bg;
            b.sdClr = 0;
        }
        return b;
    }

    public void pad(View v, Box b, float l, float t, float r, float bt) {
        v.setPadding(dp(l) + b.extL(), dp(t) + b.extT(), dp(r) + Math.round(b.ex()) + b.extR(), dp(bt) + Math.round(b.ey()) + b.extB());
    }

    /** Induk halaman (content) dan lebar tepi yang dibuang, untuk kartu memanjang. */
    public ViewGroup wideParent;
    public int widePad;

    public Card card(ViewGroup p, int kind) {
        return card(p, kind, false);
    }

    /** wide=true: kartu memanjang sampai tepi layar tanpa garis kiri-kanan (dipakai kartu tabel). */
    public Card card(ViewGroup p, int kind, boolean wantWide) {
        Card k = new Card(c, kind);
        Box b = boxFor(kind);
        boolean wide = wantWide && p == wideParent && kind != DIALOG && th.shape.isEmpty();
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        if (wide) {
            b.wide = true;
            k.bleed = widePad;
            lp.leftMargin = -widePad;
            lp.rightMargin = -widePad;
        }
        k.setBackground(b);
        pad(k, b, 12, 12, 12, 12);
        if (wide) k.setPadding(k.getPaddingLeft() + widePad, k.getPaddingTop(), k.getPaddingRight() + widePad, k.getPaddingBottom());
        k.setClipChildren(false);
        k.setClipToPadding(false);
        p.addView(k, lp);
        return k;
    }

    // ------------------------------------------------------------------ teks
    public TextView tv(ViewGroup p, CharSequence s, float sp) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTypeface(th.font);
        p.addView(t, defLp(p));
        t.setTextColor(th.inkOf(kindOf(t)));
        return t;
    }

    public TextView tvc(ViewGroup p, CharSequence s, float sp, int color) {
        TextView t = tv(p, s, sp);
        t.setTextColor(color);
        return t;
    }

    public TextView small(ViewGroup p, CharSequence s) {
        TextView t = tv(p, s, 13);
        t.setAllCaps(th.caps);
        t.setTextColor(th.mutedOf(kindOf(t)));
        return t;
    }

    public TextView note(ViewGroup p, CharSequence s) {
        TextView t = tv(p, s, 14);
        t.setTextColor(th.mutedOf(kindOf(t)));
        return t;
    }

    public TextView h2(ViewGroup p, CharSequence s) {
        TextView t = tv(p, s, 15);
        t.setAllCaps(th.caps);
        return t;
    }

    public TextView label(ViewGroup p, CharSequence s) {
        TextView t = tv(p, s, 14);
        t.setAllCaps(th.caps);
        t.setTextColor(th.mutedOf(kindOf(t)));
        mar(t, 0, 10, 0, 3);
        return t;
    }

    public TextView money(ViewGroup p, CharSequence s, float sp, int mode) {
        TextView t = tv(p, s, sp);
        int k = kindOf(t);
        if (mode > 0) t.setTextColor(th.inOf(k));
        if (mode < 0) t.setTextColor(th.outOf(k));
        return t;
    }

    public TextView tag(ViewGroup p, String s, int bg, int fg) {
        TextView t = tv(p, s, 10);
        t.setAllCaps(true);
        t.setTextColor(fg);
        Box b = new Box();
        b.dn = dn;
        b.fill = bg;
        b.bw = th.sq ? 2 : 0;
        b.border = th.ink;
        b.r = th.btnR > 0 ? 99 : 0;
        t.setBackground(b);
        t.setPadding(dp(6), dp(1), dp(6), dp(1));
        LinearLayout.LayoutParams lp = lpOf(t);
        lp.width = WRAP;
        t.setLayoutParams(lp);
        return t;
    }

    public View sep(ViewGroup p) {
        View v = new View(c);
        Box b = new Box();
        b.dn = dn;
        b.border = th.sep;
        b.bw = 2;
        b.dash = th.sq;
        if (!b.dash) {
            b.bw = 0;
            b.fill = th.sep;
        }
        v.setBackground(b);
        int kind = PLAIN;
        p.addView(v, new LinearLayout.LayoutParams(MATCH, b.dash ? dp(2) : Math.max(1, dp(1))));
        kind = kindOf(v);
        if (kind == NORMAL && th.cardGrad) {
            b.dash = false;
            b.bw = 0;
            b.fill = 0x40FFFFFF;
        }
        return v;
    }

    // ------------------------------------------------------------------ tombol
    public class Btn extends TextView {
        int style;
        boolean on;
        boolean hasOverride;
        int ovBg, ovInk;
        Box box;
        float padH = 13, padV = 9;

        Btn(int style) {
            super(Ui.this.c);
            this.style = style;
            setGravity(Gravity.CENTER);
            setTypeface(th.font);
            setTextSize(14);
            setAllCaps(th.caps);
            setClickable(true);
            setIncludeFontPadding(false);
        }

        public Btn on(boolean v) {
            on = v;
            restyle();
            return this;
        }

        public Btn color(int bg, int ink) {
            hasOverride = true;
            ovBg = bg;
            ovInk = ink;
            restyle();
            return this;
        }

        public Btn padding(float h, float v) {
            padH = h;
            padV = v;
            restyle();
            return this;
        }

        public Btn text(float sp) {
            setTextSize(sp);
            return this;
        }

        public void restyle() {
            int kind = kindOf(this);
            int[] cl = th.btnColors(kind, style);
            int bg = cl[0], ink = cl[1], bd = cl[2];
            Box b = new Box();
            b.dn = dn;
            b.r = th.btnR;
            if (style == R) {
                setBackground(null);
                setTextColor(ink);
                setPadding(dp(padH > 9 ? 8 : padH), dp(padV > 6 ? 6 : padV), dp(padH > 9 ? 8 : padH), dp(padV > 6 ? 6 : padV));
                box = null;
                return;
            }
            if (on) {
                bg = th.c1;
                ink = 0xFF111111;
                bd = style == P ? th.btnBorder : th.wBorder;
            }
            if (hasOverride) {
                bg = ovBg;
                ink = ovInk;
            }
            b.fill = bg;
            if (style == P && !on && !hasOverride && th.primGrad) {
                b.grad = true;
                b.g1 = th.c1;
                b.g2 = th.c2;
            }
            b.border = bd;
            b.bw = style == W && !on ? th.wBw : th.btnBw;
            if (style == W && on) b.bw = th.wBw;
            b.sx = th.btnSx;
            b.sy = th.btnSy;
            b.sdClr = th.btnSdClr;
            b.cr = th.btnCr;
            b.gloss = th.btnGloss;
            b.sketch = th.sketch;
            if (!th.btnShape.isEmpty()) {
                b.setShape(th.btnShape);
                b.tint = Th.alpha(ink, 0xAA);
                b.sdClr = 0;
            }
            if (kind == NORMAL && th.cardGrad && style != R) b.sdClr = 0;
            box = b;
            setBackground(b);
            setTextColor(ink);
            setPadding(dp(padH) + b.extL(), dp(padV) + b.extT(), dp(padH) + Math.round(b.ex()) + b.extR(), dp(padV) + Math.round(b.ey()) + b.extB());
        }

        @Override
        public void setPressed(boolean pr) {
            super.setPressed(pr);
            if (box != null && (box.ex() > 0 || box.ey() > 0)) {
                box.shadowOn = !pr;
                setTranslationX(pr ? box.ex() : 0);
                setTranslationY(pr ? box.ey() : 0);
                box.invalidateSelf();
            } else {
                animate().cancel();
                if (pr) {
                    performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                    animate().scaleX(0.95f).scaleY(0.95f).alpha(0.88f).setDuration(90).start();
                } else {
                    animate().scaleX(1f).scaleY(1f).alpha(isEnabled() ? 1f : 0.4f).setDuration(280)
                            .setInterpolator(new android.view.animation.OvershootInterpolator(2.4f)).start();
                }
            }
        }

        @Override
        public void setEnabled(boolean e) {
            super.setEnabled(e);
            setAlpha(e ? 1f : 0.4f);
        }
    }

    public Btn btn(ViewGroup p, CharSequence s, int style, View.OnClickListener l) {
        Btn b = new Btn(style);
        b.setText(s);
        p.addView(b, defLp(p));
        b.restyle();
        if (l != null) b.setOnClickListener(l);
        return b;
    }

    // ------------------------------------------------------------------ input
    public Box inputBox() {
        Box b = new Box();
        b.dn = dn;
        b.fill = th.inputBg;
        b.r = th.inR;
        b.bw = th.inBw;
        b.border = th.inBorder;
        b.sketch = th.sketch;
        return b;
    }

    public EditText input(ViewGroup p, String hint, int type) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setHintTextColor(th.mu);
        e.setTextColor(th.inputInk);
        e.setTypeface(th.font);
        e.setTextSize(17);
        e.setInputType(type);
        e.setSingleLine(true);
        e.setBackground(inputBox());
        e.setPadding(dp(10), dp(9), dp(10), dp(9));
        e.setMinHeight(dp(46));
        p.addView(e, new LinearLayout.LayoutParams(MATCH, WRAP));
        return e;
    }

    public EditText area(ViewGroup p, String hint, int lines) {
        EditText e = input(p, hint, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        e.setSingleLine(false);
        e.setMinLines(lines);
        e.setGravity(Gravity.TOP | Gravity.START);
        return e;
    }

    public static long numOf(EditText e) {
        String s = e.getText().toString().trim();
        if (s.isEmpty()) return 0;
        try {
            return Math.round(Double.parseDouble(s));
        } catch (Exception x) {
            return 0;
        }
    }

    public static void setText(EditText e, String s) {
        e.setText(s);
        e.setSelection(e.getText().length());
    }

    /** Jalankan r setiap teks berubah. */
    public static void watch(EditText e, Runnable r) {
        e.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int st, int c, int af) {
            }

            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                r.run();
            }
        });
    }

    public static double dbl(EditText e) {
        String s = e.getText().toString().trim();
        if (s.isEmpty()) return 0;
        try {
            return Double.parseDouble(s);
        } catch (Exception x) {
            return 0;
        }
    }

    // ------------------------------------------------------------------ pilihan (pengganti spinner)
    public class Select extends TextView {
        String[] labels = new String[0], values = new String[0];
        int idx = 0;
        public Runnable onChange;

        Select() {
            super(Ui.this.c);
            setTypeface(th.font);
            setTextSize(17);
            setTextColor(th.inputInk);
            setBackground(inputBox());
            setPadding(dp(10), dp(9), dp(10), dp(9));
            setMinHeight(dp(46));
            setGravity(Gravity.CENTER_VERTICAL);
            setClickable(true);
            setSingleLine(true);
            setEllipsize(android.text.TextUtils.TruncateAt.END);
            setOnClickListener(v -> {
                if (labels.length == 0) return;
                new AlertDialog.Builder(Ui.this.c).setItems(labels, (d, w) -> {
                    idx = w;
                    refresh();
                    if (onChange != null) onChange.run();
                }).show();
            });
        }

        void refresh() {
            setText(labels.length == 0 ? "-" : labels[Math.min(idx, labels.length - 1)] + "  ▾");
        }

        public Select options(String[] l, String[] v) {
            String cur = value();
            labels = l;
            values = v;
            idx = 0;
            for (int i = 0; i < v.length; i++) if (v[i].equals(cur)) idx = i;
            refresh();
            return this;
        }

        public Select options(String[] l) {
            return options(l, l);
        }

        public String value() {
            return values.length == 0 ? "" : values[Math.min(idx, values.length - 1)];
        }

        public Select set(String v) {
            for (int i = 0; i < values.length; i++) if (values[i].equals(v)) idx = i;
            refresh();
            return this;
        }
    }

    public Select select(ViewGroup p) {
        Select s = new Select();
        p.addView(s, new LinearLayout.LayoutParams(MATCH, WRAP));
        return s;
    }

    // ------------------------------------------------------------------ segmen
    public interface Pick {
        void on(String key);
    }

    public static class Seg {
        public final java.util.List<Btn> bs = new java.util.ArrayList<>();
        public final java.util.List<String> ks = new java.util.ArrayList<>();
        public String cur;

        public void set(String k) {
            cur = k;
            for (int i = 0; i < bs.size(); i++) bs.get(i).on(ks.get(i).equals(k));
        }
    }

    public Seg seg(ViewGroup p, String[] labels, String[] keys, String cur, Pick cb) {
        Seg s = new Seg();
        s.cur = cur;
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.HORIZONTAL);
        p.addView(r, new LinearLayout.LayoutParams(MATCH, WRAP));
        for (int i = 0; i < labels.length; i++) {
            final String k = keys[i];
            Btn b = btn(r, labels[i], W, v -> {
                s.set(k);
                if (cb != null) cb.on(k);
            });
            b.padding(2, 9).text(12);
            wt(b, 1);
            if (i > 0) mar(b, 6, 0, 0, 0);
            s.bs.add(b);
            s.ks.add(k);
            b.on(k.equals(cur));
        }
        return s;
    }

    public LinearLayout hscroll(ViewGroup p) {
        return hscroll(p, false);
    }

    /** bleed=true: pada kartu memanjang, tabel ikut sampai tepi layar. Isi selalu terpotong di batas scroll. */
    public LinearLayout hscroll(ViewGroup p, boolean bleed) {
        HorizontalScrollView h = new HorizontalScrollView(c);
        h.setHorizontalScrollBarEnabled(false);
        h.setClipChildren(true);
        h.setClipToPadding(true);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(MATCH, WRAP);
        if (bleed && p instanceof Card && ((Card) p).bleed > 0) {
            hlp.leftMargin = -p.getPaddingLeft();
            hlp.rightMargin = -p.getPaddingRight();
        }
        p.addView(h, hlp);
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        h.addView(r, new ViewGroup.LayoutParams(WRAP, WRAP));
        return r;
    }

    /** Tata letak pembungkus (wrap) sederhana. */
    public static class Flow extends ViewGroup {
        final int gap;

        public Flow(Context c, int gap) {
            super(c);
            this.gap = gap;
        }

        @Override
        protected void onMeasure(int wSpec, int hSpec) {
            int maxW = MeasureSpec.getSize(wSpec);
            int x = 0, y = 0, rowH = 0;
            for (int i = 0; i < getChildCount(); i++) {
                View v = getChildAt(i);
                ViewGroup.LayoutParams lp = v.getLayoutParams();
                int ws = MeasureSpec.makeMeasureSpec(maxW, MeasureSpec.AT_MOST);
                int hs = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
                if (lp != null && lp.width > 0) ws = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
                if (lp != null && lp.height > 0) hs = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
                v.measure(ws, hs);
                int w = v.getMeasuredWidth(), h = v.getMeasuredHeight();
                if (x > 0 && x + w > maxW) {
                    x = 0;
                    y += rowH + gap;
                    rowH = 0;
                }
                x += w + gap;
                rowH = Math.max(rowH, h);
            }
            setMeasuredDimension(maxW, y + rowH);
        }

        @Override
        protected void onLayout(boolean ch, int l, int t, int r, int b) {
            int maxW = r - l;
            int x = 0, y = 0, rowH = 0;
            for (int i = 0; i < getChildCount(); i++) {
                View v = getChildAt(i);
                int w = v.getMeasuredWidth(), h = v.getMeasuredHeight();
                if (x > 0 && x + w > maxW) {
                    x = 0;
                    y += rowH + gap;
                    rowH = 0;
                }
                v.layout(x, y, x + w, y + h);
                x += w + gap;
                rowH = Math.max(rowH, h);
            }
        }
    }

    public Flow flow(ViewGroup p) {
        Flow f = new Flow(c, dp(6));
        p.addView(f, new LinearLayout.LayoutParams(MATCH, WRAP));
        return f;
    }

    // ------------------------------------------------------------------ dialog
    public static class Dlg {
        public Dialog d;
        public Card card;
        public Runnable onClose;

        public void show() {
            card.setAlpha(0f);
            card.setScaleX(0.92f);
            card.setScaleY(0.92f);
            card.setTranslationY(card.getResources().getDisplayMetrics().density * 24f);
            d.show();
            Roll.play(card);
            card.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f).setDuration(300)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.15f)).start();
        }

        public void close() {
            d.dismiss();
        }
    }

    public Dlg dialog() {
        Dlg g = new Dlg();
        Dialog d = new Dialog(c);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ScrollView sv = new ScrollView(c);
        sv.setClipChildren(false);
        sv.setClipToPadding(false);
        LinearLayout wrap = new LinearLayout(c);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(4), dp(4), dp(4), dp(4));
        sv.addView(wrap, new ViewGroup.LayoutParams(MATCH, WRAP));
        g.card = card(wrap, DIALOG);
        pad(g.card, (Box) g.card.getBackground(), 16, 16, 16, 16);
        d.setContentView(sv);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(0));
            w.setLayout(Math.min(Math.round(screenW() * 0.94f), dp(440)), WindowManager.LayoutParams.WRAP_CONTENT);
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        d.setOnDismissListener((DialogInterface di) -> {
            if (g.onClose != null) g.onClose.run();
        });
        g.d = d;
        return g;
    }

    // ------------------------------------------------------------------ gambar base64
    private static final LruCache<String, Bitmap> CACHE = new LruCache<>(80);

    public static Bitmap decode(String dataUrl) {
        if (dataUrl == null || dataUrl.isEmpty()) return null;
        String key = dataUrl.length() + ":" + dataUrl.hashCode();
        Bitmap b = CACHE.get(key);
        if (b != null) return b;
        try {
            int i = dataUrl.indexOf(',');
            byte[] raw = Base64.decode(i >= 0 ? dataUrl.substring(i + 1) : dataUrl, Base64.DEFAULT);
            b = BitmapFactory.decodeByteArray(raw, 0, raw.length);
            if (b != null) CACHE.put(key, b);
        } catch (Exception e) {
            return null;
        }
        return b;
    }

    /** Perkecil (tanpa dipotong) lalu simpan sebagai JPEG base64, latar putih. */
    public static String encodeScaled(Bitmap src, int maxDim, int quality) {
        try {
            float k = Math.min(1f, maxDim / (float) Math.max(src.getWidth(), src.getHeight()));
            int w = Math.max(1, Math.round(src.getWidth() * k)), h = Math.max(1, Math.round(src.getHeight() * k));
            Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas cv = new Canvas(out);
            cv.drawColor(0xFFFFFFFF);
            cv.drawBitmap(src, null, new Rect(0, 0, w, h), new Paint(Paint.FILTER_BITMAP_FLAG));
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            out.compress(Bitmap.CompressFormat.JPEG, quality, bo);
            return "data:image/jpeg;base64," + Base64.encodeToString(bo.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }

    public static String encodeJpeg(Bitmap src, int size, int quality) {
        try {
            int m = Math.min(src.getWidth(), src.getHeight());
            Bitmap sq = Bitmap.createBitmap(src, (src.getWidth() - m) / 2, (src.getHeight() - m) / 2, m, m);
            Bitmap sc = Bitmap.createScaledBitmap(sq, size, size, true);
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            sc.compress(Bitmap.CompressFormat.JPEG, quality, bo);
            return "data:image/jpeg;base64," + Base64.encodeToString(bo.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }
}
