package com.tivibox.home;

/**
 * Vietnamese lunar calendar, after Ho Ngoc Duc's astronomical algorithm, computed for UTC+7.
 * Pure Java so it can be tested on the JVM (test/LunarCalendarTest.java).
 */
public final class LunarCalendar {
    private static final double TZ = 7.0;
    private static final String[] CAN = {"Giáp", "Ất", "Bính", "Đinh", "Mậu", "Kỷ", "Canh", "Tân", "Nhâm", "Quý"};
    private static final String[] CHI = {"Tý", "Sửu", "Dần", "Mão", "Thìn", "Tỵ", "Ngọ", "Mùi", "Thân", "Dậu", "Tuất", "Hợi"};
    // Indexed by java.util.Calendar.DAY_OF_WEEK (Sunday = 1).
    private static final String[] WEEKDAY = {"", "Chủ Nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"};
    private static final String[] WEEKDAY_SHORT = {"", "CN", "T2", "T3", "T4", "T5", "T6", "T7"};

    private LunarCalendar() {}

    public static final class LunarDate {
        public final int day, month, year;
        public final boolean leap;

        LunarDate(int day, int month, int year, boolean leap) {
            this.day = day;
            this.month = month;
            this.year = year;
            this.leap = leap;
        }
    }

    public static String weekdayLong(int calendarDayOfWeek) {
        return WEEKDAY[calendarDayOfWeek];
    }

    public static String weekdayShort(int calendarDayOfWeek) {
        return WEEKDAY_SHORT[calendarDayOfWeek];
    }

    /** "Bính Ngọ" for 2026. */
    public static String yearName(int lunarYear) {
        return CAN[(lunarYear + 6) % 10] + " " + CHI[(lunarYear + 8) % 12];
    }

    public static LunarDate fromSolar(int dd, int mm, int yy) {
        int dayNumber = jdFromDate(dd, mm, yy);
        int k = (int) Math.floor((dayNumber - 2415021.076998695) / 29.530588853);
        int monthStart = newMoonDay(k + 1);
        if (monthStart > dayNumber) monthStart = newMoonDay(k);
        int a11 = lunarMonth11(yy);
        int b11 = a11;
        int lunarYear;
        if (a11 >= monthStart) {
            lunarYear = yy;
            a11 = lunarMonth11(yy - 1);
        } else {
            lunarYear = yy + 1;
            b11 = lunarMonth11(yy + 1);
        }
        int lunarDay = dayNumber - monthStart + 1;
        int diff = (int) Math.floor((monthStart - a11) / 29.0);
        boolean leap = false;
        int lunarMonth = diff + 11;
        if (b11 - a11 > 365) {
            int leapDiff = leapMonthOffset(a11);
            if (diff >= leapDiff) {
                lunarMonth = diff + 10;
                if (diff == leapDiff) leap = true;
            }
        }
        if (lunarMonth > 12) lunarMonth -= 12;
        if (lunarMonth >= 11 && diff < 4) lunarYear -= 1;
        return new LunarDate(lunarDay, lunarMonth, lunarYear, leap);
    }

    static int jdFromDate(int dd, int mm, int yy) {
        int a = (14 - mm) / 12;
        int y = yy + 4800 - a;
        int m = mm + 12 * a - 3;
        int jd = dd + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045;
        if (jd < 2299161) jd = dd + (153 * m + 2) / 5 + 365 * y + y / 4 - 32083;
        return jd;
    }

    private static int newMoonDay(int k) {
        double t = k / 1236.85;
        double t2 = t * t;
        double t3 = t2 * t;
        double dr = Math.PI / 180;
        double jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * t2 - 0.000000155 * t3;
        jd1 += 0.00033 * Math.sin((166.56 + 132.87 * t - 0.009173 * t2) * dr);
        double m = 359.2242 + 29.10535608 * k - 0.0000333 * t2 - 0.00000347 * t3;
        double mpr = 306.0253 + 385.81691806 * k + 0.0107306 * t2 + 0.00001236 * t3;
        double f = 21.2964 + 390.67050646 * k - 0.0016528 * t2 - 0.00000239 * t3;
        double c1 = (0.1734 - 0.000393 * t) * Math.sin(m * dr) + 0.0021 * Math.sin(2 * dr * m);
        c1 = c1 - 0.4068 * Math.sin(mpr * dr) + 0.0161 * Math.sin(dr * 2 * mpr);
        c1 = c1 - 0.0004 * Math.sin(dr * 3 * mpr);
        c1 = c1 + 0.0104 * Math.sin(dr * 2 * f) - 0.0051 * Math.sin(dr * (m + mpr));
        c1 = c1 - 0.0074 * Math.sin(dr * (m - mpr)) + 0.0004 * Math.sin(dr * (2 * f + m));
        c1 = c1 - 0.0004 * Math.sin(dr * (2 * f - m)) - 0.0006 * Math.sin(dr * (2 * f + mpr));
        c1 = c1 + 0.0010 * Math.sin(dr * (2 * f - mpr)) + 0.0005 * Math.sin(dr * (2 * mpr + m));
        double deltaT;
        if (t < -11) {
            deltaT = 0.001 + 0.000839 * t + 0.0002261 * t2 - 0.00000845 * t3 - 0.000000081 * t * t3;
        } else {
            deltaT = -0.000278 + 0.000265 * t + 0.000262 * t2;
        }
        return (int) Math.floor(jd1 + c1 - deltaT + 0.5 + TZ / 24);
    }

    /** Sun longitude at local midnight of day `jdn`, as a sector 0..11 (30 degrees each). */
    private static int sunLongitude(int jdn) {
        return (int) Math.floor(sunLongitudeRad(jdn) / Math.PI * 6);
    }

    /** Sun longitude in radians (0..2π) at local midnight of day `jdn`. */
    private static double sunLongitudeRad(int jdn) {
        double t = (jdn - 2451545.5 - TZ / 24) / 36525;
        double t2 = t * t;
        double dr = Math.PI / 180;
        double m = 357.52910 + 35999.05030 * t - 0.0001559 * t2 - 0.00000048 * t * t2;
        double l0 = 280.46645 + 36000.76983 * t + 0.0003032 * t2;
        double dl = (1.914600 - 0.004817 * t - 0.000014 * t2) * Math.sin(dr * m);
        dl = dl + (0.019993 - 0.000101 * t) * Math.sin(dr * 2 * m) + 0.000290 * Math.sin(dr * 3 * m);
        double l = (l0 + dl) * dr;
        return l - Math.PI * 2 * Math.floor(l / (Math.PI * 2));
    }

    // ------------------------------------------------------------------ almanac (Can Chi, tiết khí, giờ hoàng đạo)

    private static final String[] TIET_KHI = {"Xuân phân", "Thanh minh", "Cốc vũ", "Lập hạ", "Tiểu mãn", "Mang chủng",
            "Hạ chí", "Tiểu thử", "Đại thử", "Lập thu", "Xử thử", "Bạch lộ", "Thu phân", "Hàn lộ", "Sương giáng",
            "Lập đông", "Tiểu tuyết", "Đại tuyết", "Đông chí", "Tiểu hàn", "Đại hàn", "Lập xuân", "Vũ thủy", "Kinh trập"};
    // Twelve day/hour stars; Thanh Long starts at Tý in lunar months 1 and 7, two branches later each month after.
    private static final String[] STARS = {"Thanh Long", "Minh Đường", "Thiên Hình", "Chu Tước", "Kim Quỹ", "Kim Đường",
            "Bạch Hổ", "Ngọc Đường", "Thiên Lao", "Huyền Vũ", "Tư Mệnh", "Câu Trần"};
    private static final boolean[] STAR_GOOD = {true, true, false, false, true, true, false, true, false, false, true, false};
    // Giờ hoàng đạo by the day's earthly branch (index chi % 6), one flag per branch Tý..Hợi.
    private static final String[] GIO_HOANG_DAO = {"110100101100", "001101001011", "110011010010",
            "101100110100", "001011001101", "010010110011"};

    /** Everything the calendar screen shows about one solar day. */
    public static final class DayInfo {
        public final LunarDate lunar;
        public final String dayCanChi, monthCanChi, yearCanChi, tietKhi, star, clashBranch;
        public final boolean goodDay;
        /** Giờ hoàng đạo as "Tý (23–1)" and so on, in day order. */
        public final String[] goodHours;

        DayInfo(LunarDate lunar, String dayCanChi, String monthCanChi, String yearCanChi, String tietKhi,
                String star, boolean goodDay, String clashBranch, String[] goodHours) {
            this.lunar = lunar;
            this.dayCanChi = dayCanChi;
            this.monthCanChi = monthCanChi;
            this.yearCanChi = yearCanChi;
            this.tietKhi = tietKhi;
            this.star = star;
            this.goodDay = goodDay;
            this.clashBranch = clashBranch;
            this.goodHours = goodHours;
        }
    }

    public static DayInfo dayInfo(int dd, int mm, int yy) {
        int jd = jdFromDate(dd, mm, yy);
        LunarDate l = fromSolar(dd, mm, yy);
        int dayChi = (jd + 1) % 12;
        String dayCanChi = CAN[(jd + 9) % 10] + " " + CHI[dayChi];
        String monthCanChi = CAN[(l.year * 12 + l.month + 3) % 10] + " " + CHI[(l.month + 1) % 12];
        String tietKhi = TIET_KHI[(int) Math.floor(sunLongitudeRad(jd + 1) / Math.PI * 12) % 24];
        int starStart = ((l.month - 1) % 6) * 2;
        int star = ((dayChi - starStart) % 12 + 12) % 12;
        String flags = GIO_HOANG_DAO[dayChi % 6];
        String[] hours = new String[6];
        int n = 0;
        for (int i = 0; i < 12; i++) {
            if (flags.charAt(i) != '1') continue;
            hours[n++] = CHI[i] + " (" + ((i * 2 + 23) % 24) + "–" + ((i * 2 + 1) % 24) + ")";
        }
        return new DayInfo(l, dayCanChi, monthCanChi, yearName(l.year), tietKhi, STARS[star], STAR_GOOD[star],
                CHI[(dayChi + 6) % 12], hours);
    }

    /** Vietnamese holidays on this day (solar and lunar), or null. Leap lunar months have no festivals. */
    public static String holiday(int dd, int mm, LunarDate l) {
        String solar;
        switch (mm * 100 + dd) {
            case 101: solar = "Tết Dương lịch"; break;
            case 214: solar = "Lễ Tình nhân"; break;
            case 308: solar = "Quốc tế Phụ nữ"; break;
            case 430: solar = "Giải phóng miền Nam"; break;
            case 501: solar = "Quốc tế Lao động"; break;
            case 601: solar = "Quốc tế Thiếu nhi"; break;
            case 902: solar = "Quốc khánh"; break;
            case 1020: solar = "Phụ nữ Việt Nam"; break;
            case 1120: solar = "Nhà giáo Việt Nam"; break;
            case 1224: solar = "Đêm Giáng sinh"; break;
            case 1225: solar = "Lễ Giáng sinh"; break;
            default: solar = null;
        }
        String lunar = null;
        if (!l.leap) {
            switch (l.month * 100 + l.day) {
                case 101: lunar = "Tết Nguyên đán"; break;
                case 102: lunar = "Mùng 2 Tết"; break;
                case 103: lunar = "Mùng 3 Tết"; break;
                case 115: lunar = "Rằm tháng Giêng"; break;
                case 310: lunar = "Giỗ Tổ Hùng Vương"; break;
                case 415: lunar = "Lễ Phật đản"; break;
                case 505: lunar = "Tết Đoan ngọ"; break;
                case 715: lunar = "Lễ Vu Lan"; break;
                case 815: lunar = "Tết Trung thu"; break;
                case 1223: lunar = "Ông Công Ông Táo"; break;
                default: lunar = null;
            }
        }
        if (solar != null && lunar != null) return lunar + " · " + solar;
        return lunar != null ? lunar : solar;
    }

    /** "Giêng", "Hai" … "Chạp": the traditional lunar month names. */
    public static String monthName(int lunarMonth) {
        String[] names = {"Giêng", "Hai", "Ba", "Tư", "Năm", "Sáu", "Bảy", "Tám", "Chín", "Mười", "Một", "Chạp"};
        return names[lunarMonth - 1];
    }

    private static int lunarMonth11(int yy) {
        double off = jdFromDate(31, 12, yy) - 2415021;
        int k = (int) Math.floor(off / 29.530588853);
        int nm = newMoonDay(k);
        if (sunLongitude(nm) >= 9) nm = newMoonDay(k - 1);
        return nm;
    }

    private static int leapMonthOffset(int a11) {
        int k = (int) Math.floor((a11 - 2415021.076998695) / 29.530588853 + 0.5);
        int last;
        int i = 1;
        int arc = sunLongitude(newMoonDay(k + i));
        do {
            last = arc;
            i++;
            arc = sunLongitude(newMoonDay(k + i));
        } while (arc != last && i < 14);
        return i - 1;
    }
}
