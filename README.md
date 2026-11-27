# Buku Toko

Aplikasi Android (Java native, tanpa WebView) untuk mencatat stok, uang laci, bon, rekap, dan struk toko sembako rumahan.

Repo: https://github.com/amanah12/bukutokov2
Versi HTML lama ada di repo lain: https://github.com/pipisss34/bukutoko

## Fitur

- Stok barang dengan kategori (Semua, Populer, Menipis, Sembako, Rokok, Jajan)
- Laci kas dan tabungan (angka bisa disembunyikan)
- Buku bon/utang pelanggan, lengkap dengan nomor WhatsApp
- **Tagih bon**: rincian bon dibuat jadi gambar, lalu dikirim ke chat WhatsApp pelanggan. Tombol Tagih mati kalau nomor kosong atau tidak valid. Pesan teks juga bisa dikirim lewat tautan `wa.me`
- Rekap harian, mingguan, bulanan, dan tahunan
- Struk pulsa, paket data, token listrik, PPOB, dan nota belanja; bisa disimpan sebagai gambar dan dibagikan
- QRIS di header dan struk
- **Kunci aplikasi** (opsional): PIN 4 digit atau pola, ditambah sidik jari di Android 10 ke atas
- **Catatan melayang**: ikon 📝 yang bisa digeser; riwayat tersimpan otomatis tiap pukul 23.59; pengingat lewat notifikasi dan kalender
- 20 tema: Classic, Lembut, Sage, Ceria, Imut, Studio, Lab, Pixel, Komik, Tinta, Liquid Glass, Material You, Terminal, Hand-drawn, Y2K Chrome, Bauhaus, Aurora, Vaporwave, Manga, dan DIY
- Gerak antarmuka: kartu muncul satu per satu, tombol dan dialog yang bereaksi saat disentuh, nominal rupiah yang berputar lalu berhenti per digit seperti papan teller
- Efek opsional: salju, hujan, angin, daun, dan air di kaca, dengan kepadatan Ringan/Sedang/Lebat
- Backup data

## Pasang

1. Buka [Releases](https://github.com/amanah12/bukutokov2/releases/tag/latest)
2. Unduh `BukuToko.apk`, lalu pasang (izinkan pemasangan dari sumber tidak dikenal)
3. Izinkan notifikasi saat diminta, supaya pengingat catatan berfungsi

Data tersimpan di perangkat dan kompatibel dengan format `tokoku_v1.json` dari versi sebelumnya. PIN atau pola disimpan terpisah (sebagai hash) dan tidak ikut cadangan data. Kalau lupa, jalan satu-satunya adalah menghapus data aplikasi, jadi buat cadangan dulu.

## Build

Build otomatis lewat GitHub Actions (`assembleRelease`), APK ditandatangani dengan keystore di `app/`.

```
./gradlew assembleRelease
```

Butuh JDK 17 dan Android SDK. Android minimum 7.0 (API 24).

## Batasan

- Navbar kaca berupa tint tembus pandang, bukan blur sungguhan
- Efek cuaca memakai baterai dan tidak tampil di atas dialog
- Link `wa.me` hanya membawa teks. Gambar tagihan dikirim lewat WhatsApp terpisah
- Pengingat memakai AlarmManager; HP dengan penghemat baterai agresif bisa menundanya

## Lisensi

[MIT](LICENSE)
