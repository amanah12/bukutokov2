package com.bukutoko.app;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;

import org.json.JSONObject;

/** Definisi tema (11 tema, setara versi web terbaru). Satuan ukuran: dp. */
public class Th {
    public static final String[][] LIST = {
            {"classic", "Classic", "#ffd966", "#a8d5ba"},
            {"soft", "Lembut", "#d4f26d", "#ffc9b3"},
            {"sage", "Sage", "#b5cfa8", "#f2a06b"},
            {"ceria", "Ceria", "#ffa94d", "#d6c7ff"},
            {"imut", "Imut", "#ffadc9", "#d0bdfa"},
            {"studio", "Studio", "#ff5a1f", "#d6d6d1"},
            {"lab", "Lab", "#c9d3ff", "#ffc4dc"},
            {"pixel", "Pixel", "#8be28b", "#f3b6ee"},
            {"komik", "Komik", "#c8f31d", "#b79cff"},
            {"tinta", "Tinta", "#ffb98a", "#ffffff"},
            {"diy", "DIY", "#c8f31d", "#ff9fbb"}};

    /** Bentuk kartu tema DIY: {kode, nama}. */
    public static final String[][] DIY_SHAPES = {
            {"bulat", "Bulat"}, {"kotak", "Kotak tipis"}, {"solatip", "Kertas + solatip"},
            {"daun", "Daun"}, {"sobek", "Kertas sobek (3D)"}, {"lipat", "Kertas pojok terlipat"},
            {"kertas", "Buku bergaris"}, {"memo", "Memo tempel"}, {"jahit", "Kain dijahit"},
            {"tebal", "Tebal (garis tebal)"}, {"putus", "Garis putus-putus"}, {"kaca", "Kaca"},
            {"hexagon", "Hexagon"}, {"lancip", "Lancip (panah)"}, {"miring", "Miring"},
            {"acak", "Tak beraturan"}, {"lendir", "Lendir / tetesan"}, {"cat", "Cat / noda"}};
    public static final String[][] DIY_FONTS = {
            {"sans", "Sans (bawaan)"}, {"serif", "Serif"}, {"mono", "Mesin ketik"},
            {"casual", "Tulisan tangan"}, {"black", "Judul sangat tebal"}, {"condensed", "Sempit"}};
    public static final String[][] DIY_NAV = {
            {"datar", "Datar"}, {"garis", "Garis tebal"}, {"melayang", "Melayang terang"}, {"gelap", "Melayang gelap"}};

    public static boolean valid(String id) {
        for (String[] t : LIST) if (t[0].equals(id)) return true;
        return false;
    }

    public static String[] def(String id) {
        for (String[] t : LIST) if (t[0].equals(id)) return t;
        return LIST[0];
    }

    public String id = "classic";
    public boolean dark;
    public int bg, card, ink, mu, bad, inClr, inputBg, wBg, wBorder, sep, accent, c1, c2;
    public float wBw = 3;

    public float cardR = 0, cardBw = 3, cardSx = 4, cardSy = 4;
    public int cardBorder, cardSdClr;
    public boolean cardDash;
    public float[] cardCr; // sudut berbeda: tl,tr,br,bl

    public float btnR = 0, btnBw = 3, btnSx = 3, btnSy = 3;
    public int btnBorder, btnSdClr;
    public float[] btnCr;
    public boolean caps = false;
    public boolean btnGloss;

    public float inR = 0, inBw = 3;
    public int inBorder;
    public int inputInk; // warna teks isian

    public Typeface font = Typeface.DEFAULT_BOLD;

    public int grid;
    public boolean lined; // kertas buku tulis
    public Bitmap bgBmp;
    public int[] glowClr = new int[0];
    public float[] glowX = new float[0], glowY = new float[0], glowR = new float[0];

    public int cardInk;
    public int dlgInk;
    public boolean cardGrad;
    public int cardG1, cardG2;

    public int primBg = 0, primInk = 0; // 0 = pakai warna utama
    public boolean primGrad;

    public int tabBg, tabInk, tabOnBg, tabOnInk, tabBorder, tabG1, tabG2;
    public float tabR, tabBw, tabBtnR;
    public boolean tabFloat, tabDiv, tabGrad, tabDash;

    // bentuk dekoratif (DIY)
    public String shape = "", btnShape = "";
    public int tint; // warna solatip / urat daun

    /** gaya persegi tebal (hanya dipakai bila tema lama kotak-kotak). */
    public boolean sq = false;

    public static int css(String s) {
        s = s.replace("#", "");
        if (s.length() == 3) {
            s = "" + s.charAt(0) + s.charAt(0) + s.charAt(1) + s.charAt(1) + s.charAt(2) + s.charAt(2);
        } else if (s.length() == 4) {
            s = "" + s.charAt(0) + s.charAt(0) + s.charAt(1) + s.charAt(1) + s.charAt(2) + s.charAt(2) + s.charAt(3) + s.charAt(3);
        }
        int r = Integer.parseInt(s.substring(0, 2), 16);
        int g = Integer.parseInt(s.substring(2, 4), 16);
        int b = Integer.parseInt(s.substring(4, 6), 16);
        int a = s.length() >= 8 ? Integer.parseInt(s.substring(6, 8), 16) : 255;
        return Color.argb(a, r, g, b);
    }

    public static int safe(String s, String def) {
        try {
            return css(s);
        } catch (Exception e) {
            return css(def);
        }
    }

    public static String hex(int c) {
        return String.format("#%02x%02x%02x", Color.red(c), Color.green(c), Color.blue(c));
    }

    public static int alpha(int c, int a) {
        return (c & 0x00FFFFFF) | (a << 24);
    }

    public static int mix(int a, int b, float t) {
        int r = Math.round(Color.red(a) * (1 - t) + Color.red(b) * t);
        int g = Math.round(Color.green(a) * (1 - t) + Color.green(b) * t);
        int bl = Math.round(Color.blue(a) * (1 - t) + Color.blue(b) * t);
        return Color.argb(Color.alpha(a), r, g, bl);
    }

    public static Typeface face(String f) {
        switch (f == null ? "" : f) {
            case "serif":
                return Typeface.create(Typeface.SERIF, Typeface.BOLD);
            case "mono":
                return Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);
            case "casual":
                return Typeface.create("casual", Typeface.BOLD);
            case "black":
                return Typeface.create("sans-serif-black", Typeface.NORMAL);
            case "condensed":
                return Typeface.create("sans-serif-condensed", Typeface.BOLD);
            default:
                return Typeface.DEFAULT_BOLD;
        }
    }

    public static Th make(String id, boolean sysDark, String c1s, String c2s, JSONObject diy) {
        if (!valid(id)) id = "classic";
        String[] df = def(id);
        Th t = new Th();
        t.id = id;
        t.c1 = safe(c1s == null || c1s.isEmpty() ? df[2] : c1s, df[2]);
        t.c2 = safe(c2s == null || c2s.isEmpty() ? df[3] : c2s, df[3]);
        t.inputInk = 0;
        switch (id) {
            case "soft":
                t.bg = css("e8e2fa"); t.card = css("fff"); t.ink = css("1f2233"); t.mu = css("6b6f85"); t.bad = css("d9534f");
                t.inClr = css("1a8a4a"); t.inputBg = css("f6f4fd"); t.wBg = css("f1eefc"); t.wBw = 0; t.wBorder = 0;
                t.sep = css("1f22331a"); t.accent = css("7b5cf0");
                soft(t, 22, css("1f223314"), 0, 4, css("3a2f8a1c"));
                t.glowClr = new int[]{css("ffffffaa")}; t.glowX = new float[]{1}; t.glowY = new float[]{0}; t.glowR = new float[]{0.55f};
                roundTabs(t, false);
                break;
            case "sage":
                t.bg = css("c9d6bd"); t.card = css("fbf5e9"); t.ink = css("27342b"); t.mu = css("5d6b60"); t.bad = css("c4452f");
                t.inClr = css("1a8a4a"); t.inputBg = css("f4eede"); t.wBg = css("ecebdc"); t.wBw = 0; t.wBorder = 0;
                t.sep = css("27342b22"); t.accent = css("e07b45");
                soft(t, 36, 0, 0, 4, css("3b5a4126"));
                t.cardCr = new float[]{36, 10, 36, 10};
                t.btnR = 0; t.btnCr = new float[]{16, 4, 16, 4};
                t.cardBw = 0; t.font = Typeface.create(Typeface.SERIF, Typeface.BOLD);
                t.glowClr = new int[]{css("fbf5e980"), css("a9bd9a66")};
                t.glowX = new float[]{1, 0}; t.glowY = new float[]{0, 1}; t.glowR = new float[]{0.5f, 0.5f};
                roundTabs(t, true);
                break;
            case "ceria":
                t.bg = css("f1f2f7"); t.card = css("fff"); t.ink = css("16161d"); t.mu = css("6b6f80"); t.bad = css("e5484d");
                t.inClr = css("1a8a4a"); t.inputBg = css("f6f7fb"); t.wBg = css("eef0f6"); t.wBw = 0; t.wBorder = 0;
                t.sep = css("16161d14"); t.accent = css("ff7a1a");
                soft(t, 32, 0, 0, 4, css("16161d1a"));
                t.cardBw = 0; t.primBg = css("16161d"); t.primInk = css("ffffff");
                roundTabs(t, true);
                t.tabBg = css("16161d"); t.tabInk = css("ffffff"); t.tabOnBg = t.c1; t.tabOnInk = css("111111");
                break;
            case "imut":
                t.bg = css("fdeef3"); t.card = css("fff"); t.ink = css("2d2150"); t.mu = css("7a6a9a"); t.bad = css("e0457b");
                t.inClr = css("1a8a4a"); t.inputBg = css("fff7fa"); t.wBg = css("f5eefc"); t.wBw = 0; t.wBorder = 0;
                t.sep = css("2d215018"); t.accent = css("a58be8");
                soft(t, 26, css("f0a6c4"), 5, 5, css("f7d3e2"));
                t.cardBw = 2; t.cardDash = true; t.primGrad = true;
                t.glowClr = new int[]{css("e9ddff"), css("ffdbe8")};
                t.glowX = new float[]{1, 0}; t.glowY = new float[]{0, 1}; t.glowR = new float[]{0.45f, 0.45f};
                roundTabs(t, true);
                t.tabBw = 2; t.tabBorder = css("f0a6c4"); t.tabDash = true;
                break;
            case "studio":
                t.dark = true;
                t.bg = css("3a3b38"); t.card = css("464743"); t.ink = css("ecece6"); t.mu = css("bdbdb6"); t.bad = css("ff8a73");
                t.inClr = css("8dffb0"); t.inputBg = css("2e2f2c"); t.wBg = 0; t.wBw = 1; t.wBorder = css("ecece6aa");
                t.sep = css("ecece622"); t.accent = css("ff5a1f");
                soft(t, 22, css("ecece655"), 0, 0, 0);
                t.inBorder = css("ecece633");
                t.font = Typeface.create("sans-serif-black", Typeface.NORMAL);
                t.tabBg = t.card; t.tabInk = t.ink; t.tabOnBg = t.c1; t.tabOnInk = css("111111");
                t.tabR = 0; t.tabBw = 1; t.tabBorder = css("ecece655"); t.tabBtnR = 999;
                break;
            case "lab":
                t.bg = css("ffffff"); t.card = css("eef0f5"); t.ink = css("0b0b0f"); t.mu = css("6a6f7d"); t.bad = css("d6335f");
                t.inClr = css("1a8a4a"); t.inputBg = css("ffffff"); t.wBg = css("e3e6ee"); t.wBw = 0; t.wBorder = 0;
                t.sep = css("0b0b0f14"); t.accent = css("0b0b0f");
                soft(t, 24, 0, 0, 0, 0);
                t.cardBw = 0; t.inR = 12; t.primBg = css("0b0b0f"); t.primInk = css("ffffff");
                t.font = Typeface.create("sans-serif-black", Typeface.NORMAL);
                t.tabBg = css("ffffff"); t.tabInk = t.ink; t.tabOnBg = css("0b0b0f"); t.tabOnInk = css("ffffff");
                t.tabR = 0; t.tabBw = 1; t.tabBorder = css("0b0b0f1a"); t.tabBtnR = 14;
                break;
            case "pixel":
                t.bg = css("cfe3fb"); t.card = css("fbe6fb"); t.ink = css("1c2a8f"); t.mu = css("4650b0"); t.bad = css("d6335f");
                t.inClr = css("1a8a4a"); t.inputBg = css("ffffff"); t.wBg = css("ffffff"); t.wBw = 3; t.wBorder = t.ink;
                t.sep = css("1c2a8f33"); t.accent = css("9b86ec");
                t.cardR = 0; t.cardBw = 3; t.cardBorder = t.ink; t.cardSx = 7; t.cardSy = 7; t.cardSdClr = css("a9b9f0");
                t.btnR = 0; t.btnBw = 3; t.btnBorder = t.ink; t.btnSx = 0; t.btnSy = 0; t.btnSdClr = 0;
                t.inR = 0; t.inBw = 3; t.inBorder = t.ink;
                t.grid = css("ffffff66"); t.caps = true;
                t.font = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);
                t.tabBg = t.card; t.tabInk = t.ink; t.tabOnBg = t.c1; t.tabOnInk = css("111111");
                t.tabR = 0; t.tabBw = 3; t.tabBorder = t.ink; t.tabBtnR = 0;
                break;
            case "komik":
                t.dark = true;
                t.bg = css("0b0b12"); t.card = css("14141d"); t.ink = css("f4f4f8"); t.mu = css("a9a9c0"); t.bad = css("ff7b6b");
                t.inClr = css("c8f31d"); t.inputBg = css("0e0e16"); t.wBg = 0; t.wBw = 1; t.wBorder = css("7a45f0");
                t.sep = css("ffffff1a"); t.accent = css("7b3cff");
                soft(t, 22, css("ffffff17"), 0, 0, 0);
                t.inBorder = css("ffffff22");
                t.font = Typeface.create("casual", Typeface.BOLD);
                t.glowClr = new int[]{css("6d3df066"), css("ff6a3d33"), css("2a8cff33")};
                t.glowX = new float[]{0, 1, 0}; t.glowY = new float[]{0, 0.4f, 1}; t.glowR = new float[]{0.45f, 0.4f, 0.4f};
                roundTabs(t, true);
                t.tabBg = css("14141d"); t.tabBw = 1; t.tabBorder = css("ffffff22");
                break;
            case "tinta":
                t.dark = true;
                t.bg = css("1b1735"); t.card = css("ffffff"); t.ink = css("ffffff"); t.mu = css("cfc9ee"); t.bad = css("ff6f93");
                t.inClr = css("1a8a4a"); t.inputBg = css("f6f4fb"); t.wBg = css("ffffff"); t.wBw = 2; t.wBorder = css("111111");
                t.sep = css("1b173522"); t.accent = css("ffb98a");
                t.cardR = 28; t.cardBw = 3; t.cardBorder = css("111111"); t.cardSx = 6; t.cardSy = 6; t.cardSdClr = css("ffb98a");
                t.btnR = 999; t.btnBw = 2; t.btnBorder = css("111111"); t.btnSx = 0; t.btnSy = 0;
                t.inR = 16; t.inBw = 2; t.inBorder = css("111111");
                t.cardInk = css("1b1735"); t.dlgInk = css("1b1735"); t.inputInk = css("1b1735");
                roundTabs(t, true);
                t.tabBg = css("ffffff"); t.tabInk = css("1b1735"); t.tabOnBg = css("111111"); t.tabOnInk = css("ffffff");
                t.tabBw = 3; t.tabBorder = css("111111");
                break;
            case "diy":
                diy(t, diy);
                break;
            default: // classic
                t.id = "classic";
                t.bg = css("fbf6e9"); t.card = css("fffefa"); t.ink = css("1f2937"); t.mu = css("5b6472"); t.bad = css("b3261e");
                t.inClr = css("1a8a4a"); t.inputBg = css("fffefa"); t.wBg = css("f3ecd8"); t.wBw = 1.5f; t.wBorder = t.ink;
                t.sep = css("1f293722"); t.accent = css("c0392b");
                t.cardR = 6; t.cardBw = 1.5f; t.cardBorder = t.ink; t.cardSx = 2; t.cardSy = 2; t.cardSdClr = css("1f293726");
                t.btnR = 6; t.btnBw = 1.5f; t.btnBorder = t.ink; t.btnSx = 0; t.btnSy = 0;
                t.inR = 6; t.inBw = 1.5f; t.inBorder = css("1f2937aa");
                t.font = Typeface.create(Typeface.SERIF, Typeface.BOLD);
                t.lined = true;
                t.tabBg = t.card; t.tabInk = t.ink; t.tabOnBg = t.c1; t.tabOnInk = css("111111");
                t.tabR = 0; t.tabBw = 2; t.tabBorder = t.ink; t.tabBtnR = 0;
        }
        if (t.cardInk == 0) t.cardInk = t.ink;
        if (t.dlgInk == 0) t.dlgInk = t.ink;
        if (t.inputInk == 0) t.inputInk = t.ink;
        return t;
    }

    // ------------------------------------------------------------ tema DIY
    private static String opt(JSONObject d, String k, String def) {
        if (d == null) return def;
        String v = d.optString(k, "");
        return v.isEmpty() ? def : v;
    }

    private static void diy(Th t, JSONObject D) {
        String dc = opt(D, "c", "bulat"), dn = opt(D, "n", "melayang");
        t.font = face(opt(D, "f", "sans"));
        boolean glass = dc.equals("kaca");
        t.bg = safe(opt(D, "bg", "#f4f1ea"), "#f4f1ea");
        int cd = safe(opt(D, "cd", "#ffffff"), "#ffffff");
        t.card = glass ? alpha(cd, 0xB3) : cd;
        t.ink = safe(opt(D, "ik", "#1f2937"), "#1f2937");
        t.mu = alpha(t.ink, 0x99);
        t.bad = css("d6335f");
        t.dark = Color.luminance(t.bg) < 0.4f;
        t.inClr = Color.luminance(cd) < 0.4f ? css("5dff9a") : css("1a8a4a");
        t.inputBg = glass ? alpha(cd, 0x99) : cd;
        t.wBg = mix(cd, t.ink | 0xFF000000, 0.09f) | 0xFF000000; t.wBw = 1; t.wBorder = alpha(t.ink, 0x33);
        t.sep = alpha(t.ink, 0x22);
        t.accent = t.c1;
        t.cardInk = t.ink;
        t.inputInk = t.ink;
        t.dlgInk = t.ink;
        t.tint = t.c1;
        t.inR = 14; t.inBw = 1.5f; t.inBorder = alpha(t.ink, 0x33);
        t.bgBmp = Ui.decode(opt(D, "img", ""));
        switch (dc) {
            case "kotak":
                t.cardR = 4; t.cardBw = 2; t.cardBorder = t.ink; t.cardSx = 0; t.cardSy = 0;
                t.btnR = 4; t.btnBw = 2; t.btnBorder = t.ink; t.btnSx = 0; t.btnSy = 0;
                t.wBw = 2; t.wBorder = t.ink; t.inR = 4; t.inBorder = t.ink;
                break;
            case "tebal":
                t.cardR = 0; t.cardBw = 3; t.cardBorder = t.ink; t.cardSx = 5; t.cardSy = 5; t.cardSdClr = t.ink;
                t.btnR = 0; t.btnBw = 3; t.btnBorder = t.ink; t.btnSx = 3; t.btnSy = 3; t.btnSdClr = t.ink;
                t.wBw = 3; t.wBorder = t.ink; t.inR = 0; t.inBw = 3; t.inBorder = t.ink; t.caps = true;
                break;
            case "putus":
                t.cardR = 26; t.cardBw = 2; t.cardDash = true; t.cardBorder = t.c1; t.cardSx = 5; t.cardSy = 5; t.cardSdClr = t.sep;
                t.btnR = 999; t.btnBw = 0; t.btnSx = 0; t.btnSy = 0;
                break;
            case "kaca":
                t.shape = "kaca"; t.cardR = 24; t.cardBw = 1; t.cardBorder = css("ffffffcc");
                t.btnR = 999; t.btnBw = 0; t.btnSx = 0; t.btnSy = 0; t.btnGloss = true;
                break;
            case "solatip":
            case "daun":
            case "sobek":
            case "lipat":
            case "kertas":
            case "memo":
            case "jahit":
            case "hexagon":
            case "lancip":
            case "miring":
            case "acak":
            case "lendir":
            case "cat":
                t.shape = dc; t.cardR = 0; t.cardBw = 0; t.cardSx = 0; t.cardSy = 0;
                t.btnBw = 0; t.btnSx = 0; t.btnSy = 0; t.btnGloss = true;
                switch (dc) {
                    case "solatip":
                        t.btnShape = "tape";
                        break;
                    case "daun":
                        t.btnR = 0; t.btnCr = new float[]{16, 3, 16, 3};
                        t.tint = css("5aa24a");
                        break;
                    case "sobek":
                    case "lipat":
                    case "kertas":
                    case "memo":
                        t.btnR = 5;
                        break;
                    case "jahit":
                        t.btnR = 12; t.btnShape = "jahit";
                        break;
                    case "lendir":
                    case "cat":
                        t.btnR = 999;
                        break;
                    default:
                        t.btnR = 8;
                }
                break;
            default: // bulat
                soft(t, 22, alpha(t.ink, 0x22), 0, 4, css("00000022"));
                t.inBorder = alpha(t.ink, 0x33);
                t.inR = 14;
                t.btnGloss = true;
        }
        t.tabBg = t.card; t.tabInk = t.ink; t.tabOnBg = t.c1; t.tabOnInk = css("111111");
        switch (dn) {
            case "garis":
                t.tabR = 0; t.tabBw = 3; t.tabBorder = t.ink; t.tabBtnR = 0;
                t.tabBg = glass ? alpha(cd, 0xE0) : cd;
                break;
            case "gelap":
                roundTabs(t, true);
                t.tabBg = css("16161d"); t.tabInk = css("ffffff"); t.tabOnBg = t.c1; t.tabOnInk = css("111111");
                break;
            case "melayang":
                roundTabs(t, true);
                t.tabBg = glass ? alpha(cd, 0xE0) : cd; t.tabInk = t.ink;
                break;
            default: // datar
                t.tabR = 0; t.tabBw = 1; t.tabBorder = t.sep; t.tabBtnR = 12;
                t.tabBg = glass ? alpha(cd, 0xE0) : cd;
        }
    }

    /** gaya "lembut": kartu membulat, tombol pil, tanpa bayangan keras. */
    private static void soft(Th t, float r, int border, float sx, float sy, int sd) {
        t.cardR = r; t.cardBw = border == 0 ? 0 : 1; t.cardBorder = border; t.cardSx = sx; t.cardSy = sy; t.cardSdClr = sd;
        t.btnR = 999; t.btnBw = 0; t.btnSx = 0; t.btnSy = 0;
        t.inR = 14; t.inBw = 1.5f; t.inBorder = css("1f223326");
        if (t.dark) t.inBorder = css("2a2a3a");
    }

    private static void roundTabs(Th t, boolean floating) {
        t.tabBg = t.card; t.tabInk = t.ink; t.tabOnBg = t.c1; t.tabOnInk = css("111111");
        t.tabFloat = floating; t.tabR = floating ? 30 : 22; t.tabBtnR = 14; t.tabBw = 0;
    }

    // ------------------------------------------------------------ warna menurut konteks (jenis kartu)
    public int inkOf(int kind) {
        if (kind == Ui.C1 || kind == Ui.C2) return 0xFF111111;
        if (kind == Ui.NORMAL) return cardInk;
        if (kind == Ui.DIALOG) return dlgInk;
        return ink;
    }

    public int mutedOf(int kind) {
        if (kind == Ui.PLAIN || (kind == Ui.DIALOG && dlgInk == ink)) return mu;
        int b = inkOf(kind);
        return (b & 0x00FFFFFF) | 0xCC000000;
    }

    public int inOf(int kind) {
        if (kind == Ui.C1 || kind == Ui.C2) return css("0f6b36");
        if (id.equals("studio") && kind == Ui.NORMAL) return css("8dffb0");
        return inClr;
    }

    public int outOf(int kind) {
        if (kind == Ui.C1 || kind == Ui.C2) return css("b3261e");
        return bad;
    }

    /** {bg, ink, border} untuk tombol. style: 0 utama, 1 putih, 2 merah. */
    public int[] btnColors(int kind, int style) {
        if (style == 2) return new int[]{0, outOf(kind), 0};
        if (style == 1) {
            int ik = (kind == Ui.C1 || kind == Ui.C2) ? css("111111") : inkOf(kind);
            if (id.equals("tinta")) ik = css("1b1735");
            return new int[]{wBg, ik, wBorder};
        }
        if (primBg != 0) return new int[]{primBg, primInk, btnBorder};
        return new int[]{c1, css("111111"), btnBorder};
    }
}
