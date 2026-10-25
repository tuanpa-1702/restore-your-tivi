package com.tivibox.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Current weather, today's range and a four-day strip. Shows a message instead while there is no data. */
public class WeatherCardView extends View {
    private final float dp;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private final Typeface regular = Typeface.create("sans-serif", Typeface.NORMAL);
    private final Typeface light = Typeface.create("sans-serif-light", Typeface.NORMAL);
    private Weather data;
    private String place = "";
    private String message = "", detail = "";
    private boolean offline;

    public WeatherCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        dp = context.getResources().getDisplayMetrics().density;
    }

    public void showNoPlace() {
        showMessage("", "Chưa chọn địa điểm",
                "Bấm nút bảng màu ở góc trên bên phải và nhập thành phố hoặc quận/huyện.");
    }

    public void showLoading(String place) {
        showMessage(place, "Đang tải thời tiết…", place);
    }

    public void showError(String place, String why) {
        showMessage(place, "Chưa tải được thời tiết", "Kiểm tra mạng của TV. Tự thử lại sau 2 phút. (" + why + ")");
    }

    private void showMessage(String place, String message, String detail) {
        this.data = null;
        this.place = place;
        this.message = message;
        this.detail = detail;
        invalidate();
    }

    public void show(Weather w, String place) {
        this.data = w;
        this.place = place;
        this.offline = false;
        invalidate();
    }

    public void markOffline() {
        offline = true;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight(), pad = 16 * dp;
        Glass.card(canvas, paint, rect, w, h, dp);

        float badge = 26 * dp;
        Glass.badge(canvas, paint, rect, pad, pad, badge, dp);
        int glyph = data == null ? Glyphs.SUN_CLOUD : Weather.glyph(data.code, data.day);
        Glyphs.draw(canvas, glyph, pad + badge / 2, pad + badge / 2, badge * .8f, 0xFFFFFFFF);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(medium);
        paint.setTextSize(15 * dp);
        paint.setColor(0xFFFFFFFF);
        canvas.drawText("Thời tiết", pad + badge + 10 * dp, Glass.baseline(paint, pad + badge / 2), paint);

        if (data == null) {
            drawMessage(canvas, w, h);
            return;
        }

        paint.setTypeface(regular);
        paint.setTextSize(9.5f * dp);
        paint.setColor(0xA6FFFFFF);
        String sub = place + " · cập nhật " + updatedText(data.fetchedAt) + (offline ? " · mất kết nối" : "");
        canvas.drawText(ellipsize(sub, w - 2 * pad), pad, pad + badge + 15 * dp, paint);

        String todayIso = isoDate(Calendar.getInstance());
        Weather.Day today = data.dayFor(todayIso);

        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTypeface(light);
        paint.setTextSize(54 * dp);
        paint.setColor(0xFFFFFFFF);
        String temp = Math.round(data.temp) + "°";
        canvas.drawText(temp, w - pad + 2 * dp, 128 * dp, paint);
        float textMax = w - 2 * pad - paint.measureText(temp) - 12 * dp;

        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(medium);
        paint.setTextSize(16 * dp);
        canvas.drawText(ellipsize(Weather.describe(data.code), textMax), pad, 92 * dp, paint);
        paint.setTypeface(regular);
        paint.setTextSize(10 * dp);
        paint.setColor(0xB3FFFFFF);
        StringBuilder line2 = new StringBuilder("Cảm giác ").append(Math.round(data.feels)).append("°");
        if (data.humidity >= 0) line2.append(" · Độ ẩm ").append(data.humidity).append("%");
        canvas.drawText(ellipsize(line2.toString(), textMax), pad, 110 * dp, paint);
        if (today != null) {
            StringBuilder line3 = new StringBuilder("Cao ").append(deg(today.max)).append(" · Thấp ").append(deg(today.min));
            if (today.rain >= 0) line3.append(" · Mưa ").append(today.rain).append("%");
            canvas.drawText(ellipsize(line3.toString(), textMax), pad, 125 * dp, paint);
        }

        float lineY = 140 * dp;
        paint.setColor(0x24FFFFFF);
        paint.setStrokeWidth(dp);
        canvas.drawLine(pad, lineY, w - pad, lineY, paint);

        List<Weather.Day> days = data.days;
        int start = 0;
        while (start < days.size() && days.get(start).date.compareTo(todayIso) < 0) start++;
        float colW = (w - 2 * pad) / 4f;
        for (int i = 0; i < 4 && start + i < days.size(); i++) {
            Weather.Day d = days.get(start + i);
            float x = pad + i * colW;
            paint.setTypeface(medium);
            paint.setTextSize(9 * dp);
            paint.setColor(0xA6FFFFFF);
            canvas.drawText(dayLabel(d.date, todayIso), x, lineY + 18 * dp, paint);
            Glyphs.draw(canvas, Weather.glyph(d.code, true), x + 9 * dp, lineY + 34 * dp, 20 * dp, 0xFFFFFFFF);
            paint.setTypeface(regular);
            paint.setTextSize(10.5f * dp);
            paint.setColor(0xFFFFFFFF);
            String hi = deg(d.max);
            canvas.drawText(hi, x + 23 * dp, lineY + 38 * dp, paint);
            paint.setColor(0x8CFFFFFF);
            canvas.drawText(" " + deg(d.min), x + 23 * dp + paint.measureText(hi), lineY + 38 * dp, paint);
        }
    }

    private void drawMessage(Canvas canvas, float w, float h) {
        float pad = 16 * dp;
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(medium);
        paint.setTextSize(15 * dp);
        paint.setColor(0xFFFFFFFF);
        canvas.drawText(message, w / 2, h * .5f, paint);
        if (detail.isEmpty()) return;
        textPaint.setTypeface(regular);
        textPaint.setTextSize(10.5f * dp);
        textPaint.setColor(0xB3FFFFFF);
        int width = (int) Math.max(1, w - 4 * pad);
        StaticLayout layout = StaticLayout.Builder.obtain(detail, 0, detail.length(), textPaint, width)
                .setAlignment(Layout.Alignment.ALIGN_CENTER).setMaxLines(3).build();
        canvas.save();
        canvas.translate(2 * pad, h * .5f + 12 * dp);
        layout.draw(canvas);
        canvas.restore();
    }

    private String ellipsize(String text, float max) {
        if (paint.measureText(text) <= max) return text;
        String t = text;
        while (t.length() > 1 && paint.measureText(t + "…") > max) t = t.substring(0, t.length() - 1);
        return t.trim() + "…";
    }

    private static String deg(double v) {
        return Double.isNaN(v) ? "—" : Math.round(v) + "°";
    }

    private static String updatedText(long at) {
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(at);
        boolean sameDay = isoDate(then).equals(isoDate(Calendar.getInstance()));
        return new SimpleDateFormat(sameDay ? "HH:mm" : "HH:mm dd/MM", Locale.US).format(new Date(at));
    }

    static String isoDate(Calendar c) {
        return String.format(Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    private static String dayLabel(String iso, String todayIso) {
        if (iso.equals(todayIso)) return "Hôm nay";
        try {
            Calendar c = Calendar.getInstance();
            c.clear();
            c.set(Integer.parseInt(iso.substring(0, 4)), Integer.parseInt(iso.substring(5, 7)) - 1,
                    Integer.parseInt(iso.substring(8, 10)));
            return LunarCalendar.weekdayShort(c.get(Calendar.DAY_OF_WEEK));
        } catch (RuntimeException e) {
            return iso;
        }
    }
}
