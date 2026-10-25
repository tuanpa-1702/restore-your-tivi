package com.tivibox.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.function.Predicate;

/** Full-screen "time is up" screen with the PIN pad. Swallows Back so it can only be left with the PIN. */
public class LockScreenView extends FrameLayout {
    private final PinPadView pad;
    private final TextView error;

    public LockScreenView(Context context, int theme, Predicate<String> unlock) {
        super(context);
        float dp = context.getResources().getDisplayMetrics().density;
        MeshBackgroundView bg = new MeshBackgroundView(context, null);
        bg.setTheme(theme);
        addView(bg, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        View dim = new View(context);
        dim.setBackgroundColor(0x66000000);
        addView(dim, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        column.setBackgroundResource(R.drawable.app_item_bg);
        int padX = Math.round(48 * dp), padY = Math.round(18 * dp);
        column.setPadding(padX, padY, padX, padY);
        addView(column, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        View icon = new View(context) {
            @Override
            protected void onDraw(Canvas c) {
                Glyphs.draw(c, Glyphs.LOCK, getWidth() / 2f, getHeight() / 2f, getHeight(), 0xFFFFC857);
            }
        };
        column.addView(icon, new LinearLayout.LayoutParams(Math.round(44 * dp), Math.round(44 * dp)));
        column.addView(text(context, "Hết giờ xem TV rồi!", 26, 0xFFFFFFFF, true), wrap(8 * dp));
        column.addView(text(context, "Nghỉ ngơi một chút nhé. Bố mẹ nhập mã PIN để mở khoá.", 14, 0xCCFFFFFF, false), wrap(4 * dp));

        pad = new PinPadView(context);
        pad.setListener(pin -> {
            if (!unlock.test(pin)) {
                setError("Sai mã PIN, thử lại.");
                pad.clear();
            }
        });
        column.addView(pad, wrap(14 * dp));
        error = text(context, "", 14, 0xFFFFC857, false);
        column.addView(error, wrap(4 * dp));
    }

    private static TextView text(Context c, String s, float sizeDp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, sizeDp);
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        return t;
    }

    private static LinearLayout.LayoutParams wrap(float topMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Math.round(topMargin);
        return lp;
    }

    void setError(String message) {
        error.setText(message);
    }

    void focusPin() {
        pad.requestFocus();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) return true;
        if (!pad.hasFocus()) pad.requestFocus();
        return super.dispatchKeyEvent(event);
    }
}
