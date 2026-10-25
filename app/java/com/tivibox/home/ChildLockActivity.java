package com.tivibox.home;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Child lock screen in the home screen's style: pick a watch time (presets or the parent's own number of minutes),
 * see the countdown, lock now, or change / switch off the timer with the PIN.
 */
public class ChildLockActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Prefs prefs;
    private FrameLayout page;
    private TextView subtitle;
    private TextView countdown;
    private Runnable back;
    private boolean fallbackLock;
    private float dp;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (countdown == null) return;
            if (!ChildLock.active(prefs) || prefs.isLocked()) {
                show();
                return;
            }
            countdown.setText(ChildLock.countdown(prefs));
            handler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_lock);
        prefs = new Prefs(this);
        dp = getResources().getDisplayMetrics().density;
        page = findViewById(R.id.page);
        subtitle = findViewById(R.id.subtitle);
        ((MeshBackgroundView) findViewById(R.id.background)).setTheme(prefs.theme());
        RoundButton badge = findViewById(R.id.badge);
        badge.setup(Glyphs.LOCK, "Khoá trẻ em");
        badge.setActive(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    @Override
    public void onBackPressed() {
        if (fallbackLock) return;
        if (back != null) back.run();
        else super.onBackPressed();
    }

    /** Picks the page for the current state. */
    private void show() {
        handler.removeCallbacks(tick);
        countdown = null;
        fallbackLock = false;
        if (prefs.isLocked()) {
            if (ChildLock.canCoverApps(this)) {
                finish(); // LockService's overlay is up
            } else {
                fallbackLock = true;
                showPin("Hết giờ xem TV rồi! Bố mẹ nhập mã PIN để mở khoá.", null, () -> {
                    ChildLock.clear(this);
                    finish();
                });
            }
        } else if (!ChildLock.canCoverApps(this)) {
            showPermission();
        } else if (ChildLock.active(prefs)) {
            showActive();
        } else {
            showPick();
        }
    }

    private void showPick() {
        back = null;
        subtitle.setText("Cho bé xem thêm bao lâu rồi khoá? Hết giờ, TV hiện màn hình khoá và dừng phim.");
        LinearLayout row = row();
        for (int m : ChildLock.MINUTES) {
            ChoiceView c = choice(row, m < 60 ? String.valueOf(m) : String.valueOf(m / 60), m < 60 ? "phút" : "tiếng", 54, true);
            c.setOnClickListener(v -> start(m));
        }
        int custom = prefs.customLockMinutes();
        ChoiceView c = choice(row, String.valueOf(custom), "phút · tuỳ chỉnh", 54, true);
        c.setOnClickListener(v -> showCustom());
        hint("◀ ▶ chọn · OK để bắt đầu · Back để quay lại");
        row.getChildAt(0).requestFocus();
    }

    private void showCustom() {
        back = this::showPick;
        subtitle.setText("Tự chọn số phút cho bé xem (1–240 phút). Số này được nhớ cho lần sau.");
        page.removeAllViews();
        LinearLayout column = column();
        StepperView stepper = new StepperView(this, prefs.customLockMinutes());
        stepper.setListener(minutes -> {
            prefs.setCustomLockMinutes(minutes);
            start(minutes);
        });
        column.addView(stepper, new LinearLayout.LayoutParams(px(220), px(200)));
        hint("▲ ▼ đổi 5 phút · ◀ ▶ đổi 1 phút · có thể bấm số trên remote · OK để bắt đầu");
        stepper.requestFocus();
    }

    private void showActive() {
        back = null;
        subtitle.setText("Đang hẹn giờ. Hết giờ, TV sẽ khoá cho đến khi bố mẹ nhập mã PIN.");
        page.removeAllViews();
        LinearLayout column = column();
        countdown = text(ChildLock.countdown(prefs), 96, 0xFFFFFFFF);
        countdown.setTypeface(android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL));
        column.addView(countdown, wrap(0));
        TextView caption = text("còn lại trước khi khoá", 14, 0xB3FFFFFF);
        column.addView(caption, wrap(0));
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        row.setClipChildren(false);
        column.addView(row, wrap(28 * dp));
        choice(row, "Khoá ngay", "không cần mã", 22, false).setOnClickListener(v -> {
            ChildLock.lockNow(this);
            finish();
        });
        choice(row, "Đổi thời gian", "cần mã PIN", 22, false).setOnClickListener(v ->
                showPin("Nhập mã PIN để đổi thời gian.", this::showActive, this::showPick));
        choice(row, "Tắt hẹn giờ", "cần mã PIN", 22, false).setOnClickListener(v ->
                showPin("Nhập mã PIN để tắt hẹn giờ.", this::showActive, () -> {
                    ChildLock.clear(this);
                    Toast.makeText(this, "Đã tắt khoá trẻ em.", Toast.LENGTH_SHORT).show();
                    finish();
                }));
        row.getChildAt(0).requestFocus();
        handler.post(tick);
    }

    /** `cancel` runs on Back (null: Back does nothing), `ok` after the right PIN. */
    private void showPin(String message, Runnable cancel, Runnable ok) {
        handler.removeCallbacks(tick);
        countdown = null;
        back = cancel;
        subtitle.setText(message);
        page.removeAllViews();
        LinearLayout column = column();
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setBackgroundResource(R.drawable.app_item_bg);
        card.setPadding(px(28), px(12), px(28), px(16));
        column.addView(card, wrap(0));
        card.addView(text("Mã PIN gồm 8 số", 14, 0xB3FFFFFF), wrap(0));
        PinPadView pad = new PinPadView(this);
        card.addView(pad, wrap(4 * dp));
        TextView error = text("", 14, 0xFFFFC857);
        card.addView(error, wrap(4 * dp));
        pad.setListener(pin -> {
            if (ChildLock.pinMatches(pin)) {
                ok.run();
            } else {
                error.setText("Sai mã PIN, thử lại.");
                pad.clear();
            }
        });
        hint("◀ ▶ ▲ ▼ chọn phím, OK để bấm · hoặc bấm số trên remote · ⌫ xoá một số");
        pad.requestFocus();
    }

    private void showPermission() {
        back = null;
        subtitle.setText("Để khoá được cả khi bé đang xem YouTube hay app khác, Tivi Home cần quyền "
                + "“Hiển thị trên các ứng dụng khác”.");
        LinearLayout row = row();
        choice(row, "Mở cài đặt quyền", "rồi bật cho Tivi Home", 22, false).setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "TV này không có trang cấp quyền. Cấp qua ADB: appops set "
                        + getPackageName() + " SYSTEM_ALERT_WINDOW allow", Toast.LENGTH_LONG).show();
            }
        });
        row.getChildAt(0).requestFocus();
    }

    private void start(int minutes) {
        ChildLock.start(this, minutes);
        Toast.makeText(this, "TV sẽ khoá sau " + ChildLock.duration(minutes) + ".", Toast.LENGTH_LONG).show();
        finish();
    }

    // ------------------------------------------------------------------ building blocks

    private LinearLayout column() {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        column.setClipChildren(false);
        page.addView(column, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.TOP));
        ((FrameLayout.LayoutParams) column.getLayoutParams()).topMargin = px(20);
        return column;
    }

    private LinearLayout row() {
        page.removeAllViews();
        LinearLayout column = column();
        ((FrameLayout.LayoutParams) column.getLayoutParams()).topMargin = px(70);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        row.setClipChildren(false);
        column.addView(row, wrap(0));
        return row;
    }

    private ChoiceView choice(LinearLayout row, String big, String caption, float bigSize, boolean lightBig) {
        ChoiceView c = new ChoiceView(this).set(big, caption, bigSize, lightBig);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(px(lightBig ? 150 : 200), px(130));
        if (row.getChildCount() > 0) lp.setMarginStart(px(16));
        row.addView(c, lp);
        return c;
    }

    private void hint(String s) {
        TextView t = text(s, 12, 0x99FFFFFF);
        page.addView(t, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
        ((FrameLayout.LayoutParams) t.getLayoutParams()).bottomMargin = px(28);
    }

    private TextView text(String s, float sizeDp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, sizeDp);
        return t;
    }

    private static LinearLayout.LayoutParams wrap(float topMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Math.round(topMargin);
        return lp;
    }

    private int px(float v) {
        return Math.round(v * dp);
    }
}
