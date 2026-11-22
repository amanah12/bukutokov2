package com.bukutoko.app;

import android.view.View;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class RekapScreen extends Screen {
    private String per = "bulan";

    private static final String[] HEAD = {"Periode", "Masuk", "Keluar", "Laba", "Sisa"};
    private static final float[] W = {96, 100, 100, 92, 104};
    private static final boolean[] RIGHT = {false, true, true, true, true};

    public RekapScreen(MainActivity a) {
        super(a);
    }

    private void legend(LinearLayout r, int color, String s) {
        View sq = new View(a);
        Ui.Box b = new Ui.Box();
        b.dn = ui.dn;
        b.fill = color;
        b.bw = 2;
        b.border = ui.th.cardInk;
        sq.setBackground(b);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ui.dp(12), ui.dp(12));
        lp.rightMargin = ui.dp(6);
        r.addView(sq, lp);
        android.widget.TextView t = ui.tv(r, s, 12);
        ui.mar(t, 0, 0, 16, 0);
    }

    @Override
    public void build(LinearLayout into) {
        int n = per.equals("hari") ? 14 : per.equals("minggu") ? 12 : per.equals("bulan") ? 12 : 10;
        TreeMap<String, double[]> B = st.rekap(per);
        List<String> ks = new ArrayList<>(B.keySet());
        if (ks.size() > n) ks = new ArrayList<>(ks.subList(ks.size() - n, ks.size()));

        Ui.Card c = ui.card(into, Ui.NORMAL);
        ui.seg(c, new String[]{"Harian", "Mingguan", "Bulanan", "Tahunan"},
                new String[]{"hari", "minggu", "bulan", "tahun"}, per, k -> {
                    per = k;
                    a.rebuild();
                });
        ui.mar(ui.h2(c, "Masuk dan keluar"), 0, 16, 0, 8);
        if (ks.isEmpty()) {
            ui.small(c, "Belum ada data.");
        } else {
            String[] lb = new String[ks.size()];
            double[] in = new double[ks.size()], out = new double[ks.size()];
            for (int i = 0; i < ks.size(); i++) {
                lb[i] = Store.bucketLabel(per, ks.get(i), false);
                in[i] = B.get(ks.get(i))[0];
                out[i] = B.get(ks.get(i))[1];
            }
            ChartView cv = new ChartView(a, lb, in, out, ui.th.c1, ui.th.c2, ui.th.cardInk,
                    "line".equals(Store.str(st.set(), "chart")));
            c.addView(cv, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        }
        LinearLayout lg = ui.row(c);
        ui.mt(lg, 8);
        legend(lg, ui.th.c1, "Masuk");
        legend(lg, ui.th.c2, "Keluar");

        Ui.Card tc = ui.card(into, Ui.NORMAL);
        ui.mt(tc, 14);
        LinearLayout hs = ui.hscroll(tc);
        LinearLayout tbl = ui.col(hs);
        head(tbl, HEAD, W, RIGHT);
        if (ks.isEmpty()) {
            ui.note(tbl, "Belum ada catatan").setPadding(ui.dp(6), ui.dp(10), ui.dp(6), ui.dp(10));
        }
        for (int i = ks.size() - 1; i >= 0; i--) {
            double[] m = B.get(ks.get(i));
            LinearLayout row = new LinearLayout(a);
            row.setOrientation(LinearLayout.HORIZONTAL);
            tbl.addView(row, new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP));
            tc(row, Store.bucketLabel(per, ks.get(i), true), W[0], false, 0);
            tc(row, Store.rp(m[0]), W[1], true, 1);
            tc(row, Store.rp(m[1]), W[2], true, -1);
            tc(row, Store.rp(m[2]), W[3], true, 0);
            tc(row, Store.rp(m[3]), W[4], true, 0);
            ui.sep(tbl);
        }
        android.widget.TextView nt = ui.note(tc, "Laba = dari penjualan lewat tombol − di stok. Sisa = uang laci di akhir periode.");
        nt.setTextSize(12);
        ui.mt(nt, 8);
    }
}
