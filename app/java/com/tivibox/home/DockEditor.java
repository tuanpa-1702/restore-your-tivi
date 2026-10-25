package com.tivibox.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Dialogs that change which apps sit on the dock. */
final class DockEditor {
    static final int DIALOG_THEME = android.R.style.Theme_DeviceDefault_Dialog_Alert;

    private DockEditor() {}

    /** Checklist of every app plus the Settings and Apps shortcuts; keeps the current order, appends new picks. */
    static void pick(Activity a, Prefs prefs, Runnable changed) {
        Map<String, AppCatalog.App> apps = AppCatalog.load(a);
        List<String> current = AppCatalog.dock(prefs, apps);
        List<String> entries = new ArrayList<>();
        entries.add(AppCatalog.SETTINGS);
        entries.add(AppCatalog.APPS);
        entries.add(AppCatalog.LAUNCHER_SETTINGS);
        for (AppCatalog.App app : AppCatalog.alphabetical(apps)) entries.add(app.pkg);
        String[] labels = new String[entries.size()];
        boolean[] checked = new boolean[entries.size()];
        for (int i = 0; i < labels.length; i++) {
            String e = entries.get(i);
            labels[i] = AppCatalog.label(e, apps) + (AppCatalog.isShortcut(e) ? " (lối tắt)" : "");
            checked[i] = current.contains(e);
        }
        new AlertDialog.Builder(a, DIALOG_THEME)
                .setTitle("Ứng dụng trên dock (tối đa " + AppCatalog.MAX_DOCK + ")")
                .setMultiChoiceItems(labels, checked, (d, i, on) -> checked[i] = on)
                .setPositiveButton("Lưu", (d, w) -> {
                    List<String> next = new ArrayList<>();
                    for (String e : current) if (checked[entries.indexOf(e)]) next.add(e);
                    for (int i = 0; i < checked.length; i++) {
                        if (checked[i] && !next.contains(entries.get(i))) next.add(entries.get(i));
                    }
                    if (next.isEmpty()) {
                        Toast.makeText(a, "Dock cần ít nhất một ô.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (next.size() > AppCatalog.MAX_DOCK) {
                        next = new ArrayList<>(next.subList(0, AppCatalog.MAX_DOCK));
                        Toast.makeText(a, "Dock chứa tối đa " + AppCatalog.MAX_DOCK + " ô; đã giữ "
                                + AppCatalog.MAX_DOCK + " ô đầu.", Toast.LENGTH_LONG).show();
                    }
                    prefs.setDock(next);
                    changed.run();
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    /** Adds or removes one app; returns false (with a message) when the dock is full or would be empty. */
    static boolean toggle(Activity a, Prefs prefs, String entry) {
        Map<String, AppCatalog.App> apps = AppCatalog.load(a);
        List<String> dock = AppCatalog.dock(prefs, apps);
        String label = AppCatalog.label(entry, apps);
        if (dock.contains(entry)) {
            if (dock.size() == 1) {
                Toast.makeText(a, "Dock cần ít nhất một ô.", Toast.LENGTH_LONG).show();
                return false;
            }
            dock.remove(entry);
            Toast.makeText(a, "Đã bỏ " + label + " khỏi dock.", Toast.LENGTH_SHORT).show();
        } else {
            if (dock.size() >= AppCatalog.MAX_DOCK) {
                Toast.makeText(a, "Dock đã đủ " + AppCatalog.MAX_DOCK + " ô. Bỏ bớt một ô trước.", Toast.LENGTH_LONG).show();
                return false;
            }
            dock.add(entry);
            Toast.makeText(a, "Đã thêm " + label + " vào dock.", Toast.LENGTH_SHORT).show();
        }
        prefs.setDock(dock);
        return true;
    }
}
