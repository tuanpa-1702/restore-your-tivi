package com.tivibox.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/**
 * One channel on the Home row: the channel's VTV logo on a glass card, cream with a green ring when focused.
 * Falls back to the channel name when there is no logo.
 */
public class ChannelChip extends View {
    private final float dp;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF rect = new RectF();
    private final Typeface bold = Typeface.create("sans-serif-medium", Typeface.BOLD);
    private String name = "";
    private Bitmap logo;

    public ChannelChip(Context context) {
        super(context);
        dp = context.getResources().getDisplayMetrics().density;
        setFocusable(true);
        setClickable(true);
    }

    void setChannel(Channels.Channel ch) {
        name = ch.name;
        int res = getResources().getIdentifier("logo_" + ch.id, "drawable", getContext().getPackageName());
        logo = res == 0 ? null : BitmapFactory.decodeResource(getResources(), res);
        setContentDescription(name);
        invalidate();
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previous) {
        super.onFocusChanged(focused, direction, previous);
        float s = focused ? 1.08f : 1f;
        animate().scaleX(s).scaleY(s).setDuration(150).start();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        boolean f = isFocused();
        if (f) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFF8EE08F);
            rect.set(0, 0, w, h);
            c.drawRoundRect(rect, 14 * dp, 14 * dp, paint);
            paint.setColor(0xFFF3EEE8);
            rect.set(3 * dp, 3 * dp, w - 3 * dp, h - 3 * dp);
            c.drawRoundRect(rect, 11 * dp, 11 * dp, paint);
        } else {
            Glass.panel(c, paint, rect, 0, 0, w, h, 14 * dp, dp, 0x21FFFFFF, 0x3DFFFFFF);
        }
        if (logo != null) {
            // Fit the logo inside the card with an 8dp margin, keeping its proportions.
            float maxW = w - 16 * dp, maxH = h - 16 * dp;
            float s = Math.min(maxW / logo.getWidth(), maxH / logo.getHeight());
            float lw = logo.getWidth() * s, lh = logo.getHeight() * s;
            rect.set((w - lw) / 2, (h - lh) / 2, (w + lw) / 2, (h + lh) / 2);
            paint.setColor(0xFFFFFFFF); // Glass.panel leaves a translucent colour on the paint; logos draw opaque
            c.drawBitmap(logo, null, rect, paint);
            return;
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(bold);
        paint.setTextSize((name.length() > 5 ? 11.5f : 14) * dp);
        paint.setColor(f ? 0xFF15151C : 0xFFFFFFFF);
        c.drawText(name, w / 2, Glass.baseline(paint, h / 2), paint);
    }
}
