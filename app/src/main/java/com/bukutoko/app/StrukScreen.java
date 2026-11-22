package com.bukutoko.app;

import android.graphics.Bitmap;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Struk dan nota: PPOB, nota belanja, e-wallet, transfer bank, riwayat. */
public class StrukScreen extends Screen {
    private static final String[] JENIS = {"Pulsa", "Paket data", "Token listrik", "Tagihan listrik", "PDAM", "BPJS", "Internet / TV", "PPOB lainnya"};
    private static final String[] JENIS_ALL = {"Pulsa", "Paket data", "Token listrik", "Tagihan listrik", "PDAM", "BPJS", "Internet / TV", "PPOB lainnya", "E-wallet", "Transfer bank"};
    private static final String[] EW = {"DANA", "OVO", "GoPay", "ShopeePay", "LinkAja", "E-wallet lainnya"};

    private static class NRow {
        String id = "", n = "";
        long q = 1, h = 0;
    }

    private String mode = "ppob";
    private final List<NRow> nl = new ArrayList<>();

    private Ui.Card cPpob, cEw, cTf, cNota, cRiw, cMake;
    private EditText pN, pP, pH, pT, pK, eN, eK, eV, eA, tB, tN, tK, tV, tA, tM, nB, nBy, nQ, rwQ;
    private Ui.Select pJ, eL, nS, stLc;
    private CheckBox stKas;
    private TextView eT, tT, nTot;
    private LinearLayout nRows, rwList;
    private Ac acP, acE, acT;

    public StrukScreen(MainActivity a) {
        super(a);
    }

    @Override
    public boolean isSub() {
        return true;
    }

    /** Layar ini punya form panjang: jangan dibangun ulang, cukup segarkan riwayat. */
    @Override
    public void onData() {
        if (rwList != null) fillHist();
    }

    // ------------------------------------------------------------------ pelanggan terdaftar (cari + pilih)
    private class Ac {
        final EditText in;
        final LinearLayout list;
        final String kind; // "ppob", "E-wallet" atau "Transfer bank"
        boolean quiet;

        Ac(LinearLayout card, final String kind) {
            this.kind = kind;
            ui.mar(ui.label(card, "Pelanggan terdaftar"), 0, 0, 0, 3);
            LinearLayout row = ui.row(card);
            LinearLayout col = ui.col(row);
            ui.wt(col, 1);
            in = ui.input(col, "Cari nama atau nomor...", Ui.INK_TEXT);
            list = ui.col(col);
            list.setBackground(ui.boxFor(Ui.NORMAL));
            list.setVisibility(View.GONE);
            Ui.Btn add = ui.btn(row, "+", Ui.P, v -> openRg(kind.equals("ppob") ? pJ.value() : kind, "", ""));
            add.padding(18, 8).text(22);
            ui.mar(add, 8, 0, 0, 0);
            Ui.watch(in, () -> {
                if (!quiet && in.hasFocus()) show();
            });
            in.setOnFocusChangeListener((v, f) -> {
                if (f) show();
                else v.postDelayed(() -> list.setVisibility(View.GONE), 250);
            });
        }

        void set(String s) {
            quiet = true;
            Ui.setText(in, s);
            quiet = false;
        }

        void hide() {
            list.setVisibility(View.GONE);
        }

        void show() {
            list.removeAllViews();
            List<Store.Pc> arr = new ArrayList<>();
            for (Store.Pc c : st.pcFilter(in.getText().toString(), null)) {
                boolean special = c.j.equals("E-wallet") || c.j.equals("Transfer bank");
                if (kind.equals("ppob") ? !special : c.j.equals(kind)) arr.add(c);
            }
            if (arr.isEmpty()) {
                TextView t = ui.tvc(list, st.pcAll().isEmpty() ? "Belum ada pelanggan. Tekan + untuk mendaftarkan." : "Tidak ditemukan", 15, ui.th.ink);
                t.setPadding(ui.dp(10), ui.dp(10), ui.dp(10), ui.dp(10));
            }
            int n = 0;
            for (final Store.Pc c : arr) {
                if (n++ >= 8) break;
                LinearLayout e = ui.col(list);
                e.setPadding(ui.dp(12), ui.dp(8), ui.dp(12), ui.dp(8));
                ui.tvc(e, c.nama, 16, ui.th.ink);
                String sub = kind.equals("ppob") ? c.no + " · " + c.j : (c.ly.isEmpty() ? "" : c.ly + " · ") + c.no;
                ui.tvc(e, sub, 13, ui.th.mu);
                e.setOnClickListener(v -> {
                    pick(c);
                    hide();
                });
            }
            list.setVisibility(View.VISIBLE);
        }
    }

    private void pick(Store.Pc c) {
        if (c.j.equals("E-wallet")) {
            if (!c.ly.isEmpty()) eL.set(c.ly);
            eN.setText(c.no);
            eK.setText(c.nama);
            acE.set(c.nama);
        } else if (c.j.equals("Transfer bank")) {
            tB.setText(c.ly);
            tN.setText(c.no);
            tK.setText(c.nama);
            acT.set(c.nama);
        } else {
            pJ.set(c.j);
            pN.setText(c.no);
            pK.setText(c.nama);
            acP.set(c.nama);
        }
    }

    // ------------------------------------------------------------------ bangun layar
    private EditText field(LinearLayout card, String label, String hint, int type) {
        ui.label(card, label);
        return ui.input(card, hint, type);
    }

    @Override
    public void build(LinearLayout into) {
        subHead(into, "Struk dan nota");
        Ui.Seg seg = ui.seg(into, new String[]{"PPOB", "Nota", "E-wallet", "Transfer", "Riwayat"},
                new String[]{"ppob", "nota", "ewallet", "transfer", "riw"}, mode, k -> setMode(k));
        ui.mt((View) into.getChildAt(into.getChildCount() - 1), 14);

        // ---- PPOB
        cPpob = ui.card(into, Ui.NORMAL);
        ui.mt(cPpob, 14);
        acP = new Ac(cPpob, "ppob");
        ui.label(cPpob, "Jenis");
        pJ = ui.select(cPpob);
        pJ.options(JENIS);
        pN = field(cPpob, "Nomor tujuan / ID pelanggan", "mis. 0812xxxxxxx", Ui.INK_NUM);
        pP = field(cPpob, "Produk / nominal", "mis. Telkomsel 10.000", Ui.INK_TEXT);
        pH = field(cPpob, "Harga total (Rp)", "0", Ui.INK_NUM);
        pT = field(cPpob, "No. token / SN (opsional)", "mis. token 20 digit", Ui.INK_TEXT);
        pK = field(cPpob, "Nama pelanggan (opsional)", "", Ui.INK_NAME);

        // ---- e-wallet
        cEw = ui.card(into, Ui.NORMAL);
        ui.mt(cEw, 14);
        acE = new Ac(cEw, "E-wallet");
        ui.label(cEw, "Layanan");
        eL = ui.select(cEw);
        eL.options(EW);
        eN = field(cEw, "Nomor / ID e-wallet", "mis. 0812xxxxxxx", Ui.INK_NUM);
        eK = field(cEw, "Nama pemilik (opsional)", "", Ui.INK_NAME);
        eV = field(cEw, "Nominal (Rp)", "0", Ui.INK_NUM);
        eA = field(cEw, "Biaya admin (Rp)", "0", Ui.INK_NUM);
        eT = ui.tv(cEw, "Total: Rp 0", 20);
        ui.mt(eT, 12);

        // ---- transfer
        cTf = ui.card(into, Ui.NORMAL);
        ui.mt(cTf, 14);
        acT = new Ac(cTf, "Transfer bank");
        tB = field(cTf, "Bank tujuan", "mis. BCA, BRI, Mandiri", Ui.INK_TEXT);
        tN = field(cTf, "Nomor rekening", "", Ui.INK_NUM);
        tK = field(cTf, "Nama penerima", "", Ui.INK_NAME);
        tV = field(cTf, "Nominal (Rp)", "0", Ui.INK_NUM);
        tA = field(cTf, "Biaya admin (Rp)", "0", Ui.INK_NUM);
        tM = field(cTf, "Berita / keterangan (opsional)", "", Ui.INK_TEXT);
        tT = ui.tv(cTf, "Total: Rp 0", 20);
        ui.mt(tT, 12);
        Runnable bk = () -> {
            eT.setText("Total: " + Store.rp(Ui.numOf(eV) + Ui.numOf(eA)));
            tT.setText("Total: " + Store.rp(Ui.numOf(tV) + Ui.numOf(tA)));
        };
        Ui.watch(eV, bk);
        Ui.watch(eA, bk);
        Ui.watch(tV, bk);
        Ui.watch(tA, bk);

        // ---- riwayat
        cRiw = ui.card(into, Ui.NORMAL);
        ui.mt(cRiw, 14);
        rwQ = ui.input(cRiw, "Cari riwayat (nama, nomor, jenis)...", Ui.INK_TEXT);
        Ui.watch(rwQ, this::fillHist);
        rwList = ui.col(cRiw);
        ui.mt(rwList, 8);

        // ---- nota
        cNota = ui.card(into, Ui.NORMAL);
        ui.mt(cNota, 14);
        ui.mar(ui.label(cNota, "Nama pembeli (opsional)"), 0, 0, 0, 3);
        nB = ui.input(cNota, "", Ui.INK_NAME);
        ui.label(cNota, "Cari barang");
        nQ = ui.input(cNota, "Ketik nama barang...", Ui.INK_TEXT);
        ui.label(cNota, "Tambah dari stok");
        LinearLayout sr = ui.row(cNota);
        nS = ui.select(sr);
        ui.wt(nS, 1);
        Ui.Btn nAdd = ui.btn(sr, "+ Tambah", Ui.W, v -> {
            JSONObject it = st.item(nS.value());
            if (it == null) {
                ui.toast("Pilih barang dulu");
                return;
            }
            if (!nl.isEmpty()) {
                NRow l = nl.get(nl.size() - 1);
                if (l.n.isEmpty() && l.h == 0) nl.remove(nl.size() - 1);
            }
            NRow r = new NRow();
            r.id = Store.str(it, "id");
            r.n = Store.str(it, "n");
            r.h = Store.lng(it, "j");
            nl.add(r);
            nQ.setText("");
            fillNota();
        });
        ui.mar(nAdd, 8, 0, 0, 0);
        nRows = ui.col(cNota);
        ui.mt(nRows, 12);
        Ui.Btn man = ui.btn(cNota, "+ Barang manual", Ui.W, v -> {
            nl.add(new NRow());
            fillNota();
        });
        nTot = ui.tv(cNota, "Total: Rp 0", 20);
        ui.mt(nTot, 12);
        nBy = field(cNota, "Uang dibayar (opsional)", "0", Ui.INK_NUM);
        Ui.watch(nQ, this::fillNs);

        // ---- bagian umum
        cMake = ui.card(into, Ui.NORMAL);
        ui.mt(cMake, 14);
        stKas = new CheckBox(a);
        stKas.setText("Catat ke Buku Kas (nota: stok barang ikut berkurang)");
        stKas.setTextColor(ui.th.cardInk);
        stKas.setTypeface(ui.th.font);
        stKas.setTextSize(14);
        cMake.addView(stKas, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        ui.label(cMake, "Masuk ke laci");
        stLc = ui.select(cMake);
        List<String> ids = new ArrayList<>(), nms = new ArrayList<>();
        for (JSONObject L : Store.list(st.lacis())) {
            ids.add(Store.str(L, "id"));
            nms.add(Store.str(L, "nama"));
        }
        stLc.options(nms.toArray(new String[0]), ids.toArray(new String[0]));
        Ui.Btn mk = ui.btn(cMake, "Buat struk", Ui.P, v -> make());
        ui.mt(mk, 14);

        if (nl.isEmpty()) nl.add(new NRow());
        fillNs();
        fillNota();
        fillHist();
        setMode(mode);
    }

    private void setMode(String m) {
        mode = m;
        a.hideKeyboard();
        cPpob.setVisibility(m.equals("ppob") ? View.VISIBLE : View.GONE);
        cNota.setVisibility(m.equals("nota") ? View.VISIBLE : View.GONE);
        cEw.setVisibility(m.equals("ewallet") ? View.VISIBLE : View.GONE);
        cTf.setVisibility(m.equals("transfer") ? View.VISIBLE : View.GONE);
        cRiw.setVisibility(m.equals("riw") ? View.VISIBLE : View.GONE);
        cMake.setVisibility(m.equals("riw") ? View.GONE : View.VISIBLE);
        if (m.equals("riw")) fillHist();
    }

    // ------------------------------------------------------------------ nota
    private void fillNs() {
        String q = nQ.getText().toString().trim().toLowerCase();
        List<String> ids = new ArrayList<>(), nms = new ArrayList<>();
        List<JSONObject> its = new ArrayList<>(Store.list(st.items()));
        Collections.sort(its, (x, y) -> Store.cmp(Store.str(x, "n"), Store.str(y, "n")));
        for (JSONObject it : its) {
            if (!q.isEmpty() && !Store.str(it, "n").toLowerCase().contains(q)) continue;
            ids.add(Store.str(it, "id"));
            nms.add(Store.str(it, "n") + " (" + Store.rp(Store.num(it, "j")) + ")");
        }
        if (ids.isEmpty()) {
            ids.add("");
            nms.add("Tidak ditemukan");
        }
        nS.options(nms.toArray(new String[0]), ids.toArray(new String[0]));
    }

    private void nTotal() {
        long t = 0;
        for (NRow r : nl) t += r.q * r.h;
        nTot.setText("Total: " + Store.rp(t));
    }

    private void fillNota() {
        nRows.removeAllViews();
        for (final NRow r : new ArrayList<>(nl)) {
            LinearLayout w = ui.col(nRows);
            w.setPadding(0, 0, 0, ui.dp(10));
            final EditText nm = ui.input(w, "Nama barang", Ui.INK_NAME);
            LinearLayout ir = ui.row(w);
            ui.mt(ir, 6);
            final EditText jq = ui.input(ir, "Jml", Ui.INK_NUM);
            ui.wt(jq, 1);
            final EditText jh = ui.input(ir, "Harga", Ui.INK_NUM);
            ui.wt(jh, 2);
            ui.mar(jh, 8, 0, 0, 0);
            nm.setText(r.n);
            jq.setText(r.q != 0 ? String.valueOf(r.q) : "");
            jh.setText(r.h != 0 ? String.valueOf(r.h) : "");
            Ui.watch(nm, () -> r.n = nm.getText().toString());
            Ui.watch(jq, () -> {
                r.q = Ui.numOf(jq);
                nTotal();
            });
            Ui.watch(jh, () -> {
                r.h = Ui.numOf(jh);
                nTotal();
            });
            ui.btn(ir, "✕", Ui.R, v -> {
                nl.remove(r);
                fillNota();
            });
        }
        nTotal();
    }

    // ------------------------------------------------------------------ riwayat
    private static String modeLabel(String m) {
        switch (m) {
            case "nota":
                return "Nota belanja";
            case "ewallet":
                return "E-wallet";
            case "transfer":
                return "Transfer bank";
            default:
                return "PPOB";
        }
    }

    private void fillHist() {
        rwList.removeAllViews();
        String q = rwQ.getText().toString().trim().toLowerCase();
        List<JSONObject> all = Store.list(st.struk());
        List<JSONObject> arr = new ArrayList<>();
        for (int i = all.size() - 1; i >= 0; i--) {
            JSONObject x = all.get(i);
            if (q.isEmpty() || Store.str(x, "ket").toLowerCase().contains(q)) arr.add(x);
        }
        if (arr.isEmpty()) {
            ui.small(rwList, all.isEmpty() ? "Belum ada riwayat. Struk yang dibuat akan muncul di sini." : "Tidak ditemukan.");
            return;
        }
        SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy HH.mm", new Locale("id", "ID"));
        boolean first = true;
        for (final JSONObject x : arr) {
            if (!first) ui.sep(rwList);
            first = false;
            LinearLayout r = ui.row(rwList);
            r.setPadding(0, ui.dp(8), 0, ui.dp(8));
            LinearLayout nm = ui.col(r);
            ui.wt(nm, 1);
            ui.tv(nm, Store.str(x, "ket"), 16);
            ui.note(nm, df.format(new Date(x.optLong("w"))) + " · " + modeLabel(Store.str(x, "mode"))).setTextSize(12);
            ui.tv(r, Store.rp(Store.num(x, "total")), 16);
            r.setOnClickListener(v -> {
                String fn = Store.str(x, "fn");
                JSONArray M = x.optJSONArray("M");
                if (M != null) showStruk(M, fn.isEmpty() ? "struk" : fn);
            });
        }
    }

    // ------------------------------------------------------------------ pelanggan terdaftar: dialog
    private void openRg(String jenis, String nm0, String no0) {
        final String[] edit = {null, null}; // id, jenis lama
        final Ui.Dlg d = ui.dialog();
        LinearLayout b = d.card;
        ui.h2(b, "Pelanggan PPOB");
        ui.label(b, "Jenis");
        final Ui.Select rJ = ui.select(b);
        rJ.options(JENIS_ALL).set(jenis);
        ui.label(b, "Nama pelanggan");
        final EditText rN = ui.input(b, "", Ui.INK_NAME);
        ui.label(b, "Nomor tujuan / ID");
        final EditText rNo = ui.input(b, "", Ui.INK_NUM);
        final LinearLayout lyW = ui.col(b);
        ui.label(lyW, "Layanan / bank");
        final EditText rLy = ui.input(lyW, "mis. DANA atau BCA", Ui.INK_TEXT);
        final Runnable lyT = () -> lyW.setVisibility(rJ.value().equals("E-wallet") || rJ.value().equals("Transfer bank") ? View.VISIBLE : View.GONE);
        rJ.onChange = lyT;
        lyT.run();
        rN.setText(nm0);
        rNo.setText(no0);

        LinearLayout r = ui.row(b);
        ui.mt(r, 12);
        final Ui.Btn save = ui.btn(r, "Simpan", Ui.P, null);
        ui.wt(save, 1);
        Ui.Btn close = ui.btn(r, "Tutup", Ui.W, v -> d.close());
        ui.mar(close, 8, 0, 0, 0);
        ui.mar(ui.label(b, "Daftar pelanggan (ketuk untuk ubah)"), 0, 16, 0, 3);
        final EditText rQ = ui.input(b, "Cari di daftar...", Ui.INK_TEXT);
        MaxScroll ms = new MaxScroll(a, Math.round(screenH() * 0.35f));
        b.addView(ms, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        final LinearLayout list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        ms.addView(list, new android.view.ViewGroup.LayoutParams(Ui.MATCH, Ui.WRAP));

        final Runnable[] fill = new Runnable[1];
        fill[0] = () -> {
            list.removeAllViews();
            List<Store.Pc> arr = st.pcFilter(rQ.getText().toString(), null);
            if (arr.isEmpty()) {
                ui.tv(list, "Belum ada data.", 15);
                return;
            }
            int n = 0;
            for (final Store.Pc c : arr) {
                if (n++ >= 60) break;
                LinearLayout row = ui.row(list);
                row.setPadding(0, ui.dp(8), 0, ui.dp(8));
                LinearLayout col = ui.col(row);
                ui.wt(col, 1);
                ui.tv(col, c.nama, 16);
                ui.note(col, c.no + " · " + c.j).setTextSize(13);
                col.setOnClickListener(v -> {
                    edit[0] = c.id;
                    edit[1] = c.j;
                    rJ.set(c.j);
                    rN.setText(c.nama);
                    rNo.setText(c.no);
                    rLy.setText(c.ly);
                    lyT.run();
                    save.setText("Simpan perubahan");
                });
                ui.btn(row, "✕", Ui.R, v -> ui.confirm("Hapus " + c.nama + " dari daftar?", () -> {
                    st.pcRemove(c);
                    fill[0].run();
                }));
            }
        };
        Ui.watch(rQ, fill[0]);
        save.setOnClickListener(v -> {
            final String j = rJ.value(), n = rN.getText().toString().trim(), no = rNo.getText().toString().trim();
            if (n.isEmpty() || no.isEmpty()) {
                ui.toast("Isi nama dan nomor");
                return;
            }
            final String ly = rLy.getText().toString().trim();
            Runnable go = () -> {
                Store.Pc p = st.pcSave(edit[0], edit[1], j, n, no, ly);
                pick(p);
                edit[0] = null;
                edit[1] = null;
                rN.setText("");
                rNo.setText("");
                rLy.setText("");
                save.setText("Simpan");
                fill[0].run();
            };
            if (edit[0] == null && st.pcDuplicate(j, no)) ui.confirm("Nomor ini sudah terdaftar di jenis yang sama. Tetap simpan?", go);
            else go.run();
        });
        fill[0].run();
        d.show();
    }

    // ------------------------------------------------------------------ buat struk
    private static String stamp() {
        return new SimpleDateFormat("dd MMM yyyy HH.mm", new Locale("id", "ID")).format(new Date());
    }

    private static String noStruk() {
        String s = Long.toString(System.currentTimeMillis(), 36).toUpperCase(Locale.US);
        return "#" + (s.length() > 6 ? s.substring(s.length() - 6) : s);
    }

    private static String slug(String s) {
        return s.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "-");
    }

    private static String group4(String t) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < t.length(); i++) {
            if (i > 0 && i % 4 == 0) sb.append(' ');
            sb.append(t.charAt(i));
        }
        return sb.toString();
    }

    private String lc() {
        return stLc.value();
    }

    private void make() {
        a.hideKeyboard();
        String nm = Store.str(st.set(), "name");
        if (nm.isEmpty()) nm = "Toko Saya";
        String ts = stamp(), no = noStruk();
        JSONArray M = new JSONArray();
        M.put(Receipt.logo());
        M.put(Receipt.c(nm, 26, true));
        double total = 0;
        String ket, fn, md = mode;

        if (mode.equals("ewallet") || mode.equals("transfer")) {
            boolean ew = mode.equals("ewallet");
            long v = Ui.numOf(ew ? eV : tV), ad = Ui.numOf(ew ? eA : tA);
            if (v <= 0) {
                ui.toast("Isi nominal");
                return;
            }
            total = v + ad;
            if (ew) {
                String L = eL.value(), N = eN.getText().toString().trim(), K = eK.getText().toString().trim();
                if (N.isEmpty()) {
                    ui.toast("Isi nomor e-wallet");
                    return;
                }
                M.put(Receipt.c("STRUK E-WALLET", 18, false));
                M.put(Receipt.c(ts, 16, false));
                M.put(Receipt.sep());
                M.put(Receipt.kv("Layanan", L));
                M.put(Receipt.kv("No / ID", N));
                if (!K.isEmpty()) M.put(Receipt.kv("Nama", K));
                ket = L + " " + N;
                fn = "struk-ewallet-" + no.substring(1);
            } else {
                String B = tB.getText().toString().trim(), N = tN.getText().toString().trim(),
                        K = tK.getText().toString().trim(), Mm = tM.getText().toString().trim();
                if (B.isEmpty() || N.isEmpty()) {
                    ui.toast("Isi bank dan nomor rekening");
                    return;
                }
                M.put(Receipt.c("STRUK TRANSFER BANK", 18, false));
                M.put(Receipt.c(ts, 16, false));
                M.put(Receipt.sep());
                M.put(Receipt.kv("Bank", B));
                M.put(Receipt.kv("No Rek", N));
                if (!K.isEmpty()) M.put(Receipt.kv("Penerima", K));
                if (!Mm.isEmpty()) M.put(Receipt.kv("Berita", Mm));
                ket = "Transfer " + B + " " + N;
                fn = "struk-transfer-" + no.substring(1);
            }
            M.put(Receipt.sep());
            M.put(Receipt.lr("Nominal", Store.rp(v)));
            if (ad > 0) M.put(Receipt.lr("Biaya admin", Store.rp(ad)));
            M.put(Receipt.sep());
            M.put(Receipt.big(Store.rp(total)));
            M.put(Receipt.sep());
            M.put(Receipt.c("Terima kasih", 0, true));
            M.put(Receipt.c(no, 14, false));
            if (stKas.isChecked()) st.addLog(ket, total, 0, ad, lc());
        } else if (mode.equals("ppob")) {
            long h = Ui.numOf(pH);
            if (h <= 0) {
                ui.toast("Isi harga");
                return;
            }
            String J = pJ.value(), N = pN.getText().toString().trim(), Pr = pP.getText().toString().trim(),
                    T = pT.getText().toString().replaceAll("\\s", ""), K = pK.getText().toString().trim();
            M.put(Receipt.c("STRUK PEMBELIAN", 18, false));
            M.put(Receipt.c(ts, 16, false));
            M.put(Receipt.sep());
            M.put(Receipt.kv("Jenis", J));
            if (!N.isEmpty()) M.put(Receipt.kv("No / ID", N));
            if (!Pr.isEmpty()) M.put(Receipt.kv("Produk", Pr));
            if (!K.isEmpty()) M.put(Receipt.kv("Nama", K));
            if (!T.isEmpty()) M.put(Receipt.kv("Token/SN", group4(T)));
            M.put(Receipt.sep());
            M.put(Receipt.big(Store.rp(h)));
            M.put(Receipt.sep());
            M.put(Receipt.c("Terima kasih", 0, true));
            M.put(Receipt.c(no, 14, false));
            total = h;
            ket = J + (N.isEmpty() ? "" : " " + N);
            fn = "struk-" + slug(J) + "-" + no.substring(1);
            if (stKas.isChecked()) st.addLog(ket, total, 0, 0, lc());
        } else {
            List<NRow> rows = new ArrayList<>();
            for (NRow r : nl) if (!r.n.trim().isEmpty() && r.q > 0 && r.h >= 0) rows.add(r);
            if (rows.isEmpty()) {
                ui.toast("Isi minimal satu barang");
                return;
            }
            long tot = 0;
            for (NRow r : rows) tot += r.q * r.h;
            total = tot;
            String B = nB.getText().toString().trim();
            M.put(Receipt.c("NOTA BELANJA", 18, false));
            M.put(Receipt.c(ts, 16, false));
            if (!B.isEmpty()) M.put(Receipt.c("Pembeli: " + B, 16, false));
            M.put(Receipt.sep());
            for (NRow r : rows) {
                M.put(Receipt.tx(r.n.trim()));
                M.put(Receipt.lr(r.q + " x " + Store.rp(r.h), Store.rp(r.q * r.h)));
            }
            M.put(Receipt.sep());
            M.put(Receipt.big(Store.rp(tot)));
            long by = Ui.numOf(nBy);
            if (by > 0 && by >= tot) {
                M.put(Receipt.lr("Bayar", Store.rp(by)));
                M.put(Receipt.lr("Kembali", Store.rp(by - tot)));
            }
            M.put(Receipt.sep());
            M.put(Receipt.c("Terima kasih", 0, true));
            M.put(Receipt.c(no, 14, false));
            ket = "Nota belanja" + (B.isEmpty() ? "" : " " + B);
            fn = "nota-" + no.substring(1);
            if (stKas.isChecked()) {
                long manual = 0;
                for (NRow r : rows) {
                    JSONObject it = r.id.isEmpty() ? null : st.item(r.id);
                    if (it != null) {
                        Store.put(it, "s", Math.max(0, Store.lng(it, "s") - r.q));
                        st.addLog("Jual " + Store.str(it, "n") + (r.q > 1 ? " x" + r.q : ""), r.q * r.h, 0,
                                (r.h - Store.num(it, "b")) * r.q, st.lcOf(it), Store.str(it, "id"), -r.q);
                    } else manual += r.q * r.h;
                }
                if (manual > 0) st.addLog(ket, manual, 0, 0, lc());
            }
        }
        st.logStruk(md, ket, total, fn, M);
        st.save();
        a.refresh();
        fillNs();
        showStruk(M, fn);
    }

    // ------------------------------------------------------------------ tampilkan struk
    private int paperMm() {
        try {
            String p = Store.str(st.set(), "paper");
            return p.isEmpty() ? 80 : Integer.parseInt(p);
        } catch (Exception e) {
            return 80;
        }
    }

    private void showStruk(final JSONArray M, final String fn) {
        final Bitmap logo = Ui.decode(Store.str(st.set(), "logo"));
        final Bitmap bm = Receipt.render(M, logo);
        final String text = Receipt.text(M);
        final Ui.Dlg d = ui.dialog();
        LinearLayout b = d.card;
        ui.h2(b, "Struk siap");
        MaxScroll ms = new MaxScroll(a, Math.round(screenH() * 0.55f));
        b.addView(ms, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        ui.mt(ms, 10);
        ImageView iv = new ImageView(a);
        iv.setImageBitmap(bm);
        iv.setAdjustViewBounds(true);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        ms.addView(iv, new android.view.ViewGroup.LayoutParams(Ui.MATCH, Ui.WRAP));

        Ui.Flow f = ui.flow(b);
        ui.mt(f, 12);
        ui.btn(f, "Simpan gambar", Ui.P, v -> Sys.saveImage(a, bm, fn));
        ui.btn(f, "Bagikan", Ui.W, v -> Sys.shareImage(a, bm, fn));
        ui.btn(f, "Cetak", Ui.W, v -> Sys.print(a, bm, fn, paperMm()));
        ui.btn(f, "Salin teks", Ui.W, v -> Sys.copy(a, text, "Teks disalin. Tempel di WhatsApp atau pesan."));
        ui.btn(f, "WhatsApp (teks)", Ui.W, v -> Sys.whatsapp(a, text));
        ui.btn(f, "Tutup", Ui.W, v -> d.close());
        ui.mt(ui.note(b, "Kalau tombol gambar tidak berfungsi di HP-mu, pakai Salin teks atau WhatsApp (teks), atau ambil screenshot."), 10);
        d.show();
    }
}
