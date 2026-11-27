package com.bukutoko.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Penyimpanan catatan, riwayat harian, dan pengingat (terpisah dari tokoku_v1.json). */
public class NoteStore {
    private static final String PREF = "catatan_v1";
    private static final int MAX_HISTORY = 400;

    static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    static String fmt(Calendar c) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime());
    }

    public static String today() { return fmt(Calendar.getInstance()); }

    public static String yesterday() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, -1);
        return fmt(c);
    }

    public static String getNote(Context c) { return sp(c).getString("note", ""); }

    public static void setNote(Context c, String t) { sp(c).edit().putString("note", t).apply(); }

    // ---- riwayat ----
    public static JSONArray history(Context c) {
        try { return new JSONArray(sp(c).getString("history", "[]")); }
        catch (JSONException e) { return new JSONArray(); }
    }

    /** Simpan isi catatan saat ini sebagai riwayat tanggal [date]; menimpa entri tanggal yang sama. */
    public static void snapshot(Context c, String date) {
        String t = getNote(c);
        if (t.trim().isEmpty()) return;
        try {
            JSONArray old = history(c);
            JSONArray n = new JSONArray();
            for (int i = 0; i < old.length(); i++) {
                JSONObject o = old.getJSONObject(i);
                if (!date.equals(o.optString("date"))) n.put(o);
            }
            n.put(new JSONObject().put("date", date).put("text", t));
            while (n.length() > MAX_HISTORY) n.remove(0);
            sp(c).edit().putString("history", n.toString()).apply();
        } catch (JSONException ignored) { }
    }

    /** Kalau alarm 23.59 terlewat (HP mati/dihemat), simpan riwayat kemarin saat app dibuka. */
    public static void catchUp(Context c) {
        JSONArray h = history(c);
        if (h.length() == 0) return;
        String y = yesterday();
        for (int i = 0; i < h.length(); i++) {
            JSONObject o = h.optJSONObject(i);
            if (o != null && y.equals(o.optString("date"))) return;
        }
        snapshot(c, y);
    }

    // ---- pengingat ----
    public static JSONArray reminders(Context c) {
        try { return new JSONArray(sp(c).getString("reminders", "[]")); }
        catch (JSONException e) { return new JSONArray(); }
    }

    public static int addReminder(Context c, long time, String text) {
        int id = sp(c).getInt("rid", 1000) + 1;
        try {
            JSONArray r = reminders(c);
            r.put(new JSONObject().put("id", id).put("time", time).put("text", text));
            sp(c).edit().putInt("rid", id).putString("reminders", r.toString()).apply();
        } catch (JSONException ignored) { }
        return id;
    }

    public static void removeReminder(Context c, int id) {
        JSONArray old = reminders(c);
        JSONArray n = new JSONArray();
        for (int i = 0; i < old.length(); i++) {
            JSONObject o = old.optJSONObject(i);
            if (o != null && o.optInt("id") != id) n.put(o);
        }
        sp(c).edit().putString("reminders", n.toString()).apply();
    }

    public static String fmtTime(long t) {
        return new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(new Date(t));
    }
}
