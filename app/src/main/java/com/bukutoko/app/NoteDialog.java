package com.bukutoko.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.provider.CalendarContract;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class NoteDialog {
    public static void show(final Activity a) {
        final Dialog d = new Dialog(a, android.R.style.Theme_DeviceDefault_Light_NoActionBar);
        int p = NoteFab.dp(a, 16);

        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(p, p, p, p);
        root.setFitsSystemWindows(true);
        root.setBackgroundColor(Color.parseColor("#FFFDF6"));

        final EditText et = new EditText(a);

        LinearLayout bar = new LinearLayout(a);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(a);
        title.setText("Catatan");
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.BLACK);
        bar.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        bar.addView(icon(a, "\u23F0", v -> showReminders(a, et)));
        bar.addView(icon(a, "\uD83D\uDD58", v -> showHistory(a, et)));
        bar.addView(icon(a, "\u2715", v -> d.dismiss()));
        root.addView(bar);

        et.setText(NoteStore.getNote(a));
        et.setHint("Tulis catatan\u2026");
        et.setGravity(Gravity.TOP);
        et.setBackground(null);
        et.setTextSize(17);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        et.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int af) { }
            public void onTextChanged(CharSequence s, int st, int b, int c) { }
            public void afterTextChanged(Editable s) { NoteStore.setNote(a, s.toString()); }
        });
        root.addView(et, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        d.setContentView(root);
        d.show();
    }

    private static TextView icon(Activity a, String s, android.view.View.OnClickListener l) {
        TextView t = new TextView(a);
        t.setText(s);
        t.setTextSize(22);
        int pd = NoteFab.dp(a, 10);
        t.setPadding(pd, pd, pd, pd);
        t.setOnClickListener(l);
        return t;
    }

    // ---------- riwayat ----------
    static void showHistory(final Activity a, final EditText et) {
        final JSONArray h = NoteStore.history(a);
        if (h.length() == 0) {
            Toast.makeText(a, "Belum ada riwayat. Catatan tersimpan otomatis tiap pukul 23.59.", Toast.LENGTH_LONG).show();
            return;
        }
        final String[] items = new String[h.length()];
        final String[] texts = new String[h.length()];
        for (int i = 0; i < h.length(); i++) {
            JSONObject o = h.optJSONObject(h.length() - 1 - i);
            String t = o.optString("text");
            texts[i] = t;
            String one = t.replace('\n', ' ');
            items[i] = o.optString("date") + "  " + (one.length() > 40 ? one.substring(0, 40) + "\u2026" : one);
        }
        new AlertDialog.Builder(a).setTitle("Riwayat catatan")
                .setItems(items, (dlg, w) -> new AlertDialog.Builder(a)
                        .setTitle(items[w].substring(0, 10))
                        .setMessage(texts[w])
                        .setPositiveButton("Pulihkan ke editor", (d2, x) -> et.setText(texts[w]))
                        .setNegativeButton("Tutup", null).show())
                .setNegativeButton("Tutup", null).show();
    }

    // ---------- pengingat ----------
    static void showReminders(final Activity a, final EditText et) {
        final JSONArray r = NoteStore.reminders(a);
        List<String> it = new ArrayList<>();
        it.add("\uFF0B Tambah pengingat");
        for (int i = 0; i < r.length(); i++) {
            JSONObject o = r.optJSONObject(i);
            it.add(NoteStore.fmtTime(o.optLong("time")) + "  " + o.optString("text"));
        }
        new AlertDialog.Builder(a).setTitle("Pengingat")
                .setItems(it.toArray(new String[0]), (dlg, w) -> {
                    if (w == 0) pickTime(a, et);
                    else {
                        final int id = r.optJSONObject(w - 1).optInt("id");
                        new AlertDialog.Builder(a).setMessage("Hapus pengingat ini?")
                                .setPositiveButton("Hapus", (d2, x) -> NoteScheduler.cancelReminder(a, id))
                                .setNegativeButton("Batal", null).show();
                    }
                })
                .setNegativeButton("Tutup", null).show();
    }

    static void pickTime(final Activity a, final EditText et) {
        final Calendar now = Calendar.getInstance();
        new DatePickerDialog(a, (dv, y, m, day) ->
                new TimePickerDialog(a, (tv, hh, mm) -> {
                    Calendar t = Calendar.getInstance();
                    t.set(y, m, day, hh, mm, 0);
                    t.set(Calendar.MILLISECOND, 0);
                    if (t.getTimeInMillis() <= System.currentTimeMillis()) {
                        Toast.makeText(a, "Pilih waktu yang akan datang", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    options(a, et, t.getTimeInMillis());
                }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show(),
                now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
    }

    static void options(final Activity a, final EditText et, final long time) {
        final boolean[] sel = {true, false};
        new AlertDialog.Builder(a).setTitle("Pengingat " + NoteStore.fmtTime(time))
                .setMultiChoiceItems(new String[]{"Notifikasi", "Tambah ke kalender"}, sel, (dlg, i, on) -> sel[i] = on)
                .setPositiveButton("Simpan", (dlg, w) -> {
                    String txt = reminderText(et.getText().toString());
                    if (sel[0]) {
                        if (Build.VERSION.SDK_INT >= 33 && a.checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                                != PackageManager.PERMISSION_GRANTED) {
                            a.requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 77);
                        }
                        NoteScheduler.addReminder(a, time, txt);
                        Toast.makeText(a, "Pengingat disimpan", Toast.LENGTH_SHORT).show();
                    }
                    if (sel[1]) openCalendar(a, time, txt);
                })
                .setNegativeButton("Batal", null).show();
    }

    static String reminderText(String note) {
        String first = note.trim().split("\n")[0].trim();
        if (first.isEmpty()) return "Catatan Buku Toko";
        return first.length() > 80 ? first.substring(0, 80) + "\u2026" : first;
    }

    static void openCalendar(Activity a, long time, String title) {
        Intent in = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, time)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, time + 30 * 60 * 1000L)
                .putExtra(CalendarContract.Events.TITLE, title);
        try { a.startActivity(in); }
        catch (ActivityNotFoundException e) { Toast.makeText(a, "Aplikasi kalender tidak ditemukan", Toast.LENGTH_LONG).show(); }
    }
}
