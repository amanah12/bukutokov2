package com.bukutoko.app;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    public Store st;
    public Ui ui;
    public Th th;

    private LinearLayout rootV, headerBox, content;
    private LinearLayout tabBar;
    private FrameLayout frameRoot;
    private SmoothScroll scroll;
    private Motion motion;
    private LiquidBar liquid;
    private Fx fx;
    private boolean liquidOn, motionOn, revealOn, gustOn, homeShown;
    private Screen cur;
    public String curKey = "home";

    private static final String[] TAB_KEYS = {"home", "kas", "bon", "rek", "set"};
    private static final String[] TAB_LABELS = {"Beranda", "Kas", "Bon", "Rekap", "Menu"};
    private final TextView[] tabViews = new TextView[5];

    public interface Result {
        void on(Intent data);
    }

    private final SparseArray<Result> results = new SparseArray<>();
    private int nextReq = 100;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        st = new Store(this);
        buildAll();
        showSplash(!st.S.optBoolean("seen", false));
    }

    @Override
    public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        String k = curKey;
        buildAll();
        show(k, false);
    }

    @Override
    public void onBackPressed() {
        if (!curKey.equals("home")) show("home");
        else super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (st != null) st.save();
    }

    // ------------------------------------------------------------------ hasil activity
    public void startForResult(Intent i, Result r) {
        int req = nextReq++;
        results.put(req, r);
        try {
            startActivityForResult(i, req);
        } catch (Exception e) {
            results.remove(req);
            ui.toast("Tidak ada aplikasi untuk aksi ini");
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        Result r = results.get(req);
        results.remove(req);
        if (r != null && res == RESULT_OK && data != null) r.on(data);
    }

    // ------------------------------------------------------------------ tema & kerangka
    public static boolean isNight(Configuration cf) {
        return (cf.uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    /** Bangun ulang kerangka (dipanggil saat tema/warna berubah). */
    public void buildAll() {
        JSONObject s = st.set();
        if (!Th.valid(Store.str(s, "tema"))) { // tema lama (buku/neon/dll) -> Classic
            String[] d = Th.def("classic");
            Store.put(s, "tema", d[0]);
            Store.put(s, "c1", d[2]);
            Store.put(s, "c2", d[3]);
            st.save();
        }
        th = Th.make(Store.str(s, "tema"), isNight(getResources().getConfiguration()), Store.str(s, "c1"), Store.str(s, "c2"), s.optJSONObject("diy"));
        ui = new Ui(this, th);
        loadFlags();

        frameRoot = new FrameLayout(this);
        frameRoot.setClipChildren(false);
        rootV = new LinearLayout(this);
        rootV.setOrientation(LinearLayout.VERTICAL);
        rootV.setBackground(new Ui.PageBg(th, ui.dn));

        scroll = new SmoothScroll(this);
        rootV.addView(scroll, new LinearLayout.LayoutParams(Ui.MATCH, 0, 1f));

        FrameLayout frame = new FrameLayout(this);
        scroll.addView(frame, new ViewGroup.LayoutParams(Ui.MATCH, Ui.WRAP));
        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(ui.dp(14), ui.dp(14), ui.dp(14), ui.dp(liquidOn ? 104 : 24));
        main.setClipChildren(false);
        main.setClipToPadding(false);
        int w = Math.min(ui.screenW(), ui.dp(640));
        frame.addView(main, new FrameLayout.LayoutParams(w, Ui.WRAP, Gravity.CENTER_HORIZONTAL));

        headerBox = new LinearLayout(this);
        headerBox.setOrientation(LinearLayout.VERTICAL);
        main.addView(headerBox, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setClipChildren(false);
        content.setClipToPadding(false);
        main.addView(content, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));

        motion = new Motion(scroll, content, ui.dn);
        scroll.setOnScroll((y, oy) -> {
            motion.check();
            if (motionOn) motion.tilt(true);
        });
        content.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            motion.check();
            if (motionOn) motion.tilt(true);
        });

        frameRoot.addView(rootV, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        liquid = null;
        if (liquidOn) {
            liquid = new LiquidBar(this, th, ui.dn, TAB_LABELS, i -> onTab(TAB_KEYS[i]));
            int bw = Math.min(ui.screenW() - ui.dp(20), ui.dp(560));
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(bw, Ui.WRAP, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            lp.bottomMargin = ui.dp(4);
            frameRoot.addView(liquid, lp);
        } else {
            buildTabs();
        }
        fx = new Fx(this, ui.dn);
        frameRoot.addView(fx, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        fx.setTick(() -> {
            if (motion != null) motion.sway(fx == null ? 0f : fx.windX, gustOn);
        });
        setContentView(frameRoot);
        applyFx();

        Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        win.setStatusBarColor(th.bg);
        int navClr = (liquidOn || th.tabFloat) ? th.bg : (th.tabGrad ? th.tabG2 : th.tabBg);
        win.setNavigationBarColor(navClr);
        boolean lightBg = !th.dark && Color.luminance(th.bg) > 0.5f;
        boolean lightNav = Color.luminance(navClr) > 0.5f;
        int fl = rootV.getSystemUiVisibility();
        fl = lightBg ? (fl | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR) : (fl & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        if (Build.VERSION.SDK_INT >= 26) {
            fl = lightNav ? (fl | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR) : (fl & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        rootV.setSystemUiVisibility(fl);
    }

    private void buildTabs() {
        Ui.Box b = new Ui.Box();
        b.dn = ui.dn;
        b.fill = th.tabBg;
        if (th.tabGrad) {
            b.grad = true;
            b.g1 = th.tabG1;
            b.g2 = th.tabG2;
        }
        b.r = th.tabR;
        b.topOnly = !th.tabFloat && th.tabR > 0;
        b.bw = th.tabR == 0 ? 0 : th.tabBw;
        b.border = th.tabBorder;
        b.dash = th.tabDash;
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        if (th.tabR == 0) {
            View line = new View(this);
            line.setBackgroundColor(th.tabBorder);
            wrap.addView(line, new LinearLayout.LayoutParams(Ui.MATCH, ui.dp(th.tabBw)));
        }
        tabBar = new LinearLayout(this);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setGravity(Gravity.CENTER_VERTICAL);
        wrap.addView(tabBar, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        if (th.tabR == 0) wrap.setBackgroundColor(th.tabBg);
        else wrap.setBackground(b);
        int pad = th.tabR == 0 ? 0 : ui.dp(6);
        tabBar.setPadding(pad, pad, pad, pad);

        for (int i = 0; i < 5; i++) {
            final String k = TAB_KEYS[i];
            TextView t = new TextView(this);
            t.setText(TAB_LABELS[i]);
            t.setTextSize(12);
            t.setTypeface(th.font);
            t.setAllCaps(th.caps);
            t.setGravity(Gravity.CENTER);
            t.setSingleLine(true);
            t.setPadding(ui.dp(4), ui.dp(th.tabR == 0 ? 15 : 12), ui.dp(4), ui.dp(th.tabR == 0 ? 15 : 12));
            t.setOnClickListener(v -> onTab(k));
            tabBar.addView(t, new LinearLayout.LayoutParams(0, Ui.WRAP, 1f));
            tabViews[i] = t;
            if (th.tabDiv && i < 4) {
                View d = new View(this);
                d.setBackgroundColor(th.tabBorder);
                tabBar.addView(d, new LinearLayout.LayoutParams(ui.dp(th.tabBw), Ui.MATCH));
            }
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP);
        if (th.tabFloat) lp.setMargins(ui.dp(12), ui.dp(4), ui.dp(12), ui.dp(10));
        rootV.addView(wrap, lp);
    }

    private void tabHighlight() {
        if (liquid != null) {
            int idx = -1;
            for (int i = 0; i < 5; i++) if (TAB_KEYS[i].equals(curKey)) idx = i;
            liquid.select(idx, true);
            return;
        }
        for (int i = 0; i < 5; i++) {
            boolean on = TAB_KEYS[i].equals(curKey);
            TextView t = tabViews[i];
            t.setTextColor(on ? th.tabOnInk : th.tabInk);
            if (on) {
                Ui.Box b = new Ui.Box();
                b.dn = ui.dn;
                b.fill = th.tabOnBg;
                b.r = th.tabBtnR;
                t.setBackground(b);
            } else t.setBackground(null);
        }
    }

    // ------------------------------------------------------------------ header
    private void buildHeader() {
        headerBox.removeAllViews();
        LinearLayout r = ui.row(headerBox);
        ui.mar(r, 0, 0, 0, 14);

        Ui.Box lb = ui.boxFor(Ui.C1);
        lb.setShape("");
        lb.cr = null;
        lb.r = th.id.equals("imut") ? 99 : Math.min(th.cardR, 16);
        if (th.cardBw > 0 && th.cardBorder == 0) lb.bw = 0;
        LinearLayout lg = new LinearLayout(this);
        lg.setGravity(Gravity.CENTER);
        lg.setBackground(lb);
        lg.setPadding(0, 0, Math.round(lb.ex()), Math.round(lb.ey()));
        ImageView iv = new ImageView(this);
        String logo = Store.str(st.set(), "logo");
        Bitmap bm = Ui.decode(logo);
        if (bm != null) {
            iv.setImageBitmap(bm);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            lg.setPadding(ui.dp(3), ui.dp(3), ui.dp(3) + Math.round(lb.ex()), ui.dp(3) + Math.round(lb.ey()));
        } else {
            iv.setImageResource(R.drawable.ic_warung);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        }
        lg.addView(iv, new LinearLayout.LayoutParams(bm != null ? Ui.MATCH : ui.dp(36), bm != null ? Ui.MATCH : ui.dp(36)));
        r.addView(lg, new LinearLayout.LayoutParams(ui.dp(52) + Math.round(lb.ex()), ui.dp(52) + Math.round(lb.ey())));

        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, Ui.WRAP, 1f);
        tl.leftMargin = ui.dp(10);
        r.addView(t, tl);
        String nm = Store.str(st.set(), "name");
        TextView n = ui.tv(t, nm.isEmpty() ? "Toko Saya" : nm, 19);
        n.setAllCaps(th.caps);
        n.setSingleLine(true);
        n.setEllipsize(android.text.TextUtils.TruncateAt.END);
        TextView d = ui.tv(t, new SimpleDateFormat("EEEE, d MMMM", new Locale("id", "ID")).format(new Date()), 14);
        d.setTextColor(th.mu);

        if (!Store.str(st.set(), "qris").isEmpty()) {
            Ui.Btn qb = ui.btn(r, "▣ QRIS", Ui.P, v -> showQris());
            qb.padding(12, 8);
            ui.mar(qb, 0, 0, 8, 0);
        }
        Ui.Btn sb = ui.btn(r, "🧾 Struk", Ui.P, v -> show("struk"));
        sb.padding(12, 8);
        Ui.Btn cb = ui.btn(r, "⚙️", Ui.P, v -> show("cfg"));
        cb.padding(12, 8).text(18);
        ui.mar(cb, 8, 0, 0, 0);
    }

    /** Tampilkan gambar QRIS toko supaya pelanggan bisa scan. */
    public void showQris() {
        Bitmap bm = Ui.decode(Store.str(st.set(), "qris"));
        if (bm == null) {
            ui.toast("Belum ada QRIS");
            return;
        }
        final Ui.Dlg d = ui.dialog();
        LinearLayout b = d.card;
        ui.h2(b, "Bayar dengan QRIS");
        ImageView iv = new ImageView(this);
        iv.setImageBitmap(bm);
        iv.setAdjustViewBounds(true);
        iv.setBackgroundColor(Color.WHITE);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        b.addView(iv, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        ui.mt(iv, 10);
        LinearLayout r = ui.row(b);
        r.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        ui.mt(r, 12);
        ui.btn(r, "Tutup", Ui.P, v -> d.close());
        d.show();
    }

    // ------------------------------------------------------------------ navigasi
    private Screen make(String k) {
        switch (k) {
            case "kas":
                return new KasScreen(this);
            case "bon":
                return new BonScreen(this);
            case "rek":
                return new RekapScreen(this);
            case "set":
                return new SetScreen(this);
            case "tab":
                return new TabScreen(this);
            case "struk":
                return new StrukScreen(this);
            case "cfg":
                return new CfgScreen(this);
            default:
                return new HomeScreen(this);
        }
    }

    public void hideKeyboard() {
        View v = getCurrentFocus();
        if (v != null) {
            InputMethodManager im = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (im != null) im.hideSoftInputFromWindow(v.getWindowToken(), 0);
            v.clearFocus();
        }
    }

    /** Ketuk tab: pindah layar, atau gulir halus ke atas kalau sudah di tab itu. */
    private void onTab(String k) {
        if (k.equals(curKey)) scroll.smoothTop();
        else show(k);
    }

    public void show(String key) {
        show(key, true);
    }

    private void show(String key, boolean anim) {
        hideKeyboard();
        homeShown = true;
        curKey = key;
        cur = make(key);
        buildHeader();
        motion.clear();
        content.removeAllViews();
        cur.build(content);
        scroll.scrollTo(0, 0);
        tabHighlight();
        motion.reveal(anim && revealOn);
        motion.tilt(motionOn);
    }

    /** Dipanggil setelah data berubah. */
    public void refresh() {
        if (cur != null) cur.onData();
    }

    /** Bangun ulang layar aktif, mempertahankan posisi gulir. */
    public void rebuild() {
        final int y = scroll.getScrollY();
        buildHeader();
        motion.clear();
        content.removeAllViews();
        cur.build(content);
        scroll.post(() -> {
            scroll.scrollTo(0, y);
            motion.tilt(motionOn);
        });
    }

    /** Ganti tema/warna: bangun ulang semuanya. */
    public void restyle() {
        final int y = scroll == null ? 0 : scroll.getScrollY();
        String k = curKey;
        buildAll();
        show(k, false);
        scroll.post(() -> scroll.scrollTo(0, y));
    }

    // ------------------------------------------------------------------ efek dan gerak
    public JSONObject fxCfg() {
        JSONObject set = st.set();
        JSONObject f = set.optJSONObject("fx");
        if (f == null) {
            f = new JSONObject();
            Store.put(set, "fx", f);
        }
        return f;
    }

    /** Efek yang menyala dari awal: gerak kartu, muncul satu per satu, navbar kaca. */
    public boolean fxOn(String key) {
        JSONObject f = fxCfg();
        if (f.has(key)) return f.optBoolean(key);
        return key.equals("motion") || key.equals("reveal") || key.equals("liquid");
    }

    private void loadFlags() {
        liquidOn = fxOn("liquid");
        motionOn = fxOn("motion");
        revealOn = fxOn("reveal");
        gustOn = fxOn("gust");
    }

    /** Terapkan pengaturan efek tanpa membangun ulang kerangka. */
    public void applyFx() {
        if (fx == null || motion == null) return;
        motionOn = fxOn("motion");
        revealOn = fxOn("reveal");
        gustOn = fxOn("gust");
        int lv = 2;
        try {
            lv = Integer.parseInt(fxCfg().optString("level", "2"));
        } catch (Exception ignored) {
        }
        boolean lightBg = !th.dark && Color.luminance(th.bg) > 0.5f;
        fx.configure(fxOn("snow"), fxOn("rain"), fxOn("wind"), fxOn("leaf"), gustOn, fxOn("glass"), lv, lightBg);
        if (!gustOn) motion.sway(0f, false);
        if (!revealOn) motion.showAll();
        motion.tilt(motionOn);
    }

    // ------------------------------------------------------------------ splash
    private void showSplash(final boolean firstRun) {
        final FrameLayout sp = new FrameLayout(this);
        sp.setBackground(new Ui.PageBg(th, ui.dn));
        sp.setClickable(true);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        sp.addView(col, new FrameLayout.LayoutParams(Ui.WRAP, Ui.WRAP, Gravity.CENTER));

        Ui.Box lb = ui.boxFor(Ui.C1);
        lb.setShape("");
        lb.cr = null;
        lb.r = th.id.equals("imut") ? 99 : Math.min(th.cardR, 26);
        if (th.cardBw > 0 && th.cardBorder == 0) lb.bw = 0;
        final LinearLayout lg = new LinearLayout(this);
        lg.setGravity(Gravity.CENTER);
        lg.setBackground(lb);
        lg.setPadding(0, 0, Math.round(lb.ex()), Math.round(lb.ey()));
        ImageView iv = new ImageView(this);
        Bitmap bm = Ui.decode(Store.str(st.set(), "logo"));
        if (bm != null) {
            iv.setImageBitmap(bm);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            lg.setPadding(ui.dp(4), ui.dp(4), ui.dp(4) + Math.round(lb.ex()), ui.dp(4) + Math.round(lb.ey()));
            lg.addView(iv, new LinearLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        } else {
            iv.setImageResource(R.drawable.ic_warung);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            lg.addView(iv, new LinearLayout.LayoutParams(ui.dp(60), ui.dp(60)));
        }
        col.addView(lg, new LinearLayout.LayoutParams(ui.dp(96) + Math.round(lb.ex()), ui.dp(96) + Math.round(lb.ey())));

        String nm = Store.str(st.set(), "name");
        final TextView n = ui.tv(col, nm.isEmpty() ? "Toko Saya" : nm, 26);
        n.setAllCaps(th.caps);
        n.setTextColor(th.ink);
        n.setGravity(Gravity.CENTER);
        n.setSingleLine(true);
        ui.mt(n, 22);
        final TextView sub = ui.tv(col, "Buku Toko · stok, kas, dan bon", 14);
        sub.setTextColor(th.mu);
        sub.setGravity(Gravity.CENTER);
        ui.mt(sub, 4);

        lg.setAlpha(0f);
        lg.setScaleX(0.55f);
        lg.setScaleY(0.55f);
        lg.setRotation(-10f);
        n.setAlpha(0f);
        n.setTranslationY(ui.dp(18));
        sub.setAlpha(0f);
        sub.setTranslationY(ui.dp(14));
        frameRoot.addView(sp, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));

        lg.animate().alpha(1f).scaleX(1f).scaleY(1f).rotation(0f).setDuration(620)
                .setInterpolator(new OvershootInterpolator(1.6f)).start();
        n.animate().alpha(1f).translationY(0f).setStartDelay(260).setDuration(460)
                .setInterpolator(new DecelerateInterpolator(1.8f)).start();
        sub.animate().alpha(0.9f).translationY(0f).setStartDelay(420).setDuration(460)
                .setInterpolator(new DecelerateInterpolator(1.8f)).start();

        sp.postDelayed(() -> {
            if (!homeShown) show("home", true);
            sp.animate().alpha(0f).scaleX(1.05f).scaleY(1.05f).setDuration(420)
                    .setInterpolator(new DecelerateInterpolator()).withEndAction(() -> {
                        ViewGroup g = (ViewGroup) sp.getParent();
                        if (g != null) g.removeView(sp);
                        if (firstRun && !isFinishing() && !st.S.optBoolean("seen", false)) Tutorial.show(this);
                    }).start();
        }, 1350);
    }

    /** Perbarui judul/logo di header tanpa membangun ulang layar. */
    public void refreshHeader() {
        buildHeader();
    }

    public ScrollView scroller() {
        return scroll;
    }
}
