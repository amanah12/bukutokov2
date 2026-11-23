package com.bukutoko.app;

import android.graphics.Bitmap;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HomeScreen extends Screen {
    private String q = "";
    private String cat;
    private LinearLayout chipsBox, listCard;
    private LinearLayout curRow;
    private String fImg = "";
    private boolean kEd;

    public HomeScreen(MainActivity a) {
        super(a);
        Map<String, Integer> m = st.popCount();
        cat = "all";
        for (JSONObject x : Store.list(st.items())) {
            if (m.containsKey(Store.str(x, "id"))) {
                cat = "pop";
                break;
            }
        }
    }

    // ------------------------------------------------------------------ grid 2 kolom
    private Ui.Card cell(LinearLayout g, int kind, boolean full) {
        if (full) {
            curRow = null;
            Ui.Card c = ui.card(g, kind);
            ui.mt(c, 12);
            return c;
        }
        if (curRow == null) {
            curRow = new LinearLayout(a);
            curRow.setOrientation(LinearLayout.HORIZONTAL);
            curRow.setBaselineAligned(false);
            curRow.setClipChildren(false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP);
            lp.topMargin = ui.dp(12);
            g.addView(curRow, lp);
        }
        boolean first = curRow.getChildCount() == 0;
        Ui.Card c = ui.card(curRow, kind);
        LinearLayout.LayoutParams lp = Ui.lpOf(c);
        lp.width = 0;
        lp.weight = 1;
        lp.height = Ui.MATCH;
        if (first) lp.rightMargin = ui.dp(6);
        else lp.leftMargin = ui.dp(6);
        c.setLayoutParams(lp);
        if (!first) curRow = null;
        return c;
    }

    @Override
    public void build(LinearLayout into) {
        LinearLayout g = ui.col(into);
        curRow = null;
        int n = st.lacis().length();
        int i = 0;
        for (JSONObject L : Store.list(st.lacis())) {
            final String id = Store.str(L, "id");
            int kind = i % 3 == 0 ? Ui.C1 : (i % 3 == 2 ? Ui.C2 : Ui.NORMAL);
            Ui.Card c = cell(g, kind, false);
            ui.small(c, Store.str(L, "nama"));
            ui.tv(c, Store.rp(st.curSisa(id)), 22);
            Ui.Btn b = ui.btn(c, "✎ Atur", Ui.W, v -> openLaci(id));
            b.padding(10, 6);
            ui.mt(b, 8);
            i++;
        }
        Ui.Card tc = cell(g, Ui.NORMAL, n % 2 == 0);
        ui.small(tc, "Tabungan 🔒");
        ui.tv(tc, "Rp ******", 22);
        Ui.Btn tb = ui.btn(tc, "Masuk →", Ui.P, v -> a.show("tab"));
        tb.padding(10, 6);
        ui.mt(tb, 8);

        String t = Store.today(), bln = t.substring(0, 7);
        double iH = 0, oH = 0, lb = 0;
        for (JSONObject e : Store.list(st.log())) {
            String d = Store.str(e, "d");
            if (d.equals(t)) {
                iH += Store.num(e, "i");
                oH += Store.num(e, "o");
            }
            if (d.length() >= 7 && d.substring(0, 7).equals(bln)) lb += Store.num(e, "l");
        }
        Ui.Card c1 = cell(g, Ui.NORMAL, false);
        ui.small(c1, "Masuk hari ini");
        ui.money(c1, Store.rp(iH), 19, 1);
        Ui.Card c2 = cell(g, Ui.C2, false);
        ui.small(c2, "Keluar hari ini");
        ui.tv(c2, Store.rp(oH), 19);

        Ui.Card c3 = cell(g, Ui.NORMAL, true);
        ui.small(c3, "Laba penjualan bulan ini");
        ui.tv(c3, Store.rp(lb), 19);
        Ui.Card c4 = cell(g, Ui.NORMAL, true);
        ui.small(c4, "Bon belum dibayar");
        ui.money(c4, Store.rp(st.bonTotal()), 19, -1);

        buildPager(g);

        // ---- stok
        LinearLayout hr = ui.row(into);
        ui.mar(hr, 0, 20, 0, 8);
        TextView h = ui.h2(hr, "Stok barang");
        ui.wt(h, 1);
        ui.btn(hr, "+ Barang", Ui.P, v -> openItem(null));

        EditText cari = ui.input(into, "Cari barang...", Ui.INK_TEXT);
        cari.setText(q);
        cari.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a1, int b1, int c1) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a1, int b1, int c1) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String v = s.toString().toLowerCase();
                if (v.equals(q)) return;
                q = v;
                fillList();
            }
        });
        chipsBox = ui.hscroll(into);
        ui.mt((View) chipsBox.getParent(), 8);
        listCard = null;
        Ui.Card lc = ui.card(into, Ui.NORMAL);
        ui.mt(lc, 12);
        listCard = lc;
        ui.note(into, "Tekan − saat barang terjual (uang masuk). Tekan + saat kulak, lalu isi jumlah dan total harga kulak hari itu.");
        fillChips();
        fillList();
    }

    // ------------------------------------------------------------------ pager total harta
    private void buildPager(LinearLayout g) {
        Ui.Card pc = cell(g, Ui.NORMAL, true);
        Ui.Box bx = (Ui.Box) pc.getBackground();
        pc.setPadding(0, 0, Math.round(bx.ex()), Math.round(bx.ey()));
        final int cw = Math.min(ui.screenW(), ui.dp(640)) - ui.dp(28) - Math.round(bx.ex());

        HorizontalScrollView hs = new HorizontalScrollView(a);
        hs.setHorizontalScrollBarEnabled(false);
        pc.addView(hs, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        hs.addView(row, new HorizontalScrollView.LayoutParams(Ui.WRAP, Ui.WRAP));

        boolean jual = "j".equals(Store.str(st.set(), "nil"));
        double l = st.curSisa(null), s = st.nilaiStok(null), b = st.bonTotal();
        LinearLayout sl = slide(row, cw, "Total harta toko: semua (geser ke kanan)", l + s + b);
        slideRow(sl, "Uang laci", Store.rp(l));
        slideRow(sl, jual ? "Nilai stok (harga jual)" : "Nilai stok (modal)", Store.rp(s));
        slideRow(sl, "Bon belum dibayar", Store.rp(b));
        TextView note = ui.tv(sl, jual
                ? "Nilai stok dihitung dari harga jual, jadi sudah termasuk laba yang belum didapat."
                : "Nilai stok dihitung dari harga modal. Barang tanpa harga modal dihitung dengan harga jual.", 14);
        ui.mt(note, 10);
        note.setAlpha(0.9f);
        for (JSONObject L : Store.list(st.lacis())) {
            String id = Store.str(L, "id");
            double l2 = st.curSisa(id), s2 = st.nilaiStok(id), b2 = st.bonSum(id);
            LinearLayout x = slide(row, cw, Store.str(L, "nama"), l2 + s2 + b2);
            slideRow(x, "Uang laci", Store.rp(l2));
            slideRow(x, "Nilai stok", Store.rp(s2));
            slideRow(x, "Bon belum dibayar", Store.rp(b2));
        }
        final int pages = row.getChildCount();

        LinearLayout dots = new LinearLayout(a);
        dots.setGravity(Gravity.CENTER);
        pc.addView(dots, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        ui.mar(dots, 0, 0, 0, 12);
        final List<View> dv = new ArrayList<>();
        boolean sq = ui.th.sq;
        for (int i = 0; i < pages; i++) {
            View d = new View(a);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ui.dp(sq ? 14 : 10), ui.dp(sq ? 14 : 10));
            lp.setMargins(ui.dp(4), 0, ui.dp(4), 0);
            dots.addView(d, lp);
            dv.add(d);
        }
        final Runnable upd = () -> {
            int cur = Math.max(0, Math.min(pages - 1, Math.round(hs.getScrollX() / (float) cw)));
            for (int i = 0; i < dv.size(); i++) {
                Ui.Box db = new Ui.Box();
                db.dn = ui.dn;
                db.fill = i == cur ? (ui.th.sq ? ui.th.c2 : ui.th.accent) : (sq ? ui.th.card : ui.th.sep);
                db.bw = sq ? 3 : 0;
                db.border = ui.th.ink;
                db.r = sq ? 0 : 99;
                dv.get(i).setBackground(db);
            }
        };
        upd.run();
        hs.setOnScrollChangeListener((v, x, y, ox, oy) -> upd.run());
        hs.setOnTouchListener((v, ev) -> {
            int act = ev.getActionMasked();
            if (act == MotionEvent.ACTION_UP || act == MotionEvent.ACTION_CANCEL) {
                hs.postDelayed(() -> {
                    int page = Math.max(0, Math.min(pages - 1, Math.round(hs.getScrollX() / (float) cw)));
                    hs.smoothScrollTo(page * cw, 0);
                }, 60);
            }
            return false;
        });
    }

    private LinearLayout slide(LinearLayout row, int cw, String title, double total) {
        LinearLayout s = new LinearLayout(a);
        s.setOrientation(LinearLayout.VERTICAL);
        s.setPadding(ui.dp(12), ui.dp(12), ui.dp(12), ui.dp(4));
        row.addView(s, new LinearLayout.LayoutParams(cw, Ui.WRAP));
        ui.small(s, title);
        ui.tv(s, Store.rp(total), 19);
        return s;
    }

    private void slideRow(LinearLayout s, String k, String v) {
        LinearLayout r = ui.row(s);
        ui.mt(r, 6);
        TextView a1 = ui.tv(r, k, 17);
        ui.wt(a1, 1);
        ui.tv(r, v, 17);
    }

    // ------------------------------------------------------------------ daftar
    private void fillChips() {
        chipsBox.removeAllViews();
        int n = st.menipisCount();
        addChip("all", "Semua");
        addChip("pop", "Populer");
        Ui.Btn m = addChip("men", "Menipis" + (n > 0 ? " " + n : ""));
        if (n > 0 && !cat.equals("men")) m.setTextColor(ui.th.bad);
        for (JSONObject k : Store.list(st.cats())) addChip(Store.str(k, "id"), Store.str(k, "nama"));
    }

    private Ui.Btn addChip(String key, String label) {
        Ui.Btn b = ui.btn(chipsBox, label, Ui.W, v -> {
            cat = key;
            fillChips();
            fillList();
        });
        b.padding(12, 9);
        b.on(cat.equals(key));
        ui.mar(b, 0, 0, 6, 0);
        return b;
    }

    private void fillList() {
        listCard.removeAllViews();
        List<JSONObject> arr = st.viewItems(q, cat);
        if (arr.isEmpty()) {
            String msg = !q.isEmpty() ? "Barang tidak ditemukan."
                    : cat.equals("pop") ? "Belum ada penjualan 30 hari terakhir. Barang terlaris muncul di sini otomatis."
                    : cat.equals("men") ? "Tidak ada barang yang menipis."
                    : cat.equals("all") ? "Belum ada barang. Tekan + Barang."
                    : "Belum ada barang di kategori ini.";
            ui.small(listCard, msg);
            return;
        }
        Map<String, Integer> pop = cat.equals("pop") ? st.popCount() : null;
        boolean first = true;
        for (JSONObject x : arr) {
            if (!first) ui.sep(listCard);
            first = false;
            final String id = Store.str(x, "id");
            long s = Store.lng(x, "s");
            LinearLayout r = ui.row(listCard);
            r.setPadding(0, ui.dp(10), 0, ui.dp(10));

            LinearLayout ph = new LinearLayout(a);
            ph.setGravity(Gravity.CENTER);
            Ui.Box pb = new Ui.Box();
            pb.dn = ui.dn;
            pb.fill = ui.th.card;
            pb.bw = 2;
            pb.border = ui.th.ink;
            pb.r = ui.th.cardR > 0 ? 12 : 0;
            ph.setBackground(pb);
            Bitmap bm = Ui.decode(Store.str(x, "img"));
            if (bm != null) {
                ImageView iv = new ImageView(a);
                iv.setImageBitmap(bm);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                ph.setPadding(ui.dp(2), ui.dp(2), ui.dp(2), ui.dp(2));
                ph.addView(iv, new LinearLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
            } else {
                TextView e = new TextView(a);
                e.setText("📦");
                e.setTextSize(20);
                ph.addView(e, new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP));
            }
            r.addView(ph, new LinearLayout.LayoutParams(ui.dp(48), ui.dp(48)));

            LinearLayout nm = new LinearLayout(a);
            nm.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams nl = new LinearLayout.LayoutParams(0, Ui.WRAP, 1f);
            nl.leftMargin = ui.dp(10);
            nl.rightMargin = ui.dp(4);
            r.addView(nm, nl);
            LinearLayout nr = ui.row(nm);
            TextView name = ui.tv(nr, Store.str(x, "n"), 17);
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            if (s <= 3) {
                int k = ui.kindOf(nr);
                TextView tg = ui.tag(nr, s <= 0 ? "habis" : "menipis", ui.th.c2, 0xFF111111);
                ui.mar(tg, 6, 0, 0, 0);
            }
            String sub = Store.rp(Store.num(x, "j")) + (Store.num(x, "b") != 0 ? " · modal " + Store.rp(Store.num(x, "b")) : "")
                    + (pop != null ? " · laku " + (pop.containsKey(id) ? pop.get(id) : 0) + "x" : "");
            TextView sm = ui.tv(nm, sub, 14);
            sm.setAlpha(0.85f);

            LinearLayout qty = new LinearLayout(a);
            qty.setGravity(Gravity.CENTER_VERTICAL);
            r.addView(qty, new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP));
            Ui.Btn mn = ui.btn(qty, "−", Ui.W, v -> {
                JSONObject it = st.item(id);
                if (it != null && st.sell(it)) a.refresh();
            });
            mn.padding(0, 0).text(22);
            ui.size(mn, 42, 42);
            TextView cnt = ui.tv(qty, String.valueOf(s), 19);
            cnt.setMinWidth(ui.dp(34));
            cnt.setGravity(Gravity.CENTER);
            if (s <= 3) cnt.setTextColor(ui.th.outOf(ui.kindOf(cnt)));
            Ui.Btn pl = ui.btn(qty, "+", Ui.W, v -> openKulak(id));
            pl.padding(0, 0).text(22);
            ui.size(pl, 42, 42);
            Ui.Btn ed = ui.btn(r, "✎", Ui.R, v -> openItem(id));
            ed.text(17);
        }
    }

    // ------------------------------------------------------------------ dialog: laci
    private void openLaci(String lc) {
        Ui.Dlg d = ui.dialog();
        LinearLayout b = d.card;
        ui.h2(b, "Sesuaikan " + st.lcNm(lc));
        TextView p = ui.tv(b, "Di aplikasi: " + Store.rp(st.curSisa(lc)), 17);
        ui.mt(p, 8);
        ui.label(b, "Uang asli di laci (Rp)");
        EditText e = ui.input(b, "0", Ui.INK_NUM | InputType.TYPE_NUMBER_FLAG_SIGNED);
        Ui.setText(e, String.valueOf(Math.round(st.curSisa(lc))));
        ui.mt(ui.note(b, "Selisihnya dicatat otomatis di Buku Kas sebagai Koreksi laci."), 8);
        LinearLayout r = ui.row(b);
        r.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        ui.mt(r, 16);
        ui.btn(r, "Batal", Ui.W, v -> d.close());
        Ui.Btn ok = ui.btn(r, "Simpan", Ui.P, v -> {
            if (e.getText().toString().trim().isEmpty()) {
                ui.toast("Isi uang asli di laci");
                return;
            }
            long dlt = Ui.numOf(e) - Math.round(st.curSisa(lc));
            if (dlt > 0) st.addLog("Koreksi laci (lebih)", dlt, 0, 0, lc);
            else if (dlt < 0) st.addLog("Koreksi laci (kurang)", 0, -dlt, 0, lc);
            d.close();
            a.refresh();
        });
        ui.mar(ok, 8, 0, 0, 0);
        d.show();
    }

    // ------------------------------------------------------------------ dialog: kulak
    private void openKulak(String id) {
        JSONObject it = st.item(id);
        if (it == null) return;
        Ui.Dlg d = ui.dialog();
        LinearLayout b = d.card;
        ui.h2(b, "Kulak: " + Store.str(it, "n"));
        ui.label(b, "Jumlah masuk");
        EditText kq = ui.input(b, "1", Ui.INK_NUM);
        kq.setText("1");
        ui.label(b, "Total harga kulak (Rp)");
        EditText kh = ui.input(b, "0", Ui.INK_NUM);
        double bb = Store.num(it, "b");
        kEd = false;
        kh.setText(bb != 0 ? String.valueOf(Math.round(bb)) : "");
        ui.mt(ui.note(b, "Modal rata-rata sekarang: " + Store.rp(bb) + " per biji"), 8);
        final boolean[] prog = {false};
        kq.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int x, int y, int z) {
            }

            @Override
            public void onTextChanged(CharSequence s, int x, int y, int z) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!kEd && Store.num(it, "b") != 0) {
                    prog[0] = true;
                    kh.setText(String.valueOf(Math.round(Ui.numOf(kq) * Store.num(it, "b"))));
                    prog[0] = false;
                }
            }
        });
        kh.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int x, int y, int z) {
            }

            @Override
            public void onTextChanged(CharSequence s, int x, int y, int z) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!prog[0]) kEd = true;
            }
        });
        LinearLayout r = ui.row(b);
        r.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        ui.mt(r, 16);
        ui.btn(r, "Batal", Ui.W, v -> d.close());
        Ui.Btn ok = ui.btn(r, "Simpan", Ui.P, v -> {
            long qn = (long) Math.floor(Ui.numOf(kq));
            long h = Ui.numOf(kh);
            if (qn <= 0) {
                ui.toast("Isi jumlah");
                return;
            }
            st.kulak(it, qn, h);
            d.close();
            a.refresh();
        });
        ui.mar(ok, 8, 0, 0, 0);
        d.show();
    }

    // ------------------------------------------------------------------ dialog: barang
    private void openItem(String id) {
        final JSONObject it = id == null ? null : st.item(id);
        Ui.Dlg d = ui.dialog();
        LinearLayout b = d.card;
        ui.h2(b, it != null ? "Edit barang" : "Barang baru");
        ui.label(b, "Nama");
        EditText fn = ui.input(b, "", Ui.INK_NAME);
        ui.label(b, "Stok");
        EditText fs = ui.input(b, "", Ui.INK_NUM);
        ui.label(b, "Harga jual (Rp)");
        EditText fj = ui.input(b, "", Ui.INK_NUM);
        ui.label(b, "Harga modal per biji (Rp, boleh kosong)");
        EditText fb = ui.input(b, "", Ui.INK_NUM);
        ui.label(b, "Foto (opsional)");
        LinearLayout fr = ui.row(b);
        LinearLayout pv = new LinearLayout(a);
        pv.setGravity(Gravity.CENTER);
        fr.addView(pv, new LinearLayout.LayoutParams(ui.dp(64), ui.dp(64)));
        LinearLayout fc = new LinearLayout(a);
        fc.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams fl = new LinearLayout.LayoutParams(0, Ui.WRAP, 1f);
        fl.leftMargin = ui.dp(10);
        fr.addView(fc, fl);
        fImg = it != null ? Store.str(it, "img") : "";
        final Runnable showPv = () -> {
            pv.removeAllViews();
            Ui.Box pb = new Ui.Box();
            pb.dn = ui.dn;
            pb.fill = ui.th.card;
            pb.bw = 2;
            pb.border = ui.th.ink;
            pb.r = ui.th.cardR > 0 ? 12 : 0;
            pv.setBackground(pb);
            Bitmap bm = Ui.decode(fImg);
            if (bm != null) {
                ImageView iv = new ImageView(a);
                iv.setImageBitmap(bm);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                pv.addView(iv, new LinearLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
            } else {
                TextView e = new TextView(a);
                e.setText("📷");
                e.setTextSize(26);
                pv.addView(e, new LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP));
            }
        };
        showPv.run();
        Ui.Btn pick = ui.btn(fc, "Pilih foto", Ui.W, v -> Sys.pickImage(a, bmp -> {
            String s = Ui.encodeJpeg(bmp, 128, 70);
            if (s == null) ui.toast("Foto gagal dipakai");
            else {
                fImg = s;
                showPv.run();
            }
        }));
        pick.padding(10, 7);
        Ui.Btn del = ui.btn(fc, "Hapus foto", Ui.R, v -> {
            fImg = "";
            showPv.run();
        });
        ui.mt(del, 4);

        ui.label(b, "Kategori");
        Ui.Select fcat = ui.select(b);
        List<String> ids = new ArrayList<>(), nms = new ArrayList<>();
        for (JSONObject k : Store.list(st.cats())) {
            ids.add(Store.str(k, "id"));
            nms.add(Store.str(k, "nama"));
        }
        fcat.options(nms.toArray(new String[0]), ids.toArray(new String[0]));

        if (it != null) {
            Ui.setText(fn, Store.str(it, "n"));
            fs.setText(String.valueOf(Store.lng(it, "s")));
            fj.setText(Store.num(it, "j") != 0 ? String.valueOf(Store.lng(it, "j")) : "");
            fb.setText(Store.num(it, "b") != 0 ? String.valueOf(Store.lng(it, "b")) : "");
            fcat.set(st.catOf(it));
        } else {
            fcat.set(st.kat(cat) != null ? cat : ids.get(0));
        }

        LinearLayout r = ui.row(b);
        ui.mt(r, 16);
        Ui.Btn rm = ui.btn(r, "Hapus", Ui.R, v -> ui.confirm("Hapus barang ini?\n(Catatan jual dan kulak di Buku Kas tetap ada)", () -> {
            Store.removeIf(st.items(), o -> Store.str(o, "id").equals(id));
            st.save();
            d.close();
            a.refresh();
        }));
        rm.setVisibility(it != null ? View.VISIBLE : View.INVISIBLE);
        View sp = new View(a);
        r.addView(sp, new LinearLayout.LayoutParams(0, 1, 1f));
        ui.btn(r, "Batal", Ui.W, v -> d.close());
        Ui.Btn ok = ui.btn(r, "Simpan", Ui.P, v -> {
            String n = fn.getText().toString().trim();
            if (n.isEmpty()) {
                ui.toast("Isi nama");
                return;
            }
            JSONObject x = it != null ? it : new JSONObject();
            if (it == null) Store.put(x, "id", Store.uid());
            Store.put(x, "n", n);
            Store.put(x, "s", Ui.numOf(fs));
            Store.put(x, "j", Ui.numOf(fj));
            Store.put(x, "b", Ui.numOf(fb));
            Store.put(x, "cat", fcat.value());
            Store.put(x, "img", fImg);
            if (it == null) st.items().put(x);
            st.save();
            d.close();
            a.refresh();
        });
        ui.mar(ok, 8, 0, 0, 0);
        d.show();
    }
}
