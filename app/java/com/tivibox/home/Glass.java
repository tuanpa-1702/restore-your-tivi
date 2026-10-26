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

    /**
     * Focus ring for cards that open a full screen (calendar, weather): the card keeps its glass so the white text
     * stays readable, and gets the standard 3dp green ring.
     */
    static void focusRing(Canvas c, Paint p, RectF r, float w, float h, float dp) {
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3 * dp);
        p.setColor(0xFF8EE08F);
        r.set(1.5f * dp, 1.5f * dp, w - 1.5f * dp, h - 1.5f * dp);
        c.drawRoundRect(r, 18 * dp, 18 * dp, p);
        p.setStyle(Paint.Style.FILL);
    }

    /** Scale-up used by focusable cards. */
    static void animateFocus(android.view.View v, boolean focused) {
        float s = focused ? 1.02f : 1f;
        v.animate().scaleX(s).scaleY(s).setDuration(150).start();
        v.invalidate();
    }

    /** Baseline that vertically centres `p`'s text on `centerY`. */
    static float baseline(Paint p, float centerY) {
        Paint.FontMetrics fm = p.getFontMetrics();
        return centerY - (fm.ascent + fm.descent) / 2;
    }
}
