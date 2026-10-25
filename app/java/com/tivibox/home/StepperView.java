package com.tivibox.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.KeyEvent;
import android.view.View;

/** Minute picker for the remote: ▲▼ change by 5, ◀▶ by 1, number keys type, OK confirms. */
public class StepperView extends View {
    interface Listener {
        void onConfirm(int minutes);
    }

    static final int MIN = 1, MAX = 240;
    private final float dp;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path arrow = new Path();
    private final Typeface light = Typeface.create("sans-serif-light", Typeface.NORMAL);
    private int value;
    private boolean typing;
    private Listener listener;

    public StepperView(Context context, int value) {
        super(context);
        dp = context.getResources().getDisplayMetrics().density;
        this.value = clamp(value);
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    private static int clamp(int v) {
        return Math.max(MIN, Math.min(MAX, v));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int digit = -1;
        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) digit = keyCode - KeyEvent.KEYCODE_0;
        if (keyCode >= KeyEvent.KEYCODE_NUMPAD_0 && keyCode <= KeyEvent.KEYCODE_NUMPAD_9) {
            digit = keyCode - KeyEvent.KEYCODE_NUMPAD_0;
        }
        if (digit >= 0) {
            int next = typing ? value * 10 + digit : digit;
            value = next > MAX ? digit : next;
            typing = true;
            invalidate();
            return true;
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                value = clamp(value < 5 ? 5 : value + 5 - value % 5);
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                value = clamp(value % 5 == 0 ? value - 5 : value - value % 5);
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                value = clamp(value + 1);
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                value = clamp(value - 1);
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                value = clamp(value);
                if (listener != null) listener.onConfirm(value);
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
        typing = false;
        invalidate();
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) return super.onKeyUp(keyCode, event);
        return true;
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float top = 22 * dp, bottom = h - 22 * dp;
        c.save();
        c.translate(0, top);
        ChoiceView.drawCard(c, paint, rect, w, bottom - top, dp, true, false);
        c.restore();
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(light);
        paint.setTextSize(64 * dp);
        paint.setColor(0xFF15151C);
        c.drawText(String.valueOf(value), w / 2, Glass.baseline(paint, top + (bottom - top) * .43f), paint);
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(13 * dp);
        paint.setColor(0xB315151C);
        String caption = value >= 60 ? "phút · " + ChildLock.duration(value) : "phút";
        c.drawText(caption, w / 2, Glass.baseline(paint, top + (bottom - top) * .8f), paint);
        paint.setColor(0xCCFFFFFF);
        float a = 8 * dp, cx = w / 2;
        arrow.reset();
        arrow.moveTo(cx, 2 * dp);
        arrow.lineTo(cx - a, 2 * dp + a);
        arrow.lineTo(cx + a, 2 * dp + a);
        arrow.close();
        arrow.moveTo(cx, h - 2 * dp);
        arrow.lineTo(cx - a, h - 2 * dp - a);
        arrow.lineTo(cx + a, h - 2 * dp - a);
        arrow.close();
        c.drawPath(arrow, paint);
    }
}
