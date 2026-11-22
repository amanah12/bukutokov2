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
    private ScrollView scroll;
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
        show("home");
        if (!st.S.optBoolean("seen", false)) Tutorial.show(this);
    }

    @Override
    public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        String k = curKey;
        buildAll();
        show(k);
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
        th = Th.make(Store.str(s, "tema"), isNight(getResources().getConfiguration()), Store.str(s, "c1"), Store.str(s, "c2"));
        ui = new Ui(this, th);

        rootV = new LinearLayout(this);
        rootV.setOrientation(LinearLayout.VERTICAL);
        rootV.setBackground(new Ui.PageBg(th, ui.dn));

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        rootV.addView(scroll, new LinearLayout.LayoutParams(Ui.MATCH, 0, 1f));

        FrameLayout frame = new FrameLayout(this);
        scroll.addView(frame, new ViewGroup.LayoutParams(Ui.MATCH, Ui.WRAP));
        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(ui.dp(14), ui.dp(14), ui.dp(14), ui.dp(24));
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

        buildTabs();
        setContentView(rootV);

        Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        win.setStatusBarColor(th.bg);
        int navClr = th.tabFloat ? th.bg : (th.tabGrad ? th.tabG2 : th.tabBg);
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
            t.setOnClickListener(v -> show(k));
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

        Ui.Btn sb = ui.btn(r, "🧾 Struk", Ui.P, v -> show("struk"));
        sb.padding(12, 8);
        Ui.Btn cb = ui.btn(r, "⚙️", Ui.P, v -> show("cfg"));
        cb.padding(12, 8).text(18);
        ui.mar(cb, 8, 0, 0, 0);
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

    public void show(String key) {
        hideKeyboard();
        curKey = key;
        cur = make(key);
        buildHeader();
        content.removeAllViews();
        cur.build(content);
        scroll.scrollTo(0, 0);
        tabHighlight();
    }

    /** Dipanggil setelah data berubah. */
    public void refresh() {
        if (cur != null) cur.onData();
    }

    /** Bangun ulang layar aktif, mempertahankan posisi gulir. */
    public void rebuild() {
        final int y = scroll.getScrollY();
        buildHeader();
        content.removeAllViews();
        cur.build(content);
        scroll.post(() -> scroll.scrollTo(0, y));
    }

    /** Ganti tema/warna: bangun ulang semuanya. */
    public void restyle() {
        final int y = scroll == null ? 0 : scroll.getScrollY();
        String k = curKey;
        buildAll();
        show(k);
        scroll.post(() -> scroll.scrollTo(0, y));
    }

    /** Perbarui judul/logo di header tanpa membangun ulang layar. */
    public void refreshHeader() {
        buildHeader();
    }

    public ScrollView scroller() {
        return scroll;
    }
}
