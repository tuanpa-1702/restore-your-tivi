package com.tivibox.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/** A choice card in the dock-tile style: big text with a caption under it. */
public class ChoiceView extends View {
    private final float dp;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Typeface light = Typeface.create("sans-serif-light", Typeface.NORMAL);
    private final Typeface medium = Typeface.create("sans-serif-medium", Typeface.BOLD);
    private String big = "", caption = "";
    private float bigSize;
    private boolean lightBig;

    public ChoiceView(Context context) {
        super(context);
        dp = context.getResources().getDisplayMetrics().density;
        setFocusable(true);
        setClickable(true);
    }

    /** Numbers are drawn light and large like the clock; words medium. */
    ChoiceView set(String big, String caption, float bigSizeDp, boolean lightBig) {
        this.big = big;
        this.caption = caption;
        this.bigSize = bigSizeDp;
        this.lightBig = lightBig;
        setContentDescription(big + " " + caption);
        invalidate();
        return this;
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previous) {
        super.onFocusChanged(focused, direction, previous);
        float s = focused ? 1.06f : 1f;
        animate().scaleX(s).scaleY(s).setDuration(150).start();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        boolean f = isFocused();
        drawCard(c, paint, rect, w, h, dp, f, false);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(lightBig ? light : medium);
        paint.setTextSize(bigSize * dp);
        paint.setColor(f ? 0xFF15151C : 0xFFFFFFFF);
        float bigY = caption.isEmpty() ? h / 2 : h * .44f;
        c.drawText(big, w / 2, Glass.baseline(paint, bigY), paint);
        if (!caption.isEmpty()) {
            paint.setTypeface(Typeface.DEFAULT);
            paint.setTextSize(13 * dp);
            paint.setColor(f ? 0xB315151C : 0xB3FFFFFF);
            c.drawText(caption, w / 2, Glass.baseline(paint, h * .78f), paint);
        }
    }

    /** Shared card look: glass when idle, cream with a green (or amber) ring when focused. */
    static void drawCard(Canvas c, Paint p, RectF r, float w, float h, float dp, boolean focused, boolean amber) {
        if (focused) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(amber ? 0xFFFFC857 : 0xFF8EE08F);
            r.set(0, 0, w, h);
            c.drawRoundRect(r, 20 * dp, 20 * dp, p);
            p.setColor(0xFFF3EEE8);
            r.set(3 * dp, 3 * dp, w - 3 * dp, h - 3 * dp);
            c.drawRoundRect(r, 17 * dp, 17 * dp, p);
        } else {
            Glass.panel(c, p, r, 0, 0, w, h, 18 * dp, dp, 0x21FFFFFF, 0x3DFFFFFF);
        }
    }
}
