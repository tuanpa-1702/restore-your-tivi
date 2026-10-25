package com.tivibox.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Launcher settings: greeting, weather place, background, dock, order reset, Android settings. */
public class SettingsActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private Prefs prefs;
    private LinearLayout rows;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        prefs = new Prefs(this);
        rows = findViewById(R.id.rows);
    }

    @Override
    protected void onResume() {
        super.onResume();
        buildRows();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdownNow();
    }

    private void buildRows() {
        View focus = getCurrentFocus();
        int keep = focus == null ? 0 : Math.max(0, rows.indexOfChild(focus));
        ((MeshBackgroundView) findViewById(R.id.background)).setTheme(prefs.theme());
        rows.removeAllViews();
        row("Lời chào", prefs.greeting(), this::editGreeting);
        row("Vị trí thời tiết", prefs.hasPlace() ? prefs.placeName() : "Chưa chọn — bấm để nhập thành phố hoặc quận/huyện",
                this::editPlace);
        row("Màu nền", MeshBackgroundView.THEME_NAMES[Math.max(0, Math.min(2, prefs.theme()))], this::pickTheme);
        row("Ứng dụng trên dock", dockSummary(), () -> DockEditor.pick(this, prefs, this::buildRows));
        row("Sắp xếp", "Giữ OK trên một ô ở màn hình chính hoặc trong Ứng dụng để di chuyển. Bấm để đặt lại thứ tự.",
                this::resetOrder);
        row("Khoá trẻ em", ChildLock.canCoverApps(this)
                        ? "Hẹn giờ 15 phút – 2 tiếng hoặc tuỳ chỉnh (đang đặt " + ChildLock.duration(prefs.customLockMinutes())
                        + "). Cũng mở được bằng nút ổ khoá trên màn hình chính."
                        : "Chưa có quyền hiển thị trên ứng dụng khác — bấm để cấp",
                () -> startActivity(new Intent(this, ChildLockActivity.class)));
        row("Cài đặt Android", "Wi-Fi, âm thanh, hiển thị, ứng dụng…", this::openAndroidSettings);
        row("Giới thiệu", "Tivi Home " + versionName() + " · thời tiết Open-Meteo · âm lịch theo giờ Việt Nam", null);
        rows.getChildAt(Math.min(keep, rows.getChildCount() - 1)).requestFocus();
    }

    private void row(String title, String value, Runnable click) {
        View v = getLayoutInflater().inflate(R.layout.item_setting, rows, false);
        ((TextView) v.findViewById(R.id.title)).setText(title);
        ((TextView) v.findViewById(R.id.value)).setText(value);
        if (click != null) v.setOnClickListener(x -> click.run());
        rows.addView(v);
    }

    private String dockSummary() {
        Map<String, AppCatalog.App> apps = AppCatalog.load(this);
        List<String> labels = new ArrayList<>();
        for (String e : AppCatalog.dock(prefs, apps)) labels.add(AppCatalog.label(e, apps));
        return String.join(" · ", labels);
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    private void editGreeting() {
        askText("Lời chào", prefs.greeting(), Prefs.DEFAULT_GREETING, text -> {
            prefs.setGreeting(text.isEmpty() ? Prefs.DEFAULT_GREETING : text);
            buildRows();
        });
    }

    private void editPlace() {
        askText("Vị trí thời tiết", prefs.placeQuery(), "Ví dụ: Cầu Giấy, Thủ Đức, Đà Nẵng", text -> {
            if (!text.isEmpty()) searchPlace(text);
        });
    }

    private void pickTheme() {
        new AlertDialog.Builder(this, DockEditor.DIALOG_THEME)
                .setTitle("Màu nền")
                .setSingleChoiceItems(MeshBackgroundView.THEME_NAMES, prefs.theme(), (d, which) -> {
                    prefs.setTheme(which);
                    d.dismiss();
                    buildRows();
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void resetOrder() {
        new AlertDialog.Builder(this, DockEditor.DIALOG_THEME)
                .setTitle("Đặt lại thứ tự?")
                .setMessage("Dock về mặc định (YouTube, Cửa hàng, Cài đặt, Ứng dụng) và danh sách Ứng dụng xếp theo tên.")
                .setPositiveButton("Đặt lại", (d, w) -> {
                    prefs.resetOrder();
                    buildRows();
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void openAndroidSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Không mở được Cài đặt.", Toast.LENGTH_LONG).show();
        }
    }

    private interface TextResult {
        void accept(String text);
    }

    private void askText(String title, String value, String hint, TextResult result) {
        float dp = getResources().getDisplayMetrics().density;
        EditText edit = new EditText(this);
        edit.setSingleLine(true);
        edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        edit.setImeOptions(EditorInfo.IME_ACTION_DONE);
        edit.setHint(hint);
        edit.setText(value);
        edit.setSelection(edit.getText().length());
        FrameLayout box = new FrameLayout(this);
        int pad = Math.round(24 * dp);
        box.setPadding(pad, Math.round(8 * dp), pad, 0);
        box.addView(edit);
        new AlertDialog.Builder(this, DockEditor.DIALOG_THEME)
                .setTitle(title)
                .setView(box)
                .setPositiveButton("Lưu", (d, w) -> result.accept(edit.getText().toString().trim()))
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void searchPlace(String query) {
        Toast.makeText(this, "Đang tìm “" + query + "”…", Toast.LENGTH_SHORT).show();
        io.execute(() -> {
            List<Weather.Place> found;
            String error = null;
            try {
                found = Weather.search(query);
            } catch (Exception e) {
                found = new ArrayList<>();
                error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            }
            final List<Weather.Place> places = found;
            final String err = error;
            handler.post(() -> {
                if (isDestroyed()) return;
                if (places.isEmpty()) {
                    Toast.makeText(this, err != null
                            ? "Không tìm được địa điểm (" + err + "). Kiểm tra mạng của TV."
                            : "Không tìm thấy “" + query + "”. Thử tên khác, ví dụ: Cầu Giấy, Thủ Đức, Đà Nẵng.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                if (places.size() == 1) {
                    choosePlace(query, places.get(0));
                    return;
                }
                String[] labels = new String[places.size()];
                for (int i = 0; i < labels.length; i++) labels[i] = places.get(i).label;
                new AlertDialog.Builder(this, DockEditor.DIALOG_THEME)
                        .setTitle("Chọn địa điểm")
                        .setItems(labels, (d, i) -> choosePlace(query, places.get(i)))
                        .setNegativeButton("Huỷ", null)
                        .show();
            });
        });
    }

    private void choosePlace(String query, Weather.Place place) {
        prefs.setPlace(query, place);
        Toast.makeText(this, "Thời tiết: " + place.label, Toast.LENGTH_SHORT).show();
        buildRows();
    }
}
