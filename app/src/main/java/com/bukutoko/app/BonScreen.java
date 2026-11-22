package com.bukutoko.app;

import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BonScreen extends Screen {
    private String bonId;
    private Runnable fillBon;

    private static class Row {
        String id, n;
        long q, h;
    }

    public BonScreen(MainActivity a) {
        super(a);
    }

    @Override
    public void build(LinearLayout into) {
        Ui.Card tc = ui.card(into, Ui.C2);
        ui.small(tc, "Total bon belum dibayar");
        ui.tv(tc, Store.rp(st.bonTotal()), 19);

        LinearLayout hr = ui.row(into);
        ui.mar(hr, 0, 20, 0, 8);
        ui.wt(ui.h2(hr, "Pelanggan"), 1);
        ui.btn(hr, "+ Pelanggan", Ui.P, v -> newCust());

        Ui.Card lc = ui.card(into, Ui.NORMAL);
        List<JSONObject> arr = new ArrayList<>(Store.list(st.bon()));
        Collections.sort(arr, (x, y) -> {
            int d = Double.compare(st.saldo(y), st.saldo(x));
            return d != 0 ? d : Store.cmp(Store.str(x, "nama"), Store.str(y, "nama"));
        });
        if (arr.isEmpty()) {
            ui.small(lc, "Belum ada pelanggan. Tekan + Pelanggan.");
            return;
        }
        boolean first = true;
        for (JSONObject c : arr) {
            if (!first) ui.sep(lc);
            first = false;
            final String id = Store.str(c, "id");
            double s = st.saldo(c);
            LinearLayout r = ui.row(lc);
            r.setPadding(0, ui.dp(10), 0, ui.dp(10));
            r.setOnClickListener(v -> openBon(id));
            LinearLayout nm = ui.col(r);
            ui.wt(nm, 1);
            if (s > 0) {
                TextView tg = ui.tag(nm, "BELUM LUNAS", ui.th.bad, 0xFFFFFFFF);
                ui.mar(tg, 0, 0, 0, 3);
            }
            ui.tv(nm, Store.str(c, "nama"), 17);
            List<JSONObject> lg = Store.list(Store.arr(c, "log"));
            if (!lg.isEmpty()) {
                TextView sm = ui.note(nm, Store.dmy(Store.str(lg.get(lg.size() - 1), "d")));
                sm.setTextSize(12);
            }
            List<String> lcs = new ArrayList<>();
            if (s > 0) {
                for (JSONObject L : Store.list(st.lacis())) {
                    if (st.saldoL(c, Store.str(L, "id")) > 0) lcs.add(Store.str(L, "nama"));
                }
            }
            TextView lt = ui.tv(r, s > 0 ? android.text.TextUtils.join(", ", lcs) : "-", 13);
            ui.size(lt, 90, -2);
            lt.setGravity(Gravity.END);
            ui.tv(r, Store.rp(Math.max(s, 0)), 17);
        }
    }

    // ------------------------------------------------------------------ pelanggan baru
    private void newCust() {
        Ui.Dlg d = ui.dialog();
        LinearLayout b = d.card;
        ui.h2(b, "Pelanggan baru");
        ui.label(b, "Nama");
        EditText n = ui.input(b, "mis. Bu Siti", Ui.INK_NAME);
        LinearLayout r = ui.row(b);
        r.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        ui.mt(r, 16);
        ui.btn(r, "Batal", Ui.W, v -> d.close());
        Ui.Btn ok = ui.btn(r, "Simpan", Ui.P, v -> {
            String nm = n.getText().toString().trim();
            if (nm.isEmpty()) {
                ui.toast("Isi nama");
                return;
            }
            JSONObject c = new JSONObject();
            Store.put(c, "id", Store.uid());
            Store.put(c, "nama", nm);
            Store.put(c, "log", new org.json.JSONArray());
            st.bon().put(c);
            st.save();
            d.close();
            a.refresh();
            openBon(Store.str(c, "id"));
        });
        ui.mar(ok, 8, 0, 0, 0);
        d.show();
    }

    // ------------------------------------------------------------------ dialog bon
    private void openBon(String id) {
        bonId = id;
        if (st.bonOf(id) == null) return;
        final Ui.Dlg d = ui.dialog();
        LinearLayout b = d.card;
        LinearLayout hr = ui.row(b);
        final TextView title = ui.h2(hr, "");
        ui.wt(title, 1);
        ui.btn(hr, "✕", Ui.W, v -> d.close()).padding(10, 6);

        Ui.Card sc = ui.card(b, Ui.C2);
        ui.mt(sc, 12);
        ui.small(sc, "Sisa bon");
        final TextView sisa = ui.tv(sc, "", 19);

        MaxScroll ms = new MaxScroll(a, Math.round(screenH() * 0.3f));
        b.addView(ms, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        final LinearLayout hist = new LinearLayout(a);
        hist.setOrientation(LinearLayout.VERTICAL);
        ms.addView(hist, new android.view.ViewGroup.LayoutParams(Ui.MATCH, Ui.WRAP));

        ui.label(b, "Laci");
        final Ui.Select laci = ui.select(b);
        List<String> ids = new ArrayList<>(), nms = new ArrayList<>();
        for (JSONObject L : Store.list(st.lacis())) {
            ids.add(Store.str(L, "id"));
            nms.add(Store.str(L, "nama"));
        }
        laci.options(nms.toArray(new String[0]), ids.toArray(new String[0]));
        String last = Store.str(st.bonOf(id), "lastLc");
        laci.set(st.hasLaci(last) ? last : st.firstLaci());
        ui.label(b, "Nominal (Rp)");
        final EditText nom = ui.input(b, "0", Ui.INK_NUM);
        ui.label(b, "Keterangan");
        final EditText ket = ui.input(b, "mis. beras 5 kg", Ui.INK_TEXT);

        fillBon = () -> {
            JSONObject c = st.bonOf(bonId);
            if (c == null) return;
            title.setText(Store.str(c, "nama"));
            sisa.setText(Store.rp(Math.max(st.saldo(c), 0)));
            hist.removeAllViews();
            List<JSONObject> lg = new ArrayList<>(Store.list(Store.arr(c, "log")));
            Collections.reverse(lg);
            for (JSONObject e : lg) {
                final String eid = Store.str(e, "id");
                boolean u = "u".equals(Store.str(e, "t"));
                LinearLayout r = ui.row(hist);
                r.setPadding(0, ui.dp(6), 0, ui.dp(6));
                LinearLayout nm = ui.col(r);
                ui.wt(nm, 1);
                String k = Store.str(e, "ket");
                ui.tv(nm, k.isEmpty() ? (u ? "Utang" : "Bayar") : k, 16);
                ui.note(nm, Store.dm(Store.str(e, "d")) + (u ? " · utang" : " · bayar")).setTextSize(12);
                ui.money(r, (u ? "+" : "−") + Store.rp(Store.num(e, "n")), 16, u ? -1 : 1);
                ui.btn(r, "✕", Ui.R, v -> delEntry(eid));
            }
        };
        fillBon.run();
        d.onClose = () -> fillBon = null;

        Ui.Btn bg = ui.btn(b, "+ Bon barang (stok berkurang)", Ui.P, v -> openBonBarang());
        ui.mt(bg, 14);
        LinearLayout r2 = ui.row(b);
        ui.mt(r2, 8);
        Ui.Btn bu = ui.btn(r2, "+ Utang nominal", Ui.P, v -> addEntry("u", nom, ket, laci));
        bu.color(ui.th.c2, 0xFF111111);
        ui.wt(bu, 1);
        Ui.Btn bp = ui.btn(r2, "Bayar", Ui.P, v -> addEntry("b", nom, ket, laci));
        ui.wt(bp, 1);
        ui.mar(bp, 10, 0, 0, 0);
        Ui.Btn del = ui.btn(b, "Hapus pelanggan", Ui.R, v -> {
            JSONObject c = st.bonOf(bonId);
            if (c == null) return;
            ui.confirm("Hapus pelanggan " + Store.str(c, "nama") + " beserta catatannya?\n(Uang yang sudah masuk ke Buku Kas tetap tercatat)", () -> {
                Store.removeIf(st.bon(), o -> Store.str(o, "id").equals(bonId));
                st.save();
                d.close();
                a.refresh();
            });
        });
        ui.mt(del, 10);
        d.show();
    }

    private void afterChange() {
        a.refresh();
        if (fillBon != null) fillBon.run();
    }

    private void addEntry(String t, EditText nom, EditText ket, Ui.Select laci) {
        JSONObject c = st.bonOf(bonId);
        long n = Ui.numOf(nom);
        if (c == null || n <= 0) {
            ui.toast("Isi nominal");
            return;
        }
        JSONObject e = new JSONObject();
        Store.put(e, "id", Store.uid());
        Store.put(e, "d", Store.today());
        Store.put(e, "t", t);
        Store.put(e, "n", n);
        Store.put(e, "ket", ket.getText().toString().trim());
        Store.put(e, "lc", laci.value());
        if (t.equals("b")) {
            String k = Store.uid();
            Store.put(e, "k", k);
            JSONObject lg = new JSONObject();
            Store.put(lg, "id", k);
            Store.put(lg, "d", Store.today());
            Store.put(lg, "ket", "Bayar bon " + Store.str(c, "nama"));
            Store.put(lg, "i", n);
            Store.put(lg, "o", 0);
            Store.put(lg, "l", 0);
            Store.put(lg, "lc", laci.value());
            st.log().put(lg);
        }
        Store.put(c, "lastLc", laci.value());
        Store.arr(c, "log").put(e);
        st.save();
        nom.setText("");
        ket.setText("");
        afterChange();
    }

    private void delEntry(String eid) {
        final JSONObject c = st.bonOf(bonId);
        if (c == null) return;
        final JSONObject en = Store.find(Store.arr(c, "log"), eid);
        if (en == null) return;
        final String k = Store.str(en, "k");
        ui.confirm("Hapus catatan ini?" + (!k.isEmpty() ? "\n(Catatan terkait di Buku Kas ikut dihapus, dan stok barang dikembalikan kalau ada)" : ""), () -> {
            if (!k.isEmpty()) {
                JSONObject lg = Store.find(st.log(), k);
                if (lg != null) {
                    String it = Store.str(lg, "it");
                    if (!it.isEmpty() && Store.num(lg, "q") != 0) {
                        JSONObject x = st.item(it);
                        if (x != null) Store.put(x, "s", Math.max(0, Store.lng(x, "s") - Store.lng(lg, "q")));
                    }
                }
                Store.removeIf(st.log(), o -> Store.str(o, "id").equals(k));
            }
            Store.removeIf(Store.arr(c, "log"), o -> Store.str(o, "id").equals(eid));
            st.save();
            afterChange();
        });
    }

    // ------------------------------------------------------------------ bon barang
    private void openBonBarang() {
        final JSONObject c = st.bonOf(bonId);
        if (c == null) return;
        final List<Row> bl = new ArrayList<>();
        final Ui.Dlg d = ui.dialog();
        LinearLayout b = d.card;
        ui.h2(b, "Bon barang: " + Store.str(c, "nama"));
        ui.label(b, "Cari barang");
        final EditText q = ui.input(b, "Ketik nama barang...", Ui.INK_TEXT);
        ui.label(b, "Tambah dari stok");
        LinearLayout sr = ui.row(b);
        final Ui.Select sel = ui.select(sr);
        ui.wt(sel, 1);
        Ui.Btn add = ui.btn(sr, "+ Tambah", Ui.W, null);
        ui.mar(add, 8, 0, 0, 0);
        final LinearLayout rows = ui.col(b);
        ui.mt(rows, 12);
        final TextView tot = ui.tv(b, "Total: Rp 0", 18);
        ui.mt(tot, 10);
        ui.label(b, "Dibayar sekarang (Rp, kosongkan kalau bon semua)");
        final EditText pay = ui.input(b, "0", Ui.INK_NUM);
        final TextView sisa = ui.tv(b, "Jadi bon: Rp 0", 17);
        ui.mt(sisa, 8);

        final Runnable calc = () -> {
            long t = 0;
            for (Row r : bl) t += r.q * r.h;
            long p = Math.max(0, Ui.numOf(pay));
            tot.setText("Total: " + Store.rp(t));
            sisa.setText("Jadi bon: " + Store.rp(Math.max(0, t - p)) + (p > t ? " (uang dibayar melebihi total)" : ""));
        };
        final Runnable fillSel = () -> {
            String s = q.getText().toString().trim().toLowerCase();
            List<String> ids = new ArrayList<>(), nms = new ArrayList<>();
            List<JSONObject> its = new ArrayList<>(Store.list(st.items()));
            Collections.sort(its, (x, y) -> Store.cmp(Store.str(x, "n"), Store.str(y, "n")));
            for (JSONObject it : its) {
                if (!s.isEmpty() && !Store.str(it, "n").toLowerCase().contains(s)) continue;
                ids.add(Store.str(it, "id"));
                nms.add(Store.str(it, "n") + " (" + Store.rp(Store.num(it, "j")) + ", stok " + Store.lng(it, "s") + ")");
            }
            if (ids.isEmpty()) {
                ids.add("");
                nms.add("Tidak ditemukan");
            }
            sel.options(nms.toArray(new String[0]), ids.toArray(new String[0]));
        };
        final Runnable[] calcRows = new Runnable[1];
        final Runnable fillRows = () -> {
            rows.removeAllViews();
            for (final Row r : new ArrayList<>(bl)) {
                LinearLayout w = ui.col(rows);
                w.setPadding(0, 0, 0, ui.dp(10));
                ui.tv(w, r.n, 17);
                LinearLayout ir = ui.row(w);
                EditText jq = ui.input(ir, "Jml", Ui.INK_NUM);
                ui.wt(jq, 1);
                EditText jh = ui.input(ir, "Harga", Ui.INK_NUM);
                ui.wt(jh, 2);
                ui.mar(jh, 8, 0, 0, 0);
                jq.setText(r.q != 0 ? String.valueOf(r.q) : "");
                jh.setText(r.h != 0 ? String.valueOf(r.h) : "");
                Ui.watch(jq, () -> {
                    r.q = Ui.numOf(jq);
                    calc.run();
                });
                Ui.watch(jh, () -> {
                    r.h = Ui.numOf(jh);
                    calc.run();
                });
                ui.btn(ir, "✕", Ui.R, v -> {
                    bl.remove(r);
                    calcRows[0].run();
                });
            }
            calc.run();
        };
        calcRows[0] = fillRows;

        add.setOnClickListener(v -> {
            JSONObject it = st.item(sel.value());
            if (it == null) {
                ui.toast("Pilih barang dulu");
                return;
            }
            Row ex = null;
            for (Row r : bl) if (r.id.equals(Store.str(it, "id"))) ex = r;
            if (ex != null) ex.q += 1;
            else {
                Row r = new Row();
                r.id = Store.str(it, "id");
                r.n = Store.str(it, "n");
                r.q = 1;
                r.h = Store.lng(it, "j");
                bl.add(r);
            }
            q.setText("");
            fillRows.run();
        });
        Ui.watch(q, fillSel);
        Ui.watch(pay, calc);
        fillSel.run();
        fillRows.run();

        LinearLayout br = ui.row(b);
        br.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        ui.mt(br, 14);
        ui.btn(br, "Batal", Ui.W, v -> d.close());
        Ui.Btn ok = ui.btn(br, "Simpan bon", Ui.P, v -> {
            List<Row> use = new ArrayList<>();
            for (Row r : bl) if (!r.id.isEmpty() && r.q > 0 && r.h >= 0) use.add(r);
            if (use.isEmpty()) {
                ui.toast("Pilih minimal satu barang");
                return;
            }
            long pay0 = Math.max(0, Ui.numOf(pay)), total = 0;
            for (Row r : use) total += r.q * r.h;
            if (pay0 > total) {
                ui.toast("Uang dibayar lebih besar dari total. Kurangi nominalnya.");
                return;
            }
            long left = pay0;
            for (Row r : use) {
                JSONObject it = st.item(r.id);
                if (it == null) continue;
                long rt = r.q * r.h, paid = Math.min(left, rt);
                left -= paid;
                long owed = rt - paid;
                String lk = Store.uid();
                String nm = Store.str(it, "n") + (r.q > 1 ? " x" + r.q : "");
                Store.put(it, "s", Math.max(0, Store.lng(it, "s") - r.q));
                JSONObject lg = new JSONObject();
                Store.put(lg, "id", lk);
                Store.put(lg, "d", Store.today());
                Store.put(lg, "ket", "Jual " + nm + " (bon " + Store.str(c, "nama") + ")");
                Store.put(lg, "i", paid);
                Store.put(lg, "o", 0);
                Store.put(lg, "l", (r.h - Store.lng(it, "b")) * r.q);
                Store.put(lg, "it", r.id);
                Store.put(lg, "q", -r.q);
                Store.put(lg, "lc", st.lcOf(it));
                st.log().put(lg);
                if (owed > 0) {
                    JSONObject e = new JSONObject();
                    Store.put(e, "id", Store.uid());
                    Store.put(e, "d", Store.today());
                    Store.put(e, "t", "u");
                    Store.put(e, "n", owed);
                    Store.put(e, "ket", nm + (paid > 0 ? " (sisa)" : ""));
                    Store.put(e, "lc", st.lcOf(it));
                    Store.put(e, "k", lk);
                    Store.arr(c, "log").put(e);
                }
            }
            st.save();
            d.close();
            afterChange();
        });
        ui.mar(ok, 8, 0, 0, 0);
        d.show();
    }
}
