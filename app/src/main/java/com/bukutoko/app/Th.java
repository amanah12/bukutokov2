package com.bukutoko.app;

import android.graphics.Color;
import android.graphics.Typeface;

/** Definisi tema (9 tema, setara dengan versi web). Satuan ukuran: dp. */
public class Th {
    public static final String[][] LIST = {
            {"buku", "Buku", "#c4f04d", "#ff7f9f"},
            {"soft", "Lembut", "#d4f26d", "#ffc9b3"},
            {"neon", "Neon", "#c8f31d", "#ff8a4c"},
            {"sage", "Sage", "#b5cfa8", "#f2a06b"},
            {"persona", "Persona 5", "#ff2d3d", "#ffffff"},
            {"ceria", "Ceria", "#ffa94d", "#d6c7ff"},
            {"biru", "Biru", "#6aa5ff", "#d3f26a"},
            {"piksel", "Piksel", "#ff9fbb", "#c3b3f7"},
            {"imut", "Imut", "#ffadc9", "#d0bdfa"}};

    public String id = "buku";
    public boolean dark;
    public int bg, card, ink, mu, bad, inClr, inputBg, wBg, wBorder, sep, accent, c1, c2;
    public float wBw = 3;

    public float cardR = 0, cardBw = 3, cardSx = 4, cardSy = 4;
    public int cardBorder, cardSdClr;
    public boolean cardDash;

    public float btnR = 0, btnBw = 3, btnSx = 3, btnSy = 3;
    public int btnBorder, btnSdClr;
    public boolean caps = true;

    public float inR = 0, inBw = 3;
    public int inBorder;

    public Typeface font = Typeface.DEFAULT_BOLD;

    public int grid;
    public int[] glowClr = new int[0];
    public float[] glowX = new float[0], glowY = new float[0], glowR = new float[0];

    public int cardInk;
    public boolean cardGrad;
    public int cardG1, cardG2;

    public int primBg = 0, primInk = 0; // 0 = pakai warna utama
    public boolean primGrad;

    public int tabBg, tabInk, tabOnBg, tabOnInk, tabBorder, tabG1, tabG2;
    public float tabR, tabBw, tabBtnR;
    public boolean tabFloat, tabDiv, tabGrad, tabDash;

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

    public static Th make(String id, boolean sysDark, String c1s, String c2s) {
        Th t = new Th();
        t.id = id;
        t.c1 = safe(c1s, "#c4f04d");
        t.c2 = safe(c2s, "#ff7f9f");
        switch (id) {
            case "soft":
                t.bg = css("e8e2fa"); t.card = css("fff"); t.ink = css("1f2233"); t.mu = css("6b6f85"); t.bad = css("d9534f");
                t.inClr = css("1a8a4a"); t.inputBg = css("f6f4fd"); t.wBg = css("f1eefc"); t.wBw = 0; t.wBorder = 0;
                t.sep = css("1f22331a"); t.accent = css("7b5cf0");
                soft(t, 20, css("1f223314"), 0, 4, css("3a2f8a1c"));
                t.glowClr = new int[]{css("ffffffaa")}; t.glowX = new float[]{1}; t.glowY = new float[]{0}; t.glowR = new float[]{0.55f};
                roundTabs(t, false);
                break;
            case "neon":
                t.dark = true;
                t.bg = css("0d0d12"); t.card = css("17171f"); t.ink = css("f3f3f7"); t.mu = css("a2a2b5"); t.bad = css("ff7b6b");
                t.inClr = css("c8f31d"); t.inputBg = css("101016"); t.wBg = 0; t.wBw = 1.5f; t.wBorder = css("6d3df0");
                t.sep = css("ffffff1a"); t.accent = css("7b3cff");
                soft(t, 20, css("2a2a3a"), 0, 0, 0);
                t.glowClr = new int[]{css("6d3df055"), css("ff8a4c33")};
                t.glowX = new float[]{0, 1}; t.glowY = new float[]{0, 1}; t.glowR = new float[]{0.45f, 0.4f};
                roundTabs(t, false);
                break;
            case "sage":
                t.bg = css("c9d6bd"); t.card = css("fbf5e9"); t.ink = css("27342b"); t.mu = css("5d6b60"); t.bad = css("c4452f");
                t.inClr = css("1a8a4a"); t.inputBg = css("f4eede"); t.wBg = css("ecebdc"); t.wBw = 0; t.wBorder = 0;
                t.sep = css("27342b22"); t.accent = css("e07b45");
                soft(t, 26, 0, 0, 4, css("3b5a4126"));
                t.cardBw = 0; t.btnR = 16; t.font = Typeface.create(Typeface.SERIF, Typeface.BOLD);
                t.glowClr = new int[]{css("fbf5e980"), css("a9bd9a66")};
                t.glowX = new float[]{1, 0}; t.glowY = new float[]{0, 1}; t.glowR = new float[]{0.5f, 0.5f};
                roundTabs(t, true);
                break;
            case "persona":
                t.dark = true;
                t.bg = css("0a0a0a"); t.card = css("141414"); t.ink = css("ffffff"); t.mu = css("cfcfcf"); t.bad = css("ff6b76");
                t.inClr = css("5dff9a"); t.inputBg = css("000000"); t.wBg = css("ffffff"); t.wBw = 0; t.wBorder = 0;
                t.sep = css("ffffff33"); t.accent = css("ff2d3d");
                t.cardR = 0; t.cardBw = 0; t.cardBorder = 0; t.cardSx = 6; t.cardSy = 6; t.cardSdClr = css("ffffff30");
                t.btnR = 0; t.btnBw = 0; t.btnSx = 3; t.btnSy = 3; t.btnSdClr = css("ffffff");
                t.inR = 0; t.inBw = 2; t.inBorder = css("ffffff");
                t.glowClr = new int[]{css("e6001240")}; t.glowX = new float[]{0}; t.glowY = new float[]{0}; t.glowR = new float[]{0.8f};
                t.tabBg = css("000000"); t.tabInk = css("ffffff"); t.tabOnBg = t.c1; t.tabOnInk = css("111111");
                t.tabBw = 4; t.tabBorder = t.c1; t.tabR = 0; t.tabBtnR = 0;
                t.cardInk = t.ink;
                return fin(t);
            case "ceria":
                t.bg = css("f1f2f7"); t.card = css("fff"); t.ink = css("16161d"); t.mu = css("6b6f80"); t.bad = css("e5484d");
                t.inClr = css("1a8a4a"); t.inputBg = css("f6f7fb"); t.wBg = css("eef0f6"); t.wBw = 0; t.wBorder = 0;
                t.sep = css("16161d14"); t.accent = css("ff7a1a");
                soft(t, 32, 0, 0, 4, css("16161d1a"));
                t.cardBw = 0; t.primBg = css("16161d"); t.primInk = css("ffffff");
                roundTabs(t, true);
                t.tabBg = css("16161d"); t.tabInk = css("ffffff"); t.tabOnBg = t.c1; t.tabOnInk = css("111111");
                break;
            case "biru":
                t.bg = css("eaf1fb"); t.card = css("fff"); t.ink = css("12306b"); t.mu = css("5a75a8"); t.bad = css("d9455f");
                t.inClr = css("1a8a4a"); t.inputBg = css("f3f7ff"); t.wBg = css("e4eeff"); t.wBw = 1; t.wBorder = css("2f6fe433");
                t.sep = css("2f6fe422"); t.accent = css("2f6fe4");
                soft(t, 26, 0, 0, 6, css("2f6fe440"));
                t.cardBw = 0; t.cardGrad = true; t.cardG1 = css("4f8ef7"); t.cardG2 = css("2457d6");
                t.glowClr = new int[]{css("cfe0ff")}; t.glowX = new float[]{1}; t.glowY = new float[]{0}; t.glowR = new float[]{0.45f};
                roundTabs(t, true);
                t.tabGrad = true; t.tabG1 = css("4f8ef7"); t.tabG2 = css("2457d6"); t.tabInk = css("ffffff");
                t.tabOnBg = css("ffffff"); t.tabOnInk = css("2457d6");
                t.cardInk = css("ffffff");
                return fin(t);
            case "piksel":
                t.bg = css("fbf3e4"); t.card = css("fffdf7"); t.ink = css("2a2340"); t.mu = css("6a6288"); t.bad = css("d6335f");
                t.inClr = css("1a8a4a"); t.inputBg = css("fffdf7"); t.wBg = css("fffdf7"); t.wBw = 3; t.wBorder = t.ink;
                t.sep = t.ink; t.accent = css("9b86ec");
                t.cardR = 0; t.cardBw = 3; t.cardBorder = t.ink; t.cardSx = 8; t.cardSy = 8; t.cardSdClr = css("9b86ec");
                t.btnR = 0; t.btnBw = 3; t.btnBorder = t.ink; t.btnSx = 5; t.btnSy = 5; t.btnSdClr = t.ink;
                t.inR = 0; t.inBw = 3; t.inBorder = t.ink;
                t.grid = css("b9a6f040");
                t.font = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);
                t.tabBg = t.card; t.tabInk = t.ink; t.tabOnBg = t.c1; t.tabOnInk = css("111111");
                t.tabBw = 3; t.tabBorder = t.ink; t.tabDiv = true;
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
            default: // buku
                t.id = "buku";
                t.dark = sysDark;
                if (sysDark) {
                    t.bg = css("17171a"); t.card = css("232328"); t.ink = css("f2f2f2"); t.mu = css("aaaaaa"); t.bad = css("ff7b6b");
                    t.grid = css("ffffff22"); t.inClr = css("4cd98a");
                } else {
                    t.bg = css("f4f1ea"); t.card = css("fff"); t.ink = css("111111"); t.mu = css("555555"); t.bad = css("c0392b");
                    t.grid = css("1111111a"); t.inClr = css("1a8a4a");
                }
                t.inputBg = t.card; t.wBg = t.card; t.wBw = 3; t.wBorder = t.ink; t.sep = t.ink; t.accent = t.c2;
                t.cardR = 0; t.cardBw = 3; t.cardBorder = t.ink; t.cardSx = 4; t.cardSy = 4; t.cardSdClr = t.ink;
                t.btnR = 0; t.btnBw = 3; t.btnBorder = t.ink; t.btnSx = 3; t.btnSy = 3; t.btnSdClr = t.ink;
                t.inR = 0; t.inBw = 3; t.inBorder = t.ink;
                t.tabBg = t.card; t.tabInk = t.ink; t.tabOnBg = t.c1; t.tabOnInk = css("111111");
                t.tabBw = 3; t.tabBorder = t.ink; t.tabDiv = true;
        }
        t.cardInk = t.ink;
        return fin(t);
    }

    private static Th fin(Th t) {
        if (t.cardInk == 0) t.cardInk = t.ink;
        return t;
    }

    /** gaya "lembut": kartu membulat, tombol pil, tanpa bayangan keras. */
    private static void soft(Th t, float r, int border, float sx, float sy, int sd) {
        t.cardR = r; t.cardBw = border == 0 ? 0 : 1; t.cardBorder = border; t.cardSx = sx; t.cardSy = sy; t.cardSdClr = sd;
        t.btnR = 999; t.btnBw = 0; t.btnSx = 0; t.btnSy = 0; t.caps = false;
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
        return ink;
    }

    public int mutedOf(int kind) {
        if (kind == Ui.PLAIN || kind == Ui.DIALOG) return mu;
        int b = inkOf(kind);
        return (b & 0x00FFFFFF) | 0xCC000000;
    }

    public int inOf(int kind) {
        if (kind == Ui.C1 || kind == Ui.C2) return css("0f6b36");
        if (id.equals("biru") && kind == Ui.NORMAL) return css("d3f26a");
        return inClr;
    }

    public int outOf(int kind) {
        if (kind == Ui.C1 || kind == Ui.C2) return css("b3261e");
        if (id.equals("biru") && kind == Ui.NORMAL) return css("ffc2cf");
        return bad;
    }

    /** {bg, ink, border} untuk tombol. style: 0 utama, 1 putih, 2 merah. */
    public int[] btnColors(int kind, int style) {
        boolean biru = id.equals("biru");
        if (style == 2) return new int[]{0, outOf(kind), 0};
        if (style == 1) {
            if (biru) {
                if (kind == Ui.NORMAL) return new int[]{css("ffffff26"), css("ffffff"), css("ffffff55")};
                if (kind == Ui.C1 || kind == Ui.C2) return new int[]{css("ffffffb3"), css("111111"), css("00000022")};
            }
            int ik = id.equals("persona") ? css("111111") : ink;
            return new int[]{wBg, ik, wBorder};
        }
        if (biru) {
            if (kind == Ui.NORMAL) return new int[]{css("ffffff"), css("2457d6"), 0};
            if (kind == Ui.C1 || kind == Ui.C2) return new int[]{css("2457d6"), css("ffffff"), 0};
        }
        if (primBg != 0) return new int[]{primBg, primInk, btnBorder};
        return new int[]{c1, css("111111"), btnBorder};
    }
}
