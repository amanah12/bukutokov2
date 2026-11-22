package com.bukutoko.app;

import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TabScreen extends Screen {
    public TabScreen(MainActivity a) {
        super(a);
    }

    @Override
    public boolean isSub() {
        return true;
    }

    @Override
    public void build(LinearLayout into) {
        subHead(into, "Tabungan");

        Ui.Card top = ui.card(into, Ui.C1);
        ui.mt(top, 14);
        ui.small(top, "Jumlah tabungan");
        ui.tv(top, Store.rp(st.tabSal()), 32);
        Ui.Btn ed = ui.btn(top, "✎ Ubah jumlah", Ui.W, v -> openEdit());
        ed.padding(10, 6);
        ui.mt(ed, 8);

        Ui.Card f = ui.card(into, Ui.NORMAL);
        ui.mt(f, 14);
        ui.label(f, "Nominal (Rp)").setPadding(0, 0, 0, 0);
        final EditText nom = ui.input(f, "0", Ui.INK_NUM);
        ui.label(f, "Keterangan");
        final EditText ket = ui.input(f, "mis. sisihkan untuk beli etalase", Ui.INK_TEXT);
        LinearLayout r = ui.row(f);
        ui.mt(r, 12);
        Ui.Btn mn = ui.btn(r, "− Keluar", Ui.P, v -> add(-1, nom, ket));
        mn.color(ui.th.c2, 0xFF111111);
        ui.wt(mn, 1);
        Ui.Btn pl = ui.btn(r, "+ Masuk", Ui.P, v -> add(1, nom, ket));
        ui.wt(pl, 1);
        ui.mar(pl, 10, 0, 0, 0);

        Ui.Card hc = ui.card(into, Ui.NORMAL);
        ui.mt(hc, 14);
        ui.h2(hc, "Riwayat");
        LinearLayout box = ui.col(hc);
        ui.mt(box, 8);
        List<JSONObject> ls = new ArrayList<>(Store.list(st.tab()));
        Collections.reverse(ls);
        if (ls.isEmpty()) ui.small(box, "Belum ada riwayat.");
        boolean first = true;
        for (JSONObject e : ls) {
            if (!first) ui.sep(box);
            first = false;
            final String id = Store.str(e, "id");
            boolean masuk = Store.num(e, "i") != 0;
            LinearLayout row = ui.row(box);
            row.setPadding(0, ui.dp(8), 0, ui.dp(8));
            LinearLayout nm = ui.col(row);
            ui.wt(nm, 1);
            String k = Store.str(e, "ket");
            ui.tv(nm, k.isEmpty() ? (masuk ? "Pemasukan" : "Pengeluaran") : k, 17);
            ui.note(nm, Store.dmy(Store.str(e, "d")) + (masuk ? " · pemasukan" : " · pengeluaran"));
            double v = masuk ? Store.num(e, "i") : Store.num(e, "o");
            ui.money(row, (masuk ? "+" : "−") + Store.rp(v), 17, masuk ? 1 : -1);
            ui.btn(row, "✕", Ui.R, x -> ui.confirm("Hapus catatan ini?", () -> {
                Store.removeIf(st.tab(), o -> Store.str(o, "id").equals(id));
                st.save();
                a.refresh();
            }));
        }
    }

    private void add(int sg, EditText nom, EditText ket) {
        long n = Ui.numOf(nom);
        if (n <= 0) {
            ui.toast("Isi nominal");
            return;
        }
        final String k = ket.getText().toString().trim();
        Runnable go = () -> {
            JSONObject e = new JSONObject();
            Store.put(e, "id", Store.uid());
            Store.put(e, "d", Store.today());
            Store.put(e, "i", sg > 0 ? n : 0);
            Store.put(e, "o", sg < 0 ? n : 0);
            Store.put(e, "ket", k);
            st.tab().put(e);
            st.save();
            a.refresh();
        };
        if (sg < 0 && n > st.tabSal()) ui.confirm("Pengeluaran lebih besar dari tabungan. Tetap catat?", go);
        else go.run();
    }

    private void openEdit() {
        Ui.Dlg d = ui.dialog();
        LinearLayout b = d.card;
        ui.h2(b, "Ubah jumlah tabungan");
        TextView p = ui.tv(b, "Sekarang: " + Store.rp(st.tabSal()), 17);
        ui.mt(p, 8);
        ui.label(b, "Jumlah baru (Rp)");
        EditText e = ui.input(b, "0", Ui.INK_NUM);
        Ui.setText(e, String.valueOf(Math.round(st.tabSal())));
        ui.mt(ui.note(b, "Selisihnya dicatat otomatis di riwayat sebagai Koreksi jumlah."), 8);
        LinearLayout r = ui.row(b);
        r.setGravity(android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
        ui.mt(r, 16);
        ui.btn(r, "Batal", Ui.W, v -> d.close());
        Ui.Btn ok = ui.btn(r, "Simpan", Ui.P, v -> {
            if (e.getText().toString().trim().isEmpty()) {
                ui.toast("Isi jumlah");
                return;
            }
            long dlt = Ui.numOf(e) - Math.round(st.tabSal());
            if (dlt != 0) {
                JSONObject x = new JSONObject();
                Store.put(x, "id", Store.uid());
                Store.put(x, "d", Store.today());
                Store.put(x, "i", dlt > 0 ? dlt : 0);
                Store.put(x, "o", dlt < 0 ? -dlt : 0);
                Store.put(x, "ket", "Koreksi jumlah");
                st.tab().put(x);
            }
            st.save();
            d.close();
            a.refresh();
        });
        ui.mar(ok, 8, 0, 0, 0);
        d.show();
    }
}
