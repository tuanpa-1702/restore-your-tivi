package com.tivibox.home;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/** Frosted-glass look without blur: translucent white fill and a hairline border. */
final class Glass {
    private Glass() {}

    static void card(Canvas c, Paint p, RectF r, float w, float h, float dp) {
        panel(c, p, r, 0, 0, w, h, 18 * dp, dp, 0x1CFFFFFF, 0x33FFFFFF);
    }

    static void badge(Canvas c, Paint p, RectF r, float x, float y, float size, float dp) {
        panel(c, p, r, x, y, x + size, y + size, 8 * dp, dp, 0x26FFFFFF, 0x3DFFFFFF);
    }

    static void panel(Canvas c, Paint p, RectF r, float x0, float y0, float x1, float y1,
                      float radius, float dp, int fill, int border) {
        p.setShader(null);
        p.setStyle(Paint.Style.FILL);
        p.setColor(fill);
        r.set(x0, y0, x1, y1);
        c.drawRoundRect(r, radius, radius, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp);
        p.setColor(border);
        r.set(x0 + dp / 2, y0 + dp / 2, x1 - dp / 2, y1 - dp / 2);
        c.drawRoundRect(r, radius, radius, p);
        p.setStyle(Paint.Style.FILL);
    }

    /** Baseline that vertically centres `p`'s text on `centerY`. */
    static float baseline(Paint p, float centerY) {
        Paint.FontMetrics fm = p.getFontMetrics();
        return centerY - (fm.ascent + fm.descent) / 2;
    }
}
