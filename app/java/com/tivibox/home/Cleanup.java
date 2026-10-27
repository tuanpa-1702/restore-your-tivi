package com.tivibox.home;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Environment;
import android.os.StatFs;

import java.io.File;
import java.util.Locale;

/**
 * "Dọn dẹp": RAM and storage figures, closing background apps, and Tivi Home's own cache. Android does not let a
 * normal app clear other apps' caches (CLEAR_APP_CACHE is signature|privileged), and since Android 14
 * killBackgroundProcesses only affects the caller's own processes, so results are measured, never assumed.
 */
final class Cleanup {
    private Cleanup() {}

    static ActivityManager.MemoryInfo memory(Context c) {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ((ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(mi);
        return mi;
    }

    /** "RAM trống 820 MB / 2 GB · bộ nhớ trống 5,1 GB". */
    static String summary(Context c) {
        ActivityManager.MemoryInfo mi = memory(c);
        StatFs fs = new StatFs(Environment.getDataDirectory().getPath());
        return "RAM trống " + size(mi.availMem) + " / " + size(mi.totalMem)
                + " · bộ nhớ trống " + size(fs.getAvailableBytes());
    }

    /** Asks Android to close the background processes of every installed app except Tivi Home; returns the count asked. */
    static int closeBackgroundApps(Context c) {
        ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        int n = 0;
        for (ApplicationInfo app : c.getPackageManager().getInstalledApplications(0)) {
            if (app.packageName.equals(c.getPackageName())) continue;
            am.killBackgroundProcesses(app.packageName);
            n++;
        }
        return n;
    }

    static long ownCacheBytes(Context c) {
        return du(c.getCacheDir()) + du(c.getCodeCacheDir());
    }

    static void clearOwnCache(Context c) {
        clear(c.getCacheDir());
        clear(c.getCodeCacheDir());
    }

    private static long du(File f) {
        if (f == null || !f.exists()) return 0;
        if (f.isFile()) return f.length();
        long sum = 0;
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) sum += du(k);
        return sum;
    }

    private static void clear(File dir) {
        File[] kids = dir == null ? null : dir.listFiles();
        if (kids == null) return;
        for (File k : kids) {
            if (k.isDirectory()) clear(k);
            k.delete();
        }
    }

    static String size(long bytes) {
        if (bytes >= 1L << 30) return String.format(new Locale("vi"), "%.1f GB", bytes / (double) (1L << 30));
        if (bytes >= 1L << 20) return (bytes >> 20) + " MB";
        if (bytes >= 1L << 10) return (bytes >> 10) + " KB";
        return bytes + " B";
    }
}
