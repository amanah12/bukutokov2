package com.bukutoko.app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.Collator;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Penyimpanan data + seluruh logika bisnis. Format JSON sama persis dengan versi web
 * (kunci "tokoku_v1"), jadi file cadangan lama bisa dipulihkan langsung.
 */
public class Store {
    public JSONObject S;
    private final File file;
    private static final Collator COL = Collator.getInstance(new Locale("id", "ID"));
    private static final DecimalFormat DF;

    static {
        DecimalFormatSymbols sy = new DecimalFormatSymbols(Locale.US);
        sy.setGroupingSeparator('.');
        DF = new DecimalFormat("#,##0", sy);
    }

    public interface P {
        boolean ok(JSONObject o);
    }

    public static class Pc {
        public String id, nama, no, ly, j;
    }

    public Store(Context c) {
        file = new File(c.getFilesDir(), "tokoku_v1.json");
        load();
    }

    // ---------------------------------------------------------------- util JSON
    public static void put(JSONObject o, String k, Object v) {
        try {
            o.put(k, v);
        } catch (JSONException e) {
            // abaikan
        }
    }

    public static double num(JSONObject o, String k) {
        double v = o.optDouble(k, 0);
        return Double.isNaN(v) ? 0 : v;
    }

    public static long lng(JSONObject o, String k) {
        return Math.round(num(o, k));
    }

    public static String str(JSONObject o, String k) {
        if (o == null || !o.has(k) || o.isNull(k)) return "";
        return o.optString(k, "");
    }

    public static JSONArray arr(JSONObject o, String k) {
        JSONArray a = o.optJSONArray(k);
        if (a == null) {
            a = new JSONArray();
            put(o, k, a);
        }
        return a;
    }

    public static JSONObject obj(JSONObject o, String k) {
        JSONObject a = o.optJSONObject(k);
        if (a == null) {
            a = new JSONObject();
            put(o, k, a);
        }
        return a;
    }

    public static List<JSONObject> list(JSONArray a) {
        List<JSONObject> r = new ArrayList<>();
        if (a == null) return r;
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null) r.add(o);
        }
        return r;
    }

    public static void removeIf(JSONArray a, P p) {
        for (int i = a.length() - 1; i >= 0; i--) {
            JSONObject o = a.optJSONObject(i);
            if (o != null && p.ok(o)) a.remove(i);
        }
    }

    public static JSONObject find(JSONArray a, String id) {
        for (JSONObject o : list(a)) if (str(o, "id").equals(id)) return o;
        return null;
    }

    // ---------------------------------------------------------------- format
    public static String rp(double n) {
        return "Rp " + DF.format(Math.round(n));
    }

    public static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    public static String dm(String d) {
        if (d == null || d.length() < 10) return d == null ? "" : d;
        return d.substring(8) + "/" + d.substring(5, 7);
    }

    public static String dmy(String d) {
        if (d == null || d.length() < 10) return d == null ? "" : d;
        return d.substring(8) + "/" + d.substring(5, 7) + "/" + d.substring(0, 4);
    }

    public static String uid() {
        String r = Long.toString((long) (Math.random() * 1.0e9), 36) + "0000";
        return Long.toString(System.currentTimeMillis(), 36) + r.substring(0, 4);
    }

    public static int cmp(String a, String b) {
        return COL.compare(a, b);
    }

    // ---------------------------------------------------------------- load / save
    public void load() {
        S = null;
        try {
            if (file.exists()) {
                FileInputStream in = new FileInputStream(file);
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
                in.close();
                S = new JSONObject(bo.toString("UTF-8"));
            }
        } catch (Exception e) {
            S = null;
        }
        if (S == null) S = new JSONObject();
        normalize();
    }

    public boolean save() {
        try {
            File tmp = new File(file.getParentFile(), "tokoku_v1.json.tmp");
            FileOutputStream o = new FileOutputStream(tmp);
            o.write(S.toString().getBytes("UTF-8"));
            o.flush();
            o.getFD().sync();
            o.close();
            if (!tmp.renameTo(file)) {
                file.delete();
                return tmp.renameTo(file);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private JSONObject mk(String id, String nama, double modal) {
        JSONObject o = new JSONObject();
        put(o, "id", id);
        put(o, "nama", nama);
        put(o, "modal", modal);
        return o;
    }

    public void normalize() {
        if (!S.has("modal")) put(S, "modal", 0);
        arr(S, "items");
        arr(S, "log");
        JSONObject st = obj(S, "set");
        if (!st.has("name")) put(st, "name", "");
        if (!st.has("logo")) put(st, "logo", "");
        if (!st.has("c1")) put(st, "c1", "#c4f04d");
        if (!st.has("c2")) put(st, "c2", "#ff7f9f");
        JSONArray la = S.optJSONArray("lacis");
        if (la == null || la.length() == 0) {
            la = new JSONArray();
            la.put(mk("s", "Laci Sembako", num(S, "modal")));
            la.put(mk("j", "Laci Jajan", num(S, "modalJ")));
            la.put(mk("r", "Laci Rokok", num(S, "modalR")));
            put(S, "lacis", la);
        }
        JSONArray ca = S.optJSONArray("cats");
        if (ca == null || ca.length() == 0) {
            ca = new JSONArray();
            ca.put(cat("sembako", "Sembako", "s"));
            ca.put(cat("jajan", "Jajan", "j"));
            ca.put(cat("rokok", "Rokok", "r"));
            put(S, "cats", ca);
        }
        arr(S, "bon");
        arr(S, "tab");
        arr(S, "struk");
        obj(S, "ppobDb");
    }

    private JSONObject cat(String id, String nama, String laci) {
        JSONObject o = new JSONObject();
        put(o, "id", id);
        put(o, "nama", nama);
        put(o, "laci", laci);
        return o;
    }

    // ---------------------------------------------------------------- akses
    public JSONObject set() {
        return obj(S, "set");
    }

    public JSONArray items() {
        return arr(S, "items");
    }

    public JSONArray log() {
        return arr(S, "log");
    }

    public JSONArray lacis() {
        return arr(S, "lacis");
    }

    public JSONArray cats() {
        return arr(S, "cats");
    }

    public JSONArray bon() {
        return arr(S, "bon");
    }

    public JSONArray tab() {
        return arr(S, "tab");
    }

    public JSONArray struk() {
        return arr(S, "struk");
    }

    public JSONObject ppobDb() {
        return obj(S, "ppobDb");
    }

    public JSONObject item(String id) {
        return find(items(), id);
    }

    public JSONObject laci(String id) {
        return find(lacis(), id);
    }

    public JSONObject kat(String id) {
        return find(cats(), id);
    }

    public JSONObject bonOf(String id) {
        return find(bon(), id);
    }

    public String firstLaci() {
        return str(lacis().optJSONObject(0), "id");
    }

    public boolean hasLaci(String id) {
        return laci(id) != null;
    }

    public String lcNm(String id) {
        JSONObject l = laci(id);
        return l == null ? "" : str(l, "nama");
    }

    public String lcOf(JSONObject x) {
        String c = str(x, "cat");
        if (c.isEmpty()) c = "sembako";
        JSONObject k = kat(c);
        if (k != null && hasLaci(str(k, "laci"))) return str(k, "laci");
        return firstLaci();
    }

    public String catOf(JSONObject x) {
        String c = str(x, "cat");
        if (c.isEmpty()) c = "sembako";
        return kat(c) != null ? c : str(cats().optJSONObject(0), "id");
    }

    public String lcE(JSONObject e) {
        String v = str(e, "lc");
        if (v.isEmpty() || v.equals("u")) v = "s";
        return hasLaci(v) ? v : firstLaci();
    }

    public double MT() {
        double a = 0;
        for (JSONObject l : list(lacis())) a += num(l, "modal");
        return a;
    }

    /** lc == null: semua laci. */
    public double curSisa(String lc) {
        double a = 0;
        if (lc == null) a = MT();
        else {
            JSONObject l = laci(lc);
            if (l != null) a = num(l, "modal");
        }
        for (JSONObject e : list(log())) {
            if (lc == null || lcE(e).equals(lc)) a += num(e, "i") - num(e, "o");
        }
        return a;
    }

    public double saldoL(JSONObject c, String lc) {
        double a = 0;
        for (JSONObject e : list(arr(c, "log"))) {
            if (lcE(e).equals(lc)) a += "u".equals(str(e, "t")) ? num(e, "n") : -num(e, "n");
        }
        return a;
    }

    public double saldo(JSONObject c) {
        double a = 0;
        for (JSONObject e : list(arr(c, "log"))) a += "u".equals(str(e, "t")) ? num(e, "n") : -num(e, "n");
        return a;
    }

    public double bonSum(String lc) {
        double a = 0;
        for (JSONObject c : list(bon())) a += Math.max(0, saldoL(c, lc));
        return a;
    }

    public double bonTotal() {
        double a = 0;
        for (JSONObject c : list(bon())) a += Math.max(0, saldo(c));
        return a;
    }

    /** Nilai stok. lc == null: semua. */
    public double nilaiStok(String lc) {
        boolean j = "j".equals(str(set(), "nil"));
        double a = 0;
        for (JSONObject x : list(items())) {
            if (lc != null && !lcOf(x).equals(lc)) continue;
            double h = j ? num(x, "j") : (num(x, "b") != 0 ? num(x, "b") : num(x, "j"));
            a += num(x, "s") * h;
        }
        return a;
    }

    public double tabSal() {
        double a = 0;
        for (JSONObject e : list(tab())) a += num(e, "i") - num(e, "o");
        return a;
    }

    // ---------------------------------------------------------------- log kas
    public JSONObject addLog(String ket, double i, double o, double l, String lc, String itId, Long q) {
        JSONObject e = new JSONObject();
        put(e, "id", uid());
        put(e, "d", today());
        put(e, "ket", ket);
        put(e, "i", i);
        put(e, "o", o);
        put(e, "l", l);
        if (itId != null) put(e, "it", itId);
        if (q != null) put(e, "q", q);
        if (lc != null) put(e, "lc", lc);
        log().put(e);
        save();
        return e;
    }

    public JSONObject addLog(String ket, double i, double o, double l, String lc) {
        return addLog(ket, i, o, l, lc, null, null);
    }

    public boolean sell(JSONObject it) {
        long s = lng(it, "s");
        if (s <= 0) return false;
        put(it, "s", s - 1);
        double j = num(it, "j");
        addLog("Jual " + str(it, "n"), j, 0, j - num(it, "b"), lcOf(it), str(it, "id"), -1L);
        return true;
    }

    public void kulak(JSONObject it, long q, long h) {
        long s = lng(it, "s");
        if (h > 0) {
            double b = num(it, "b");
            double nb;
            if (b == 0 || s <= 0) nb = Math.round((double) h / q);
            else nb = Math.round((s * b + h) / (double) (s + q));
            put(it, "b", nb);
            addLog("Kulak " + q + " " + str(it, "n"), 0, h, 0, lcOf(it), str(it, "id"), q);
        }
        put(it, "s", s + q);
        save();
    }

    /** Hapus catatan kas; stok disesuaikan kalau terkait barang. Mengembalikan nama barang yang disesuaikan (atau null). */
    public String itemOfLog(JSONObject en) {
        String it = str(en, "it");
        if (it.isEmpty()) return null;
        JSONObject x = item(it);
        return x == null ? null : str(x, "n");
    }

    public void deleteLog(String id) {
        JSONObject en = find(log(), id);
        if (en == null) return;
        String it = str(en, "it");
        if (!it.isEmpty()) {
            JSONObject x = item(it);
            if (x != null) put(x, "s", Math.max(0, lng(x, "s") - lng(en, "q")));
        }
        removeIf(log(), o -> str(o, "id").equals(id));
        save();
    }

    public Map<String, Integer> popCount() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, -30);
        String t = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime());
        Map<String, Integer> m = new HashMap<>();
        for (JSONObject e : list(log())) {
            String it = str(e, "it");
            if (!it.isEmpty() && num(e, "q") < 0 && str(e, "d").compareTo(t) >= 0) {
                Integer v = m.get(it);
                m.put(it, (v == null ? 0 : v) - (int) num(e, "q"));
            }
        }
        return m;
    }

    public List<JSONObject> viewItems(String q, String cat) {
        List<JSONObject> r = new ArrayList<>();
        Comparator<JSONObject> nm = (a, b) -> cmp(str(a, "n"), str(b, "n"));
        if (q != null && !q.isEmpty()) {
            for (JSONObject x : list(items())) if (str(x, "n").toLowerCase().contains(q)) r.add(x);
            Collections.sort(r, nm);
            return r;
        }
        if (cat.equals("pop")) {
            final Map<String, Integer> m = popCount();
            for (JSONObject x : list(items())) if (m.containsKey(str(x, "id"))) r.add(x);
            Collections.sort(r, (a, b) -> {
                int d = m.get(str(b, "id")) - m.get(str(a, "id"));
                return d != 0 ? d : nm.compare(a, b);
            });
            return r.size() > 10 ? new ArrayList<>(r.subList(0, 10)) : r;
        }
        if (cat.equals("men")) {
            for (JSONObject x : list(items())) if (num(x, "s") <= 3) r.add(x);
        } else if (cat.equals("all")) {
            r.addAll(list(items()));
        } else {
            for (JSONObject x : list(items())) if (catOf(x).equals(cat)) r.add(x);
        }
        Collections.sort(r, nm);
        return r;
    }

    public int menipisCount() {
        int n = 0;
        for (JSONObject x : list(items())) if (num(x, "s") <= 3) n++;
        return n;
    }

    // ---------------------------------------------------------------- laci & kategori
    public void deleteLaci(String id, String to) {
        JSONObject L = laci(id), T = laci(to);
        if (L == null || T == null) return;
        for (JSONObject e : list(log())) if (lcE(e).equals(id)) put(e, "lc", to);
        for (JSONObject c : list(bon()))
            for (JSONObject e : list(arr(c, "log"))) if (lcE(e).equals(id)) put(e, "lc", to);
        for (JSONObject k : list(cats())) if (str(k, "laci").equals(id)) put(k, "laci", to);
        put(T, "modal", num(T, "modal") + num(L, "modal"));
        removeIf(lacis(), o -> str(o, "id").equals(id));
        save();
    }

    public void deleteKat(String id, String to) {
        JSONObject K = kat(id);
        if (K == null || kat(to) == null) return;
        for (JSONObject x : list(items())) if (catOf(x).equals(id)) put(x, "cat", to);
        removeIf(cats(), o -> str(o, "id").equals(id));
        save();
    }

    public JSONObject addLaci() {
        JSONObject o = mk("l" + uid(), "Laci baru", 0);
        lacis().put(o);
        save();
        return o;
    }

    public JSONObject addKat() {
        JSONObject o = cat("c" + uid(), "Kategori baru", firstLaci());
        cats().put(o);
        save();
        return o;
    }

    // ---------------------------------------------------------------- cadangan
    public boolean restore(String text) {
        try {
            JSONObject d = new JSONObject(text);
            if (d.optJSONArray("items") == null || d.optJSONArray("log") == null) return false;
            S = d;
            normalize();
            save();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void resetAll() {
        JSONObject n = new JSONObject();
        put(n, "modal", 0);
        put(n, "items", new JSONArray());
        put(n, "log", new JSONArray());
        put(n, "set", set());
        put(n, "seen", true);
        JSONArray la = new JSONArray();
        for (JSONObject l : list(lacis())) la.put(mk(str(l, "id"), str(l, "nama"), 0));
        put(n, "lacis", la);
        put(n, "cats", cats());
        S = n;
        normalize();
        save();
    }

    // ---------------------------------------------------------------- rekap
    public static String bucketKey(String per, String d) {
        if (per.equals("hari")) return d;
        if (per.equals("bulan")) return d.substring(0, 7);
        if (per.equals("tahun")) return d.substring(0, 4);
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Integer.parseInt(d.substring(0, 4)), Integer.parseInt(d.substring(5, 7)) - 1, Integer.parseInt(d.substring(8, 10)));
        int back = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        c.add(Calendar.DAY_OF_MONTH, -back);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime());
    }

    public static String bucketLabel(String per, String k, boolean full) {
        if (per.equals("hari")) return dm(k);
        if (per.equals("minggu")) return (full ? "Mgg " : "") + dm(k);
        if (per.equals("tahun")) return k;
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Integer.parseInt(k.substring(0, 4)), Integer.parseInt(k.substring(5, 7)) - 1, 1);
        return new SimpleDateFormat(full ? "MMM yyyy" : "MMM yy", new Locale("id", "ID")).format(c.getTime());
    }

    /** key -> {masuk, keluar, laba, sisa} */
    public TreeMap<String, double[]> rekap(String per) {
        TreeMap<String, double[]> B = new TreeMap<>();
        List<JSONObject> ls = list(log());
        Collections.sort(ls, (a, b) -> str(a, "d").compareTo(str(b, "d")));
        double rr = MT();
        for (JSONObject e : ls) {
            String d = str(e, "d");
            if (d.length() < 10) continue;
            String k = bucketKey(per, d);
            double[] m = B.get(k);
            if (m == null) {
                m = new double[4];
                B.put(k, m);
            }
            m[0] += num(e, "i");
            m[1] += num(e, "o");
            m[2] += num(e, "l");
            rr += num(e, "i") - num(e, "o");
            m[3] = rr;
        }
        return B;
    }

    // ---------------------------------------------------------------- pelanggan PPOB
    public List<Pc> pcAll() {
        List<Pc> r = new ArrayList<>();
        JSONObject db = ppobDb();
        java.util.Iterator<String> it = db.keys();
        while (it.hasNext()) {
            String j = it.next();
            for (JSONObject c : list(db.optJSONArray(j))) {
                Pc p = new Pc();
                p.id = str(c, "id");
                p.nama = str(c, "nama");
                p.no = str(c, "no");
                p.ly = str(c, "ly");
                p.j = j;
                r.add(p);
            }
        }
        return r;
    }

    public List<Pc> pcFilter(String q, String jenis) {
        String s = q == null ? "" : q.trim().toLowerCase();
        List<Pc> r = new ArrayList<>();
        for (Pc c : pcAll()) {
            if (jenis != null && !c.j.equals(jenis)) continue;
            if (!s.isEmpty() && !c.nama.toLowerCase().contains(s) && !c.no.contains(s)) continue;
            r.add(c);
        }
        Collections.sort(r, (a, b) -> cmp(a.nama, b.nama));
        return r;
    }

    public void pcRemove(Pc c) {
        JSONArray a = ppobDb().optJSONArray(c.j);
        if (a != null) removeIf(a, o -> str(o, "id").equals(c.id));
        save();
    }

    /** Simpan / ubah pelanggan. oldId/oldJ null = baru. Mengembalikan data tersimpan. */
    public Pc pcSave(String oldId, String oldJ, String j, String nama, String no, String ly) {
        JSONObject db = ppobDb();
        JSONArray target = arr(db, j);
        JSONObject c = null;
        if (oldId != null) {
            JSONArray old = db.optJSONArray(oldJ);
            if (old != null) {
                for (int i = 0; i < old.length(); i++) {
                    JSONObject o = old.optJSONObject(i);
                    if (o != null && str(o, "id").equals(oldId)) {
                        c = o;
                        old.remove(i);
                        break;
                    }
                }
            }
            if (c == null) {
                c = new JSONObject();
                put(c, "id", oldId);
            }
        } else {
            c = new JSONObject();
            put(c, "id", uid());
        }
        put(c, "nama", nama);
        put(c, "no", no);
        put(c, "ly", ly);
        target.put(c);
        save();
        Pc p = new Pc();
        p.id = str(c, "id");
        p.nama = nama;
        p.no = no;
        p.ly = ly;
        p.j = j;
        return p;
    }

    public boolean pcDuplicate(String j, String no) {
        JSONArray a = ppobDb().optJSONArray(j);
        if (a == null) return false;
        for (JSONObject o : list(a)) if (str(o, "no").equals(no)) return true;
        return false;
    }

    // ---------------------------------------------------------------- riwayat struk
    public void logStruk(String mode, String ket, double total, String fn, JSONArray M) {
        JSONObject o = new JSONObject();
        put(o, "id", uid());
        put(o, "w", System.currentTimeMillis());
        put(o, "mode", mode);
        put(o, "ket", ket);
        put(o, "total", total);
        put(o, "fn", fn);
        put(o, "M", M);
        JSONArray a = struk();
        a.put(o);
        while (a.length() > 300) a.remove(0);
        save();
    }
}
