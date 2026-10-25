package com.tivibox.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/** Round glass button for the top bar (customise, Wi-Fi). */
public class RoundButton extends View {
    private final float dp;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int glyph;
    private boolean active;

    public RoundButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        dp = context.getResources().getDisplayMetrics().density;
        setFocusable(true);
        setClickable(true);
    }

    public void setup(int glyph, String description) {
        this.glyph = glyph;
        setContentDescription(description);
        invalidate();
    }

    /** Amber fill while the thing it controls is on (the child-lock timer). */
    public void setActive(boolean active) {
        if (this.active == active) return;
        this.active = active;
        invalidate();
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previous) {
        super.onFocusChanged(focused, direction, previous);
        float s = focused ? 1.12f : 1f;
        animate().scaleX(s).scaleY(s).setDuration(150).start();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f, r = Math.min(cx, cy) - dp;
        boolean f = isFocused();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(f ? 0xF2FFFFFF : active ? 0xFFFFC857 : 0x26FFFFFF);
        c.drawCircle(cx, cy, r, paint);
        if (!f && !active) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp);
            paint.setColor(0x47FFFFFF);
            c.drawCircle(cx, cy, r, paint);
        }
        Glyphs.draw(c, glyph, cx, cy, r * 1.1f, f || active ? 0xFF1E1E26 : 0xFFFFFFFF);
    }
}
