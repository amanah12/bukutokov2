package com.bukutoko.app;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Locale;

/** Tagih bon lewat WhatsApp: gambar rincian bon + pesan teks via wa.me. */
public class Tagih {
    /** 08xx, 8xx, +62 8xx, atau 62 8xx menjadi 628xx. Kosong kalau tidak valid. */
    public static String norm(String raw) {
        String d = raw == null ? "" : raw.replaceAll("[^0-9]", "");
        if (d.startsWith("00")) d = d.substring(2);
        if (d.startsWith("0")) d = "62" + d.substring(1);
        else if (d.startsWith("8")) d = "62" + d;
        return d.length() >= 10 && d.length() <= 15 ? d : "";
    }

    public static void show(final MainActivity a, JSONObject c) {
        final Ui ui = a.ui;
        final Store st = a.st;
        final String num = norm(Store.str(c, "wa"));
        if (num.isEmpty()) {
            ui.toast("Isi nomor WhatsApp dulu");
            return;
        }
        double sisa = st.saldo(c);
        if (sisa <= 0) {
            ui.toast("Tidak ada sisa bon untuk ditagih");
            return;
        }
        String shop = Store.str(st.set(), "name");
        if (shop.isEmpty()) shop = "Toko Saya";
        final String nama = Store.str(c, "nama");

        JSONArray M = new JSONArray();
        M.put(Receipt.logo());
        M.put(Receipt.c(shop, 26, true));
        M.put(Receipt.c("TAGIHAN BON", 18, false));
        M.put(Receipt.c(Store.dmy(Store.today()), 16, false));
        M.put(Receipt.sep());
        M.put(Receipt.kv("Nama", nama));
        M.put(Receipt.sep());
        List<JSONObject> lg = Store.list(Store.arr(c, "log"));
        int from = Math.max(0, lg.size() - 12);
        if (from > 0) M.put(Receipt.tx("(" + from + " catatan sebelumnya tidak ditampilkan)"));
        for (int i = from; i < lg.size(); i++) {
            JSONObject e = lg.get(i);
            boolean u = "u".equals(Store.str(e, "t"));
            String k = Store.str(e, "ket");
            if (k.isEmpty()) k = u ? "Utang" : "Bayar";
            if (k.length() > 16) k = k.substring(0, 15) + "\u2026";
            M.put(Receipt.lr(Store.dm(Store.str(e, "d")) + " " + k, (u ? "+" : "-") + Store.rp(Store.num(e, "n"))));
        }
        M.put(Receipt.sep());
        M.put(Receipt.c("Sisa bon", 16, false));
        M.put(Receipt.big(Store.rp(sisa)));
        M.put(Receipt.sep());
        final String text = "Halo " + nama + ", berikut tagihan bon di " + shop + ":\n\n" + Receipt.text(M)
                + "\n\nMohon segera dilunasi. Terima kasih.";
        M.put(Receipt.c("Mohon segera dilunasi", 0, true));
        M.put(Receipt.c("Terima kasih", 0, false));

        Bitmap logo = Ui.decode(Store.str(st.set(), "logo"));
        Bitmap qr = Ui.decode(Store.str(st.set(), "qris"));
        boolean useQ = qr != null && st.set().optBoolean("qrisStruk", false);
        JSONArray M2 = M;
        if (useQ) {
            M2 = new JSONArray();
            for (int i = 0; i < M.length(); i++) M2.put(M.opt(i));
            M2.put(Receipt.sep());
            M2.put(Receipt.cell("qr", null, null, 0));
        }
        final Bitmap bm = Receipt.render(M2, logo, useQ ? qr : null);
        final String fn = "tagihan-" + nama.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "-") + "-" + Store.today();

        final Ui.Dlg d = ui.dialog();
        LinearLayout b = d.card;
        ui.h2(b, "Tagihan siap");
        Screen.MaxScroll ms = new Screen.MaxScroll(a, Math.round(a.getResources().getDisplayMetrics().heightPixels * 0.5f));
        b.addView(ms, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        ui.mt(ms, 10);
        ImageView iv = new ImageView(a);
        iv.setImageBitmap(bm);
        iv.setAdjustViewBounds(true);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        ms.addView(iv, new android.view.ViewGroup.LayoutParams(Ui.MATCH, Ui.WRAP));

        Ui.Flow f = ui.flow(b);
        ui.mt(f, 12);
        ui.btn(f, "Kirim gambar ke WhatsApp", Ui.P, v -> sendImage(a, bm, fn, num));
        ui.btn(f, "Kirim teks (wa.me)", Ui.W, v -> openWaMe(a, num, text));
        ui.btn(f, "Simpan gambar", Ui.W, v -> Sys.saveImage(a, bm, fn));
        ui.btn(f, "Tutup", Ui.W, v -> d.close());
        ui.mt(ui.note(b, "Link wa.me hanya bisa membawa teks. Untuk gambar, WhatsApp akan terbuka di chat pelanggan; kalau tidak langsung masuk ke chat-nya, pilih kontaknya dari daftar."), 10);
        d.show();
    }

    /** Buka https://wa.me/NOMOR?text=... di browser; browser lalu melompat ke aplikasi WhatsApp. */
    public static void openWaMe(MainActivity a, String num, String text) {
        Uri u = Uri.parse("https://wa.me/" + num + "?text=" + Uri.encode(text));
        try {
            Intent i = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER);
            i.setData(u);
            a.startActivity(i);
        } catch (Exception e) {
            try {
                a.startActivity(new Intent(Intent.ACTION_VIEW, u));
            } catch (Exception e2) {
                a.ui.toast("Tidak ada browser atau WhatsApp");
            }
        }
    }

    /** Kirim gambar langsung ke chat nomor tersebut (WhatsApp / WhatsApp Business); cadangan: menu bagikan biasa. */
    private static void sendImage(MainActivity a, Bitmap bm, String fn, String num) {
        try {
            File dir = new File(a.getCacheDir(), "share");
            dir.mkdirs();
            File f = new File(dir, fn.replaceAll("[^A-Za-z0-9._-]", "_") + ".png");
            FileOutputStream o = new FileOutputStream(f);
            bm.compress(Bitmap.CompressFormat.PNG, 100, o);
            o.close();
            Uri u = FileProvider.getUriForFile(a, a.getPackageName() + ".files", f);
            for (String p : new String[]{"com.whatsapp", "com.whatsapp.w4b"}) {
                try {
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("image/png");
                    i.setPackage(p);
                    i.putExtra(Intent.EXTRA_STREAM, u);
                    i.putExtra("jid", num + "@s.whatsapp.net");
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    a.startActivity(i);
                    return;
                } catch (ActivityNotFoundException ignored) { }
            }
        } catch (Exception ignored) { }
        Sys.shareImage(a, bm, fn);
    }
}
