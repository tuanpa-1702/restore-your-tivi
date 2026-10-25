import com.tivibox.home.Telex;

/** Plain JVM test, run by build.ps1: Telex typing of the TV keyboard. */
public class TelexTest {
    private static int failures;

    public static void main(String[] args) {
        check("Xin chaof Thor Miu!", "Xin chào Thỏ Miu!");
        check("Caauf Giaays", "Cầu Giấy");
        check("Vieetj Nam", "Việt Nam");
        check("nguwowif", "người");
        check("dduowngf", "đường");
        check("Haf Nooji", "Hà Nội");
        check("Ddaf Nawngx", "Đà Nẵng");
        check("Thur DDuwcs", "Thủ Đức");
        check("hoaf", "hoà");
        check("hoafn", "hoàn");
        check("toans", "toán");
        check("quas", "quá");
        check("w", "ư");
        check("tuw", "tư");
        check("ass", "as");
        check("aaa", "aa");
        check("ddd", "dd");
        check("asz", "a");
        check("TIEENGS VIEETJ", "TIẾNG VIỆT");
        check("ddi 123", "đi 123");
        if (failures > 0) {
            System.out.println(failures + " Telex test(s) failed");
            System.exit(1);
        }
        System.out.println("Telex tests passed");
    }

    private static void check(String keys, String want) {
        String got = Telex.typeAll(keys);
        if (!want.equals(got)) {
            failures++;
            System.out.println("FAIL \"" + keys + "\": want \"" + want + "\", got \"" + got + "\"");
        }
    }
}
