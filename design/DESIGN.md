# Tivi Home — Design system

Giao diện màn hình chính cho Android TV / TV box, điều khiển hoàn toàn bằng remote (D-pad). Phong cách: **nền gradient
nhiều đốm màu, thẻ kính mờ (glass), chữ trắng, đồng hồ lớn nét mảnh, focus nền kem + viền xanh lá**. Ngôn ngữ giao diện:
tiếng Việt.

Nguồn giá trị chính xác: [`tokens/tokens.json`](tokens/tokens.json) (và bản CSS [`tokens/tokens.css`](tokens/tokens.css)).
Bản mẫu chạy được: [`reference/`](reference/) — mở `reference/home.html` trong trình duyệt. Ảnh: [`screenshots/`](screenshots/).
Bản cài đặt thật (Android TV launcher) nằm ở thư mục gốc repo: [`../app/`](../app/).

---

## 1. Nguyên tắc

1. **Đọc được từ xa 3 mét.** Chữ nhỏ nhất 7.5dp chỉ dùng cho ngày âm phụ; nội dung chính ≥ 12dp; số quan trọng rất lớn
   (đồng hồ 138dp, nhiệt độ 54dp).
2. **Một thứ đang được chọn, luôn nhìn thấy rõ.** Mọi phần tử chọn được có cùng một kiểu focus (mục 4). Không dùng
   hover, không dùng con trỏ.
3. **Kính, không phải khối đặc.** Bề mặt là lớp trắng trong suốt 11–15% + viền trắng 20–24% trên nền gradient; không đổ
   bóng, không blur thật (TV yếu, RAM 1 GB).
4. **Bố cục cố định, co giãn đều.** Thiết kế trên sân khấu 960×540dp, phóng to/thu nhỏ đồng đều theo màn hình (1280×720
   → ×1.333, 1920×1080 → ×2). Không reflow, không responsive kiểu web.
5. **Ít chữ, tiếng Việt tự nhiên, thân thiện.** Ví dụ: "Xin chào Thỏ Miu!", "Hết giờ xem TV rồi!", "Nghỉ ngơi một chút nhé."

## 2. Sân khấu và lưới

| Thông số | Giá trị |
|---|---|
| Sân khấu | 960 × 540 dp, canh giữa, letterbox nếu tỉ lệ khác 16:9 |
| Lề | trái/phải 40dp, trên 32dp |
| Khoảng cách chuẩn | 14dp giữa các thẻ, các ô dock |

**Màn hình chính** (từ trên xuống, tổng chiều cao ~503dp):

| Vùng | Cao | Nội dung |
|---|---|---|
| Thanh trên | 32 | Trái: chấm xanh 7dp + lời chào. Phải: nút tròn 32dp (ổ khoá trẻ em, Wi-Fi), cách 10dp; khi khoá trẻ em đang hẹn giờ, chữ vàng "Còn 14 phút" bên trái nút |
| Hero | 142 | Trái: đồng hồ `HH:mm` 138dp Light. Phải, canh đáy: thứ (34dp Bold), ngày dương (17dp), dòng âm lịch (12.5dp) |
| Hàng thẻ | 199 | 2 thẻ kính **rộng bằng nhau**: Lịch tháng · Thời tiết |
| (khoảng cách) | 17 | |
| Dock | 79 | 1–6 ô chia đều chiều ngang |

## 3. Màu và nền

- Chữ: trắng `#FFFFFF`; phụ `rgba(255,255,255,.70)`; mờ `.55`. Trên nền focus: `#15151C`.
- Kính: thẻ `rgba(255,255,255,.11)` viền `.20`; ô dock / nút `.13` viền `.24`; huy hiệu `.15` viền `.24`; đường kẻ `.14`.
- Nhấn: xanh focus/ngày hôm nay `#8EE08F` (chữ trên nó `#0F3B1C`), vàng `#FFC857` (khoá trẻ em, đang di chuyển, lỗi),
  hồng Chủ nhật `#F7A1B5`, vàng nhạt mùng 1 âm `#FFD27A`, chấm lời chào `#6EE7A8`, tím thương hiệu `#5B3FE0`.
- **Nền mesh**: màu nền + 5 đốm gradient tròn (đốm cuối là màu tối đặt ở giữa-trên để vùng đồng hồ dịu lại). Mỗi đốm:
  màu đặc ở tâm → 50% alpha ở 45% bán kính → trong suốt ở bán kính. Ba theme: **Tím hồng** (mặc định), **Đại dương**,
  **Hoàng hôn** — toạ độ trong `tokens.json > background`, sẵn CSS `--bg-tim-hong` / `--bg-dai-duong` / `--bg-hoang-hon`.
  Mọi màn hình (kể cả màn khoá) dùng cùng theme đang chọn.

## 4. Focus và trạng thái (quan trọng nhất)

| Trạng thái | Cách vẽ |
|---|---|
| Bình thường | Kính: nền trắng 13%, viền 1dp trắng 24%, bo 18dp |
| **Focus** | Nền kem `#F3EEE8`, viền 3dp `#8EE08F`, bo ngoài 20dp; chữ và icon chuyển `#15151C`; phóng to: ô dock ×1.035, thẻ lựa chọn ×1.06, nút tròn ×1.12 (nút tròn focus = nền trắng 95%, không viền xanh); 150ms ease-out |
| **Đang di chuyển** | Như focus nhưng viền vàng `#FFC857`; ô dock có thêm 2 tam giác ◀ ▶ ở hai mép |
| Bật/hoạt động | Nút tròn nền vàng `#FFC857`, icon tối (ví dụ khoá trẻ em đang hẹn giờ) |

## 5. Chữ

Font: **Roboto** (sans-serif mặc định của Android). Thang cỡ (dp / độ đậm):

| Vai trò | Cỡ | Đậm |
|---|---|---|
| Đồng hồ | 138 | Light 300, letter-spacing −0.02em |
| Đếm ngược khoá trẻ em | 96 | Light |
| Số lớn (nhiệt độ, thẻ chọn phút) | 54 | Light |
| Thứ trong tuần | 34 | Bold |
| Tiêu đề màn hình | 26 | Bold |
| Nhãn ô dock | 22 (co dần tới 14 nếu dài, rồi "…") | Bold |
| Ngày dương | 17 | Regular |
| Tiêu đề thẻ | 15 | Medium |
| Lời chào | 13.5 | Medium |
| Nội dung / mô tả | 12.5–13 | Regular |
| Số ngày trong lịch | 12 | Medium |
| Chi tiết nhỏ | 10 | Regular |
| Tiêu đề cột lịch (T2…CN) | 8.5 | Medium |
| Ngày âm | 7.5 | Regular |

## 6. Component

**Thẻ kính (card)** — padding 16dp, bo 18dp. Tiêu đề thẻ 15dp Medium. Có thể có huy hiệu 26dp (kính, bo 8dp) chứa icon.

**Lịch tháng** — tiêu đề "Tháng 10"; lưới 7 cột bắt đầu **Thứ Hai** (T2 … T7, CN); mỗi ô: số ngày dương 12dp + ngày âm
7.5dp ngay sau, cách 3dp. Ngày mùng 1 âm viết `1/9` màu vàng nhạt. Cột CN chữ hồng. Hôm nay: khối xanh `#8EE08F` bo 6dp,
chữ xanh đậm. Hàng cao tối đa 24dp (co lại khi tháng có 6 tuần). Âm lịch Việt Nam tính theo UTC+7 (thuật toán Hồ Ngọc
Đức — bản JS ở `reference/lunar.js`).

**Thời tiết** — huy hiệu icon + "Thời tiết"; dòng phụ "Địa điểm · cập nhật HH:mm" 9.5dp; tình trạng 16dp Medium; 2 dòng
chi tiết 10dp ("Cảm giác 32° · Độ ẩm 71%", "Cao 33° · Thấp 26° · Mưa 82%"); nhiệt độ 54dp Light canh phải; đường kẻ;
4 cột dự báo "Hôm nay, T2, T3, T4": icon 20dp + cao (trắng) + thấp (mờ).

**Ô dock** — cao 79dp; hộp icon 38dp bo 10dp cách mép trái 18dp; nhãn cách icon 12dp. Ô YouTube có hộp icon đen
`#0B0B0B`. App bất kỳ dùng icon thật của app thay cho hộp kính.

**Nút tròn** — 32dp, icon nét 55% đường kính.

**Lưới Ứng dụng** — 6 cột, ô kính cao 112dp bo 14dp, khoảng cách 14dp; giữa ô là icon app 46dp cắt bo 11dp (không dùng
banner ngang), dưới icon 12dp là tên app 12dp, quá dài thì "…". Focus: viền xanh 3dp trên nền kính (không đổi nền kem);
đang di chuyển: viền vàng. Tiêu đề màn hình 26dp + dòng hướng dẫn 12dp.

**Danh sách cài đặt** — cột rộng 640dp; mỗi dòng kính bo 14dp, padding 12/18dp: tên 16dp Medium + giá trị 13dp mờ.

**Thẻ lựa chọn** — 150×130dp (số lớn 54dp Light + chú thích 13dp) hoặc 200×130dp cho chữ (22dp Bold); hàng canh giữa,
cách nhau 16dp.

**Bàn phím PIN** — 8 ô số 30×40dp xếp nhóm `dd mm hh mm` (cách nhóm thêm 10dp), ô kế tiếp viền xanh 2dp; bàn phím 3×4
phím 64×42dp bo 12dp cách 10dp: `1–9`, `⌫`, `0`, `OK` (OK nền xanh 30%). Phím đang chọn dùng kiểu focus chuẩn. Đủ 8 số
thì tự kiểm tra; sai thì dòng vàng "Sai mã PIN, thử lại." và xoá ô.

**Bàn phím (IME)** — bảng phím ở nửa dưới màn hình, nền mesh theo theme + đường kẻ trắng 20% ở mép trên; dòng gợi ý
11dp mờ ở trên cùng; phím 58×36dp bo 10dp cách 6dp, hàng 10 đơn vị: số · `qwertyuiop` · `asdfghjkl-` ·
`⇧ zxcvbnm ⌫` (⇧, ⌫ rộng 1.5) · `?123 VI , . [dấu cách ×4] Xong` (?123 và Xong rộng 1.5, Xong nền xanh 30%,
⇧ bật = nền vàng). `?123` đổi sang trang ký hiệu cùng lưới: số · `@#$%&*-+=/` · `()[]{}!?:;` · `'"_~^<>|` + ⌫ ·
hàng cuối với phím `ABC` để quay lại. Chữ phím 20dp
Light, nhãn chữ 13dp Medium; phím đang chọn dùng kiểu focus chuẩn. Tiếng Việt gõ bằng Telex, không có hàng phím dấu.

**Màn khoá (overlay)** — phủ toàn màn hình: nền theme + lớp đen 40%; giữa là thẻ kính chứa ổ khoá vàng 44dp, "Hết giờ
xem TV rồi!" 26dp Bold, câu phụ 14dp, bàn phím PIN. Không có nút thoát; Back bị chặn.

## 7. Icon

- Icon chức năng: **nét** (stroke) tròn đầu, dày ~7.5% ô icon, một màu (trắng, hoặc `#1E1E26` khi focus): ổ khoá, Wi-Fi,
  YouTube (khung + tam giác), cửa hàng (túi + tam giác), bánh răng, lưới 4 ô, bảng màu.
- Icon thời tiết: **khối màu**: mặt trời `#FFC53D`, trăng `#F6E7A6`, mây `#F2F5FA` / xám `#C9D2E0`, mưa `#6CB8FF`,
  sấm `#FFD54A`.
- SVG 24×24 trong [`reference/icons.js`](reference/icons.js).

## 8. Điều hướng bằng remote

- ◀ ▶ ▲ ▼ đi đúng một ô; không xuyên qua thẻ không chọn được (lịch, thời tiết chỉ để xem).
- Từ dock bấm ▲ lên nút tròn gần nhất; từ nút tròn ▼ xuống dock. Mở lại màn hình thì focus về chỗ cũ.
- OK = mở; **giữ OK** = menu (Di chuyển, Thêm/Bỏ khỏi dock…); Back = quay lại; Home luôn về màn hình chính.
- Chế độ di chuyển: ◀ ▶ (▲ ▼ trong lưới) đổi chỗ, OK hoặc Back để xong.
- Mỗi màn hình con có dòng hướng dẫn phím ở đáy (12dp, mờ), ví dụ "◀ ▶ chọn · OK để bắt đầu · Back để quay lại".

## 9. Nên / không nên

| Nên | Không nên |
|---|---|
| Dùng đúng token màu, bo góc, cỡ chữ | Tự thêm màu mới, đổ bóng, gradient trên thẻ |
| Giữ sân khấu 960×540 và phóng đều | Bố cục responsive, cuộn ngang |
| Một kiểu focus cho mọi thứ | Focus chỉ đổi màu chữ, hoặc viền mảnh khó thấy |
| Số lớn nét mảnh (Light), nhãn đậm (Bold) | Chữ Light cho nhãn nhỏ |
| Chữ tiếng Việt ngắn, thân thiện | Thuật ngữ kỹ thuật, câu dài |
| Màn khoá cùng theme với màn chính | Hộp thoại hệ thống mặc định |
