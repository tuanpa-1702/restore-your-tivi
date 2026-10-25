# Câu lệnh mẫu cho AI

Các câu lệnh dưới đây đã có sẵn link repo https://github.com/tuanpa-1702/restore-your-tivi, chỉ cần thay phần trong `<...>`.

## 1. Thiết kế một màn hình mới theo phong cách Tivi Home

```
Đọc design system tại https://github.com/tuanpa-1702/restore-your-tivi (bắt đầu từ AGENTS.md, rồi design/DESIGN.md và design/tokens/tokens.json,
xem mẫu trong design/reference/ và ảnh trong design/screenshots/).
Thiết kế màn hình "<tên màn hình, ví dụ: Danh sách kênh truyền hình>" cho TV theo đúng design system đó:
sân khấu 960x540, nền mesh "Tím hồng", thẻ kính, focus nền kem viền xanh, chữ Roboto, điều khiển bằng remote.
Xuất ra một file HTML độc lập dùng design/tokens/tokens.css.
```

## 2. Thêm một widget vào màn hình chính

```
Theo design system ở https://github.com/tuanpa-1702/restore-your-tivi, thiết kế thêm thẻ "<tên widget, ví dụ: Lịch học của bé>" thay cho thẻ thời tiết
trên màn hình chính (cùng kích thước, kiểu thẻ kính, tiêu đề 15dp, có huy hiệu icon). Dựa trên design/reference/home.html.
```

## 3. Dựng lại bằng nền tảng khác

```
Dùng design system ở https://github.com/tuanpa-1702/restore-your-tivi để dựng lại màn hình chính bằng <Jetpack Compose / Flutter / SwiftUI / Figma>.
Giữ nguyên token màu, cỡ chữ, bo góc, kiểu focus và sân khấu 960x540 phóng to đều theo màn hình.
```

## 4. Cài Tivi Home lên TV của bạn (dùng AI có quyền chạy lệnh: Claude Code, Codex, Cursor…)

```
Clone https://github.com/tuanpa-1702/restore-your-tivi, đọc AGENTS.md và README.md. TV box Android của tôi ở IP <192.168.1.50>, đã bật USB debugging.
Build APK bằng build.ps1, cài bằng install.ps1, chụp màn hình kiểm tra. Hỏi tôi trước khi đặt làm Home mặc định
hoặc tắt launcher cũ; chỉ tắt (pm disable-user), không gỡ ứng dụng hệ thống.
```

## 5. Kiểm tra một thiết kế có đúng phong cách không

```
So sánh thiết kế sau với design system ở https://github.com/tuanpa-1702/restore-your-tivi (design/DESIGN.md mục "Nên / không nên" và design/screenshots/).
Liệt kê chỗ sai token, sai kiểu focus hoặc sai bố cục, kèm cách sửa.
```

## Mẹo

- Nếu AI không mở được link repo, dán thêm link file thô:
  `https://raw.githubusercontent.com/tuanpa-1702/restore-your-tivi/main/design/DESIGN.md` và `.../design/tokens/tokens.json`.
- Với AI đọc được ảnh, đính kèm `design/screenshots/01-home.png` để nó so sánh trực quan.
- Claude Code / Cursor / Codex clone repo về sẽ tự đọc `AGENTS.md` (Claude Code đọc qua `CLAUDE.md`).
