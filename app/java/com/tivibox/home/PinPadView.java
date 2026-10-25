package com.tivibox.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.KeyEvent;
import android.view.View;

/**
 * Eight-digit PIN entry with an on-screen number pad for the TV remote: ◀▶▲▼ pick a key, OK presses it.
 * Number keys on the remote type directly. The PIN is checked as soon as the eighth digit is in.
 * Entered digits show as boxes grouped dd mm hh mm.
 */
public class PinPadView extends View {
    interface Listener {
        void onSubmit(String pin);
    }

    private static final int N = ChildLock.PIN_LENGTH;
    private static final String[] KEYS = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0", "OK"};
    private static final int COLS = 3, ROWS = 4, DEL = 9, OK = 11;
    // Display boxes 30x40dp, 6dp apart, 10dp extra between groups; keys 64x42dp, 10dp apart.
    private static final float BOX_W = 30, BOX_H = 40, BOX_GAP = 6, GROUP_GAP = 10;
    private static final float KEY_W = 64, KEY_H = 42, KEY_GAP = 10, PAD_TOP = 58;

    private final float dp;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Typeface light = Typeface.create("sans-serif-light", Typeface.NORMAL);
    private final Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private final StringBuilder pin = new StringBuilder();
    private int selected = 0;
    private Listener listener;

    public PinPadView(Context context) {
        super(context);
        dp = context.getResources().getDisplayMetrics().density;
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    void clear() {
        pin.setLength(0);
        invalidate();
    }

    private float displayWidth() {
        return (N * BOX_W + (N - 1) * BOX_GAP + 3 * GROUP_GAP) * dp;
    }

    private float padWidth() {
        return (COLS * KEY_W + (COLS - 1) * KEY_GAP) * dp;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        float w = Math.max(displayWidth(), padWidth()) + 8 * dp;
        float h = (PAD_TOP + ROWS * KEY_H + (ROWS - 1) * KEY_GAP) * dp + 8 * dp;
        setMeasuredDimension(Math.round(w), Math.round(h));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
            type(keyCode - KeyEvent.KEYCODE_0);
            return true;
        }
        if (keyCode >= KeyEvent.KEYCODE_NUMPAD_0 && keyCode <= KeyEvent.KEYCODE_NUMPAD_9) {
            type(keyCode - KeyEvent.KEYCODE_NUMPAD_0);
            return true;
        }
        int col = selected % COLS, row = selected / COLS;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                col = Math.max(0, col - 1);
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                col = Math.min(COLS - 1, col + 1);
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                row = Math.max(0, row - 1);
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                row = Math.min(ROWS - 1, row + 1);
                break;
            case KeyEvent.KEYCODE_DEL:
                backspace();
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                press(selected);
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
        selected = row * COLS + col;
        invalidate();
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) return super.onKeyUp(keyCode, event);
        return true;
    }

    private void press(int key) {
        if (key == DEL) backspace();
        else if (key == OK) submit();
        else type(Integer.parseInt(KEYS[key]));
    }

    private void type(int digit) {
        if (pin.length() >= N) return;
        pin.append(digit);
        invalidate();
        if (pin.length() == N) submit();
    }

    private void backspace() {
        if (pin.length() > 0) pin.setLength(pin.length() - 1);
        invalidate();
    }

    private void submit() {
        if (pin.length() == N && listener != null) listener.onSubmit(pin.toString());
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth();
        boolean focused = hasFocus();

        // Entered digits.
        float x = (w - displayWidth()) / 2, top = 4 * dp;
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(light);
        paint.setTextSize(24 * dp);
        for (int i = 0; i < N; i++) {
            boolean next = i == pin.length();
            Glass.panel(c, paint, rect, x, top, x + BOX_W * dp, top + BOX_H * dp, 8 * dp, dp,
                    0x26FFFFFF, next && focused ? 0xFF8EE08F : 0x4DFFFFFF);
            if (next && focused) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2 * dp);
                paint.setColor(0xFF8EE08F);
                rect.set(x, top, x + BOX_W * dp, top + BOX_H * dp);
                c.drawRoundRect(rect, 8 * dp, 8 * dp, paint);
                paint.setStyle(Paint.Style.FILL);
            }
            if (i < pin.length()) {
                paint.setColor(0xFFFFFFFF);
                c.drawText(String.valueOf(pin.charAt(i)), x + BOX_W * dp / 2, Glass.baseline(paint, top + BOX_H * dp / 2), paint);
            }
            x += (BOX_W + BOX_GAP) * dp + (i % 2 == 1 ? GROUP_GAP * dp : 0);
        }

        // Number pad.
        float left = (w - padWidth()) / 2, padTop = PAD_TOP * dp;
        for (int k = 0; k < KEYS.length; k++) {
            float kx = left + (k % COLS) * (KEY_W + KEY_GAP) * dp;
            float ky = padTop + (k / COLS) * (KEY_H + KEY_GAP) * dp;
            boolean at = focused && k == selected;
            c.save();
            c.translate(kx, ky);
            if (at) {
                ChoiceView.drawCard(c, paint, rect, KEY_W * dp, KEY_H * dp, dp, true, false);
            } else {
                int fill = k == OK ? 0x4D8EE08F : 0x21FFFFFF;
                Glass.panel(c, paint, rect, 0, 0, KEY_W * dp, KEY_H * dp, 12 * dp, dp, fill, 0x3DFFFFFF);
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            boolean word = k == DEL || k == OK;
            paint.setTypeface(word ? medium : light);
            paint.setTextSize((word ? 17 : 24) * dp);
            paint.setColor(at ? 0xFF15151C : 0xFFFFFFFF);
            c.drawText(KEYS[k], KEY_W * dp / 2, Glass.baseline(paint, KEY_H * dp / 2), paint);
            c.restore();
        }
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, android.graphics.Rect previous) {
        super.onFocusChanged(gainFocus, direction, previous);
        invalidate();
    }
}
