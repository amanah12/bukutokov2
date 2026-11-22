package com.bukutoko.app;

import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Atur laci uang dan kategori barang. */
public class CfgScreen extends Screen {
    public CfgScreen(MainActivity a) {
        super(a);
    }

    @Override
    public boolean isSub() {
        return true;
    }

    @Override
    public void build(LinearLayout into) {
        subHead(into, "Atur tata letak");

        Ui.Card lc = ui.card(into, Ui.NORMAL);
        ui.mt(lc, 14);
        ui.h2(lc, "Laci uang");
        ui.mt(ui.small(lc, "Ganti judul dan modal awal tiap laci, atau tambah laci baru. Kalau laci dihapus, catatan dan kategorinya dipindah ke laci lain."), 6);
        LinearLayout lb = ui.col(lc);
        ui.mt(lb, 10);
        boolean first = true;
        for (JSONObject L : Store.list(st.lacis())) {
            if (!first) ui.sep(lb);
            first = false;
            final String id = Store.str(L, "id");
            LinearLayout w = ui.col(lb);
            w.setPadding(0, ui.dp(8), 0, ui.dp(8));
            ui.mar(ui.label(w, "Judul laci"), 0, 0, 0, 3);
            EditText nm = ui.input(w, "", Ui.INK_NAME);
            Ui.setText(nm, Store.str(L, "nama"));
            Ui.watch(nm, () -> {
                Store.put(L, "nama", nm.getText().toString());
                st.save();
            });
            ui.label(w, "Modal awal (Rp)");
            EditText md = ui.input(w, "0", Ui.INK_NUM | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
            long m0 = Store.lng(L, "modal");
            md.setText(m0 != 0 ? String.valueOf(m0) : "");
            Ui.watch(md, () -> {
                Store.put(L, "modal", Ui.numOf(md));
                st.save();
            });
            Ui.Btn del = ui.btn(w, "Hapus", Ui.R, v -> askDel(true, id));
            ui.mt(del, 4);
        }
        Ui.Btn al = ui.btn(lc, "+ Tambah laci", Ui.P, v -> {
            st.addLaci();
            a.refresh();
        });
        ui.mt(al, 8);

        Ui.Card cc = ui.card(into, Ui.NORMAL);
        ui.mt(cc, 14);
        ui.h2(cc, "Kategori barang");
        ui.mt(ui.small(cc, "Tiap kategori masuk ke satu laci. Uang dari barang yang terjual mengikuti laci kategorinya."), 6);
        LinearLayout cb = ui.col(cc);
        ui.mt(cb, 10);

        List<String> ids = new ArrayList<>(), nms = new ArrayList<>();
        for (JSONObject L : Store.list(st.lacis())) {
            ids.add(Store.str(L, "id"));
            nms.add(Store.str(L, "nama"));
        }
        first = true;
        for (JSONObject K : Store.list(st.cats())) {
            if (!first) ui.sep(cb);
            first = false;
            final String id = Store.str(K, "id");
            LinearLayout w = ui.col(cb);
            w.setPadding(0, ui.dp(8), 0, ui.dp(8));
            ui.mar(ui.label(w, "Nama kategori"), 0, 0, 0, 3);
            EditText nm = ui.input(w, "", Ui.INK_NAME);
            Ui.setText(nm, Store.str(K, "nama"));
            Ui.watch(nm, () -> {
                Store.put(K, "nama", nm.getText().toString());
                st.save();
            });
            ui.label(w, "Masuk ke laci");
            Ui.Select sel = ui.select(w);
            sel.options(nms.toArray(new String[0]), ids.toArray(new String[0])).set(Store.str(K, "laci"));
            sel.onChange = () -> {
                Store.put(K, "laci", sel.value());
                st.save();
            };
            Ui.Btn del = ui.btn(w, "Hapus", Ui.R, v -> askDel(false, id));
            ui.mt(del, 4);
        }
        Ui.Btn ac = ui.btn(cc, "+ Tambah kategori", Ui.P, v -> {
            st.addKat();
            a.refresh();
        });
        ui.mt(ac, 8);
    }

    private void askDel(boolean laci, String id) {
        JSONObject it = laci ? st.laci(id) : st.kat(id);
        if (it == null) return;
        if ((laci ? st.lacis() : st.cats()).length() <= 1) {
            ui.toast(laci ? "Minimal harus ada satu laci" : "Minimal harus ada satu kategori");
            return;
        }
        Ui.Dlg d = ui.dialog();
        LinearLayout b = d.card;
        ui.h2(b, "Hapus " + Store.str(it, "nama") + "?");
        ui.mt(ui.tv(b, laci ? "Catatan kas, bon, dan kategori di laci ini dipindah ke laci:" : "Barang di kategori ini dipindah ke kategori:", 17), 8);
        Ui.Select sel = ui.select(b);
        ui.mt(sel, 8);
        List<String> ids = new ArrayList<>(), nms = new ArrayList<>();
        for (JSONObject x : Store.list(laci ? st.lacis() : st.cats())) {
            if (Store.str(x, "id").equals(id)) continue;
            ids.add(Store.str(x, "id"));
            nms.add(Store.str(x, "nama"));
        }
        sel.options(nms.toArray(new String[0]), ids.toArray(new String[0]));
        LinearLayout r = ui.row(b);
        r.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        ui.mt(r, 16);
        ui.btn(r, "Batal", Ui.W, v -> d.close());
        Ui.Btn ok = ui.btn(r, "Hapus", Ui.P, v -> {
            if (laci) st.deleteLaci(id, sel.value());
            else st.deleteKat(id, sel.value());
            d.close();
            a.refresh();
        });
        ui.mar(ok, 8, 0, 0, 0);
        d.show();
    }
}
