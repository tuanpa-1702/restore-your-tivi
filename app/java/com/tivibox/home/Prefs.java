package com.tivibox.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Everything the launcher remembers: greeting, theme, dock, app order, weather place and the last forecast. */
final class Prefs {
    static final String DEFAULT_GREETING = "Xin chào Thỏ Miu!";

    private final SharedPreferences sp;

    Prefs(Context context) {
        sp = context.getSharedPreferences("home", Context.MODE_PRIVATE);
    }

    String greeting() {
        return sp.getString("greeting", DEFAULT_GREETING);
    }

    void setGreeting(String greeting) {
        sp.edit().putString("greeting", greeting).apply();
    }

    int theme() {
        return sp.getInt("theme", 0);
    }

    void setTheme(int theme) {
        sp.edit().putInt("theme", theme).apply();
    }

    /** Saved dock entries, or null when the user never changed the dock. */
    List<String> dock() {
        String s = sp.getString("dock", null);
        return s == null ? null : split(s);
    }

    void setDock(List<String> dock) {
        sp.edit().putString("dock", TextUtils.join(",", dock)).apply();
    }

    List<String> appsOrder() {
        return split(sp.getString("apps_order", ""));
    }

    void setAppsOrder(List<String> order) {
        sp.edit().putString("apps_order", TextUtils.join(",", order)).apply();
    }

    void resetOrder() {
        sp.edit().remove("dock").remove("apps_order").apply();
    }

    /** When the child-lock timer runs out (epoch ms), 0 when no timer is set. */
    long lockDeadline() {
        return sp.getLong("lock_deadline", 0);
    }

    void setLockDeadline(long at) {
        sp.edit().putLong("lock_deadline", at).apply();
    }

    boolean isLocked() {
        return sp.getBoolean("locked", false);
    }

    void setLocked(boolean locked) {
        sp.edit().putBoolean("locked", locked).apply();
    }

    /** The parent's own timer length, offered next to the presets. */
    int customLockMinutes() {
        return sp.getInt("lock_custom_minutes", 45);
    }

    void setCustomLockMinutes(int minutes) {
        sp.edit().putInt("lock_custom_minutes", minutes).apply();
    }

    void clearLock() {
        sp.edit().remove("lock_deadline").remove("locked").apply();
    }

    boolean hasPlace() {
        return sp.contains("lat") && sp.contains("lon");
    }

    String placeQuery() {
        return sp.getString("place_query", "");
    }

    String placeName() {
        return sp.getString("place_name", "");
    }

    double lat() {
        return Double.longBitsToDouble(sp.getLong("lat", 0));
    }

    double lon() {
        return Double.longBitsToDouble(sp.getLong("lon", 0));
    }

    void setPlace(String query, Weather.Place place) {
        sp.edit()
                .putString("place_query", query)
                .putString("place_name", place.name)
                .putLong("lat", Double.doubleToRawLongBits(place.lat))
                .putLong("lon", Double.doubleToRawLongBits(place.lon))
                .remove("weather_json")
                .remove("weather_time")
                .apply();
    }

    String weatherJson() {
        return sp.getString("weather_json", null);
    }

    long weatherTime() {
        return sp.getLong("weather_time", 0);
    }

    void saveWeather(String json, long at) {
        sp.edit().putString("weather_json", json).putLong("weather_time", at).apply();
    }

    private static List<String> split(String s) {
        List<String> out = new ArrayList<>();
        if (s.isEmpty()) return out;
        for (String part : Arrays.asList(s.split(","))) if (!part.isEmpty()) out.add(part);
        return out;
    }
}
