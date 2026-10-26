package com.tivibox.home;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.widget.FrameLayout;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Full weather screen. Shows the cached forecast at once and refreshes it when old, incomplete or on OK. */
public class WeatherActivity extends Activity {
    private static final long FRESH_MS = 30 * 60 * 1000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private Prefs prefs;
    private WeatherFullView view;
    private boolean loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);
        float dp = getResources().getDisplayMetrics().density;
        FrameLayout root = new FrameLayout(this);
        MeshBackgroundView bg = new MeshBackgroundView(this, null);
        bg.setTheme(prefs.theme());
        root.addView(bg, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        view = new WeatherFullView(this);
        root.addView(view, new FrameLayout.LayoutParams(Math.round(960 * dp), Math.round(540 * dp)));
        setContentView(root);
        Stage.fitOnLayout(root, view);

        Weather cached = cached();
        view.show(cached, prefs.placeName(), "");
        if (prefs.hasPlace() && (cached == null || cached.hours.isEmpty()
                || System.currentTimeMillis() - prefs.weatherTime() > FRESH_MS)) {
            refresh();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdownNow();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            refresh();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private Weather cached() {
        String json = prefs.weatherJson();
        if (json == null) return null;
        try {
            return Weather.parse(json, prefs.weatherTime());
        } catch (Exception e) {
            return null;
        }
    }

    private void refresh() {
        if (loading || !prefs.hasPlace()) return;
        loading = true;
        view.show(cached(), prefs.placeName(), "đang làm mới…");
        final double lat = prefs.lat(), lon = prefs.lon();
        io.execute(() -> {
            try {
                String json = Weather.fetch(lat, lon);
                long at = System.currentTimeMillis();
                Weather w = Weather.parse(json, at);
                handler.post(() -> {
                    loading = false;
                    if (isDestroyed()) return;
                    prefs.saveWeather(json, at);
                    view.show(w, prefs.placeName(), "");
                });
            } catch (Exception e) {
                handler.post(() -> {
                    loading = false;
                    if (isDestroyed()) return;
                    view.show(cached(), prefs.placeName(), "không tải được, kiểm tra mạng");
                });
            }
        });
    }
}
