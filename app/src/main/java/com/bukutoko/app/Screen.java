package com.bukutoko.app;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Satu layar. build() mengisi wadah konten; onData() dipanggil saat data berubah. */
public abstract class Screen {
    protected final MainActivity a;
    protected final Store st;
    protected final Ui ui;

    protected Screen(MainActivity a) {
        this.a = a;
        this.st = a.st;
        this.ui = a.ui;
    }

    public abstract void build(LinearLayout into);

    /** Default: bangun ulang seluruh layar. Layar dengan form panjang menimpa ini. */
    public void onData() {
        a.rebuild();
    }

    public boolean isSub() {
        return false;
    }

    // ------------------------------------------------------------------ helper bersama

    /** ScrollView yang tingginya dibatasi (mengikuti isi, maksimal max px). */
    public static class MaxScroll extends ScrollView {
        private final int max;

        public MaxScroll(Context c, int max) {
            super(c);
            this.max = max;
        }

        @Override
        protected void onMeasure(int w, int h) {
            super.onMeasure(w, View.MeasureSpec.makeMeasureSpec(max, View.MeasureSpec.AT_MOST));
        }
    }

    protected int screenH() {
        return a.getResources().getDisplayMetrics().heightPixels;
    }

    /** Baris atas layar turunan: tombol kembali + judul di kanan. */
    protected void subHead(LinearLayout into, String title) {
        LinearLayout r = ui.row(into);
        ui.btn(r, "← Kembali", Ui.W, v -> a.show("home"));
        TextView h = ui.h2(r, title);
        ui.wt(h, 1);
        h.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
    }

    private void styleCell(TextView t, float w, boolean right) {
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setGravity((right ? Gravity.END : Gravity.START) | Gravity.CENTER_VERTICAL);
        t.setPadding(ui.dp(6), ui.dp(8), ui.dp(6), ui.dp(8));
        ui.size(t, w, -2);
    }

    /** Sel tabel berlebar tetap (dp). mode: 0 biasa, 1 masuk (hijau), -1 keluar (merah). */
    protected TextView tc(LinearLayout row, String s, float w, boolean right, int mode) {
        TextView t = mode == 0 ? ui.tv(row, s, 14) : ui.money(row, s, 14, mode);
        styleCell(t, w, right);
        return t;
    }

    /** Baris judul tabel berlatar warna utama. */
    protected LinearLayout head(LinearLayout tbl, String[] names, float[] widths, boolean[] right) {
        LinearLayout hr = new LinearLayout(a);
        hr.setOrientation(LinearLayout.HORIZONTAL);
        Ui.Box b = new Ui.Box();
        b.dn = ui.dn;
        b.fill = ui.th.c1;
        hr.setBackground(b);
        tbl.addView(hr, new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP));
        for (int i = 0; i < names.length; i++) {
            TextView t = ui.tv(hr, names[i], 13);
            t.setTextColor(0xFF111111);
            t.setAllCaps(ui.th.caps);
            styleCell(t, widths[i], right[i]);
        }
        return hr;
    }
}
