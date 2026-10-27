package com.tivibox.home;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * The screensaver, shared by the system dream (TiviDream) and the preview. Drawn on the 960x540 stage scaled to
 * the screen. Content drifts a little every minute so nothing burns into the panel.
 */
public class ScreensaverView extends View {
    static final String[] NAMES = {"Đồng hồ kính", "Lịch âm dương", "Thời tiết", "Đêm tối giản", "Ảnh nền", "Tuỳ chỉnh"};
    static final String[] DESCRIPTIONS = {
            "Đồng hồ lớn, ngày dương và âm lịch trên thẻ kính",
            "Ngày âm, Can Chi, giờ hoàng đạo và ngày lễ sắp tới",
            "Đồng hồ cùng thời tiết hiện tại và 4 ngày tới",
            "Nền đen, đồng hồ mờ di chuyển chậm — dịu mắt ban đêm",
            "Ảnh nền màn hình chính, không làm mờ, kèm đồng hồ",
            "Trình chiếu ảnh bạn chọn, tuỳ thời gian mỗi ảnh và đồng hồ",
    };
    static final int CUSTOM = 5;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF rect = new RectF();
    private final Random random = new Random();
    private final Typeface thin = Typeface.create("sans-serif-thin", Typeface.NORMAL);
    private final Typeface light = Typeface.create("sans-serif-light", Typeface.NORMAL);
    private final Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private final Typeface bold = Typeface.create("sans-serif-medium", Typeface.BOLD);
    private final Prefs prefs;
    private final int template;
    private Bitmap background, slide, previousSlide;
    private float fade = 1f, dx, dy;
    private int slideIndex, lastMinute = -1;
    private long nextSlideAt;
    private String dateKey = "", nextHoliday = "";
    private Weather weather;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            Calendar now = Calendar.getInstance();
            int minute = now.get(Calendar.MINUTE);
            if (minute != lastMinute) {
                lastMinute = minute;
                drift();
                invalidate();
            }
            if (template == CUSTOM && System.currentTimeMillis() >= nextSlideAt) nextSlide();
            handler.postDelayed(this, 1000);
        }
    };

    public ScreensaverView(Context context, int template) {
        super(context);
        prefs = new Prefs(context);
        this.template = Math.max(0, Math.min(CUSTOM, template));
        setKeepScreenOn(true);
        String json = prefs.weatherJson();
        if (json != null) {
            try {
                weather = Weather.parse(json, prefs.weatherTime());
            } catch (Exception ignored) {
                weather = null;
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        handler.post(tick);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        background = null;
    }

    private void drift() {
        // Night mode wanders across the whole screen; the others move a few dp.
        dx = (random.nextFloat() * 2 - 1) * (template == 3 ? 300 : 24);
        dy = (random.nextFloat() * 2 - 1) * (template == 3 ? 170 : 16);
    }

    private void nextSlide() {
        List<File> files = Photos.slides(getContext());
        nextSlideAt = System.currentTimeMillis() + Math.max(5, prefs.saverInterval()) * 1000L;
        if (files.isEmpty() || getWidth() == 0) return;
        File f = files.get(slideIndex % files.size());
        slideIndex++;
        Bitmap b = Photos.sharp(f, getWidth(), getHeight(), true); // photo-frame style: whole photo, never cropped
        if (b == null) return;
        if (previousSlide != null) previousSlide.recycle();
        previousSlide = slide;
        slide = b;
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f).setDuration(1200);
        a.addUpdateListener(v -> {
            fade = (float) v.getAnimatedValue();
            invalidate();
        });
        a.start();
    }

    // ------------------------------------------------------------------ drawing

    private float u, ox, oy; // stage unit and stage origin on screen

    private float x(float v) {
        return ox + v * u;
    }

    private float y(float v) {
        return oy + v * u;
    }

    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight();
        if (w == 0) return;
        u = Math.min(w / 960f, h / 540f);
        ox = (w - 960 * u) / 2;
        oy = (h - 540 * u) / 2;
        drawBackground(c, w, h);
        Calendar now = Calendar.getInstance();
        switch (template) {
            case 0: drawClockCard(c, now); break;
            case 1: drawLunar(c, now); break;
            case 2: drawWeather(c, now); break;
            case 3: drawNight(c, now); break;
            case 4: drawCornerClock(c, now, true); break;
            default:
                if (slide == null && Photos.slides(getContext()).isEmpty()) {
                    centered(c, "Chưa có ảnh cho màn hình chờ", 22, 0xFFFFFFFF, medium, 260);
                    centered(c, "Vào Cài đặt giao diện → Màn hình chờ → Ảnh trình chiếu để chọn ảnh", 14, 0xB3FFFFFF,
                            Typeface.DEFAULT, 292);
                } else if (prefs.saverClock()) {
                    drawCornerClock(c, now, false);
                }
        }
    }

    private void drawBackground(Canvas c, int w, int h) {
        if (template == 3) {
            c.drawColor(0xFF000000);
            return;
        }
        if (template == CUSTOM && slide != null) {
            c.drawColor(0xFF000000);
            if (previousSlide != null && fade < 1f) c.drawBitmap(previousSlide, 0, 0, paint);
            paint.setAlpha(Math.round(255 * fade));
            c.drawBitmap(slide, 0, 0, paint);
            paint.setAlpha(255);
            return;
        }
        if (background == null) {
            File wp = Photos.wallpaperFile(getContext());
            if (template == 4 && wp.exists()) background = Photos.sharp(wp, w, h, prefs.wallpaperFit());
            if (background == null) {
                Bitmap photo = Photos.wallpaper(getContext(), w, h, prefs.wallpaperBlur(), prefs.wallpaperFit());
                background = photo != null ? photo : MeshBackgroundView.render(prefs.theme(), w / 2, h / 2);
            }
        }
        rect.set(0, 0, w, h);
        c.drawBitmap(background, null, rect, paint);
    }

    private void glass(Canvas c, float x0, float y0, float x1, float y1) {
        paint.setShadowLayer(0, 0, 0, 0);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x24FFFFFF);
        rect.set(x(x0), y(y0), x(x1), y(y1));
        c.drawRoundRect(rect, 22 * u, 22 * u, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1, u));
        paint.setColor(0x40FFFFFF);
        c.drawRoundRect(rect, 22 * u, 22 * u, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void text(Canvas c, String s, float px, float py, float size, int color, Typeface tf, Paint.Align align) {
        paint.setTextAlign(align);
        paint.setTypeface(tf);
        paint.setTextSize(size * u);
        paint.setColor(color);
        c.drawText(s, x(px), y(py), paint);
    }

    private void centered(Canvas c, String s, float size, int color, Typeface tf, float py) {
        text(c, s, 480, py, size, color, tf, Paint.Align.CENTER);
    }

    private static String clock(Calendar now) {
        return String.format(Locale.US, "%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
    }

    private static String dateLine(Calendar now) {
        return LunarCalendar.weekdayLong(now.get(Calendar.DAY_OF_WEEK)) + ", " + now.get(Calendar.DAY_OF_MONTH)
                + " tháng " + (now.get(Calendar.MONTH) + 1) + ", " + now.get(Calendar.YEAR);
    }

    private static String lunarLine(Calendar now) {
        LunarCalendar.LunarDate l = LunarCalendar.fromSolar(now.get(Calendar.DAY_OF_MONTH), now.get(Calendar.MONTH) + 1,
                now.get(Calendar.YEAR));
        return "Âm lịch " + l.day + "/" + l.month + (l.leap ? " nhuận" : "") + " · năm " + LunarCalendar.yearName(l.year);
    }

    // 0 · Đồng hồ kính
    private void drawClockCard(Canvas c, Calendar now) {
        float ax = dx, ay = dy;
        glass(c, 230 + ax, 140 + ay, 730 + ax, 400 + ay);
        text(c, clock(now), 480 + ax, 290 + ay, 124, 0xFFFFFFFF, thin, Paint.Align.CENTER);
        text(c, dateLine(now), 480 + ax, 336 + ay, 20, 0xE6FFFFFF, Typeface.DEFAULT, Paint.Align.CENTER);
        text(c, lunarLine(now), 480 + ax, 364 + ay, 14, 0xFFFFD27A, Typeface.DEFAULT, Paint.Align.CENTER);
    }

    // 1 · Lịch âm dương
    private void drawLunar(Canvas c, Calendar now) {
        int d = now.get(Calendar.DAY_OF_MONTH), m = now.get(Calendar.MONTH) + 1, yy = now.get(Calendar.YEAR);
        LunarCalendar.DayInfo info = LunarCalendar.dayInfo(d, m, yy);
        String key = yy + "-" + m + "-" + d;
        if (!key.equals(dateKey)) {
            dateKey = key;
            nextHoliday = upcomingHoliday(now);
        }
        float ax = dx, ay = dy;
        glass(c, 150 + ax, 110 + ay, 810 + ax, 430 + ay);
        text(c, String.valueOf(d), 300 + ax, 290 + ay, 150, 0xFFFFFFFF, thin, Paint.Align.CENTER);
        text(c, LunarCalendar.weekdayLong(now.get(Calendar.DAY_OF_WEEK)), 300 + ax, 336 + ay, 24, 0xFFFFFFFF, bold, Paint.Align.CENTER);
        text(c, "Tháng " + m + ", " + yy, 300 + ax, 364 + ay, 16, 0xD9FFFFFF, Typeface.DEFAULT, Paint.Align.CENTER);
        text(c, clock(now), 300 + ax, 404 + ay, 22, 0x99FFFFFF, light, Paint.Align.CENTER);
        paint.setColor(0x33FFFFFF);
        c.drawRect(x(452 + ax), y(150 + ay), x(453 + ax), y(390 + ay), paint);

        float lx = 486 + ax;
        text(c, "ÂM LỊCH", lx, 170 + ay, 12, 0x8CFFFFFF, medium, Paint.Align.LEFT);
        text(c, info.lunar.day + " tháng " + LunarCalendar.monthName(info.lunar.month), lx, 208 + ay, 30, 0xFFFFD27A, medium, Paint.Align.LEFT);
        text(c, "Năm " + info.yearCanChi, lx, 234 + ay, 15, 0xE6FFFFFF, Typeface.DEFAULT, Paint.Align.LEFT);
        text(c, "Ngày " + info.dayCanChi + " · Tháng " + info.monthCanChi, lx, 262 + ay, 15, 0xE6FFFFFF, Typeface.DEFAULT, Paint.Align.LEFT);
        text(c, "Ngày " + info.star + (info.goodDay ? " — hoàng đạo" : " — hắc đạo"), lx, 288 + ay, 15,
                info.goodDay ? 0xFF8EE08F : 0xFFF7A1B5, medium, Paint.Align.LEFT);
        text(c, "GIỜ HOÀNG ĐẠO", lx, 322 + ay, 12, 0x8CFFFFFF, medium, Paint.Align.LEFT);
        StringBuilder hours = new StringBuilder();
        for (String hr : info.goodHours) {
            if (hours.length() > 0) hours.append(", ");
            hours.append(hr.substring(0, hr.indexOf(' ')));
        }
        text(c, hours.toString(), lx, 346 + ay, 15, 0xE6FFFFFF, Typeface.DEFAULT, Paint.Align.LEFT);
        if (!nextHoliday.isEmpty()) text(c, nextHoliday, lx, 392 + ay, 15, 0xFFFFC857, medium, Paint.Align.LEFT);
    }

    /** "Hôm nay: Tết Trung thu" or "Còn 24 ngày đến Nhà giáo Việt Nam" (looking up to a year ahead). */
    private static String upcomingHoliday(Calendar today) {
        Calendar c = (Calendar) today.clone();
        for (int i = 0; i < 370; i++) {
            int d = c.get(Calendar.DAY_OF_MONTH), m = c.get(Calendar.MONTH) + 1;
            String h = LunarCalendar.holiday(d, m, LunarCalendar.fromSolar(d, m, c.get(Calendar.YEAR)));
            if (h != null) return i == 0 ? "Hôm nay: " + h : i == 1 ? "Ngày mai: " + h : "Còn " + i + " ngày đến " + h;
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        return "";
    }

    // 2 · Thời tiết
    private void drawWeather(Canvas c, Calendar now) {
        float ax = dx, ay = dy;
        text(c, clock(now), 70 + ax, 300 + ay, 140, 0xFFFFFFFF, thin, Paint.Align.LEFT);
        text(c, dateLine(now), 78 + ax, 348 + ay, 20, 0xE6FFFFFF, Typeface.DEFAULT, Paint.Align.LEFT);
        text(c, lunarLine(now), 78 + ax, 376 + ay, 14, 0xFFFFD27A, Typeface.DEFAULT, Paint.Align.LEFT);
        glass(c, 560 + ax, 150 + ay, 900 + ax, 400 + ay);
        if (weather == null) {
            text(c, "Chưa có dữ liệu thời tiết", 730 + ax, 280 + ay, 16, 0xB3FFFFFF, Typeface.DEFAULT, Paint.Align.CENTER);
            return;
        }
        Glyphs.draw(c, Weather.glyph(weather.code, weather.day), x(630 + ax), y(220 + ay), 80 * u, 0xFFFFFFFF);
        text(c, Math.round(weather.temp) + "°", 884 + ax, 250 + ay, 64, 0xFFFFFFFF, light, Paint.Align.RIGHT);
        text(c, Weather.describe(weather.code), 584 + ax, 296 + ay, 18, 0xFFFFFFFF, medium, Paint.Align.LEFT);
        text(c, prefs.placeName(), 584 + ax, 318 + ay, 13, 0xA6FFFFFF, Typeface.DEFAULT, Paint.Align.LEFT);
        String today = WeatherCardView.isoDate(now);
        int shown = 0;
        float colW = 316f / 4;
        for (Weather.Day day : weather.days) {
            if (day.date.compareTo(today) < 0 || shown >= 4) continue;
            float cx = 584 + ax + colW * shown + colW / 2 - 12;
            String label = shown == 0 ? "Hôm nay" : weekdayOf(day.date);
            text(c, label, cx, 346 + ay, 11, 0xA6FFFFFF, medium, Paint.Align.CENTER);
            Glyphs.draw(c, Weather.glyph(day.code, true), x(cx - 14), y(368 + ay), 22 * u, 0xFFFFFFFF);
            text(c, Double.isNaN(day.max) ? "—" : Math.round(day.max) + "°", cx + 12, 374 + ay, 13, 0xFFFFFFFF, Typeface.DEFAULT, Paint.Align.CENTER);
            shown++;
        }
    }

    private static String weekdayOf(String iso) {
        try {
            Calendar c = Calendar.getInstance();
            c.clear();
            c.set(Integer.parseInt(iso.substring(0, 4)), Integer.parseInt(iso.substring(5, 7)) - 1, Integer.parseInt(iso.substring(8, 10)));
            return LunarCalendar.weekdayShort(c.get(Calendar.DAY_OF_WEEK));
        } catch (RuntimeException e) {
            return "";
        }
    }

    // 3 · Đêm tối giản
    private void drawNight(Canvas c, Calendar now) {
        text(c, clock(now), 480 + dx, 290 + dy, 84, 0x8CFFFFFF, thin, Paint.Align.CENTER);
        text(c, dateLine(now), 480 + dx, 326 + dy, 15, 0x59FFFFFF, Typeface.DEFAULT, Paint.Align.CENTER);
    }

    // 4 · Ảnh nền, 5 · Tuỳ chỉnh: clock in a corner over a photo, with a soft shadow for contrast
    private void drawCornerClock(Canvas c, Calendar now, boolean big) {
        paint.setShadowLayer(12 * u, 0, 2 * u, 0x99000000);
        float ax = dx * 0.5f, ay = dy * 0.5f;
        if (big) {
            text(c, clock(now), 60 + ax, 440 + ay, 110, 0xFFFFFFFF, thin, Paint.Align.LEFT);
            text(c, dateLine(now), 68 + ax, 478 + ay, 20, 0xF2FFFFFF, Typeface.DEFAULT, Paint.Align.LEFT);
            text(c, lunarLine(now), 68 + ax, 504 + ay, 14, 0xFFFFD27A, Typeface.DEFAULT, Paint.Align.LEFT);
        } else {
            text(c, clock(now), 900 + ax, 470 + ay, 64, 0xFFFFFFFF, light, Paint.Align.RIGHT);
            text(c, dateLine(now), 900 + ax, 500 + ay, 15, 0xF2FFFFFF, Typeface.DEFAULT, Paint.Align.RIGHT);
        }
        paint.setShadowLayer(0, 0, 0, 0);
    }
}
