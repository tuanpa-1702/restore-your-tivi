package com.tivibox.home;

import java.text.Normalizer;

/**
 * Puts a Vietnamese tone mark on the right vowel of a word ("Tho" + hỏi = "Thỏ", "Viêt" + nặng = "Việt").
 * Pressing the same tone again removes it; a different tone replaces it. Pure Java, tested in test/VietToneTest.java.
 */
public final class VietTone {
    public static final char SAC = '́', HUYEN = '̀', HOI = '̉', NGA = '̃', NANG = '̣';
    private static final String TONES = "̣́̀̉̃";
    private static final String VOWELS = "aăâeêioôơuưy";

    private VietTone() {}

    public static String apply(String word, char tone) {
        String nfd = Normalizer.normalize(word, Normalizer.Form.NFD);
        boolean had = nfd.indexOf(tone) >= 0;
        StringBuilder bare = new StringBuilder();
        for (int i = 0; i < nfd.length(); i++) {
            if (TONES.indexOf(nfd.charAt(i)) < 0) bare.append(nfd.charAt(i));
        }
        String base = Normalizer.normalize(bare, Normalizer.Form.NFC);
        if (had) return base;
        int at = target(base);
        if (at < 0) return word;
        String out = base.substring(0, at + 1) + tone + base.substring(at + 1);
        return Normalizer.normalize(out, Normalizer.Form.NFC);
    }

    /** Index of the vowel that carries the tone, -1 when the word has no vowel. */
    static int target(String base) {
        String w = base.toLowerCase();
        int e = w.length() - 1;
        while (e >= 0 && VOWELS.indexOf(w.charAt(e)) < 0) e--;
        if (e < 0) return -1;
        int s = e;
        while (s > 0 && VOWELS.indexOf(w.charAt(s - 1)) >= 0) s--;
        // "qu" and "gi" start a word as consonants: quá, giữa.
        if (s == 1 && e > s && (w.startsWith("qu") || w.startsWith("gi"))) s = 2;
        int special = -1;
        for (int i = s; i <= e; i++) {
            char c = w.charAt(i);
            if (c == 'ơ') return i;
            if ("ăâêôư".indexOf(c) >= 0 && special < 0) special = i;
        }
        if (special >= 0) return special;
        if (s == e) return s;
        if (e < w.length() - 1) return e; // vowels followed by a consonant: toán, tuấn
        String cluster = w.substring(s, e + 1);
        if (cluster.equals("oa") || cluster.equals("oe") || cluster.equals("uy")) return e; // hoà, khoẻ, thuỷ
        if (e - s >= 2) return s + 1; // oai, uya: middle vowel
        return s; // ai, ao, ia, ua: first vowel
    }
}
