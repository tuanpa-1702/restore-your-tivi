import com.tivibox.home.VietTone;

/** Plain JVM test, run by build.ps1: tone placement of the TV keyboard. */
public class VietToneTest {
    private static int failures;

    public static void main(String[] args) {
        check("Tho", VietTone.HOI, "Thỏ");
        check("Viêt", VietTone.NANG, "Việt");
        check("Câu", VietTone.HUYEN, "Cầu");
        check("Giây", VietTone.SAC, "Giấy");
        check("ngươi", VietTone.HUYEN, "người");
        check("hoa", VietTone.HUYEN, "hoà");
        check("toan", VietTone.SAC, "toán");
        check("quan", VietTone.SAC, "quán");
        check("gi", VietTone.HUYEN, "gì");
        check("mai", VietTone.SAC, "mái");
        check("mua", VietTone.SAC, "múa");
        check("Nam", VietTone.NANG, "Nạm");
        check("Ha", VietTone.HUYEN, "Hà");
        check("Đa", VietTone.NGA, "Đã");
        check("thuy", VietTone.HOI, "thuỷ");
        check("Thỏ", VietTone.HOI, "Tho");    // same tone again removes it
        check("Thỏ", VietTone.SAC, "Thó");    // another tone replaces it
        check("xyz", VietTone.SAC, "xýz");
        check("brr", VietTone.SAC, "brr");    // no vowel: unchanged
        if (failures > 0) {
            System.out.println(failures + " tone test(s) failed");
            System.exit(1);
        }
        System.out.println("Vietnamese tone tests passed");
    }

    private static void check(String word, char tone, String want) {
        String got = VietTone.apply(word, tone);
        if (!want.equals(got)) {
            failures++;
            System.out.println("FAIL " + word + " + U+" + Integer.toHexString(tone) + ": want " + want + ", got " + got);
        }
    }
}
