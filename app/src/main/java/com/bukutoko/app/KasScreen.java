package com.bukutoko.app;

import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KasScreen extends Screen {
    private String selJenis = "in", selLaci = "";

    private static final String[] HEAD = {"Tgl", "Ket", "Masuk", "Keluar", "Sisa", ""};
    private static final float[] W = {52, 150, 92, 92, 100, 44};
    private static final boolean[] RIGHT = {false, false, true, true, true, false};

    public KasScreen(MainActivity a) {
        super(a);
    }

    private void tight(TextView t) {
        ui.mar(t, 0, 0, 0, 3);
    }

    @Override
    public void build(LinearLayout into) {
        if (selLaci.isEmpty() || !st.hasLaci(selLaci)) selLaci = st.firstLaci();

        Ui.Card f = ui.card(into, Ui.NORMAL);
        LinearLayout r = ui.row(f);
        LinearLayout c1 = ui.col(r);
        ui.wt(c1, 1);
        tight(ui.label(c1, "Jenis"));
        final Ui.Select jenis = ui.select(c1);
        jenis.options(new String[]{"Masuk", "Keluar / kulak", "Pribadi (jajan cilok dll)"},
                new String[]{"in", "out", "jajan"}).set(selJenis);
        jenis.onChange = () -> selJenis = jenis.value();
        LinearLayout c2 = ui.col(r);
        ui.wt(c2, 1);
        ui.mar(c2, 10, 0, 0, 0);
        tight(ui.label(c2, "Nominal"));
        final EditText nom = ui.input(c2, "0", Ui.INK_NUM);

        ui.label(f, "Laci");
        final Ui.Select laci = ui.select(f);
        List<String> ids = new ArrayList<>(), nms = new ArrayList<>();
        for (JSONObject L : Store.list(st.lacis())) {
            ids.add(Store.str(L, "id"));
            nms.add(Store.str(L, "nama"));
        }
        laci.options(nms.toArray(new String[0]), ids.toArray(new String[0])).set(selLaci);
        laci.onChange = () -> selLaci = laci.value();

        ui.label(f, "Keterangan");
        final EditText ket = ui.input(f, "mis. kulak telur, cilok", Ui.INK_TEXT);

        Ui.Flow fl = ui.flow(f);
        ui.mt(fl, 12);
        ui.btn(fl, "Simpan", Ui.P, v -> {
            long n = Ui.numOf(nom);
            if (n <= 0) {
                ui.toast("Isi nominal");
                return;
            }
            String j = jenis.value(), k = ket.getText().toString().trim(), lc = laci.value();
            if (j.equals("in")) st.addLog(k.isEmpty() ? "Pemasukan" : k, n, 0, 0, lc);
            else if (j.equals("out")) st.addLog(k.isEmpty() ? "Pengeluaran" : k, 0, n, 0, lc);
            else st.addLog("Jajan: " + (k.isEmpty() ? "pribadi" : k), 0, n, 0, lc);
            a.refresh();
        });
        Ui.Btn mb = ui.btn(fl, "Atur laci dan modal", Ui.W, v -> a.show("cfg"));
        mb.setLayoutParams(Ui.lpOf(mb));

        // ---- tabel catatan harian
        Ui.Card tc = ui.card(into, Ui.NORMAL);
        ui.mt(tc, 14);
        TextView h = ui.h2(tc, "Catatan harian");
        ui.mar(h, 0, 0, 0, 8);
        LinearLayout hs = ui.hscroll(tc);
        LinearLayout tbl = ui.col(hs);
        head(tbl, HEAD, W, RIGHT);

        double run = st.MT(), tin = 0, tout = 0;
        List<JSONObject> ls = Store.list(st.log());
        Collections.sort(ls, (x, y) -> Store.str(x, "d").compareTo(Store.str(y, "d")));
        List<JSONObject> rows = new ArrayList<>();
        List<Double> sisa = new ArrayList<>();
        for (JSONObject e : ls) {
            double i = Store.num(e, "i"), o = Store.num(e, "o");
            tin += i;
            tout += o;
            run += i - o;
            rows.add(e);
            sisa.add(run);
        }
        for (int k = rows.size() - 1; k >= 0; k--) {
            final JSONObject e = rows.get(k);
            final String id = Store.str(e, "id");
            LinearLayout row = new LinearLayout(a);
            row.setOrientation(LinearLayout.HORIZONTAL);
            tbl.addView(row, new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP));
            double i = Store.num(e, "i"), o = Store.num(e, "o");
            tc(row, Store.dm(Store.str(e, "d")), W[0], false, 0);
            String lcn = st.lcE(e).equals(st.firstLaci()) ? "" : " (" + st.lcNm(st.lcE(e)) + ")";
            tc(row, Store.str(e, "ket") + lcn, W[1], false, 0);
            tc(row, i != 0 ? Store.rp(i) : "", W[2], true, 1);
            tc(row, o != 0 ? Store.rp(o) : "", W[3], true, -1);
            tc(row, Store.rp(sisa.get(k)), W[4], true, 0);
            Ui.Btn del = ui.btn(row, "✕", Ui.R, v -> {
                JSONObject en = Store.find(st.log(), id);
                if (en == null) return;
                String adj = st.itemOfLog(en);
                ui.confirm("Hapus catatan ini?" + (adj != null ? "\n(Stok " + adj + " dikembalikan)" : "\n(Stok tidak berubah)"), () -> {
                    st.deleteLog(id);
                    a.refresh();
                });
            });
            ui.size(del, W[5], -2);
            ui.sep(tbl);
        }
        if (rows.isEmpty()) {
            TextView em = ui.note(tbl, "Belum ada catatan.");
            em.setPadding(ui.dp(6), ui.dp(10), ui.dp(6), ui.dp(10));
        }
        LinearLayout ft = new LinearLayout(a);
        ft.setOrientation(LinearLayout.HORIZONTAL);
        tbl.addView(ft, new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP));
        tc(ft, "", W[0], false, 0);
        tc(ft, "Jumlah", W[1], false, 0);
        tc(ft, Store.rp(tin), W[2], true, 1);
        tc(ft, Store.rp(tout), W[3], true, -1);
        tc(ft, Store.rp(st.MT() + tin - tout), W[4], true, 0);
        tc(ft, "", W[5], false, 0);
    }
}
