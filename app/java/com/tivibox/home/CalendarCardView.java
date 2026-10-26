package com.tivibox.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import java.util.Calendar;

/** Month grid, Monday first, with the lunar day after each solar day ("1/9" on the first of a lunar month). */
public class CalendarCardView extends View {
    private static final String[] HEAD = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
    private static final int TODAY = 0xFF8EE08F;
    private static final int ON_TODAY = 0xFF0F3B1C;
    private static final int SUNDAY = 0xFFF7A1B5;
    private static final int LUNAR_FIRST = 0xFFFFD27A;

    private final float dp;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private final Typeface regular = Typeface.create("sans-serif", Typeface.NORMAL);
    private final int[] lunarDay = new int[32];
    private final int[] lunarMonth = new int[32];
    private int year, month, today, days, firstColumn;

    public CalendarCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        dp = context.getResources().getDisplayMetrics().density;
        setFocusable(true);
        setClickable(true);
        setContentDescription("Lịch, mở lịch âm dương");
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, android.graphics.Rect previous) {
        super.onFocusChanged(focused, direction, previous);
        Glass.animateFocus(this, focused);
    }

    /** month is 1..12. */
    public void setDate(int y, int m, int d) {
        if (y == year && m == month && d == today) return;
        boolean sameMonth = y == year && m == month;
        year = y;
        month = m;
        today = d;
        if (!sameMonth) computeMonth();
        invalidate();
    }

    private void computeMonth() {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month - 1, 1);
        days = c.getActualMaximum(Calendar.DAY_OF_MONTH);
        firstColumn = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        for (int d = 1; d <= days; d++) {
            LunarCalendar.LunarDate l = LunarCalendar.fromSolar(d, month, year);
            lunarDay[d] = l.day;
            lunarMonth[d] = l.month;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight(), pad = 16 * dp;
        Glass.card(canvas, paint, rect, w, h, dp);
        if (isFocused()) Glass.focusRing(canvas, paint, rect, w, h, dp);
        if (year == 0) return;

        float titleBase = pad + 14 * dp;
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(medium);
        paint.setTextSize(15 * dp);
        paint.setColor(0xFFFFFFFF);
        canvas.drawText("Tháng " + month, pad, titleBase, paint);

        float colW = (w - 2 * pad) / 7f;
        float headBase = titleBase + 18 * dp;
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(medium);
        paint.setTextSize(8.5f * dp);
        for (int i = 0; i < 7; i++) {
            paint.setColor(i == 6 ? 0xCCF7A1B5 : 0x99FFFFFF);
            canvas.drawText(HEAD[i], pad + i * colW + 6 * dp, headBase, paint);
        }

        int rows = (firstColumn + days + 6) / 7;
        float top = headBase + 6 * dp;
        float rowH = Math.min(24 * dp, (h - 10 * dp - top) / rows);
        for (int d = 1; d <= days; d++) {
            int cell = firstColumn + d - 1;
            int col = cell % 7, row = cell / 7;
            float x = pad + col * colW + 6 * dp;
            float cy = top + row * rowH + rowH / 2;
            boolean isToday = d == today;
            if (isToday) {
                paint.setColor(TODAY);
                rect.set(x - 5 * dp, cy - rowH / 2 + 1.5f * dp, pad + (col + 1) * colW - 4 * dp, cy + rowH / 2 - 1.5f * dp);
                canvas.drawRoundRect(rect, 6 * dp, 6 * dp, paint);
            }
            float base = cy + 4.2f * dp;
            String num = String.valueOf(d);
            paint.setTypeface(medium);
            paint.setTextSize(12 * dp);
            paint.setColor(isToday ? ON_TODAY : col == 6 ? SUNDAY : 0xFFFFFFFF);
            canvas.drawText(num, x, base, paint);
            float lx = x + paint.measureText(num) + 3 * dp;
            boolean first = lunarDay[d] == 1;
            paint.setTypeface(regular);
            paint.setTextSize(7.5f * dp);
            paint.setColor(isToday ? 0xCC0F3B1C : first ? LUNAR_FIRST : 0x8CFFFFFF);
            canvas.drawText(first ? "1/" + lunarMonth[d] : String.valueOf(lunarDay[d]), lx, base, paint);
        }
    }
}
