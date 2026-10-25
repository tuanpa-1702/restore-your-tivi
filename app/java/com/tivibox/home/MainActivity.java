package com.tivibox.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** The home screen: clock, date with lunar date, month calendar, weather and the dock. */
public class MainActivity extends Activity {
    private static final long WEATHER_EVERY_MS = 30 * 60 * 1000L;
    private static final long WEATHER_RETRY_MS = 2 * 60 * 1000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final List<String> dock = new ArrayList<>();
    private Map<String, AppCatalog.App> apps;
    private Prefs prefs;
    private MeshBackgroundView background;
    private View stage;
    private LinearLayout dockRow;
    private RoundButton lock, wifi;
    private TextView greeting, weekday, date, lunar, lockStatus;
    private CalendarCardView calendar;
    private WeatherCardView weather;
    private int focusedTile;
    private boolean topBarFocused;
    private int moving = -1;
    private int weatherRequest;
    private boolean resumed;
    private String shownDate = "";

    private final BroadcastReceiver timeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshDate();
            refreshLock();
        }
    };

    private final Runnable weatherTick = new Runnable() {
        @Override
        public void run() {
            handler.postDelayed(this, WEATHER_EVERY_MS);
            refreshWeather(false);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = new Prefs(this);
        background = findViewById(R.id.background);
        stage = findViewById(R.id.stage);
        dockRow = findViewById(R.id.dock);
        greeting = findViewById(R.id.greeting);
        weekday = findViewById(R.id.weekday);
        date = findViewById(R.id.date);
        lunar = findViewById(R.id.lunar);
        calendar = findViewById(R.id.calendar);
        weather = findViewById(R.id.weather);

        lockStatus = findViewById(R.id.lock_status);
        lock = findViewById(R.id.btn_lock);
        lock.setup(Glyphs.LOCK, "Khoá trẻ em");
        lock.setOnClickListener(v -> startActivity(new Intent(this, ChildLockActivity.class)));
        wifi = findViewById(R.id.btn_wifi);
        wifi.setup(Glyphs.WIFI, "Wi-Fi");
        wifi.setOnClickListener(v -> openSettings(Settings.ACTION_WIFI_SETTINGS));

        findViewById(R.id.root).addOnLayoutChangeListener(
                (v, l, t, r, b, ol, ot, or, ob) -> fitStage(v.getWidth(), v.getHeight()));
    }

    /** The layout is designed on a 960x540 dp stage; scale it to whatever the TV reports. */
    private void fitStage(int width, int height) {
        int sw = stage.getWidth(), sh = stage.getHeight();
        if (sw == 0 || sh == 0 || width == 0 || height == 0) return;
        float s = Math.min(width / (float) sw, height / (float) sh);
        stage.setPivotX(0);
        stage.setPivotY(0);
        stage.setScaleX(s);
        stage.setScaleY(s);
        stage.setTranslationX((width - sw * s) / 2);
        stage.setTranslationY((height - sh * s) / 2);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_TIME_TICK);
        f.addAction(Intent.ACTION_TIME_CHANGED);
        f.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        f.addAction(Intent.ACTION_DATE_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(timeReceiver, f, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(timeReceiver, f);
        }
        resumed = true;
        background.setTheme(prefs.theme());
        String g = prefs.greeting().trim();
        greeting.setText(g.isEmpty() ? "Xin chào" : g);
        refreshDate();
        buildDock();
        if (topBarFocused) lock.requestFocus();
        else focusTile(focusedTile);
        LockService.sync(this);
        refreshLock();
        handler.removeCallbacks(weatherTick);
        handler.post(weatherTick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        resumed = false;
        if (moving >= 0) finishMove();
        unregisterReceiver(timeReceiver);
        handler.removeCallbacks(weatherTick);
        View focus = getCurrentFocus();
        topBarFocused = focus == lock || focus == wifi;
        int i = focus == null ? -1 : dockRow.indexOfChild(focus);
        if (i >= 0) focusedTile = i;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdownNow();
    }

    @Override
    public void onBackPressed() {
        // Home has nowhere to go back to.
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (moving >= 0) finishMove();
        topBarFocused = false;
        focusedTile = 0;
        focusTile(0);
    }

    // ------------------------------------------------------------------ dock

    private void buildDock() {
        apps = AppCatalog.load(this);
        dock.clear();
        dock.addAll(AppCatalog.dock(prefs, apps));
        float dp = getResources().getDisplayMetrics().density;
        dockRow.removeAllViews();
        for (int i = 0; i < dock.size(); i++) {
            TileView t = new TileView(this, null);
            t.setId(View.generateViewId());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            if (i > 0) lp.setMarginStart(Math.round(14 * dp));
            t.setOnClickListener(v -> openEntry(dock.get(dockRow.indexOfChild(v))));
            t.setOnLongClickListener(v -> {
                showTileMenu(dockRow.indexOfChild(v));
                return true;
            });
            dockRow.addView(t, lp);
        }
        bindTiles();
        int first = dockRow.getChildAt(0).getId(), last = dockRow.getChildAt(dock.size() - 1).getId();
        lock.setNextFocusDownId(first);
        wifi.setNextFocusDownId(last);
        for (int i = 0; i < dock.size(); i++) {
            dockRow.getChildAt(i).setNextFocusUpId(i < (dock.size() + 1) / 2 ? R.id.btn_lock : R.id.btn_wifi);
        }
    }

    private void bindTiles() {
        for (int i = 0; i < dock.size(); i++) {
            String e = dock.get(i);
            AppCatalog.App app = apps.get(e);
            TileView t = (TileView) dockRow.getChildAt(i);
            t.setup(AppCatalog.label(e, apps), AppCatalog.glyph(e), app == null ? null : app.icon(getPackageManager()));
            t.setMoving(i == moving);
        }
    }

    private void focusTile(int i) {
        if (dockRow.getChildCount() == 0) return;
        dockRow.getChildAt(Math.max(0, Math.min(dockRow.getChildCount() - 1, i))).requestFocus();
    }

    private void openEntry(String entry) {
        if (AppCatalog.SETTINGS.equals(entry)) {
            openSettings(Settings.ACTION_SETTINGS);
        } else if (AppCatalog.APPS.equals(entry)) {
            startActivity(new Intent(this, AppsActivity.class));
        } else if (AppCatalog.LAUNCHER_SETTINGS.equals(entry)) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (!AppCatalog.launch(this, entry)) {
            Toast.makeText(this, "Không mở được " + AppCatalog.label(entry, apps) + ".", Toast.LENGTH_LONG).show();
        }
    }

    private void showTileMenu(int i) {
        if (i < 0) return;
        String entry = dock.get(i);
        String[] items = {"Di chuyển", "Bỏ khỏi dock", "Chọn ứng dụng trên dock…"};
        new AlertDialog.Builder(this, DockEditor.DIALOG_THEME)
                .setTitle(AppCatalog.label(entry, apps))
                .setItems(items, (d, which) -> {
                    if (which == 0) {
                        startMove(i);
                    } else if (which == 1) {
                        if (DockEditor.toggle(this, prefs, entry)) {
                            buildDock();
                            focusTile(i);
                        }
                    } else {
                        DockEditor.pick(this, prefs, () -> {
                            buildDock();
                            focusTile(i);
                        });
                    }
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void startMove(int i) {
        moving = i;
        bindTiles();
        focusTile(i);
        Toast.makeText(this, "Bấm ◀ ▶ để đổi chỗ, OK để xong", Toast.LENGTH_LONG).show();
    }

    private void finishMove() {
        moving = -1;
        prefs.setDock(dock);
        bindTiles();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (moving < 0) return super.dispatchKeyEvent(e);
        int code = e.getKeyCode();
        boolean handled = code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT
                || code == KeyEvent.KEYCODE_DPAD_UP || code == KeyEvent.KEYCODE_DPAD_DOWN
                || code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER
                || code == KeyEvent.KEYCODE_BACK;
        if (!handled) return super.dispatchKeyEvent(e);
        if (e.getAction() != KeyEvent.ACTION_DOWN) return true;
        int target = moving;
        if (code == KeyEvent.KEYCODE_DPAD_LEFT) target = moving - 1;
        else if (code == KeyEvent.KEYCODE_DPAD_RIGHT) target = moving + 1;
        else if (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_BACK) {
            int at = moving;
            finishMove();
            focusTile(at);
            return true;
        }
        if (target >= 0 && target < dock.size() && target != moving) {
            Collections.swap(dock, moving, target);
            moving = target;
            bindTiles();
            focusTile(target);
        }
        return true;
    }

    // ------------------------------------------------------------------ child lock

    private void refreshLock() {
        String status = ChildLock.status(prefs);
        lockStatus.setText(status);
        lockStatus.setVisibility(status.isEmpty() ? View.GONE : View.VISIBLE);
        lock.setActive(!status.isEmpty());
        // Without "display over other apps" the service cannot cover the screen, so the lock screen does.
        if (prefs.isLocked() && !ChildLock.canCoverApps(this)) {
            startActivity(new Intent(this, ChildLockActivity.class));
        }
    }

    // ------------------------------------------------------------------ date and weather

    private void refreshDate() {
        Calendar now = Calendar.getInstance();
        int y = now.get(Calendar.YEAR), m = now.get(Calendar.MONTH) + 1, d = now.get(Calendar.DAY_OF_MONTH);
        String key = y + "-" + m + "-" + d;
        if (key.equals(shownDate)) return;
        shownDate = key;
        weekday.setText(LunarCalendar.weekdayLong(now.get(Calendar.DAY_OF_WEEK)));
        date.setText(d + " tháng " + m + ", " + y);
        LunarCalendar.LunarDate l = LunarCalendar.fromSolar(d, m, y);
        lunar.setText("Âm lịch " + l.day + "/" + l.month + (l.leap ? " nhuận" : "")
                + " · năm " + LunarCalendar.yearName(l.year));
        calendar.setDate(y, m, d);
        weather.invalidate(); // "Hôm nay" moves with the date
    }

    private void refreshWeather(boolean force) {
        if (!prefs.hasPlace()) {
            weather.showNoPlace();
            return;
        }
        final String place = prefs.placeName();
        final long savedAt = prefs.weatherTime();
        Weather cached = null;
        String json = prefs.weatherJson();
        if (json != null) {
            try {
                cached = Weather.parse(json, savedAt);
            } catch (Exception ignored) {
                // A corrupt cache is simply refetched.
            }
        }
        if (cached != null) weather.show(cached, place);
        else weather.showLoading(place);
        if (!force && cached != null && System.currentTimeMillis() - savedAt < WEATHER_EVERY_MS - 60_000) return;

        final boolean hasCache = cached != null;
        final double lat = prefs.lat(), lon = prefs.lon();
        final int request = ++weatherRequest;
        io.execute(() -> {
            try {
                String fresh = Weather.fetch(lat, lon);
                long at = System.currentTimeMillis();
                Weather w = Weather.parse(fresh, at);
                handler.post(() -> {
                    if (request != weatherRequest || isDestroyed()) return;
                    prefs.saveWeather(fresh, at);
                    weather.show(w, place);
                });
            } catch (Exception e) {
                String why = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                handler.post(() -> {
                    if (request != weatherRequest || isDestroyed()) return;
                    if (hasCache) weather.markOffline();
                    else weather.showError(place, why);
                    if (resumed) {
                        handler.removeCallbacks(weatherTick);
                        handler.postDelayed(weatherTick, WEATHER_RETRY_MS);
                    }
                });
            }
        });
    }

    private void openSettings(String action) {
        try {
            startActivity(new Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException e) {
            if (!Settings.ACTION_SETTINGS.equals(action)) {
                openSettings(Settings.ACTION_SETTINGS);
            } else {
                Toast.makeText(this, "Không mở được Cài đặt.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
