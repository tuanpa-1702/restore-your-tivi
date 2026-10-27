package com.tivibox.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/** Soft colour-blob gradient, rendered once at half resolution and scaled up (cheap on 1 GB boxes). */
public class MeshBackgroundView extends View {
    static final String[] THEME_NAMES = {"Tím hồng", "Đại dương", "Hoàng hôn"};

    private static final int[] BASE = {0xFF0C0A22, 0xFF051421, 0xFF1A0A14};
    // Five blobs per theme; the last one is dark and carves the calm band behind the clock.
    private static final int[][] COLORS = {
            {0xFF6A4BF5, 0xFFE2468F, 0xFFF08A2E, 0xFF16A6B6, 0xFF0A0A1C},
            {0xFF1D4ED8, 0xFF0EA5E9, 0xFF14B8A6, 0xFF22D3EE, 0xFF041019},
            {0xFFF97316, 0xFFE11D48, 0xFF7C3AED, 0xFFF59E0B, 0xFF12060C},
    };
    // {x, y, radius} per blob: x and radius as fractions of the width, y of the height.
    private static final float[][] SHAPES = {
            {.12f, .02f, .62f, .98f, .02f, .5f, .22f, 1.08f, .32f, .8f, 1.02f, .52f, .55f, .3f, .3f},
            {.1f, 0f, .6f, .95f, .05f, .45f, .3f, 1.05f, .4f, .85f, 1f, .45f, .55f, .32f, .3f},
            {.1f, .02f, .55f, .95f, .05f, .5f, .25f, 1.05f, .4f, .85f, 1.02f, .45f, .55f, .32f, .3f},
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Rect dst = new Rect();
    private Bitmap cache;
    private int theme;

    public MeshBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setTheme(int theme) {
        theme = Math.max(0, Math.min(BASE.length - 1, theme));
        invalidate(); // the wallpaper may have changed even when the theme did not
        if (theme == this.theme && cache != null) return;
        this.theme = theme;
        cache = null;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        cache = null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getWidth() == 0 || getHeight() == 0) return;
        dst.set(0, 0, getWidth(), getHeight());
        // A home wallpaper chosen in "Cài đặt giao diện" replaces the gradient; the glass cards sit on top of it.
        Prefs prefs = new Prefs(getContext());
        Bitmap photo = Photos.wallpaper(getContext(), getWidth(), getHeight(), prefs.wallpaperBlur(), prefs.wallpaperFit());
        if (photo != null) {
            canvas.drawBitmap(photo, null, dst, paint);
            return;
        }
        if (cache == null) cache = render(theme, Math.max(1, getWidth() / 2), Math.max(1, getHeight() / 2));
        canvas.drawBitmap(cache, null, dst, paint);
    }

    /** The mesh gradient of `theme` at w x h (also used by the screensaver). */
    static Bitmap render(int theme, int w, int h) {
        theme = Math.max(0, Math.min(BASE.length - 1, theme));
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.drawColor(BASE[theme]);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        int[] colors = COLORS[theme];
        float[] shapes = SHAPES[theme];
        for (int i = 0; i < colors.length; i++) {
            int color = colors[i];
            int clear = color & 0x00FFFFFF;
            p.setShader(new RadialGradient(shapes[i * 3] * w, shapes[i * 3 + 1] * h, Math.max(1f, shapes[i * 3 + 2] * w),
                    new int[]{color, clear | 0x80000000, clear}, new float[]{0f, .45f, 1f}, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, p);
        }
        return b;
    }
}
