package com.tivibox.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

/**
 * Dock tile: glass when idle, cream with a green ring when focused, amber ring with arrows while it is being moved.
 * Built-in entries draw a glyph; other apps show their own icon.
 */
public class TileView extends View {
    private final float dp;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path arrow = new Path();
    private final Typeface bold = Typeface.create("sans-serif-medium", Typeface.BOLD);
    private String label = "";
    private int glyph;
    private Drawable icon;
    private boolean moving;

    public TileView(Context context, AttributeSet attrs) {
        super(context, attrs);
        dp = context.getResources().getDisplayMetrics().density;
        setFocusable(true);
        setClickable(true);
        setLongClickable(true);
    }

    public void setup(String label, int glyph, Drawable icon) {
        this.label = label;
        this.glyph = glyph;
        this.icon = icon;
        setContentDescription(label);
        invalidate();
    }

    public void setMoving(boolean moving) {
        if (this.moving == moving) return;
        this.moving = moving;
        invalidate();
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previous) {
        super.onFocusChanged(focused, direction, previous);
        float s = focused ? 1.035f : 1f;
        animate().scaleX(s).scaleY(s).setDuration(150).start();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        boolean f = isFocused() || moving;
        if (f) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(moving ? 0xFFFFC857 : 0xFF8EE08F);
            rect.set(0, 0, w, h);
            c.drawRoundRect(rect, 20 * dp, 20 * dp, paint);
            paint.setColor(0xFFF3EEE8);
            rect.set(3 * dp, 3 * dp, w - 3 * dp, h - 3 * dp);
            c.drawRoundRect(rect, 17 * dp, 17 * dp, paint);
        } else {
            Glass.panel(c, paint, rect, 0, 0, w, h, 18 * dp, dp, 0x21FFFFFF, 0x3DFFFFFF);
        }

        float box = 38 * dp, bx = (moving ? 22 : 18) * dp, by = (h - box) / 2;
        if (glyph == 0 && icon != null) {
            icon.setBounds(Math.round(bx), Math.round(by), Math.round(bx + box), Math.round(by + box));
            icon.draw(c);
        } else {
            int glyphColor;
            if (glyph == Glyphs.YOUTUBE) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(0xFF0B0B0B);
                rect.set(bx, by, bx + box, by + box);
                c.drawRoundRect(rect, 10 * dp, 10 * dp, paint);
                glyphColor = 0xFFFFFFFF;
            } else if (f) {
                Glass.panel(c, paint, rect, bx, by, bx + box, by + box, 10 * dp, dp, 0x14000000, 0x24000000);
                glyphColor = 0xFF1E1E26;
            } else {
                Glass.panel(c, paint, rect, bx, by, bx + box, by + box, 10 * dp, dp, 0x24FFFFFF, 0x40FFFFFF);
                glyphColor = 0xFFFFFFFF;
            }
            Glyphs.draw(c, glyph, bx + box / 2, by + box / 2, box * .62f, glyphColor);
        }

        float tx = bx + box + 12 * dp;
        float max = w - tx - (moving ? 22 : 12) * dp;
        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(bold);
        float size = 22 * dp;
        paint.setTextSize(size);
        while (paint.measureText(label) > max && size > 14 * dp) {
            size -= dp;
            paint.setTextSize(size);
        }
        String text = label;
        if (paint.measureText(text) > max) {
            while (text.length() > 1 && paint.measureText(text + "…") > max) text = text.substring(0, text.length() - 1);
            text = text.trim() + "…";
        }
        paint.setColor(f ? 0xFF15151C : 0xF2FFFFFF);
        c.drawText(text, tx, Glass.baseline(paint, h / 2), paint);

        if (moving) {
            paint.setColor(0xFF7A5A00);
            float cy = h / 2, a = 6 * dp;
            arrow.reset();
            arrow.moveTo(7 * dp, cy);
            arrow.lineTo(7 * dp + a, cy - a);
            arrow.lineTo(7 * dp + a, cy + a);
            arrow.close();
            arrow.moveTo(w - 7 * dp, cy);
            arrow.lineTo(w - 7 * dp - a, cy - a);
            arrow.lineTo(w - 7 * dp - a, cy + a);
            arrow.close();
            c.drawPath(arrow, paint);
        }
    }
}
