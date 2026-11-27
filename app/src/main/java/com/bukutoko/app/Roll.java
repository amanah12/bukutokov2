package com.bukutoko.app;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nominal rupiah berputar acak lalu berhenti satu digit per satu digit, dari kiri ke kanan
 * (seperti papan angka teller). Hanya mengubah tampilan digit; teks akhir selalu sama dengan aslinya.
 */
public class Roll {
    public static boolean enabled = true;

    private static final long TICK_MS = 45L;
    private static final long FIRST_LOCK_MS = 240L;  // digit pertama berhenti setelah ini
    private static final long SPREAD_MS = 640L;      // sisa digit berhenti merata dalam rentang ini
    private static final long MAX_STEP_MS = 110L;
    private static final Pattern NUM = Pattern.compile("Rp\\s?([0-9][0-9.,]*)");
    private static final Random RND = new Random();
    private static final Handler H = new Handler(Looper.getMainLooper());
    private static final List<Item> ACTIVE = new ArrayList<>();
    private static boolean ticking = false;

    private static class Item {
        TextView tv;
        String fin;
        String last;
        int[] pos;   // indeks karakter digit yang diacak
        long t0;
    }

    /** Putar semua nominal "Rp ..." yang ada di dalam [root]. Panggil setelah isi layar dibangun. */
    public static void play(View root) {
        if (!enabled || root == null) return;
        long now = SystemClock.uptimeMillis();
        walk(root, now);
        if (!ACTIVE.isEmpty() && !ticking) {
            ticking = true;
            H.postDelayed(TICK, TICK_MS);
        }
    }

    private static void walk(View v, long now) {
        if (v instanceof EditText) return;
        if (v instanceof TextView) {
            add((TextView) v, now);
            return;
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) walk(g.getChildAt(i), now);
        }
    }

    private static void add(TextView tv, long now) {
        CharSequence cs = tv.getText();
        if (cs == null || cs.length() == 0 || cs.length() > 60) return;
        String s = cs.toString();
        if (!s.contains("Rp")) return;
        for (Item it : ACTIVE) if (it.tv == tv) return;
        List<Integer> pos = new ArrayList<>();
        Matcher m = NUM.matcher(s);
        while (m.find()) {
            for (int i = m.start(1); i < m.end(1); i++) {
                if (Character.isDigit(s.charAt(i))) pos.add(i);
            }
        }
        if (pos.size() < 2) return; // angka satu digit tidak perlu diputar
        Item it = new Item();
        it.tv = tv;
        it.fin = s;
        it.pos = new int[pos.size()];
        for (int i = 0; i < it.pos.length; i++) it.pos[i] = pos.get(i);
        it.t0 = now;
        it.last = scramble(it, 0L);
        tv.setFontFeatureSettings("tnum");
        tv.setText(it.last);
        ACTIVE.add(it);
    }

    private static long lockAt(int k, int n) {
        long step = n <= 1 ? 0 : Math.min(MAX_STEP_MS, SPREAD_MS / (n - 1));
        return FIRST_LOCK_MS + k * step;
    }

    /** Bangun teks pada waktu [el] ms sejak mulai. */
    private static String scramble(Item it, long el) {
        char[] c = it.fin.toCharArray();
        int n = it.pos.length;
        for (int k = 0; k < n; k++) {
            if (el < lockAt(k, n)) {
                boolean lead = k == 0 && c[it.pos[k]] != '0';
                c[it.pos[k]] = (char) ('0' + (lead ? 1 + RND.nextInt(9) : RND.nextInt(10)));
            }
        }
        return new String(c);
    }

    private static final Runnable TICK = new Runnable() {
        @Override
        public void run() {
            long now = SystemClock.uptimeMillis();
            for (int i = ACTIVE.size() - 1; i >= 0; i--) {
                Item it = ACTIVE.get(i);
                TextView tv = it.tv;
                // layar sudah berganti atau teks diubah aplikasi: lepaskan tanpa menyentuh
                if (!tv.isAttachedToWindow() || !tv.getText().toString().equals(it.last)) {
                    tv.setFontFeatureSettings(null);
                    ACTIVE.remove(i);
                    continue;
                }
                long el = now - it.t0;
                if (el >= lockAt(it.pos.length - 1, it.pos.length)) {
                    tv.setText(it.fin);
                    tv.setFontFeatureSettings(null);
                    ACTIVE.remove(i);
                } else {
                    it.last = scramble(it, el);
                    tv.setText(it.last);
                }
            }
            if (ACTIVE.isEmpty()) ticking = false;
            else H.postDelayed(this, TICK_MS);
        }
    };
}
