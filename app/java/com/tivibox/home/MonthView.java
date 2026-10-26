package com.tivibox.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.KeyEvent;
import android.view.View;

import java.util.Calendar;

/**
 * Full-size month grid for the calendar screen, Monday first. ◀ ▶ ▲ ▼ move the selected day (crossing into the
 * next or previous month as needed), CH+/CH− and ⏪/⏩ change month, OK jumps back to today.
 * Each cell: solar day, lunar day (1/9 on the first of a lunar month), an amber dot for holidays and a green dot
 * for hoàng đạo days.
 */
public class MonthView extends View {
    interface Listener {
        void onSelected(int year, int month, int day);
    }

    private static final String[] HEAD = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};

    private final float dp;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path arrow = new Path();
    private final Typeface light = Typeface.create("sans-serif-light", Typeface.NORMAL);
    private final Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private final Typeface bold = Typeface.create("sans-serif-medium", Typeface.BOLD);
    private final int[] lunarDay = new int[32], lunarMonth = new int[32];
    private final boolean[] holiday = new boolean[32], good = new boolean[32];
    private int year, month, day, days, firstColumn;
    private int todayY, todayM, todayD;
    private Listener listener;

    public MonthView(Context context) {
        super(context);
        dp = context.getResources().getDisplayMetrics().density;
        setFocusable(true);
        setFocusableInTouchMode(true);
        Calendar now = Calendar.getInstance();
        todayY = now.get(Calendar.YEAR);
        todayM = now.get(Calendar.MONTH) + 1;
        todayD = now.get(Calendar.DAY_OF_MONTH);
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    void select(int y, int m, int d) {
        if (y != year || m != month) {
            year = y;
            month = m;
            computeMonth();
        }
        day = Math.max(1, Math.min(days, d));
        invalidate();
        if (listener != null) listener.onSelected(year, month, day);
    }

    private void computeMonth() {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month - 1, 1);
        days = c.getActualMaximum(Calendar.DAY_OF_MONTH);
        firstColumn = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        for (int d = 1; d <= days; d++) {
            LunarCalendar.DayInfo info = LunarCalendar.dayInfo(d, month, year);
            lunarDay[d] = info.lunar.day;
            lunarMonth[d] = info.lunar.month;
            holiday[d] = LunarCalendar.holiday(d, month, info.lunar) != null;
            good[d] = info.goodDay;
        }
    }

    /** Moves the selection by `delta` days through the real calendar. */
    private void moveDays(int delta) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month - 1, day);
        c.add(Calendar.DAY_OF_MONTH, delta);
        select(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    private void moveMonths(int delta) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month - 1, 1);
        c.add(Calendar.MONTH, delta);
        int y = c.get(Calendar.YEAR), m = c.get(Calendar.MONTH) + 1;
        select(y, m, Math.min(day, c.getActualMaximum(Calendar.DAY_OF_MONTH)));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT: moveDays(-1); return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT: moveDays(1); return true;
            case KeyEvent.KEYCODE_DPAD_UP: moveDays(-7); return true;
            case KeyEvent.KEYCODE_DPAD_DOWN: moveDays(7); return true;
            case KeyEvent.KEYCODE_CHANNEL_UP:
            case KeyEvent.KEYCODE_PAGE_UP:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                moveMonths(-1);
                return true;
            case KeyEvent.KEYCODE_CHANNEL_DOWN:
            case KeyEvent.KEYCODE_PAGE_DOWN:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                moveMonths(1);
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                select(todayY, todayM, todayD);
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), pad = 18 * dp;
        Glass.card(c, paint, rect, w, h, dp);
        if (year == 0) return;

        // Header: ◀ Tháng 10 · 2026 ▶
        float headY = pad + 14 * dp;
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(bold);
        paint.setTextSize(20 * dp);
        paint.setColor(0xFFFFFFFF);
        String title = "Tháng " + month + " · " + year;
        c.drawText(title, w / 2, Glass.baseline(paint, headY), paint);
        float half = paint.measureText(title) / 2 + 22 * dp;
        paint.setColor(0xB3FFFFFF);
        triangle(c, w / 2 - half, headY, -1);
        triangle(c, w / 2 + half, headY, 1);

        float colW = (w - 2 * pad) / 7f;
        float headRow = headY + 32 * dp;
        paint.setTypeface(medium);
        paint.setTextSize(11 * dp);
        for (int i = 0; i < 7; i++) {
            paint.setColor(i == 6 ? 0xCCF7A1B5 : 0x99FFFFFF);
            c.drawText(HEAD[i], pad + i * colW + colW / 2, headRow, paint);
        }

        float top = headRow + 10 * dp, legend = 22 * dp;
        int rows = (firstColumn + days + 6) / 7;
        float rowH = (h - pad - legend - top) / rows;
        float gap = 3 * dp;
        for (int d = 1; d <= days; d++) {
            int cell = firstColumn + d - 1, col = cell % 7, row = cell / 7;
            float x0 = pad + col * colW + gap, y0 = top + row * rowH + gap;
            float x1 = x0 + colW - 2 * gap, y1 = y0 + rowH - 2 * gap;
            boolean selected = d == day, today = year == todayY && month == todayM && d == todayD;
            boolean focus = selected && hasFocus();
            if (focus) {
                // Cream fill only, same rounded box as the "today" outline.
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(0xFFF3EEE8);
                rect.set(x0, y0, x1, y1);
                c.drawRoundRect(rect, 12 * dp, 12 * dp, paint);
            } else if (selected) {
                Glass.panel(c, paint, rect, x0, y0, x1, y1, 12 * dp, dp, 0x33FFFFFF, 0x80FFFFFF);
            } else if (today) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2 * dp);
                paint.setColor(0xFF8EE08F);
                rect.set(x0, y0, x1, y1);
                c.drawRoundRect(rect, 12 * dp, 12 * dp, paint);
                paint.setStyle(Paint.Style.FILL);
            }
            float cx = (x0 + x1) / 2, cy = (y0 + y1) / 2;
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(light);
            paint.setTextSize(Math.min(24 * dp, rowH * 0.42f));
            paint.setColor(focus ? 0xFF15151C : col == 6 ? 0xFFF7A1B5 : 0xFFFFFFFF);
            c.drawText(String.valueOf(d), cx, cy + 1 * dp, paint);
            boolean first = lunarDay[d] == 1;
            paint.setTypeface(Typeface.DEFAULT);
            paint.setTextSize(10.5f * dp);
            paint.setColor(focus ? 0xB315151C : first || lunarDay[d] == 15 ? 0xFFFFD27A : 0x99FFFFFF);
            c.drawText(first ? "1/" + lunarMonth[d] : String.valueOf(lunarDay[d]), cx, cy + 15 * dp, paint);
            float dotY = y0 + 8 * dp;
            if (holiday[d]) dot(c, x1 - 9 * dp, dotY, 0xFFFFC857);
            if (good[d]) dot(c, x0 + 9 * dp, dotY, focus ? 0xFF2E9E4F : 0xFF8EE08F);
        }

        // Legend
        float ly = h - pad - 4 * dp;
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(11 * dp);
        float lx = pad + 6 * dp;
        dot(c, lx, ly - 4 * dp, 0xFF8EE08F);
        paint.setColor(0xB3FFFFFF);
        c.drawText("Ngày hoàng đạo", lx + 10 * dp, ly, paint);
        lx += 10 * dp + paint.measureText("Ngày hoàng đạo") + 22 * dp;
        dot(c, lx, ly - 4 * dp, 0xFFFFC857);
        paint.setColor(0xB3FFFFFF);
        c.drawText("Ngày lễ", lx + 10 * dp, ly, paint);
        lx += 10 * dp + paint.measureText("Ngày lễ") + 22 * dp;
        paint.setColor(0xFFFFD27A);
        c.drawText("1/9", lx, ly, paint);
        paint.setColor(0xB3FFFFFF);
        c.drawText(" mùng 1 · rằm âm lịch", lx + paint.measureText("1/9"), ly, paint);
    }

    private void dot(Canvas c, float x, float y, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        c.drawCircle(x, y, 3.5f * dp, paint);
    }

    private void triangle(Canvas c, float x, float cy, int dir) {
        float a = 7 * dp;
        arrow.reset();
        arrow.moveTo(x + dir * a, cy);
        arrow.lineTo(x - dir * a * 0.2f, cy - a);
        arrow.lineTo(x - dir * a * 0.2f, cy + a);
        arrow.close();
        c.drawPath(arrow, paint);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, android.graphics.Rect previous) {
        super.onFocusChanged(gainFocus, direction, previous);
        invalidate();
    }
}
