# Lịch sử phiên bản

## 2.1.4 — 01/11/2026
- **Khoá ký mới** cho bản phát hành: không cài đè được lên bản cũ, phải gỡ bản cũ trước (mất cài đặt trong app).
  Chứng chỉ SHA-256: `b9b881f63c412690f217ae4ad02df44d9ccaf8c1f9a3f0b02eab5230c5d72778`.
- Thẻ thời tiết: số nhiệt độ cỡ cố định 100dp, chân số ngang dòng chi tiết cuối.

## 2.1.3 — 01/11/2026
- Thẻ thời tiết: số nhiệt độ cao từ ngang chữ "Thời tiết" xuống dòng chi tiết cuối cùng (tự co theo bề rộng thẻ).

## 2.1.2 — 01/11/2026
- Thẻ thời tiết: nhiệt độ to hơn (72dp); bên trái thêm gió, chỉ số UV và giờ mặt trời mọc / lặn.

## 2.1.1 — 01/11/2026
- Thẻ thời tiết trên màn hình chính bỏ hàng dự báo 4 ngày; thông tin hiện tại (tình trạng, nhiệt độ, cảm giác, độ ẩm,
  cao/thấp, mưa) căn giữa thẻ. Dự báo theo giờ và 7 ngày vẫn có trong màn Thời tiết (chọn thẻ, bấm OK).

## 2.1 — 01/11/2026
- **Hàng kênh VTV** trên màn hình chính, giữa thẻ lịch/thời tiết và dock: logo VTV1–VTV9 (và VTV5 Tây Nam Bộ). Bấm OK
  để xem trực tiếp ngay trong Tivi Home (luồng phát miễn phí của VTVgo), ▲ ▼ / CH+ CH− đổi kênh, Back để thoát.
- **Cài đặt giao diện → Kênh VTV**: chọn kênh hiện trên hàng, hoặc ẩn cả hàng. Khi hàng kênh hiện, dock thấp hơn
  (60dp) để hàng logo cao hơn.
- **Ảnh nền mẫu** (núi lúc hoàng hôn) có sẵn trong APK: lần đầu cài là ảnh nền mặc định; "Dùng ảnh mẫu Tivi Home"
  trong mục Ảnh nền để chọn lại.

## 2.0 — 01/11/2026
- **Hỗ trợ Android 5.0 – 7.1** (trước đây cần Android 8.0): mọi hàm của Android mới đều có đường thay thế cho máy cũ
  (quyền, màn khoá trẻ em, thông báo, âm thanh khi khoá, chữ trên thẻ thời tiết, danh sách ổ đĩa, hộp thoại).
- Bàn phím Tivi: bỏ dòng chú thích phía trên bàn phím; phím "Nói" thay bằng icon micro. Dòng trạng thái chỉ hiện khi
  đang gõ bằng giọng nói.

## 1.9 — 01/11/2026
- Nút **Ngõ vào** ở góc trên bên phải màn hình chính (cạnh ổ khoá) cho TV có HDMI / AV / truyền hình: danh sách ngõ
  vào lấy từ TV, chọn để chuyển sang ngõ đó bằng ứng dụng Live TV của TV.
- Chỉ hiện khi TV có ngõ vào thật (HDMI/AV, hoặc bộ thu truyền hình của hệ thống); bỏ qua các "ngõ vào" giả do ứng
  dụng như VLC đăng ký. TV box không có ngõ vào nên không hiện nút.

## 1.8.3 — 28/10/2026
- Bàn phím Tivi: phím đang chọn giữ nguyên bo góc và viền như các phím khác, chỉ đổi nền sang màu kem (trước đây có
  khung viền xanh to, bo khác).

## 1.8.2 — 28/10/2026
- **Gõ bằng giọng nói** trên Bàn phím Tivi: phím **Nói** ở hàng dưới; nút micro/tìm kiếm trên remote cũng bắt đầu nói
  khi bàn phím đang mở. Chữ hiện dần trên dòng gợi ý, nói xong thì chèn vào ô đang nhập. VI nhận tiếng Việt, ABC nhận
  tiếng Anh. Dùng dịch vụ giọng nói của TV (Google), cần mạng.
- Lần đầu bấm Nói, TV hỏi quyền micro; `install.ps1` cấp sẵn quyền này.
- Báo lỗi bằng tiếng Việt: không nghe rõ, mất mạng, không mở được micro, TV chưa có tiếng Việt.

## 1.8.1 — 27/10/2026
- **Dọn dẹp** trong Cài đặt giao diện: RAM và bộ nhớ trống thật của TV, *Giải phóng RAM* (báo số MB thực sự giải
  phóng), *Xoá bộ nhớ đệm Tivi Home*, *Xoá bộ nhớ đệm ứng dụng khác* (mở trang Bộ nhớ của TV). Trên Android 14,
  ứng dụng thường không được đóng hay xoá bộ nhớ đệm của ứng dụng khác.
- Footer Cài đặt giao diện: "Tivi Home V<phiên bản> · Developed by TuanPA".

## 1.8 — 27/10/2026
- **Ảnh nền màn hình chính** sau lớp kính mờ: tự quét ảnh trong bộ nhớ trong và USB, độ mờ Rõ nét / Mờ nhẹ / Mờ
  nhiều, Lấp đầy hoặc Vừa khung.
- **Màn hình chờ** 6 mẫu: Đồng hồ kính, Lịch âm dương, Thời tiết, Đêm tối giản, Ảnh nền, Tuỳ chỉnh (trình chiếu tối
  đa 30 ảnh). Thời gian chờ, xem thử, đặt làm màn hình chờ của TV.
- Cài đặt giao diện xếp thẻ 2 cột, footer ghi phiên bản.

## 1.7 — 26/10/2026
- Màn **Lịch âm dương**: Can Chi, tiết khí, ngày hoàng đạo / hắc đạo, xung tuổi, giờ hoàng đạo, ngày lễ.
- Màn **Thời tiết**: hiện tại, 16 giờ tới, 7 ngày tới.
- Thẻ lịch và thẻ thời tiết trên màn hình chính chọn được bằng remote.

## 1.6.1 — 25/10/2026
- Bản phát hành đầu tiên trên GitHub: màn hình chính có âm lịch và thời tiết, dock, màn Ứng dụng, khoá trẻ em,
  Bàn phím Tivi (Telex, bảng ?123).
