package com.tivibox.home;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Screensaver settings: template (5 designs + custom slideshow), preview, idle time, slideshow photos, interval,
 * clock. Making Tivi Home the system screensaver and changing the idle time need WRITE_SECURE_SETTINGS and
 * WRITE_SETTINGS, which install.ps1 grants over ADB; without them the TV's own screensaver settings are opened.
 */
public class ScreensaverSettingsActivity extends Activity {
    private static final int[] TIMEOUT_MIN = {0, 5, 10, 15, 30, 60};
    private static final String[] TIMEOUT_NAMES = {"Tắt màn hình chờ", "5 phút", "10 phút", "15 phút", "30 phút", "1 giờ"};
    private static final int[] INTERVAL_SEC = {10, 30, 60, 300};
    private static final String[] INTERVAL_NAMES = {"10 giây", "30 giây", "1 phút", "5 phút"};

    private Prefs prefs;
    private CardGrid grid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        prefs = new Prefs(this);
        grid = new CardGrid(this, findViewById(R.id.rows));
        ((TextView) findViewById(R.id.screen_title)).setText("Màn hình chờ");
        ((TextView) findViewById(R.id.screen_hint)).setText("OK để thay đổi · BACK để quay lại");
    }

    @Override
    protected void onResume() {
        super.onResume();
        build();
    }

    private void build() {
        int keep = grid.focusedIndex();
        ((MeshBackgroundView) findViewById(R.id.background)).setTheme(prefs.theme());
        grid.clear();
        int t = prefs.saverTemplate();
        row("Mẫu màn hình chờ", ScreensaverView.NAMES[t] + " — " + ScreensaverView.DESCRIPTIONS[t], this::pickTemplate);
        row("Xem thử", "Mở màn hình chờ ngay để xem, bấm phím bất kỳ để thoát", () ->
                startActivity(new Intent(this, ScreensaverPreviewActivity.class)
                        .putExtra(ScreensaverPreviewActivity.EXTRA_TEMPLATE, prefs.saverTemplate())));
        row("Thời gian chờ", timeoutText(this) + " không bấm remote thì bật màn hình chờ", this::pickTimeout);
        row("Dùng làm màn hình chờ của TV", isActive(this)
                        ? "Đang bật: TV dùng màn hình chờ Tivi Home"
                        : "Chưa bật — bấm để đặt Tivi Home làm màn hình chờ của TV",
                this::apply);
        if (t == ScreensaverView.CUSTOM) {
            int n = Photos.slides(this).size();
            row("Ảnh trình chiếu", n == 0 ? "Chưa có ảnh — bấm để chọn từ TV hoặc USB" : n + " ảnh · bấm để chọn lại",
                    () -> startActivity(ImagePickerActivity.open(this, ImagePickerActivity.MODE_SLIDES)));
            row("Thời gian mỗi ảnh", intervalName(prefs.saverInterval()), this::pickInterval);
            row("Hiện đồng hồ", prefs.saverClock() ? "Bật — đồng hồ và ngày ở góc dưới bên phải" : "Tắt",
                    () -> {
                        prefs.setSaverClock(!prefs.saverClock());
                        build();
                    });
        }
        grid.finish(keep);
    }

    private void row(String title, String value, Runnable click) {
        grid.add(title, value, click);
    }

    private void pickTemplate() {
        String[] items = new String[ScreensaverView.NAMES.length];
        for (int i = 0; i < items.length; i++) items[i] = (i + 1) + ". " + ScreensaverView.NAMES[i];
        new AlertDialog.Builder(this, DockEditor.DIALOG_THEME)
                .setTitle("Mẫu màn hình chờ")
                .setSingleChoiceItems(items, prefs.saverTemplate(), (d, which) -> {
                    prefs.setSaverTemplate(which);
                    d.dismiss();
                    build();
                    if (which == ScreensaverView.CUSTOM && Photos.slides(this).isEmpty()) {
                        startActivity(ImagePickerActivity.open(this, ImagePickerActivity.MODE_SLIDES));
                    }
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void pickInterval() {
        int current = 1;
        for (int i = 0; i < INTERVAL_SEC.length; i++) if (INTERVAL_SEC[i] == prefs.saverInterval()) current = i;
        new AlertDialog.Builder(this, DockEditor.DIALOG_THEME)
                .setTitle("Thời gian mỗi ảnh")
                .setSingleChoiceItems(INTERVAL_NAMES, current, (d, which) -> {
                    prefs.setSaverInterval(INTERVAL_SEC[which]);
                    d.dismiss();
                    build();
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private static String intervalName(int seconds) {
        for (int i = 0; i < INTERVAL_SEC.length; i++) if (INTERVAL_SEC[i] == seconds) return INTERVAL_NAMES[i];
        return seconds + " giây";
    }

    // ------------------------------------------------------------------ system settings

    static boolean isActive(Context c) {
        ContentResolver cr = c.getContentResolver();
        String comp = Settings.Secure.getString(cr, "screensaver_components");
        return Settings.Secure.getInt(cr, "screensaver_enabled", 1) == 1 && comp != null
                && comp.contains(c.getPackageName() + "/");
    }

    /** "10 phút" or "Tắt", from the TV's own settings. */
    static String timeoutText(Context c) {
        ContentResolver cr = c.getContentResolver();
        if (Settings.Secure.getInt(cr, "screensaver_enabled", 1) == 0) return "Tắt";
        int ms = Settings.System.getInt(cr, Settings.System.SCREEN_OFF_TIMEOUT, 0);
        if (ms <= 0 || ms >= Integer.MAX_VALUE / 2) return "Không bao giờ";
        int min = Math.max(1, Math.round(ms / 60000f));
        return min >= 60 && min % 60 == 0 ? (min / 60) + " giờ" : min + " phút";
    }

    private boolean canWriteSecure() {
        return checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
    }

    private void pickTimeout() {
        new AlertDialog.Builder(this, DockEditor.DIALOG_THEME)
                .setTitle("Thời gian chờ")
                .setItems(TIMEOUT_NAMES, (d, which) -> setTimeout(TIMEOUT_MIN[which]))
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void setTimeout(int minutes) {
        ContentResolver cr = getContentResolver();
        if (minutes == 0) {
            if (!canWriteSecure()) {
                needPermission();
                return;
            }
            Settings.Secure.putInt(cr, "screensaver_enabled", 0);
            build();
            return;
        }
        if (!Settings.System.canWrite(this)) {
            needPermission();
            return;
        }
        Settings.System.putInt(cr, Settings.System.SCREEN_OFF_TIMEOUT, minutes * 60_000);
        if (canWriteSecure()) Settings.Secure.putInt(cr, "screensaver_enabled", 1);
        Toast.makeText(this, "Màn hình chờ bật sau " + minutes + " phút không dùng remote", Toast.LENGTH_SHORT).show();
        build();
    }

    private void apply() {
        if (!canWriteSecure()) {
            needPermission();
            return;
        }
        ContentResolver cr = getContentResolver();
        String comp = new ComponentName(this, TiviDream.class).flattenToShortString();
        Settings.Secure.putString(cr, "screensaver_components", comp);
        Settings.Secure.putString(cr, "screensaver_default_component", comp);
        Settings.Secure.putInt(cr, "screensaver_enabled", 1);
        Settings.Secure.putInt(cr, "screensaver_activate_on_sleep", 1);
        Toast.makeText(this, "Đã đặt Tivi Home làm màn hình chờ", Toast.LENGTH_SHORT).show();
        build();
    }

    /** Without the ADB-granted permissions: explain, then offer the TV's screensaver settings. */
    private void needPermission() {
        new AlertDialog.Builder(this, DockEditor.DIALOG_THEME)
                .setTitle("Cần cấp quyền")
                .setMessage("Để Tivi Home tự đổi màn hình chờ và thời gian chờ của TV, chạy lại install.ps1 "
                        + "(script cấp quyền qua ADB), hoặc chọn \"Màn hình chờ Tivi Home\" và thời gian chờ "
                        + "trong cài đặt của TV.")
                .setPositiveButton("Mở cài đặt màn hình chờ của TV", (d, w) -> openSystemDreamSettings())
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void openSystemDreamSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_DREAM_SETTINGS));
        } catch (ActivityNotFoundException e) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (ActivityNotFoundException ignored) {
                Toast.makeText(this, "Không mở được Cài đặt.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
