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
        public boolean grad, dash, topOnly, shadowOn = true;
        public float r, bw, sx, sy, dn = 1;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        public float ex() {
            return sdClr != 0 ? sx * dn : 0;
        }

        public float ey() {
            return sdClr != 0 ? sy * dn : 0;
        }

        private void shape(RectF rc) {
            path.reset();
            float rr = Math.min(r * dn, Math.min(rc.width(), rc.height()) / 2f);
            if (rr <= 0) path.addRect(rc, Path.Direction.CW);
            else if (topOnly) path.addRoundRect(rc, new float[]{rr, rr, rr, rr, 0, 0, 0, 0}, Path.Direction.CW);
            else path.addRoundRect(rc, rr, rr, Path.Direction.CW);
        }

        @Override
        public void draw(Canvas cv) {
            Rect b = getBounds();
            float ex = ex(), ey = ey();
            RectF body = new RectF(b.left, b.top, b.right - ex, b.bottom - ey);
            if (body.width() <= 0 || body.height() <= 0) return;
            if (sdClr != 0 && shadowOn && (ex > 0 || ey > 0)) {
                RectF sh = new RectF(body);
                sh.offset(ex, ey);
                shape(sh);
                p.setShader(null);
                p.setPathEffect(null);
                p.setStyle(Paint.Style.FILL);
                p.setColor(sdClr);
                cv.drawPath(path, p);
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

    /** Latar halaman: warna + kisi + cahaya radial. */
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
            if (th.grid != 0) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(Math.max(1f, dn));
                p.setColor(th.grid);
                float step = 22 * dn;
                for (float x = b.left; x < b.right; x += step) cv.drawLine(x, b.top, x, b.bottom, p);
                for (float y = b.top; y < b.bottom; y += step) cv.drawLine(b.left, y, b.right, y, p);
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
        if (kind == C1) b.fill = th.c1;
        else if (kind == C2) b.fill = th.c2;
        else if (kind == DIALOG) {
            b.fill = th.card;
            if (b.sdClr == 0) b.sdClr = 0x55000000;
            if (b.sx == 0 && b.sy == 0) b.sy = 6;
        } else if (th.cardGrad) {
            b.grad = true;
            b.g1 = th.cardG1;
            b.g2 = th.cardG2;
        } else b.fill = th.card;
        return b;
    }

    public void pad(View v, Box b, float l, float t, float r, float bt) {
        v.setPadding(dp(l), dp(t), dp(r) + Math.round(b.ex()), dp(bt) + Math.round(b.ey()));
    }

    public Card card(ViewGroup p, int kind) {
        Card k = new Card(c, kind);
        Box b = boxFor(kind);
        k.setBackground(b);
        pad(k, b, 12, 12, 12, 12);
        k.setClipChildren(false);
        k.setClipToPadding(false);
        p.addView(k, new LinearLayout.LayoutParams(MATCH, WRAP));
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
        b.bw = th.id.equals("piksel") || th.id.equals("buku") ? 2 : 0;
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
        b.dash = th.id.equals("buku") || th.id.equals("piksel");
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
            if (kind == NORMAL && th.cardGrad && style != R) b.sdClr = 0;
            box = b;
            setBackground(b);
            setTextColor(ink);
            setPadding(dp(padH), dp(padV), dp(padH) + Math.round(b.ex()), dp(padV) + Math.round(b.ey()));
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
                setAlpha(pr ? 0.7f : 1f);
            }
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
        return b;
    }

    public EditText input(ViewGroup p, String hint, int type) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setHintTextColor(th.mu);
        e.setTextColor(th.ink);
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
            setTextColor(th.ink);
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
        HorizontalScrollView h = new HorizontalScrollView(c);
        h.setHorizontalScrollBarEnabled(false);
        h.setClipChildren(false);
        h.setClipToPadding(false);
        p.addView(h, new LinearLayout.LayoutParams(MATCH, WRAP));
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
                v.measure(MeasureSpec.makeMeasureSpec(maxW, MeasureSpec.AT_MOST), MeasureSpec.UNSPECIFIED);
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
            d.show();
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
