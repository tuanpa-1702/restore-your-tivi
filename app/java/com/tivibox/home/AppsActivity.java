package com.tivibox.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Every launchable app plus "Cài đặt giao diện", in the user's own order.
 * Hold OK on an entry to move it or put it on the dock.
 */
public class AppsActivity extends Activity {
    private final List<String> entries = new ArrayList<>();
    private Map<String, AppCatalog.App> apps = new HashMap<>();
    private Prefs prefs;
    private GridView grid;
    private Adapter adapter;
    private int moving = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apps);
        prefs = new Prefs(this);
        ((MeshBackgroundView) findViewById(R.id.background)).setTheme(prefs.theme());
        grid = findViewById(R.id.grid);
        adapter = new Adapter();
        grid.setAdapter(adapter);
        grid.setOnItemClickListener((parent, view, position, id) -> open(entries.get(position)));
        grid.setOnItemLongClickListener((parent, view, position, id) -> {
            showMenu(position);
            return true;
        });
        grid.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        int keep = grid.getSelectedItemPosition();
        apps = AppCatalog.load(this);
        entries.clear();
        entries.addAll(AppCatalog.ordered(apps, prefs.appsOrder()));
        adapter.notifyDataSetChanged();
        if (keep >= 0 && keep < entries.size()) grid.setSelection(keep);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (moving >= 0) finishMove();
    }

    private void open(String entry) {
        if (AppCatalog.LAUNCHER_SETTINGS.equals(entry)) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (!AppCatalog.launch(this, entry)) {
            Toast.makeText(this, "Không mở được " + AppCatalog.label(entry, apps) + ".", Toast.LENGTH_SHORT).show();
        }
    }

    private void showMenu(int position) {
        String entry = entries.get(position);
        boolean onDock = AppCatalog.dock(prefs, apps).contains(entry);
        boolean isApp = !AppCatalog.isShortcut(entry);
        List<String> items = new ArrayList<>();
        items.add("Di chuyển");
        items.add(onDock ? "Bỏ khỏi dock" : "Thêm vào dock");
        if (isApp) items.add("Thông tin ứng dụng");
        new AlertDialog.Builder(this, DockEditor.DIALOG_THEME)
                .setTitle(AppCatalog.label(entry, apps))
                .setItems(items.toArray(new String[0]), (d, which) -> {
                    if (which == 0) {
                        moving = position;
                        grid.setSelector(R.drawable.app_item_moving);
                        grid.setSelection(position);
                        Toast.makeText(this, "Bấm ◀ ▶ ▲ ▼ để đổi chỗ, OK để xong", Toast.LENGTH_LONG).show();
                    } else if (which == 1) {
                        DockEditor.toggle(this, prefs, entry);
                    } else {
                        try {
                            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:" + entry)));
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(this, "TV này không mở được trang thông tin ứng dụng.", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void finishMove() {
        moving = -1;
        prefs.setAppsOrder(entries);
        grid.setSelector(R.drawable.app_selector);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (moving < 0) return super.dispatchKeyEvent(e);
        int code = e.getKeyCode();
        int cols = Math.max(1, grid.getNumColumns());
        int step;
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_LEFT: step = -1; break;
            case KeyEvent.KEYCODE_DPAD_RIGHT: step = 1; break;
            case KeyEvent.KEYCODE_DPAD_UP: step = -cols; break;
            case KeyEvent.KEYCODE_DPAD_DOWN: step = cols; break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_BACK:
                if (e.getAction() == KeyEvent.ACTION_DOWN) {
                    int at = moving;
                    finishMove();
                    grid.setSelection(at);
                }
                return true;
            default:
                return super.dispatchKeyEvent(e);
        }
        if (e.getAction() != KeyEvent.ACTION_DOWN) return true;
        int target = Math.max(0, Math.min(entries.size() - 1, moving + step));
        if (target != moving) {
            entries.add(target, entries.remove(moving));
            moving = target;
            adapter.notifyDataSetChanged();
            grid.setSelection(target);
        }
        return true;
    }

    private final class Adapter extends BaseAdapter {
        @Override
        public int getCount() {
            return entries.size();
        }

        @Override
        public Object getItem(int position) {
            return entries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView != null ? convertView
                    : getLayoutInflater().inflate(R.layout.item_app, parent, false);
            String entry = entries.get(position);
            AppCatalog.App app = apps.get(entry);
            ImageView icon = v.findViewById(R.id.icon);
            icon.setClipToOutline(true);
            if (app != null) {
                icon.setImageDrawable(app.icon(getPackageManager()));
            } else {
                icon.setImageDrawable(new GlyphDrawable(AppCatalog.glyph(entry), AppCatalog.LAUNCHER_SETTINGS_COLOR));
            }
            ((TextView) v.findViewById(R.id.label)).setText(AppCatalog.label(entry, apps));
            return v;
        }
    }
}
