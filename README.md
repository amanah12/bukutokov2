# Buku Toko

Aplikasi Android (Java native, tanpa WebView) untuk mencatat stok, uang laci, bon, rekap, dan struk toko sembako rumahan. Dibuat sederhana supaya mudah dipakai sehari-hari.

Repo: https://github.com/amanah12/bukutokov2
Versi HTML lama ada di repo terpisah: https://github.com/pipisss34/bukutoko

## Fitur

- Stok barang dengan kategori (Semua, Populer, Menipis, Sembako, Rokok, Jajan)
- Laci kas dan tabungan (angka bisa disembunyikan)
- Buku bon/utang pelanggan
- Rekap harian, mingguan, bulanan, tahunan
- Struk pulsa, paket data, token listrik, PPOB, dan nota belanja; bisa disimpan sebagai gambar dan dibagikan
- QRIS di header dan struk
- 11 tema: classic, soft, sage, ceria, imut, studio, lab, pixel, komik, tinta, diy
- Efek visual (opsional): salju, hujan, angin/daun, air di kaca, dengan kepadatan Ringan/Sedang/Lebat
- Backup data
- **Catatan melayang**: ikon 📝 yang bisa digeser dan dibuka kapan saja
  - Ketuk ikon untuk menulis; tersimpan otomatis saat mengetik
  - Ikon 🕘 di pojok menampilkan riwayat catatan yang tersimpan otomatis tiap pukul 23.59
  - Ikon ⏰ membuat pengingat: notifikasi pada waktu yang dipilih dan/atau acara di kalender

## Pasang

1. Buka halaman [Releases](https://github.com/amanah12/bukutokov2/releases/tag/latest)
2. Unduh `BukuToko.apk` lalu pasang (izinkan "Pasang dari sumber tidak dikenal")
3. Untuk pengingat, izinkan notifikasi saat diminta

Data disimpan di perangkat dan kompatibel dengan format `tokoku_v1.json` dari versi sebelumnya.

## Build

Build otomatis lewat GitHub Actions (`assembleRelease`); APK ditandatangani dengan keystore di `app/`.

Build manual:

```
./gradlew assembleRelease
```

Butuh JDK 17 dan Android SDK. Android minimum 6.0 (API 23).

## Batasan

- Navbar kaca berupa tint tembus pandang, bukan blur sungguhan
- Efek cuaca memakai baterai dan tidak tampil di atas dialog
- Pengingat dijadwalkan lewat AlarmManager; sebagian HP dengan penghemat baterai agresif bisa menundanya

## Lisensi

[MIT](LICENSE)
