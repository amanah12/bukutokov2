package com.bukutoko.app;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.json.JSONObject;

/** Tab Menu: profil, tema, warna, grafik, cadangan. */
public class SetScreen extends Screen {
    private static final String[][] SW = {
            {"#c4f04d", "#ff7f9f"}, {"#ffd93d", "#6ec1ff"}, {"#b79cff", "#ffa94d"},
            {"#7ee0a1", "#ffe066"}, {"#ff8a80", "#ffe0b2"}};

    public SetScreen(MainActivity a) {
        super(a);
    }

    @Override
    public void build(LinearLayout into) {
        final JSONObject set = st.set();

        // ---- profil
        Ui.Card p = ui.card(into, Ui.NORMAL);
        ui.h2(p, "Profil toko");
        ui.label(p, "Nama toko");
        EditText nm = ui.input(p, "mis. Toko Berkah", Ui.INK_NAME);
        nm.setText(Store.str(set, "name"));
        Ui.watch(nm, () -> {
            Store.put(set, "name", nm.getText().toString());
            st.save();
            a.refreshHeader();
        });
        ui.label(p, "Logo (opsional)");
        Ui.Btn pl = ui.btn(p, "Pilih logo", Ui.W, v -> Sys.pickImage(a, bmp -> {
            String s = Ui.encodeJpeg(bmp, 128, 85);
            if (s == null) {
                ui.toast("Logo gagal dipakai");
                return;
            }
            Store.put(set, "logo", s);
            st.save();
            a.refreshHeader();
        }));
        ui.mt(pl, 4);
        Ui.Btn pd = ui.btn(p, "Pakai ikon warung", Ui.W, v -> {
            Store.put(set, "logo", "");
            st.save();
            a.refreshHeader();
        });
        ui.mt(pd, 8);

        // ---- tema
        Ui.Card t = ui.card(into, Ui.NORMAL);
        ui.mt(t, 14);
        ui.h2(t, "Tema");
        Ui.Flow fl = ui.flow(t);
        ui.mt(fl, 8);
        String cur = a.th.id;
        for (final String[] th : Th.LIST) {
            Ui.Btn b = ui.btn(fl, th[1], Ui.W, v -> {
                Store.put(set, "tema", th[0]);
                if (!th[0].equals("diy")) {
                    Store.put(set, "c1", th[2]);
                    Store.put(set, "c2", th[3]);
                }
                st.save();
                a.restyle();
            });
            b.padding(12, 9);
            b.on(th[0].equals(cur));
        }
        ui.mt(ui.note(t, "Memilih tema juga mengganti warna. Warna masih bisa diubah lagi di bawah."), 8);

        // ---- tema DIY
        if (cur.equals("diy")) diyCard(into, set);

        // ---- warna
        Ui.Card w = ui.card(into, Ui.NORMAL);
        ui.mt(w, 14);
        ui.h2(w, "Warna");
        ui.label(w, "Pilihan cepat");
        Ui.Flow sf = ui.flow(w);
        for (final String[] pr : SW) {
            LinearLayout sw = new LinearLayout(a);
            sw.setOrientation(LinearLayout.HORIZONTAL);
            Ui.Box bx = new Ui.Box();
            bx.dn = ui.dn;
            bx.bw = 2;
            bx.border = a.th.ink;
            sw.setBackground(bx);
            sw.setPadding(ui.dp(2), ui.dp(2), ui.dp(2), ui.dp(2));
            View l = new View(a);
            l.setBackgroundColor(Th.css(pr[0]));
            View r = new View(a);
            r.setBackgroundColor(Th.css(pr[1]));
            sw.addView(l, new LinearLayout.LayoutParams(0, Ui.MATCH, 1f));
            sw.addView(r, new LinearLayout.LayoutParams(0, Ui.MATCH, 1f));
            sw.setOnClickListener(v -> {
                Store.put(set, "c1", pr[0]);
                Store.put(set, "c2", pr[1]);
                st.save();
                a.restyle();
            });
            sf.addView(sw, new LinearLayout.LayoutParams(ui.dp(56), ui.dp(40)));
        }
        LinearLayout cr = ui.row(w);
        ui.mt(cr, 4);
        colorField(cr, "Warna utama", set, "c1");
        colorField(cr, "Warna aksen", set, "c2");

        // ---- efek dan gerak
        Ui.Card fc = ui.card(into, Ui.NORMAL);
        ui.mt(fc, 14);
        ui.h2(fc, "Efek dan gerak");
        TextView fh = ui.tv(fc, "Efek memakai baterai. Kalau HP terasa berat, turunkan kepadatan atau matikan efeknya. Efek tidak tampil di atas jendela dialog.", 14);
        ui.mar(fh, 0, 6, 0, 6);
        final String[][] FXL = {
                {"motion", "Gerak kartu saat menggulir"}, {"reveal", "Elemen muncul satu per satu"},
                {"liquid", "Navbar kaca cair"}, {"snow", "Turun salju"}, {"rain", "Turun hujan"},
                {"wind", "Hembusan angin"}, {"leaf", "Hembusan daun"}, {"gust", "Hembusan card"},
                {"glass", "Air mengalir di kaca"}};
        final JSONObject fxc = a.fxCfg();
        for (String[] fe : FXL) {
            final String key = fe[0];
            CheckBox fcb = new CheckBox(a);
            fcb.setText(fe[1]);
            fcb.setTextColor(ui.th.cardInk);
            fcb.setTypeface(ui.th.font);
            fcb.setTextSize(14);
            fcb.setChecked(a.fxOn(key));
            fcb.setOnCheckedChangeListener((v, on) -> {
                Store.put(fxc, key, on);
                st.save();
                if (key.equals("liquid")) a.restyle();
                else a.applyFx();
            });
            fc.addView(fcb, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        }
        ui.label(fc, "Kepadatan efek cuaca");
        final Ui.Select flv = ui.select(fc);
        flv.options(new String[]{"Ringan", "Sedang", "Lebat"}, new String[]{"1", "2", "3"})
                .set(fxc.optString("level", "2"));
        flv.onChange = () -> {
            Store.put(fxc, "level", flv.value());
            st.save();
            a.applyFx();
        };

        // ---- grafik dan hitungan
        Ui.Card g = ui.card(into, Ui.NORMAL);
        ui.mt(g, 14);
        ui.h2(g, "Grafik dan hitungan");
        ui.label(g, "Jenis grafik");
        final Ui.Select sc = ui.select(g);
        sc.options(new String[]{"Balok", "Garis"}, new String[]{"bar", "line"})
                .set("line".equals(Store.str(set, "chart")) ? "line" : "bar");
        sc.onChange = () -> {
            Store.put(set, "chart", sc.value());
            st.save();
        };
        ui.label(g, "Nilai stok dihitung dari");
        final Ui.Select sn = ui.select(g);
        sn.options(new String[]{"Harga modal", "Harga jual"}, new String[]{"b", "j"})
                .set("j".equals(Store.str(set, "nil")) ? "j" : "b");
        sn.onChange = () -> {
            Store.put(set, "nil", sn.value());
            st.save();
        };
        ui.label(g, "Lebar cetak struk");
        final Ui.Select sp = ui.select(g);
        String pap = Store.str(set, "paper");
        sp.options(new String[]{"58 mm (printer thermal kecil)", "80 mm (printer thermal)", "Lebar penuh (kertas biasa)"},
                new String[]{"58", "80", "0"}).set(pap.isEmpty() ? "80" : pap);
        sp.onChange = () -> {
            Store.put(set, "paper", sp.value());
            st.save();
        };

        // ---- QRIS
        Ui.Card q = ui.card(into, Ui.NORMAL);
        ui.mt(q, 14);
        ui.h2(q, "QRIS");
        TextView qn = ui.tv(q, "Unggah gambar QRIS tokomu agar pelanggan bisa scan untuk membayar. Tombol QRIS muncul di bagian atas aplikasi.", 14);
        ui.mar(qn, 0, 6, 0, 0);
        Bitmap qbm = Ui.decode(Store.str(set, "qris"));
        if (qbm != null) {
            ImageView qi = new ImageView(a);
            qi.setImageBitmap(qbm);
            qi.setAdjustViewBounds(true);
            qi.setBackgroundColor(Color.WHITE);
            q.addView(qi, new LinearLayout.LayoutParams(ui.dp(220), Ui.WRAP));
            ui.mar(qi, 0, 10, 0, 10);
        } else {
            ui.mar(ui.note(q, "Belum ada QRIS."), 0, 10, 0, 10);
        }
        Ui.Btn qp = ui.btn(q, "Pilih gambar QRIS", Ui.W, v -> Sys.pickImage(a, bmp -> {
            String s = Ui.encodeScaled(bmp, 1000, 92);
            if (s == null) {
                ui.toast("Gambar gagal dipakai");
                return;
            }
            Store.put(set, "qris", s);
            st.save();
            a.rebuild();
            a.refreshHeader();
        }));
        ui.mt(qp, 4);
        Ui.Btn qd = ui.btn(q, "Hapus QRIS", Ui.W, v -> ui.confirm("Hapus QRIS?", () -> {
            Store.put(set, "qris", "");
            Store.put(set, "qrisStruk", false);
            st.save();
            a.rebuild();
            a.refreshHeader();
        }));
        ui.mt(qd, 8);
        final CheckBox qc = new CheckBox(a);
        qc.setText("Cetak QRIS di bawah struk");
        qc.setTextColor(ui.th.cardInk);
        qc.setTypeface(ui.th.font);
        qc.setTextSize(14);
        qc.setChecked(set.optBoolean("qrisStruk", false));
        qc.setOnCheckedChangeListener((cb, on) -> {
            Store.put(set, "qrisStruk", on);
            st.save();
        });
        q.addView(qc, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        ui.mt(qc, 12);

        // ---- bantuan
        Ui.Card h = ui.card(into, Ui.NORMAL);
        ui.mt(h, 14);
        ui.h2(h, "Bantuan");
        ui.mt(ui.btn(h, "Lihat tutorial", Ui.P, v -> Tutorial.show(a)), 10);

        // ---- cadangan
        Ui.Card k = ui.card(into, Ui.NORMAL);
        ui.mt(k, 14);
        ui.h2(k, "Cadangan data");
        TextView nt = ui.tv(k, "Data tersimpan di HP ini. Salin cadangan berkala supaya aman kalau aplikasi dihapus.", 13);
        ui.mar(nt, 0, 8, 0, 8);
        final EditText bk = ui.area(k, "", 5);
        bk.setVisibility(View.GONE);
        ui.mt(bk, 10);

        Ui.Flow f1 = ui.flow(k);
        ui.btn(f1, "Salin cadangan", Ui.P, v -> {
            String s = st.S.toString();
            bk.setVisibility(View.VISIBLE);
            bk.setText(s);
            Sys.copy(a, s, "Cadangan disalin. Tempel di tempat aman.");
        });
        ui.btn(f1, "Simpan ke file", Ui.W, v ->
                Sys.saveText(a, "bukutoko-cadangan-" + Store.today() + ".json", st.S.toString()));
        Ui.Flow f2 = ui.flow(k);
        ui.mt(f2, 8);
        ui.btn(f2, "Pulihkan dari teks", Ui.W, v -> {
            if (bk.getVisibility() == View.GONE) {
                bk.setVisibility(View.VISIBLE);
                bk.setText("");
                bk.setHint("Tempel cadangan di sini, lalu tekan Pulihkan dari teks lagi");
                return;
            }
            restore(bk.getText().toString());
        });
        ui.btn(f2, "Pilih file", Ui.W, v -> Sys.pickText(a, this::restore));
        Ui.Btn rs = ui.btn(k, "Hapus semua data", Ui.R, v -> ui.confirm("Hapus SEMUA data?", () -> {
            st.resetAll();
            a.restyle();
        }));
        ui.mt(rs, 12);
    }

    private void restore(String text) {
        boolean ok;
        try {
            JSONObject d = new JSONObject(text);
            ok = d.optJSONArray("items") != null && d.optJSONArray("log") != null;
        } catch (Exception e) {
            ok = false;
        }
        if (!ok) {
            ui.toast("Cadangan tidak valid");
            return;
        }
        ui.confirm("Pulihkan data dari cadangan? Data di HP ini akan diganti.", () -> {
            if (st.restore(text)) {
                ui.toast("Data dipulihkan");
                a.restyle();
            } else ui.toast("Cadangan tidak valid");
        });
    }

    // ------------------------------------------------------------------ tema DIY
    private JSONObject diyDef(JSONObject set) {
        JSONObject d = set.optJSONObject("diy");
        if (d == null) {
            d = new JSONObject();
            Store.put(set, "diy", d);
        }
        String[][] defs = {{"c", "bulat"}, {"f", "sans"}, {"n", "melayang"}, {"bg", "#f4f1ea"}, {"cd", "#ffffff"}, {"ik", "#1f2937"}};
        for (String[] x : defs) if (d.optString(x[0], "").isEmpty()) Store.put(d, x[0], x[1]);
        return d;
    }

    private Ui.Select pick(LinearLayout card, String label, String[][] opts, final JSONObject d, final String key) {
        ui.label(card, label);
        final Ui.Select s = ui.select(card);
        String[] l = new String[opts.length], v = new String[opts.length];
        for (int i = 0; i < opts.length; i++) {
            v[i] = opts[i][0];
            l[i] = opts[i][1];
        }
        s.options(l, v).set(d.optString(key, v[0]));
        s.onChange = () -> {
            Store.put(d, key, s.value());
            st.save();
            a.restyle();
        };
        return s;
    }

    private void diyCard(LinearLayout into, final JSONObject set) {
        final JSONObject d = diyDef(set);
        Ui.Card c = ui.card(into, Ui.NORMAL);
        ui.mt(c, 14);
        ui.h2(c, "Tema DIY");
        pick(c, "Bentuk kartu dan tombol", Th.DIY_SHAPES, d, "c");
        pick(c, "Font", Th.DIY_FONTS, d, "f");
        pick(c, "Navigasi bawah", Th.DIY_NAV, d, "n");
        LinearLayout r = ui.row(c);
        ui.mt(r, 4);
        colorField(r, "Latar", d, "bg");
        colorField(r, "Kartu", d, "cd");
        colorField(r, "Teks", d, "ik");
        ui.label(c, "Gambar latar (opsional)");
        Ui.Btn pi = ui.btn(c, "Pilih gambar latar", Ui.W, v -> Sys.pickImage(a, bmp -> {
            String s = Ui.encodeScaled(bmp, 720, 70);
            if (s == null) {
                ui.toast("Gambar gagal dipakai");
                return;
            }
            Store.put(d, "img", s);
            st.save();
            a.restyle();
        }));
        ui.mt(pi, 4);
        Ui.Btn px = ui.btn(c, "Hapus gambar latar", Ui.W, v -> {
            Store.put(d, "img", "");
            st.save();
            a.restyle();
        });
        ui.mt(px, 8);
        ui.mt(ui.note(c, "Warna utama dan aksen diatur di kartu Warna di bawah."), 10);
    }

    // ------------------------------------------------------------------ pemilih warna
    private void colorField(LinearLayout row, String label, JSONObject set, String key) {
        LinearLayout c = ui.col(row);
        ui.wt(c, 1);
        ui.label(c, label);
        int clr = Th.safe(Store.str(set, key), "#888888");
        Ui.Btn b = ui.btn(c, Th.hex(clr), Ui.W, v -> pickColor(Th.safe(Store.str(set, key), "#888888"), nc -> {
            Store.put(set, key, Th.hex(nc));
            st.save();
            a.restyle();
        }));
        b.color(clr, Color.luminance(clr) > 0.5f ? 0xFF111111 : 0xFFFFFFFF);
        ui.size(b, -1, 44);
        if (key.equals("c2") || key.equals("cd") || key.equals("ik")) ui.mar(c, 8, 0, 0, 0);
    }

    private interface Clr {
        void on(int c);
    }

    private void pickColor(int init, Clr cb) {
        final Ui.Dlg d = ui.dialog();
        LinearLayout b = d.card;
        ui.h2(b, "Pilih warna");
        final View pv = new View(a);
        final int[] rgb = {Color.red(init), Color.green(init), Color.blue(init)};
        final TextView hex = ui.tv(b, "", 17);
        hex.setGravity(Gravity.CENTER);
        ui.mt(hex, 8);
        b.addView(pv, new LinearLayout.LayoutParams(Ui.MATCH, ui.dp(56)));
        ui.mt(pv, 8);
        final Runnable upd = () -> {
            int c = Color.rgb(rgb[0], rgb[1], rgb[2]);
            Ui.Box bx = new Ui.Box();
            bx.dn = ui.dn;
            bx.fill = c;
            bx.bw = 2;
            bx.border = a.th.ink;
            pv.setBackground(bx);
            hex.setText(Th.hex(c));
        };
        String[] names = {"Merah", "Hijau", "Biru"};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            ui.label(b, names[i]);
            SeekBar sb = new SeekBar(a);
            sb.setMax(255);
            sb.setProgress(rgb[i]);
            sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar s, int p, boolean u) {
                    rgb[idx] = p;
                    upd.run();
                }

                @Override
                public void onStartTrackingTouch(SeekBar s) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar s) {
                }
            });
            b.addView(sb, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        }
        upd.run();
        LinearLayout r = ui.row(b);
        r.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        ui.mt(r, 16);
        ui.btn(r, "Batal", Ui.W, v -> d.close());
        Ui.Btn ok = ui.btn(r, "Pilih", Ui.P, v -> {
            d.close();
            cb.on(Color.rgb(rgb[0], rgb[1], rgb[2]));
        });
        ui.mar(ok, 8, 0, 0, 0);
        d.show();
    }
}
