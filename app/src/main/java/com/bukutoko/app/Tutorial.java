package com.bukutoko.app;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Tutorial pengenalan (dialog beberapa langkah). */
public class Tutorial {
    private static final String[][] TUT = {
            {"🛒", "Selamat datang!", "Aplikasi ini seperti buku catatan toko. Ia menghitung stok barang, uang di laci, dan laba, jadi kamu tidak perlu menghitung manual lagi."},
            {"📦", "1. Isi stok barang", "Di Beranda tekan + Barang. Isi nama, jumlah stok, harga jual, dan harga beli (harga kulak). Barang yang sering terjual dulu saja."},
            {"➖", "2. Jual dan kulak", "Ada barang terjual? Tekan −: stok berkurang dan uang laci bertambah otomatis. Habis kulak? Tekan +, isi jumlah dan total harga kulak: stok bertambah, uang laci berkurang, dan harga modal dihitung rata-rata."},
            {"📒", "3. Buku Kas", "Untuk uang di luar barang: catat Masuk, Keluar, dan Jajan di Buku Kas. Jajan cilok dicatat sebagai Jajan supaya uang laci tetap nyambung."},
            {"📊", "4. Rekap", "Tab Rekap menampilkan grafik dan tabel uang masuk, keluar, dan laba. Pilih Harian, Mingguan, Bulanan, atau Tahunan. Jenis grafik bisa diganti di Menu."},
            {"📝", "5. Buku Bon", "Pelanggan ngutang? Buka tab Bon, tambah nama pelanggan, lalu tekan + Utang. Saat dia bayar, tekan Bayar: uangnya otomatis masuk ke Buku Kas. Total bon yang belum dibayar tampil di Beranda."},
            {"🏪", "Laci dan kategori", "Laci uang dicatat terpisah dan masing-masing punya modal awal sendiri (atur lewat ikon ⚙️ di kanan atas: tambah, hapus, dan ganti judul laci atau kategori). Kategori barang menentukan jualannya masuk ke laci mana. Geser kartu Total harta toko untuk melihat total semua, lalu tiap laci satu per satu. Di daftar stok ada tombol kategori, Populer (barang paling laku), dan Menipis."},
            {"✅", "6. Cek tiap malam", "Hitung uang asli di laci, lalu samakan dengan angka di Beranda. Kalau beda, lihat Buku Kas untuk mencari catatan yang kelewat."},
            {"⚙️", "7. Atur sendiri", "Di tab Menu kamu bisa isi nama toko, pasang logo, ganti tema dan warna, dan menyalin cadangan data. Tutorial ini bisa dibuka lagi kapan saja."}
    };

    private final MainActivity a;
    private final Ui ui;
    private final Ui.Dlg d;
    private int ti = 0;

    private Tutorial(MainActivity a) {
        this.a = a;
        this.ui = a.ui;
        this.d = ui.dialog();
        d.onClose = () -> {
            Store.put(a.st.S, "seen", true);
            a.st.save();
        };
    }

    public static void show(MainActivity a) {
        Tutorial t = new Tutorial(a);
        t.step();
        t.d.show();
    }

    private void step() {
        LinearLayout b = d.card;
        b.removeAllViews();
        String[] t = TUT[ti];
        TextView em = ui.tv(b, t[0], 56);
        em.setGravity(Gravity.CENTER);
        TextView h = ui.h2(b, t[1]);
        h.setTextSize(20);
        h.setGravity(Gravity.CENTER);
        ui.mt(h, 12);
        TextView p = ui.tv(b, t[2], 17);
        ui.mt(p, 8);

        LinearLayout dots = new LinearLayout(a);
        dots.setGravity(Gravity.CENTER);
        b.addView(dots, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        ui.mt(dots, 16);
        boolean sq = ui.th.id.equals("buku") || ui.th.id.equals("piksel");
        for (int i = 0; i < TUT.length; i++) {
            View v = new View(a);
            Ui.Box bx = new Ui.Box();
            bx.dn = ui.dn;
            bx.fill = i == ti ? (sq ? ui.th.c2 : ui.th.accent) : (sq ? ui.th.card : ui.th.sep);
            bx.bw = sq ? 3 : 0;
            bx.border = ui.th.ink;
            bx.r = sq ? 0 : 99;
            v.setBackground(bx);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ui.dp(sq ? 14 : 10), ui.dp(sq ? 14 : 10));
            lp.setMargins(ui.dp(4), 0, ui.dp(4), 0);
            dots.addView(v, lp);
        }

        LinearLayout r = ui.row(b);
        ui.mt(r, 16);
        Ui.Btn skip = ui.btn(r, "Lewati", Ui.W, v -> d.close());
        skip.setVisibility(ti == TUT.length - 1 ? View.INVISIBLE : View.VISIBLE);
        View sp = new View(a);
        r.addView(sp, new LinearLayout.LayoutParams(0, 1, 1f));
        ui.btn(r, ti == TUT.length - 1 ? "Mulai ✓" : "Lanjut →", Ui.P, v -> {
            if (ti >= TUT.length - 1) d.close();
            else {
                ti++;
                step();
            }
        });
    }
}
