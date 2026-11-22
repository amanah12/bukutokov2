package com.bukutoko.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.print.pdf.PrintedPdfDocument;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Fungsi sistem: pilih file, simpan, bagikan, cetak, salin. */
public class Sys {
    public interface Img {
        void on(Bitmap b);
    }

    public interface Txt {
        void on(String s);
    }

    public static Bitmap decodeUri(Context c, Uri u, int max) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            InputStream in = c.getContentResolver().openInputStream(u);
            BitmapFactory.decodeStream(in, null, o);
            if (in != null) in.close();
            int s = 1;
            while (o.outWidth / s > max * 2 || o.outHeight / s > max * 2) s *= 2;
            o.inJustDecodeBounds = false;
            o.inSampleSize = s;
            in = c.getContentResolver().openInputStream(u);
            Bitmap b = BitmapFactory.decodeStream(in, null, o);
            if (in != null) in.close();
            return b;
        } catch (Exception e) {
            return null;
        }
    }

    public static void pickImage(MainActivity a, Img cb) {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        a.startForResult(i, data -> {
            Uri u = data.getData();
            Bitmap b = u == null ? null : decodeUri(a, u, 1024);
            if (b == null) a.ui.toast("File bukan gambar");
            else cb.on(b);
        });
    }

    public static void pickText(MainActivity a, Txt cb) {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        a.startForResult(i, data -> {
            Uri u = data.getData();
            try {
                InputStream in = a.getContentResolver().openInputStream(u);
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
                in.close();
                cb.on(bo.toString("UTF-8"));
            } catch (Exception e) {
                a.ui.toast("File tidak bisa dibaca");
            }
        });
    }

    public static void saveText(MainActivity a, String name, String text) {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.setType("application/json");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.putExtra(Intent.EXTRA_TITLE, name);
        a.startForResult(i, data -> {
            try {
                OutputStream o = a.getContentResolver().openOutputStream(data.getData());
                o.write(text.getBytes("UTF-8"));
                o.close();
                a.ui.toast("File cadangan disimpan");
            } catch (Exception e) {
                a.ui.toast("Gagal menyimpan file");
            }
        });
    }

    public static void saveImage(MainActivity a, Bitmap bm, String name) {
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                ContentValues v = new ContentValues();
                v.put(MediaStore.Images.Media.DISPLAY_NAME, name + ".png");
                v.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                v.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/BukuToko");
                Uri u = a.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
                OutputStream o = a.getContentResolver().openOutputStream(u);
                bm.compress(Bitmap.CompressFormat.PNG, 100, o);
                o.close();
                a.ui.toast("Struk disimpan di galeri (Pictures/BukuToko)");
            } catch (Exception e) {
                a.ui.toast("Gagal menyimpan gambar. Coba Bagikan atau screenshot.");
            }
            return;
        }
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.setType("image/png");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.putExtra(Intent.EXTRA_TITLE, name + ".png");
        a.startForResult(i, data -> {
            try {
                OutputStream o = a.getContentResolver().openOutputStream(data.getData());
                bm.compress(Bitmap.CompressFormat.PNG, 100, o);
                o.close();
                a.ui.toast("Struk disimpan");
            } catch (Exception e) {
                a.ui.toast("Gagal menyimpan gambar");
            }
        });
    }

    public static void shareImage(MainActivity a, Bitmap bm, String name) {
        try {
            File dir = new File(a.getCacheDir(), "share");
            dir.mkdirs();
            File f = new File(dir, name.replaceAll("[^A-Za-z0-9._-]", "_") + ".png");
            FileOutputStream o = new FileOutputStream(f);
            bm.compress(Bitmap.CompressFormat.PNG, 100, o);
            o.close();
            Uri u = FileProvider.getUriForFile(a, a.getPackageName() + ".files", f);
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("image/png");
            i.putExtra(Intent.EXTRA_STREAM, u);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            a.startActivity(Intent.createChooser(i, "Bagikan struk"));
        } catch (Exception e) {
            a.ui.toast("Gagal membagikan. Simpan gambar lalu kirim dari galeri.");
        }
    }

    public static void copy(MainActivity a, String text, String okMsg) {
        try {
            ClipboardManager cm = (ClipboardManager) a.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("Buku Toko", text));
            a.ui.toast(okMsg);
        } catch (Exception e) {
            a.ui.toast("Gagal menyalin.");
        }
    }

    public static void whatsapp(MainActivity a, String text) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/?text=" + Uri.encode(text)));
            a.startActivity(i);
        } catch (Exception e) {
            a.ui.toast("WhatsApp tidak tersedia");
        }
    }

    /** Cetak struk. mm = lebar kertas (58/80), 0 = ukuran A4. */
    public static void print(MainActivity a, Bitmap bm, String name, int mm) {
        try {
            PrintManager pm = (PrintManager) a.getSystemService(Context.PRINT_SERVICE);
            PrintAttributes.MediaSize ms;
            if (mm > 0) {
                int w = Math.round(mm / 25.4f * 1000f);
                int h = Math.max(1000, Math.round(w * (float) bm.getHeight() / bm.getWidth()));
                ms = new PrintAttributes.MediaSize("struk" + mm, "Struk " + mm + " mm", w, h);
            } else {
                ms = PrintAttributes.MediaSize.ISO_A4;
            }
            PrintAttributes attrs = new PrintAttributes.Builder()
                    .setMediaSize(ms)
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build();
            pm.print(name, new PrintDocumentAdapter() {
                PrintedPdfDocument doc;

                @Override
                public void onLayout(PrintAttributes oldA, PrintAttributes newA, CancellationSignal cs,
                                     LayoutResultCallback cb, Bundle extras) {
                    doc = new PrintedPdfDocument(a, newA);
                    if (cs.isCanceled()) {
                        cb.onLayoutCancelled();
                        return;
                    }
                    PrintDocumentInfo info = new PrintDocumentInfo.Builder(name + ".pdf")
                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(1).build();
                    cb.onLayoutFinished(info, true);
                }

                @Override
                public void onWrite(PageRange[] pages, ParcelFileDescriptor dest, CancellationSignal cs,
                                    WriteResultCallback cb) {
                    try {
                        android.graphics.pdf.PdfDocument.Page pg = doc.startPage(0);
                        Canvas cv = pg.getCanvas();
                        float sc = cv.getWidth() / (float) bm.getWidth();
                        cv.save();
                        cv.scale(sc, sc);
                        cv.drawBitmap(bm, 0, 0, null);
                        cv.restore();
                        doc.finishPage(pg);
                        doc.writeTo(new FileOutputStream(dest.getFileDescriptor()));
                    } catch (IOException e) {
                        cb.onWriteFailed(e.toString());
                        return;
                    } finally {
                        doc.close();
                    }
                    cb.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
                }
            }, attrs);
        } catch (Exception e) {
            a.ui.toast("Cetak tidak didukung di sini. Gunakan Bagikan atau Simpan gambar.");
        }
    }
}
