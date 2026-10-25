package com.tivibox.home;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Launchable apps and the entries shown on the dock and in Apps: packages plus the launcher's own shortcuts. */
final class AppCatalog {
    static final String SETTINGS = "@settings";
    static final String APPS = "@apps";
    static final String LAUNCHER_SETTINGS = "@launcher_settings";
    static final int LAUNCHER_SETTINGS_COLOR = 0xFF5B3FE0;
    static final String STORE = "com.android.vending";
    static final List<String> YOUTUBE = Arrays.asList(
            "com.google.android.youtube.tv", "com.google.android.youtube.googletv", "com.google.android.youtube");
    static final int MAX_DOCK = 6;

    static final class App {
        final String pkg, label;
        final ResolveInfo info;
        final boolean leanback;
        private Drawable icon;

        App(String pkg, String label, ResolveInfo info, boolean leanback) {
            this.pkg = pkg;
            this.label = label;
            this.info = info;
            this.leanback = leanback;
        }

        Drawable icon(PackageManager pm) {
            if (icon == null) icon = info.loadIcon(pm);
            return icon;
        }
    }

    private AppCatalog() {}

    /** Every launchable app keyed by package (TV entry preferred), without this launcher. */
    static Map<String, App> load(Context c) {
        PackageManager pm = c.getPackageManager();
        Map<String, App> apps = new LinkedHashMap<>();
        String[] categories = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
        for (String category : categories) {
            Intent i = new Intent(Intent.ACTION_MAIN).addCategory(category);
            for (ResolveInfo ri : pm.queryIntentActivities(i, 0)) {
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(c.getPackageName()) || apps.containsKey(pkg)) continue;
                boolean leanback = Intent.CATEGORY_LEANBACK_LAUNCHER.equals(category);
                apps.put(pkg, new App(pkg, String.valueOf(ri.loadLabel(pm)), ri, leanback));
            }
        }
        return apps;
    }

    static List<App> alphabetical(Map<String, App> apps) {
        List<App> list = new ArrayList<>(apps.values());
        Collator collator = Collator.getInstance(new Locale("vi"));
        list.sort((a, b) -> collator.compare(a.label, b.label));
        return list;
    }

    /**
     * Entries for the Apps screen: saved order first; "Cài đặt giao diện" leads until the user moves it;
     * apps not seen before follow in alphabetical order.
     */
    static List<String> ordered(Map<String, App> apps, List<String> order) {
        List<String> out = new ArrayList<>();
        for (String e : order) {
            if ((LAUNCHER_SETTINGS.equals(e) || apps.containsKey(e)) && !out.contains(e)) out.add(e);
        }
        if (!out.contains(LAUNCHER_SETTINGS)) out.add(0, LAUNCHER_SETTINGS);
        for (App a : alphabetical(apps)) if (!out.contains(a.pkg)) out.add(a.pkg);
        return out;
    }

    static List<String> defaultDock(Map<String, App> apps) {
        List<String> dock = new ArrayList<>();
        for (String y : YOUTUBE) {
            if (apps.containsKey(y)) {
                dock.add(y);
                break;
            }
        }
        if (apps.containsKey(STORE)) dock.add(STORE);
        dock.add(SETTINGS);
        dock.add(APPS);
        return dock;
    }

    /** The saved dock without apps that are gone; the default dock when nothing usable is saved. */
    static List<String> dock(Prefs prefs, Map<String, App> apps) {
        List<String> saved = prefs.dock();
        List<String> dock = new ArrayList<>();
        if (saved != null) {
            for (String e : saved) {
                if ((isShortcut(e) || apps.containsKey(e)) && !dock.contains(e) && dock.size() < MAX_DOCK) dock.add(e);
            }
        }
        return dock.isEmpty() ? defaultDock(apps) : dock;
    }

    static boolean isShortcut(String entry) {
        return SETTINGS.equals(entry) || APPS.equals(entry) || LAUNCHER_SETTINGS.equals(entry);
    }

    static String label(String entry, Map<String, App> apps) {
        if (SETTINGS.equals(entry)) return "Cài đặt";
        if (APPS.equals(entry)) return "Ứng dụng";
        if (LAUNCHER_SETTINGS.equals(entry)) return "Cài đặt giao diện";
        if (STORE.equals(entry)) return "Cửa hàng";
        if (YOUTUBE.contains(entry)) return "YouTube";
        App a = apps.get(entry);
        return a != null ? a.label : entry;
    }

    /** A drawn glyph for the built-in tiles, 0 for apps that show their own icon. */
    static int glyph(String entry) {
        if (SETTINGS.equals(entry)) return Glyphs.GEAR;
        if (APPS.equals(entry)) return Glyphs.GRID;
        if (LAUNCHER_SETTINGS.equals(entry)) return Glyphs.PALETTE;
        if (STORE.equals(entry)) return Glyphs.STORE;
        if (YOUTUBE.contains(entry)) return Glyphs.YOUTUBE;
        return 0;
    }

    static boolean launch(Context c, String pkg) {
        PackageManager pm = c.getPackageManager();
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = pm.getLaunchIntentForPackage(pkg);
        if (i == null) return false;
        try {
            c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false;
        }
    }
}
