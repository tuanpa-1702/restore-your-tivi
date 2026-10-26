package com.tivibox.home;

import android.app.Activity;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Calendar;

/** Full calendar: details of the selected day (lunar date, Can Chi, tiết khí, giờ hoàng đạo…) left, month right. */
public class CalendarActivity extends Activity {
    private TextView bigDay, weekday, solarMonth, lunarDate, lunarYear, canChi, tietKhi, star, clash, goodHours, holiday;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        ((MeshBackgroundView) findViewById(R.id.background)).setTheme(new Prefs(this).theme());
        Stage.fitOnLayout(findViewById(R.id.root), findViewById(R.id.stage));
        bigDay = findViewById(R.id.big_day);
        weekday = findViewById(R.id.weekday);
        solarMonth = findViewById(R.id.solar_month);
        lunarDate = findViewById(R.id.lunar_date);
        lunarYear = findViewById(R.id.lunar_year);
        canChi = findViewById(R.id.can_chi);
        tietKhi = findViewById(R.id.tiet_khi);
        star = findViewById(R.id.star);
        clash = findViewById(R.id.clash);
        goodHours = findViewById(R.id.good_hours);
        holiday = findViewById(R.id.holiday);

        MonthView month = new MonthView(this);
        month.setListener(this::show);
        ((FrameLayout) findViewById(R.id.month_holder)).addView(month,
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        Calendar now = Calendar.getInstance();
        month.select(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH));
        month.requestFocus();
    }

    private void show(int y, int m, int d) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(y, m - 1, d);
        LunarCalendar.DayInfo info = LunarCalendar.dayInfo(d, m, y);
        LunarCalendar.LunarDate l = info.lunar;
        bigDay.setText(String.valueOf(d));
        weekday.setText(LunarCalendar.weekdayLong(c.get(Calendar.DAY_OF_WEEK)));
        solarMonth.setText("Tháng " + m + ", " + y);
        lunarDate.setText(l.day + " tháng " + LunarCalendar.monthName(l.month) + (l.leap ? " (nhuận)" : ""));
        lunarYear.setText("Năm " + info.yearCanChi);
        canChi.setText(labelled("Ngày ", info.dayCanChi + "  ·  ", "Tháng ", info.monthCanChi));
        tietKhi.setText(labelled("Tiết khí ", info.tietKhi));
        star.setText("Ngày " + info.star + (info.goodDay ? " — hoàng đạo" : " — hắc đạo"));
        star.setTextColor(info.goodDay ? 0xFF8EE08F : 0xFFF7A1B5);
        clash.setText(labelled("Xung tuổi ", info.clashBranch));
        String[] h = info.goodHours;
        goodHours.setText(h[0] + "  ·  " + h[1] + "  ·  " + h[2] + "\n" + h[3] + "  ·  " + h[4] + "  ·  " + h[5]);
        String fest = LunarCalendar.holiday(d, m, l);
        holiday.setVisibility(fest == null ? View.GONE : View.VISIBLE);
        if (fest != null) holiday.setText(fest);
    }

    /** Alternates dim labels and bright values: labelled("Ngày ", "Canh Ngọ", …). */
    private static CharSequence labelled(String... parts) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        for (int i = 0; i < parts.length; i++) {
            int start = sb.length();
            sb.append(parts[i]);
            if (i % 2 == 0) {
                sb.setSpan(new ForegroundColorSpan(0x99FFFFFF), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return sb;
    }
}
