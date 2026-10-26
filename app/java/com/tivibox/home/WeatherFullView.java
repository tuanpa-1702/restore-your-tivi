package com.tivibox.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

import java.util.Calendar;
import java.util.List;

/**
 * Full weather screen, drawn on the 960x540 dp stage: current conditions on the left, the next hours top right,
 * seven days bottom right.
 */
public class WeatherFullView extends View {
    private final float dp;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Typeface light = Typeface.create("sans-serif-light", Typeface.NORMAL);
    private final Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private final Typeface bold = Typeface.create("sans-serif-medium", Typeface.BOLD);
    private Weather data;
    private String place = "", status = "";

    public WeatherFullView(Context context) {
        super(context);
        dp = context.getResources().getDisplayMetrics().density;
    }

    void show(Weather w, String place, String status) {
        this.data = w;
        this.place = place;
        this.status = status;
        invalidate();
    }

    private float u(float v) {
        return v * dp;
    }

    @Override
    protected void onDraw(Canvas c) {
        // Header
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(bold);
        paint.setTextSize(u(26));
        paint.setColor(0xFFFFFFFF);
        c.drawText("Thời tiết", u(40), u(56), paint);
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(u(12));
        paint.setColor(0xA6FFFFFF);
        String sub = place.isEmpty() ? "Chưa chọn địa điểm" : place;
        if (data != null) sub += " · cập nhật " + clock(data.fetchedAt);
        if (!status.isEmpty()) sub += " · " + status;
        c.drawText(sub, u(40), u(76), paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        c.drawText("OK để làm mới · BACK để thoát", u(920), u(76), paint);

        if (data == null) {
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(medium);
            paint.setTextSize(u(18));
            paint.setColor(0xFFFFFFFF);
            c.drawText(place.isEmpty() ? "Hãy chọn địa điểm trong Cài đặt giao diện" : "Đang tải thời tiết…",
                    u(480), u(300), paint);
            return;
        }
        drawCurrent(c);
        drawHours(c);
        drawDays(c);
    }

    private void card(Canvas c, float x0, float y0, float x1, float y1) {
        Glass.panel(c, paint, rect, u(x0), u(y0), u(x1), u(y1), u(18), dp, 0x1CFFFFFF, 0x33FFFFFF);
    }

    private void drawCurrent(Canvas c) {
        card(c, 40, 96, 380, 516);
        Weather w = data;
        Glyphs.draw(c, Weather.glyph(w.code, w.day), u(112), u(168), u(104), 0xFFFFFFFF);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(light);
        paint.setTextSize(u(78));
        paint.setColor(0xFFFFFFFF);
        c.drawText(Math.round(w.temp) + "°", u(184), u(198), paint);
        paint.setTypeface(medium);
        paint.setTextSize(u(21));
        c.drawText(Weather.describe(w.code), u(62), u(256), paint);
        Weather.Day today = w.dayFor(WeatherCardView.isoDate(Calendar.getInstance()));
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(u(14));
        paint.setColor(0xCCFFFFFF);
        if (today != null) c.drawText("Cao " + deg(today.max) + "  ·  Thấp " + deg(today.min), u(62), u(280), paint);
        paint.setColor(0x24FFFFFF);
        c.drawRect(u(62), u(298), u(358), u(299), paint);

        String[][] rows = {
                {"Cảm giác như", deg(w.feels)},
                {"Độ ẩm", w.humidity >= 0 ? w.humidity + "%" : "—"},
                {"Gió", Double.isNaN(w.wind) ? "—" : Math.round(w.wind) + " km/h"},
                {"Khả năng mưa", today != null && today.rain >= 0 ? today.rain + "%" : "—"},
                {"Chỉ số UV", today != null && !Double.isNaN(today.uv) ? uvText(today.uv) : "—"},
                {"Mặt trời mọc / lặn", today != null && !today.sunrise.isEmpty() ? today.sunrise + " / " + today.sunset : "—"},
        };
        float y = u(328);
        for (String[] r : rows) {
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setTypeface(Typeface.DEFAULT);
            paint.setTextSize(u(13));
            paint.setColor(0x99FFFFFF);
            c.drawText(r[0], u(62), y, paint);
            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setTypeface(medium);
            paint.setTextSize(u(14));
            paint.setColor(0xFFFFFFFF);
            c.drawText(r[1], u(358), y, paint);
            y += u(30);
        }
    }

    private void drawHours(Canvas c) {
        card(c, 394, 96, 920, 270);
        title(c, "Theo giờ", 414, 122);
        List<Weather.Hour> all = data.upcoming(16);
        if (all.isEmpty()) {
            note(c, "Chưa có dự báo theo giờ — bấm OK để tải", 657, 190);
            return;
        }
        int n = Math.min(8, (all.size() + 1) / 2);
        float colW = (920 - 394 - 32) / 8f;
        for (int i = 0; i < n; i++) {
            Weather.Hour h = all.get(i * 2);
            float cx = 410 + colW * i + colW / 2;
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(medium);
            paint.setTextSize(u(12));
            paint.setColor(0xB3FFFFFF);
            c.drawText(i == 0 ? "Bây giờ" : hourLabel(h.time), u(cx), u(150), paint);
            Glyphs.draw(c, Weather.glyph(h.code, h.day), u(cx), u(182), u(34), 0xFFFFFFFF);
            paint.setTypeface(Typeface.DEFAULT);
            paint.setTextSize(u(16));
            paint.setColor(0xFFFFFFFF);
            c.drawText(deg(h.temp), u(cx), u(224), paint);
            if (h.rain > 0) {
                paint.setTextSize(u(11));
                paint.setColor(0xFF8CCBFF);
                c.drawText(h.rain + "%", u(cx), u(246), paint);
            }
        }
    }

    private void drawDays(Canvas c) {
        card(c, 394, 284, 920, 516);
        title(c, "7 ngày tới", 414, 310);
        List<Weather.Day> days = data.days;
        String todayIso = WeatherCardView.isoDate(Calendar.getInstance());
        int start = 0;
        while (start < days.size() && days.get(start).date.compareTo(todayIso) < 0) start++;
        double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;
        for (int i = start; i < days.size(); i++) {
            if (!Double.isNaN(days.get(i).min)) lo = Math.min(lo, days.get(i).min);
            if (!Double.isNaN(days.get(i).max)) hi = Math.max(hi, days.get(i).max);
        }
        float rowH = 25.5f, y = 328;
        for (int i = start; i < days.size() && i < start + 7; i++, y += rowH) {
            Weather.Day d = days.get(i);
            float cy = y + rowH / 2 - 4;
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setTypeface(i == start ? bold : medium);
            paint.setTextSize(u(13));
            paint.setColor(0xFFFFFFFF);
            c.drawText(dayLabel(d.date, i == start), u(414), u(cy + 5), paint);
            Glyphs.draw(c, Weather.glyph(d.code, true), u(540), u(cy), u(24), 0xFFFFFFFF);
            if (d.rain >= 0) {
                paint.setTypeface(Typeface.DEFAULT);
                paint.setTextSize(u(11));
                paint.setColor(d.rain >= 30 ? 0xFF8CCBFF : 0x66FFFFFF);
                c.drawText(d.rain + "%", u(562), u(cy + 4), paint);
            }
            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setTextSize(u(13));
            paint.setColor(0x99FFFFFF);
            c.drawText(deg(d.min), u(640), u(cy + 5), paint);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setColor(0xFFFFFFFF);
            c.drawText(deg(d.max), u(866), u(cy + 5), paint);
            // Range bar on a scale shared by the whole week.
            float bx0 = 652, bx1 = 856;
            paint.setColor(0x26FFFFFF);
            rect.set(u(bx0), u(cy - 2.5f), u(bx1), u(cy + 2.5f));
            c.drawRoundRect(rect, u(3), u(3), paint);
            if (hi > lo && !Double.isNaN(d.min) && !Double.isNaN(d.max)) {
                float a = (float) (bx0 + (bx1 - bx0) * (d.min - lo) / (hi - lo));
                float b = (float) (bx0 + (bx1 - bx0) * (d.max - lo) / (hi - lo));
                paint.setColor(0xFFFFFFFF); // the shader takes its alpha from the paint colour
                paint.setShader(new LinearGradient(u(a), 0, u(Math.max(b, a + 6)), 0, 0xFF6CB8FF, 0xFFFFB74D, Shader.TileMode.CLAMP));
                rect.set(u(a), u(cy - 2.5f), u(Math.max(b, a + 6)), u(cy + 2.5f));
                c.drawRoundRect(rect, u(3), u(3), paint);
                paint.setShader(null);
            }
        }
    }

    private void title(Canvas c, String text, float x, float y) {
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(medium);
        paint.setTextSize(u(15));
        paint.setColor(0xFFFFFFFF);
        c.drawText(text, u(x), u(y), paint);
    }

    private void note(Canvas c, String text, float cx, float y) {
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(u(13));
        paint.setColor(0x99FFFFFF);
        c.drawText(text, u(cx), u(y), paint);
    }

    private static String deg(double v) {
        return Double.isNaN(v) ? "—" : Math.round(v) + "°";
    }

    private static String uvText(double uv) {
        long v = Math.round(uv);
        String level = v <= 2 ? "thấp" : v <= 5 ? "trung bình" : v <= 7 ? "cao" : v <= 10 ? "rất cao" : "nguy hiểm";
        return v + " · " + level;
    }

    private static String hourLabel(String iso) {
        int t = iso.indexOf('T');
        if (t < 0 || iso.length() < t + 3) return iso;
        return Integer.parseInt(iso.substring(t + 1, t + 3)) + "h";
    }

    private static String dayLabel(String iso, boolean today) {
        if (today) return "Hôm nay";
        try {
            Calendar c = Calendar.getInstance();
            c.clear();
            int y = Integer.parseInt(iso.substring(0, 4)), m = Integer.parseInt(iso.substring(5, 7)),
                    d = Integer.parseInt(iso.substring(8, 10));
            c.set(y, m - 1, d);
            return LunarCalendar.weekdayShort(c.get(Calendar.DAY_OF_WEEK)) + "  " + d + "/" + m;
        } catch (RuntimeException e) {
            return iso;
        }
    }

    private static String clock(long at) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(at);
        return String.format(java.util.Locale.US, "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }
}
