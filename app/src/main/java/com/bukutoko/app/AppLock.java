package com.bukutoko.app;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.SystemClock;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Base64;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * Kunci aplikasi opsional: PIN 4 digit atau pola, plus sidik jari (Android 10+).
 * Disimpan terpisah dari tokoku_v1.json (hash + salt), jadi tidak ikut cadangan data.
 */
public class AppLock {
    private static final String PREF = "applock_v1";
    private static final long GRACE_MS = 60_000L; // aplikasi di background kurang dari ini: tidak dikunci ulang
    private static boolean unlocked = false;
    private static long stopAt = 0;
    private static Dialog cur;
    private static int fails = 0;
    private static long blockedUntil = 0;

    // ------------------------------------------------------------------ penyimpanan
    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static String mode(Context c) { return sp(c).getString("mode", "none"); }

    public static boolean enabled(Context c) { return !"none".equals(mode(c)); }

    public static boolean bioOn(Context c) { return sp(c).getBoolean("bio", false); }

    public static void setBio(Context c, boolean on) { sp(c).edit().putBoolean("bio", on).apply(); }

    private static String hash(String salt, String secret) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Base64.encodeToString(md.digest((salt + ":" + secret).getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }

    public static void setSecret(Context c, String mode, String secret) {
        byte[] r = new byte[16];
        new SecureRandom().nextBytes(r);
        String salt = Base64.encodeToString(r, Base64.NO_WRAP);
        sp(c).edit().putString("mode", mode).putString("salt", salt).putString("hash", hash(salt, secret)).apply();
        unlocked = true;
    }

    public static void disable(Context c) {
        sp(c).edit().clear().apply();
        unlocked = true;
    }

    private static boolean check(Context c, String secret) {
        SharedPreferences p = sp(c);
        String h = hash(p.getString("salt", ""), secret);
        return MessageDigest.isEqual(h.getBytes(StandardCharsets.UTF_8), p.getString("hash", "").getBytes(StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------ siklus hidup (dipanggil MainActivity)
    public static void reset() {
        unlocked = false;
        stopAt = 0;
    }

    public static void onStop() { stopAt = SystemClock.elapsedRealtime(); }

    public static void dismiss() {
        if (cur != null) {
            try { cur.dismiss(); } catch (Exception ignored) { }
            cur = null;
        }
    }

    public static void onResume(final MainActivity a) {
        if (!enabled(a)) {
            unlocked = true;
            return;
        }
        if (unlocked && stopAt > 0 && SystemClock.elapsedRealtime() - stopAt > GRACE_MS) unlocked = false;
        stopAt = 0;
        if (!unlocked && (cur == null || !cur.isShowing())) prompt(a, false, () -> unlocked = true);
    }

    // ------------------------------------------------------------------ sidik jari
    static boolean bioAvailable(Context c) {
        if (Build.VERSION.SDK_INT < 29) return false;
        try {
            BiometricManager bm = c.getSystemService(BiometricManager.class);
            return bm != null && bm.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void bio(final MainActivity a, final Runnable ok) {
        if (Build.VERSION.SDK_INT < 28) return;
        try {
            BiometricPrompt bp = new BiometricPrompt.Builder(a)
                    .setTitle("Buka Buku Toko")
                    .setNegativeButton("Pakai PIN / pola", a.getMainExecutor(), (di, w) -> { })
                    .build();
            bp.authenticate(new CancellationSignal(), a.getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult r) {
                    ok.run();
                }
            });
        } catch (Throwable ignored) { }
    }

    // ------------------------------------------------------------------ layar kunci
    private static TextView label(MainActivity a, String s, float sp, int color) {
        TextView t = new TextView(a);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(a.ui.th.font);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private static TextView key(MainActivity a, String s, View.OnClickListener l) {
        Th th = a.ui.th;
        TextView t = label(a, s, 26, th.ink);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setStroke(a.ui.dp(2), th.mu);
        g.setColor(0);
        t.setBackground(g);
        int sz = a.ui.dp(70), m = a.ui.dp(7);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sz, sz);
        lp.setMargins(m, m, m, m);
        t.setLayoutParams(lp);
        if (l != null) t.setOnClickListener(l);
        else t.setVisibility(View.INVISIBLE);
        return t;
    }

    /** Tampilkan layar kunci. cancelable=true dipakai untuk verifikasi di Pengaturan. ok dijalankan setelah benar. */
    public static void prompt(final MainActivity a, final boolean cancelable, final Runnable ok) {
        final String mode = mode(a);
        if ("none".equals(mode)) {
            ok.run();
            return;
        }
        final Th th = a.ui.th;
        final Dialog d = new Dialog(a, android.R.style.Theme_DeviceDefault_NoActionBar);
        d.setCancelable(cancelable);
        d.setCanceledOnTouchOutside(false);
        if (d.getWindow() != null) d.getWindow().setBackgroundDrawable(new ColorDrawable(th.bg));
        if (!cancelable) {
            d.setOnKeyListener((di, k, ev) -> {
                if (k == KeyEvent.KEYCODE_BACK) {
                    if (ev.getAction() == KeyEvent.ACTION_UP) a.moveTaskToBack(true);
                    return true;
                }
                return false;
            });
        }
        d.setOnDismissListener(di -> {
            if (cur == d) cur = null;
        });

        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(th.bg);
        int pd = a.ui.dp(20);
        root.setPadding(pd, pd, pd, pd);

        TextView title = label(a, "Buku Toko terkunci", 22, th.ink);
        root.addView(title);
        final TextView msg = label(a, "pin".equals(mode) ? "Masukkan PIN" : "Gambar pola", 14, th.mu);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mlp.setMargins(0, a.ui.dp(8), 0, a.ui.dp(12));
        root.addView(msg, mlp);

        final Runnable success = () -> {
            fails = 0;
            cur = null;
            d.dismiss();
            ok.run();
        };
        // true kalau benar; kalau salah, tampilkan pesan
        final java.util.function.Predicate<String> verify = secret -> {
            long now = SystemClock.elapsedRealtime();
            if (now < blockedUntil) {
                msg.setTextColor(th.bad);
                msg.setText("Tunggu " + ((blockedUntil - now) / 1000 + 1) + " detik lalu coba lagi");
                return false;
            }
            if (check(a, secret)) {
                success.run();
                return true;
            }
            fails++;
            msg.setTextColor(th.bad);
            if (fails >= 5) {
                fails = 0;
                blockedUntil = now + 30_000L;
                msg.setText("Salah 5 kali. Tunggu 30 detik.");
            } else {
                msg.setText(("pin".equals(mode) ? "PIN" : "Pola") + " salah (" + fails + "/5)");
            }
            return false;
        };

        final boolean useBio = bioOn(a) && bioAvailable(a);

        if ("pin".equals(mode)) {
            final StringBuilder pin = new StringBuilder();
            final TextView dots = label(a, "", 30, th.ink);
            root.addView(dots);
            final Runnable upd = () -> {
                StringBuilder s = new StringBuilder();
                for (int i = 0; i < 4; i++) {
                    if (i > 0) s.append(' ');
                    s.append(i < pin.length() ? '\u25CF' : '\u25CB');
                }
                dots.setText(s);
            };
            upd.run();
            String[][] rows = {{"1", "2", "3"}, {"4", "5", "6"}, {"7", "8", "9"}, {"B", "0", "D"}};
            for (String[] rw : rows) {
                LinearLayout r = new LinearLayout(a);
                r.setGravity(Gravity.CENTER);
                for (final String k : rw) {
                    TextView kv;
                    if (k.equals("B")) {
                        kv = useBio ? key(a, "\uD83D\uDC46", v -> bio(a, success)) : key(a, "", null);
                    } else if (k.equals("D")) {
                        kv = key(a, "\u232B", v -> {
                            if (pin.length() > 0) pin.setLength(pin.length() - 1);
                            upd.run();
                        });
                    } else {
                        kv = key(a, k, v -> {
                            if (pin.length() >= 4) return;
                            pin.append(k);
                            upd.run();
                            if (pin.length() == 4) {
                                String s = pin.toString();
                                pin.setLength(0);
                                if (!verify.test(s)) upd.run();
                            }
                        });
                    }
                    r.addView(kv);
                }
                root.addView(r);
            }
        } else {
            PatternView pv = new PatternView(a, th.ink, th.c1, seq -> {
                if (seq.split("-").length < 4) {
                    msg.setTextColor(th.bad);
                    msg.setText("Minimal 4 titik");
                    return;
                }
                verify.test(seq);
            });
            root.addView(pv, new LinearLayout.LayoutParams(a.ui.dp(280), a.ui.dp(280)));
            if (useBio) {
                TextView bb = label(a, "\uD83D\uDC46 Pakai sidik jari", 16, th.ink);
                bb.setPadding(0, a.ui.dp(16), 0, a.ui.dp(8));
                bb.setOnClickListener(v -> bio(a, success));
                root.addView(bb);
            }
        }

        if (cancelable) {
            TextView cb = label(a, "Batal", 16, th.mu);
            cb.setPadding(a.ui.dp(20), a.ui.dp(16), a.ui.dp(20), a.ui.dp(8));
            cb.setOnClickListener(v -> d.dismiss());
            root.addView(cb);
        }

        d.setContentView(root);
        if (useBio) d.setOnShowListener(di -> bio(a, success));
        cur = d;
        d.show();
    }

    // ------------------------------------------------------------------ pola 3x3
    public static class PatternView extends View {
        public interface Done {
            void on(String seq);
        }

        private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG), line = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int[] seq = new int[9];
        private int n = 0;
        private float fx, fy;
        private boolean down;
        private final int ink, hi;
        private final Done cb;

        public PatternView(Context c, int ink, int hi, Done cb) {
            super(c);
            this.ink = ink;
            this.hi = hi;
            this.cb = cb;
            line.setStyle(Paint.Style.STROKE);
            line.setStrokeWidth(c.getResources().getDisplayMetrics().density * 5);
            line.setStrokeCap(Paint.Cap.ROUND);
        }

        private float cx(int i) { return getWidth() / 6f + (i % 3) * getWidth() / 3f; }

        private float cy(int i) { return getHeight() / 6f + (i / 3) * getHeight() / 3f; }

        private boolean has(int i) {
            for (int k = 0; k < n; k++) if (seq[k] == i) return true;
            return false;
        }

        private void hit(MotionEvent e) {
            for (int i = 0; i < 9; i++) {
                if (n < 9 && !has(i) && Math.hypot(e.getX() - cx(i), e.getY() - cy(i)) < getWidth() / 9f) seq[n++] = i;
            }
            fx = e.getX();
            fy = e.getY();
        }

        @Override
        protected void onDraw(Canvas c) {
            line.setColor(hi);
            for (int i = 1; i < n; i++) c.drawLine(cx(seq[i - 1]), cy(seq[i - 1]), cx(seq[i]), cy(seq[i]), line);
            if (down && n > 0) c.drawLine(cx(seq[n - 1]), cy(seq[n - 1]), fx, fy, line);
            float r = getWidth() / 20f;
            for (int i = 0; i < 9; i++) {
                boolean on = has(i);
                dot.setColor(on ? hi : ink);
                if (!on) dot.setAlpha(110);
                c.drawCircle(cx(i), cy(i), on ? r * 1.4f : r, dot);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                    n = 0;
                    down = true;
                    hit(e);
                    invalidate();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    hit(e);
                    invalidate();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    down = false;
                    StringBuilder s = new StringBuilder();
                    for (int i = 0; i < n; i++) s.append(i > 0 ? "-" : "").append(seq[i]);
                    n = 0;
                    invalidate();
                    if (s.length() > 0 && cb != null) cb.on(s.toString());
                    return true;
                default:
                    return super.onTouchEvent(e);
            }
        }
    }

    // ------------------------------------------------------------------ pengaturan
    private static void guard(MainActivity a, Runnable r) {
        if (enabled(a)) prompt(a, true, r);
        else r.run();
    }

    private static EditText pinInput(Ui ui, ViewGroup p) {
        EditText e = ui.input(p, "4 angka", Ui.INK_NUM);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        return e;
    }

    private static void setPin(final MainActivity a, final Runnable done) {
        final Ui ui = a.ui;
        final Ui.Dlg d = ui.dialog();
        LinearLayout b = d.card;
        ui.h2(b, "Atur PIN");
        ui.label(b, "PIN baru (4 angka)");
        final EditText e1 = pinInput(ui, b);
        ui.label(b, "Ulangi PIN");
        final EditText e2 = pinInput(ui, b);
        LinearLayout r = ui.row(b);
        r.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        ui.mt(r, 16);
        ui.btn(r, "Batal", Ui.W, v -> d.close());
        Ui.Btn ok = ui.btn(r, "Simpan", Ui.P, v -> {
            String p1 = e1.getText().toString(), p2 = e2.getText().toString();
            if (p1.length() != 4) {
                ui.toast("PIN harus 4 angka");
                return;
            }
            if (!p1.equals(p2)) {
                ui.toast("PIN tidak sama");
                return;
            }
            setSecret(a, "pin", p1);
            d.close();
            ui.toast("PIN diaktifkan");
            done.run();
        });
        ui.mar(ok, 8, 0, 0, 0);
        d.show();
    }

    private static void setPattern(final MainActivity a, final Runnable done) {
        final Ui ui = a.ui;
        final Ui.Dlg d = ui.dialog();
        LinearLayout b = d.card;
        ui.h2(b, "Atur pola");
        final TextView hint = ui.tv(b, "Gambar pola baru (minimal 4 titik)", 14);
        final String[] first = {null};
        PatternView pv = new PatternView(a, ui.th.cardInk, ui.th.c1, seq -> {
            if (seq.split("-").length < 4) {
                first[0] = null;
                hint.setText("Terlalu pendek. Minimal 4 titik, coba lagi.");
                return;
            }
            if (first[0] == null) {
                first[0] = seq;
                hint.setText("Gambar sekali lagi untuk konfirmasi");
                return;
            }
            if (!first[0].equals(seq)) {
                first[0] = null;
                hint.setText("Pola tidak sama. Ulangi dari awal.");
                return;
            }
            setSecret(a, "pola", seq);
            d.close();
            ui.toast("Pola diaktifkan");
            done.run();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ui.dp(260), ui.dp(260));
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        lp.topMargin = ui.dp(12);
        b.addView(pv, lp);
        LinearLayout r = ui.row(b);
        r.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        ui.mt(r, 12);
        ui.btn(r, "Batal", Ui.W, v -> d.close());
        d.show();
    }

    /** Kartu "Kunci aplikasi" untuk layar Menu. */
    public static void buildSettings(final MainActivity a, LinearLayout into) {
        final Ui ui = a.ui;
        Ui.Card c = ui.card(into, Ui.NORMAL);
        ui.mt(c, 14);
        ui.h2(c, "Kunci aplikasi");
        final TextView status = ui.tv(c, "", 14);
        ui.mar(status, 0, 6, 0, 4);
        TextView warn = ui.tv(c, "Kalau lupa PIN atau pola, satu-satunya jalan adalah menghapus data aplikasi. Buat cadangan dulu di bagian Cadangan data.", 13);
        ui.mar(warn, 0, 0, 0, 8);

        final Runnable[] refresh = new Runnable[1];
        Ui.Flow f = ui.flow(c);
        ui.btn(f, "Atur PIN", Ui.P, v -> guard(a, () -> setPin(a, refresh[0])));
        ui.btn(f, "Atur pola", Ui.W, v -> guard(a, () -> setPattern(a, refresh[0])));
        final Ui.Btn off = ui.btn(f, "Matikan kunci", Ui.R, v -> guard(a, () ->
                ui.confirm("Matikan kunci aplikasi?", () -> {
                    disable(a);
                    refresh[0].run();
                })));

        final CheckBox cb = new CheckBox(a);
        cb.setText("Buka dengan sidik jari");
        cb.setTextColor(ui.th.cardInk);
        cb.setTypeface(ui.th.font);
        cb.setTextSize(14);
        c.addView(cb, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        final CheckBox.OnCheckedChangeListener cbl = (v, on) -> setBio(a, on);

        refresh[0] = () -> {
            boolean en = enabled(a);
            String m = mode(a);
            status.setText(!en ? "Kunci mati." : "Kunci aktif: " + ("pin".equals(m) ? "PIN 4 digit" : "pola") + ".");
            off.setVisibility(en ? View.VISIBLE : View.GONE);
            cb.setOnCheckedChangeListener(null);
            cb.setChecked(bioOn(a));
            cb.setOnCheckedChangeListener(cbl);
            cb.setVisibility(en && bioAvailable(a) ? View.VISIBLE : View.GONE);
        };
        refresh[0].run();
    }
}
