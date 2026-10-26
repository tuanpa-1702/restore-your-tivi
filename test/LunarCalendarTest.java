import com.tivibox.home.LunarCalendar;
import com.tivibox.home.LunarCalendar.LunarDate;

/** Plain JVM test, run by build.ps1: known Vietnamese lunar dates. */
public class LunarCalendarTest {
    private static int failures;

    public static void main(String[] args) {
        // From the reference screenshot: 1/10/2026 = 21/8, 10/10/2026 = 1/9, 23/10/2026 = 14/9.
        check(1, 10, 2026, 21, 8, 2026, false);
        check(10, 10, 2026, 1, 9, 2026, false);
        check(23, 10, 2026, 14, 9, 2026, false);
        // Tết Bính Ngọ, Tết Ất Tỵ, Tết Giáp Thìn.
        check(17, 2, 2026, 1, 1, 2026, false);
        check(29, 1, 2025, 1, 1, 2025, false);
        check(10, 2, 2024, 1, 1, 2024, false);
        // Last day of the lunar year before Tết 2026 belongs to 2025.
        check(16, 2, 2026, 29, 12, 2025, false);
        // Leap months: 2/2023 nhuận starts 22/3/2023, 4/2020 nhuận starts 23/5/2020, 6/2025 nhuận starts 25/7/2025.
        check(22, 3, 2023, 1, 2, 2023, true);
        check(23, 5, 2020, 1, 4, 2020, true);
        check(25, 7, 2025, 1, 6, 2025, true);
        check(23, 8, 2025, 1, 7, 2025, false);
        // Mùng 1 Tết Giáp Thìn (10/2/2024): ngày Giáp Thìn, tháng Bính Dần, Kim Quỹ hoàng đạo, tiết Lập xuân;
        // ngày Thìn: giờ hoàng đạo Dần, Thìn, Tỵ, Thân, Dậu, Hợi.
        LunarCalendar.DayInfo tet = LunarCalendar.dayInfo(10, 2, 2024);
        expect("Giáp Thìn", tet.dayCanChi, "day can chi 10/2/2024");
        expect("Bính Dần", tet.monthCanChi, "month can chi 10/2/2024");
        expect("Kim Quỹ true", tet.star + " " + tet.goodDay, "star 10/2/2024");
        expect("Lập xuân", tet.tietKhi, "tiet khi 10/2/2024");
        expect("Dần (3–5),Thìn (7–9),Tỵ (9–11),Thân (15–17),Dậu (17–19),Hợi (21–23)",
                String.join(",", tet.goodHours), "gio hoang dao 10/2/2024");
        expect("Tuất", tet.clashBranch, "clash 10/2/2024");
        expect("Sương giáng", LunarCalendar.dayInfo(25, 10, 2026).tietKhi, "tiet khi 25/10/2026");
        expect("Hạ chí", LunarCalendar.dayInfo(1, 7, 2026).tietKhi, "tiet khi 1/7/2026");
        expect("Tết Trung thu", LunarCalendar.holiday(25, 9, LunarCalendar.fromSolar(25, 9, 2026)), "Trung thu 2026");
        expect("Bính Ngọ", LunarCalendar.yearName(2026));
        expect("Ất Tỵ", LunarCalendar.yearName(2025));
        expect("Giáp Thìn", LunarCalendar.yearName(2024));
        if (failures > 0) {
            System.out.println(failures + " lunar test(s) failed");
            System.exit(1);
        }
        System.out.println("Lunar calendar tests passed");
    }

    private static void check(int d, int m, int y, int ld, int lm, int ly, boolean leap) {
        LunarDate l = LunarCalendar.fromSolar(d, m, y);
        String got = l.day + "/" + l.month + "/" + l.year + (l.leap ? " leap" : "");
        String want = ld + "/" + lm + "/" + ly + (leap ? " leap" : "");
        expect(want, got, d + "/" + m + "/" + y);
    }

    private static void expect(String want, String got) {
        expect(want, got, want);
    }

    private static void expect(String want, String got, String what) {
        if (!want.equals(got)) {
            failures++;
            System.out.println("FAIL " + what + ": want " + want + ", got " + got);
        }
    }
}
