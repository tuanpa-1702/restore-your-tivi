package com.tivibox.home;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Open-Meteo forecast and geocoding (free, no API key). Network calls must run off the UI thread. */
final class Weather {
    private static final String FORECAST = "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f"
            + "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m,is_day"
            + "&hourly=temperature_2m,weather_code,precipitation_probability,is_day"
            + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,"
            + "sunrise,sunset,uv_index_max,wind_speed_10m_max"
            + "&timezone=auto&forecast_days=7";
    private static final String GEOCODE = "https://geocoding-api.open-meteo.com/v1/search?count=8&language=vi&format=json&name=";

    static final class Day {
        String date; // yyyy-MM-dd
        int code;
        double max, min, uv = Double.NaN, windMax = Double.NaN;
        int rain; // max precipitation probability, -1 when unknown
        String sunrise = "", sunset = ""; // HH:mm local time
    }

    static final class Hour {
        String time; // yyyy-MM-ddTHH:mm
        double temp;
        int code, rain;
        boolean day;
    }

    static final class Place {
        final String name, label;
        final double lat, lon;

        Place(String name, String label, double lat, double lon) {
            this.name = name;
            this.label = label;
            this.lat = lat;
            this.lon = lon;
        }
    }

    double temp, feels, wind;
    int humidity, code;
    boolean day;
    long fetchedAt;
    final List<Day> days = new ArrayList<>();
    /** Hourly forecast; empty for caches saved by older versions. */
    final List<Hour> hours = new ArrayList<>();
    String currentTime = ""; // yyyy-MM-ddTHH:mm of the "current" block

    Day dayFor(String isoDate) {
        for (Day d : days) if (d.date.equals(isoDate)) return d;
        return null;
    }

    static String fetch(double lat, double lon) throws IOException {
        return get(String.format(Locale.US, FORECAST, lat, lon));
    }

    static Weather parse(String json, long fetchedAt) throws JSONException {
        JSONObject o = new JSONObject(json);
        JSONObject cur = o.getJSONObject("current");
        Weather w = new Weather();
        w.fetchedAt = fetchedAt;
        w.temp = cur.getDouble("temperature_2m");
        w.feels = cur.optDouble("apparent_temperature", w.temp);
        w.humidity = cur.optInt("relative_humidity_2m", -1);
        w.wind = cur.optDouble("wind_speed_10m", Double.NaN);
        w.code = cur.optInt("weather_code", 3);
        w.day = cur.optInt("is_day", 1) == 1;
        JSONObject daily = o.optJSONObject("daily");
        if (daily != null) {
            JSONArray time = daily.getJSONArray("time");
            JSONArray codes = daily.optJSONArray("weather_code");
            JSONArray max = daily.optJSONArray("temperature_2m_max");
            JSONArray min = daily.optJSONArray("temperature_2m_min");
            JSONArray rain = daily.optJSONArray("precipitation_probability_max");
            JSONArray sunrise = daily.optJSONArray("sunrise"), sunset = daily.optJSONArray("sunset");
            JSONArray uv = daily.optJSONArray("uv_index_max"), wind = daily.optJSONArray("wind_speed_10m_max");
            for (int i = 0; i < time.length(); i++) {
                Day d = new Day();
                d.date = time.getString(i);
                d.code = codes == null ? 3 : codes.optInt(i, 3);
                d.max = max == null ? Double.NaN : max.optDouble(i, Double.NaN);
                d.min = min == null ? Double.NaN : min.optDouble(i, Double.NaN);
                d.rain = rain == null ? -1 : rain.optInt(i, -1);
                d.sunrise = clock(sunrise == null ? "" : sunrise.optString(i, ""));
                d.sunset = clock(sunset == null ? "" : sunset.optString(i, ""));
                d.uv = uv == null ? Double.NaN : uv.optDouble(i, Double.NaN);
                d.windMax = wind == null ? Double.NaN : wind.optDouble(i, Double.NaN);
                w.days.add(d);
            }
        }
        w.currentTime = cur.optString("time", "");
        JSONObject hourly = o.optJSONObject("hourly");
        if (hourly != null) {
            JSONArray time = hourly.getJSONArray("time");
            JSONArray temp = hourly.optJSONArray("temperature_2m"), codes = hourly.optJSONArray("weather_code");
            JSONArray rain = hourly.optJSONArray("precipitation_probability"), isDay = hourly.optJSONArray("is_day");
            for (int i = 0; i < time.length(); i++) {
                Hour h = new Hour();
                h.time = time.getString(i);
                h.temp = temp == null ? Double.NaN : temp.optDouble(i, Double.NaN);
                h.code = codes == null ? 3 : codes.optInt(i, 3);
                h.rain = rain == null ? -1 : rain.optInt(i, -1);
                h.day = isDay == null || isDay.optInt(i, 1) == 1;
                w.hours.add(h);
            }
        }
        return w;
    }

    /** Vietnam first (as typed, then without accents), then worldwide. */
    static List<Place> search(String query) throws IOException, JSONException {
        List<Place> out = searchOnce(query, true);
        String plain = stripAccents(query);
        if (out.isEmpty() && !plain.equals(query)) out = searchOnce(plain, true);
        if (out.isEmpty()) out = searchOnce(query, false);
        return out;
    }

    private static List<Place> searchOnce(String q, boolean vietnamOnly) throws IOException, JSONException {
        String url = GEOCODE + URLEncoder.encode(q, "UTF-8") + (vietnamOnly ? "&countryCode=VN" : "");
        JSONArray results = new JSONObject(get(url)).optJSONArray("results");
        List<Place> out = new ArrayList<>();
        if (results == null) return out;
        for (int i = 0; i < results.length(); i++) {
            JSONObject r = results.getJSONObject(i);
            String name = str(r, "name");
            if (name.isEmpty()) continue;
            StringBuilder label = new StringBuilder(name);
            String admin2 = str(r, "admin2"), admin1 = str(r, "admin1"), country = str(r, "country");
            if (!admin2.isEmpty() && !admin2.equals(name)) label.append(", ").append(admin2);
            if (!admin1.isEmpty() && !admin1.equals(name)) label.append(", ").append(admin1);
            if (!vietnamOnly && !country.isEmpty()) label.append(", ").append(country);
            out.add(new Place(name, label.toString(), r.getDouble("latitude"), r.getDouble("longitude")));
        }
        return out;
    }

    static String describe(int code) {
        switch (code) {
            case 0: return "Trời quang";
            case 1: return "Ít mây";
            case 2: return "Có mây";
            case 3: return "Nhiều mây";
            case 45: case 48: return "Sương mù";
            case 51: return "Mưa phùn nhẹ";
            case 53: return "Mưa phùn";
            case 55: return "Mưa phùn dày";
            case 56: case 57: return "Mưa phùn lạnh";
            case 61: return "Mưa nhỏ";
            case 63: return "Mưa vừa";
            case 65: return "Mưa to";
            case 66: case 67: return "Mưa lạnh";
            case 71: case 73: case 75: case 77: return "Tuyết";
            case 80: return "Mưa rào nhẹ";
            case 81: return "Mưa rào";
            case 82: return "Mưa rào rất to";
            case 85: case 86: return "Mưa tuyết";
            case 95: return "Dông";
            case 96: case 99: return "Dông, mưa đá";
            default: return "—";
        }
    }

    static int glyph(int code, boolean day) {
        switch (code) {
            case 0: return day ? Glyphs.SUN : Glyphs.MOON;
            case 1: case 2: return day ? Glyphs.SUN_CLOUD : Glyphs.MOON_CLOUD;
            case 45: case 48: return Glyphs.FOG;
            case 51: case 53: case 55: case 56: case 57: return Glyphs.DRIZZLE;
            case 61: case 63: case 65: case 66: case 67: case 80: case 81: case 82: return Glyphs.RAIN;
            case 71: case 73: case 75: case 77: case 85: case 86: return Glyphs.SNOW;
            case 95: case 96: case 99: return Glyphs.THUNDER;
            default: return Glyphs.CLOUD;
        }
    }

    /** "2026-10-26T05:48" → "05:48". */
    private static String clock(String iso) {
        int t = iso.indexOf('T');
        return t >= 0 && iso.length() >= t + 6 ? iso.substring(t + 1, t + 6) : "";
    }

    /** Hours from the current one onwards (at most `count`). */
    List<Hour> upcoming(int count) {
        List<Hour> out = new ArrayList<>();
        String from = currentTime.length() >= 13 ? currentTime.substring(0, 13) : "";
        for (Hour h : hours) {
            if (out.size() >= count) break;
            if (from.isEmpty() || h.time.compareTo(from) >= 0) out.add(h);
        }
        return out;
    }

    static String stripAccents(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return n.replace('đ', 'd').replace('Đ', 'D');
    }

    private static String str(JSONObject o, String key) {
        return o.isNull(key) ? "" : o.optString(key, "").trim();
    }

    private static String get(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(10000);
        c.setReadTimeout(10000);
        c.setRequestProperty("User-Agent", "TiviHome/1.0");
        try {
            int status = c.getResponseCode();
            if (status != 200) throw new IOException("HTTP " + status);
            try (InputStream in = c.getInputStream()) {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                byte[] chunk = new byte[8192];
                int n;
                while ((n = in.read(chunk)) > 0) buf.write(chunk, 0, n);
                return new String(buf.toByteArray(), StandardCharsets.UTF_8);
            }
        } finally {
            c.disconnect();
        }
    }
}
