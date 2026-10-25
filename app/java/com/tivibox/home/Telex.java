package com.tivibox.home;

import java.text.Normalizer;

/**
 * Telex typing for the TV keyboard. s f r x j put sắc huyền hỏi ngã nặng, z removes the tone;
 * aa ee oo → â ê ô, aw ow uw → ă ơ ư (a lone w → ư), dd → đ. Typing the same mark again undoes it and keeps the
 * letter ("ass" → "as"). Pure Java, tested in test/TelexTest.java.
 */
public final class Telex {
    private static final String TONE_KEYS = "sfrxj";
    private static final char[] TONES = {VietTone.SAC, VietTone.HUYEN, VietTone.HOI, VietTone.NGA, VietTone.NANG};
    private static final String TONE_MARKS = "̣́̀̉̃";
    private static final String VOWELS = "aăâeêioôơuưy";

    private Telex() {}

    /** The word before the cursor after typing `c`; plain `word + c` when Telex does nothing. */
    public static String type(String word, char c) {
        char lc = Character.toLowerCase(c);
        char tone = toneOf(word);
        String bare = stripTone(word);
        boolean hasVowel = vowelIndex(bare) >= 0;

        int t = TONE_KEYS.indexOf(lc);
        if (t >= 0 && hasVowel) {
            if (tone == TONES[t]) return bare + c;          // same tone again: undo, keep the letter
            return VietTone.apply(bare, TONES[t]);
        }
        if (lc == 'z' && tone != 0) return bare;

        String changed = null;
        if (lc == 'd') changed = doubleD(bare, c);
        else if (lc == 'a' || lc == 'e' || lc == 'o') changed = circumflex(bare, lc, c);
        else if (lc == 'w') changed = horn(bare, c);
        if (changed != null) return tone != 0 ? VietTone.apply(changed, tone) : changed;

        // Plain letter: keep the tone on the right vowel as the word grows (hoa + n → hoan).
        return tone != 0 && Character.isLetter(c) ? VietTone.apply(bare + c, tone) : word + c;
    }

    private static String doubleD(String w, char c) {
        if (w.isEmpty()) return null;
        char last = w.charAt(w.length() - 1);
        if (last == 'd' || last == 'D') return w.substring(0, w.length() - 1) + (last == 'D' ? 'Đ' : 'đ');
        if (last == 'đ' || last == 'Đ') return w.substring(0, w.length() - 1) + (last == 'Đ' ? 'D' : 'd') + c;
        return null;
    }

    /** aa → â, ee → ê, oo → ô on the vowel just typed; typing it a third time undoes it. */
    private static String circumflex(String w, char lc, char c) {
        int i = lastVowel(w);
        if (i < 0 || !onlyConsonantsAfter(w, i, 1)) return null;
        char v = w.charAt(i);
        char lv = Character.toLowerCase(v);
        boolean upper = Character.isUpperCase(v);
        String plain = "aeo", hat = "âêô", other = "ăơ"; // ă/ơ + a/o switch to â/ô
        int p = plain.indexOf(lc);
        char target = hat.charAt(p);
        if (lv == lc || (lc == 'a' && lv == 'ă') || (lc == 'o' && lv == 'ơ')) {
            return w.substring(0, i) + (upper ? Character.toUpperCase(target) : target) + w.substring(i + 1);
        }
        if (lv == target) {
            return w.substring(0, i) + (upper ? Character.toUpperCase(lc) : lc) + w.substring(i + 1) + c;
        }
        return null;
    }

    /** uo → ươ, a → ă, o → ơ, u → ư on the last vowel group; a lone w → ư; w again undoes it. */
    private static String horn(String w, char c) {
        String lw = w.toLowerCase();
        int i = lastVowel(w);
        if (i < 0) return w + (Character.isUpperCase(c) ? 'Ư' : 'ư');
        if (!onlyConsonantsAfter(w, i, 2)) return null;
        int s = i;
        while (s > 0 && VOWELS.indexOf(lw.charAt(s - 1)) >= 0) s--;
        int uo = lw.lastIndexOf("uo");
        if (uo < 0) uo = lw.lastIndexOf("ưo");
        if (uo >= s && uo + 1 <= i) {
            return w.substring(0, uo) + keepCase(w.charAt(uo), 'ư') + keepCase(w.charAt(uo + 1), 'ơ') + w.substring(uo + 2);
        }
        for (int k = i; k >= s; k--) {
            char lv = lw.charAt(k);
            int from = "aou".indexOf(lv);
            if (from >= 0) return w.substring(0, k) + keepCase(w.charAt(k), "ăơư".charAt(from)) + w.substring(k + 1);
            int back = "ăơư".indexOf(lv);
            if (back >= 0) return w.substring(0, k) + keepCase(w.charAt(k), "aou".charAt(back)) + w.substring(k + 1) + c;
        }
        return null;
    }

    private static char keepCase(char original, char replacement) {
        return Character.isUpperCase(original) ? Character.toUpperCase(replacement) : replacement;
    }

    private static boolean onlyConsonantsAfter(String w, int i, int max) {
        return w.length() - 1 - i <= max;
    }

    private static int lastVowel(String w) {
        String lw = w.toLowerCase();
        for (int i = lw.length() - 1; i >= 0; i--) if (VOWELS.indexOf(lw.charAt(i)) >= 0) return i;
        return -1;
    }

    private static int vowelIndex(String w) {
        return lastVowel(w);
    }

    static char toneOf(String w) {
        String nfd = Normalizer.normalize(w, Normalizer.Form.NFD);
        for (int i = 0; i < nfd.length(); i++) {
            if (TONE_MARKS.indexOf(nfd.charAt(i)) >= 0) return nfd.charAt(i);
        }
        return 0;
    }

    static String stripTone(String w) {
        String nfd = Normalizer.normalize(w, Normalizer.Form.NFD);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nfd.length(); i++) if (TONE_MARKS.indexOf(nfd.charAt(i)) < 0) sb.append(nfd.charAt(i));
        return Normalizer.normalize(sb, Normalizer.Form.NFC);
    }

    /** Types a whole key sequence, word by word (spaces end words). For tests. */
    public static String typeAll(String keys) {
        StringBuilder out = new StringBuilder();
        String word = "";
        for (char c : keys.toCharArray()) {
            if (!Character.isLetter(c)) {
                out.append(word).append(c);
                word = "";
            } else {
                word = type(word, c);
            }
        }
        return out.append(word).toString();
    }
}
