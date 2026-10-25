package com.tivibox.home;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** App-style icon for the launcher's own entries: a glyph on a coloured rounded square. */
final class GlyphDrawable extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final int glyph, color;

    GlyphDrawable(int glyph, int color) {
        this.glyph = glyph;
        this.color = color;
    }

    @Override
    public void draw(Canvas c) {
        Rect b = getBounds();
        float s = Math.min(b.width(), b.height());
        float cx = b.exactCenterX(), cy = b.exactCenterY();
        rect.set(cx - s / 2, cy - s / 2, cx + s / 2, cy + s / 2);
        paint.setColor(color);
        c.drawRoundRect(rect, s * .22f, s * .22f, paint);
        Glyphs.draw(c, glyph, cx, cy, s * .62f, 0xFFFFFFFF);
    }

    @Override
    public int getIntrinsicWidth() {
        return 96;
    }

    @Override
    public int getIntrinsicHeight() {
        return 96;
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter filter) {
        paint.setColorFilter(filter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
