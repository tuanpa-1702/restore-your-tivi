package com.tivibox.home;

import android.content.Context;
import android.provider.Settings;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Child lock: a watch timer (15 min to 2 h) after which LockService covers the screen until a parent enters
 * the PIN. The PIN is the current date and time as ddMMHHmm (25/10 18:54 is 25101854).
 */
final class ChildLock {
    static final int[] MINUTES = {15, 30, 60, 120};
    static final String[] LABELS = {"15 phút", "30 phút", "1 tiếng", "2 tiếng"};
    static final int PIN_LENGTH = 8;

    private ChildLock() {}

    /** Accepts this minute and the previous one, so a PIN typed across a minute change still works. */
    static boolean pinMatches(String pin) {
        long now = System.currentTimeMillis();
        for (int back = 0; back <= 1; back++) {
            if (format(now - back * 60_000L).equals(pin)) return true;
        }
        return false;
    }

    static String format(long time) {
        return new SimpleDateFormat("ddMMHHmm", Locale.US).format(new Date(time));
    }

    static boolean canCoverApps(Context c) {
        return Settings.canDrawOverlays(c);
    }

    static void start(Context c, int minutes) {
        Prefs p = new Prefs(c);
        p.setLocked(false);
        p.setLockDeadline(System.currentTimeMillis() + minutes * 60_000L);
        LockService.sync(c);
    }

    static void lockNow(Context c) {
        new Prefs(c).setLocked(true);
        LockService.sync(c);
    }

    static void clear(Context c) {
        new Prefs(c).clearLock();
        LockService.sync(c);
    }

    static boolean active(Prefs p) {
        return p.isLocked() || p.lockDeadline() > 0;
    }

    /** "Còn 14 phút", "Còn 1 giờ 5 phút", "Đang khoá", or "" when the lock is off. */
    static String status(Prefs p) {
        if (p.isLocked()) return "Đang khoá";
        long deadline = p.lockDeadline();
        if (deadline <= 0) return "";
        long left = deadline - System.currentTimeMillis();
        if (left <= 60_000L) return "Sắp khoá";
        long minutes = (left + 59_999L) / 60_000L;
        if (minutes < 60) return "Còn " + minutes + " phút";
        return "Còn " + minutes / 60 + " giờ" + (minutes % 60 == 0 ? "" : " " + minutes % 60 + " phút");
    }

    /** "45 phút", "1 tiếng", "1 tiếng 15 phút". */
    static String duration(int minutes) {
        if (minutes < 60) return minutes + " phút";
        return minutes / 60 + " tiếng" + (minutes % 60 == 0 ? "" : " " + minutes % 60 + " phút");
    }

    /** Time left as 14:05 or 1:02:05. */
    static String countdown(Prefs p) {
        long left = Math.max(0, p.lockDeadline() - System.currentTimeMillis());
        long s = (left + 999) / 1000;
        long h = s / 3600, m = s % 3600 / 60, sec = s % 60;
        return h > 0 ? String.format(Locale.US, "%d:%02d:%02d", h, m, sec) : String.format(Locale.US, "%02d:%02d", m, sec);
    }
}
