package com.tivibox.home;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/** Small canvas-drawn icons, so the launcher ships no bitmaps. UI thread only (shared Paint). */
final class Glyphs {
    static final int YOUTUBE = 1, STORE = 2, GEAR = 3, GRID = 4, PALETTE = 5, WIFI = 6, LOCK = 7,
            FOLDER = 8, PHOTOS = 9, DRIVE = 21, UP = 22;
    static final int SUN = 10, MOON = 11, SUN_CLOUD = 12, MOON_CLOUD = 13, CLOUD = 14,
            FOG = 15, DRIZZLE = 16, RAIN = 17, SNOW = 18, THUNDER = 19;

    private static final int CLOUD_LIGHT = 0xFFF2F5FA;
    private static final int CLOUD_GREY = 0xFFC9D2E0;
    private static final Paint P = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Path PATH = new Path();
    private static final Path CUT = new Path();
    private static final RectF R = new RectF();

    private Glyphs() {}

    /** Draws glyph `type` centred on (cx, cy) inside a square of side `s`. Weather glyphs keep their own colours. */
    static void draw(Canvas c, int type, float cx, float cy, float s, int color) {
        P.setShader(null);
        stroke(color, s * 0.075f);
        switch (type) {
            case YOUTUBE:
                R.set(cx - .32f * s, cy - .23f * s, cx + .32f * s, cy + .23f * s);
                c.drawRoundRect(R, .1f * s, .1f * s, P);
                fill(color);
                triangle(c, cx - .07f * s, cy - .11f * s, cx - .07f * s, cy + .11f * s, cx + .12f * s, cy);
                break;
            case STORE:
                R.set(cx - .3f * s, cy - .16f * s, cx + .3f * s, cy + .38f * s);
                c.drawRoundRect(R, .07f * s, .07f * s, P);
                R.set(cx - .14f * s, cy - .4f * s, cx + .14f * s, cy + .08f * s);
                c.drawArc(R, 180, 180, false, P);
                fill(color);
                triangle(c, cx - .07f * s, cy + .02f * s, cx - .07f * s, cy + .22f * s, cx + .1f * s, cy + .12f * s);
                break;
            case GEAR:
                c.drawCircle(cx, cy, .2f * s, P);
                c.drawCircle(cx, cy, .07f * s, P);
                P.setStrokeWidth(.1f * s);
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI / 4 * i;
                    float dx = (float) Math.cos(a), dy = (float) Math.sin(a);
                    c.drawLine(cx + dx * .31f * s, cy + dy * .31f * s, cx + dx * .38f * s, cy + dy * .38f * s, P);
                }
                break;
            case GRID:
                for (int i = -1; i <= 1; i += 2) {
                    for (int j = -1; j <= 1; j += 2) {
                        float x = cx + i * .17f * s, y = cy + j * .17f * s;
                        R.set(x - .12f * s, y - .12f * s, x + .12f * s, y + .12f * s);
                        c.drawRoundRect(R, .05f * s, .05f * s, P);
                    }
                }
                break;
            case PALETTE:
                c.drawCircle(cx, cy, .34f * s, P);
                c.drawCircle(cx + .1f * s, cy + .15f * s, .07f * s, P);
                fill(color);
                c.drawCircle(cx - .15f * s, cy - .04f * s, .05f * s, P);
                c.drawCircle(cx - .04f * s, cy - .18f * s, .05f * s, P);
                c.drawCircle(cx + .13f * s, cy - .11f * s, .05f * s, P);
                break;
            case LOCK:
                R.set(cx - .28f * s, cy - .04f * s, cx + .28f * s, cy + .36f * s);
                c.drawRoundRect(R, .08f * s, .08f * s, P);
                R.set(cx - .17f * s, cy - .38f * s, cx + .17f * s, cy + .04f * s);
                c.drawArc(R, 180, 180, false, P);
                c.drawLine(cx - .17f * s, cy - .17f * s, cx - .17f * s, cy - .04f * s, P);
                c.drawLine(cx + .17f * s, cy - .17f * s, cx + .17f * s, cy - .04f * s, P);
                fill(color);
                c.drawCircle(cx, cy + .14f * s, .055f * s, P);
                break;
            case FOLDER:
                PATH.reset();
                PATH.moveTo(cx - .36f * s, cy + .28f * s);
                PATH.lineTo(cx - .36f * s, cy - .26f * s);
                PATH.lineTo(cx - .1f * s, cy - .26f * s);
                PATH.lineTo(cx - .02f * s, cy - .15f * s);
                PATH.lineTo(cx + .36f * s, cy - .15f * s);
                PATH.lineTo(cx + .36f * s, cy + .28f * s);
                PATH.close();
                c.drawPath(PATH, P);
                break;
            case PHOTOS:
                R.set(cx - .34f * s, cy - .26f * s, cx + .34f * s, cy + .26f * s);
                c.drawRoundRect(R, .06f * s, .06f * s, P);
                PATH.reset();
                PATH.moveTo(cx - .3f * s, cy + .2f * s);
                PATH.lineTo(cx - .07f * s, cy - .02f * s);
                PATH.lineTo(cx + .07f * s, cy + .1f * s);
                PATH.lineTo(cx + .17f * s, cy + .01f * s);
                PATH.lineTo(cx + .3f * s, cy + .13f * s);
                c.drawPath(PATH, P);
                fill(color);
                c.drawCircle(cx - .13f * s, cy - .1f * s, .055f * s, P);
                break;
            case DRIVE:
                R.set(cx - .36f * s, cy - .14f * s, cx + .36f * s, cy + .18f * s);
                c.drawRoundRect(R, .07f * s, .07f * s, P);
                c.drawLine(cx - .24f * s, cy + .02f * s, cx + .04f * s, cy + .02f * s, P);
                fill(color);
                c.drawCircle(cx + .2f * s, cy + .02f * s, .045f * s, P);
                break;
            case UP:
                c.drawLine(cx, cy + .28f * s, cx, cy - .26f * s, P);
                c.drawLine(cx - .2f * s, cy - .06f * s, cx, cy - .26f * s, P);
                c.drawLine(cx + .2f * s, cy - .06f * s, cx, cy - .26f * s, P);
                break;
            case WIFI: {
                float by = cy + .24f * s;
                for (float r : new float[]{.46f, .31f, .16f}) {
                    R.set(cx - r * s, by - r * s, cx + r * s, by + r * s);
                    c.drawArc(R, 225, 90, false, P);
                }
                fill(color);
                c.drawCircle(cx, by - .01f * s, .05f * s, P);
                break;
            }
            case SUN:
                sun(c, cx, cy, .2f * s);
                break;
            case MOON:
                moon(c, cx, cy, .3f * s);
                break;
            case SUN_CLOUD:
                sun(c, cx - .13f * s, cy - .12f * s, .15f * s);
                cloud(c, cx + .06f * s, cy + .06f * s, .8f * s, CLOUD_LIGHT);
                break;
            case MOON_CLOUD:
                moon(c, cx - .12f * s, cy - .13f * s, .2f * s);
                cloud(c, cx + .06f * s, cy + .06f * s, .8f * s, CLOUD_LIGHT);
                break;
            case CLOUD:
                cloud(c, cx + .14f * s, cy - .1f * s, .62f * s, 0xFFAEB9CB);
                cloud(c, cx - .04f * s, cy + .04f * s, .86f * s, CLOUD_LIGHT);
                break;
            case FOG:
                cloud(c, cx, cy - .14f * s, .8f * s, 0xFFDDE3EC);
                stroke(0xFFC9D1DD, .06f * s);
                c.drawLine(cx - .32f * s, cy + .24f * s, cx + .22f * s, cy + .24f * s, P);
                c.drawLine(cx - .2f * s, cy + .36f * s, cx + .34f * s, cy + .36f * s, P);
                break;
            case DRIZZLE:
                cloud(c, cx, cy - .12f * s, .82f * s, CLOUD_LIGHT);
                stroke(0xFF8CCBFF, .06f * s);
                for (int k = -1; k <= 1; k++) {
                    float x = cx + k * .18f * s;
                    c.drawLine(x + .02f * s, cy + .21f * s, x - .01f * s, cy + .29f * s, P);
                }
                break;
            case RAIN:
                cloud(c, cx, cy - .12f * s, .82f * s, CLOUD_GREY);
                stroke(0xFF6CB8FF, .065f * s);
                for (int k = -1; k <= 1; k++) {
                    float x = cx + k * .18f * s;
                    c.drawLine(x + .05f * s, cy + .18f * s, x - .04f * s, cy + .38f * s, P);
                }
                break;
            case SNOW:
                cloud(c, cx, cy - .12f * s, .82f * s, CLOUD_LIGHT);
                fill(0xFFFFFFFF);
                for (int k = -1; k <= 1; k++) {
                    c.drawCircle(cx + k * .18f * s, cy + (k == 0 ? .34f : .25f) * s, .045f * s, P);
                }
                break;
            case THUNDER:
                cloud(c, cx, cy - .12f * s, .82f * s, 0xFFB4BFD1);
                fill(0xFFFFD54A);
                PATH.reset();
                PATH.moveTo(cx + .03f * s, cy + .06f * s);
                PATH.lineTo(cx - .09f * s, cy + .3f * s);
                PATH.lineTo(cx, cy + .3f * s);
                PATH.lineTo(cx - .05f * s, cy + .47f * s);
                PATH.lineTo(cx + .12f * s, cy + .22f * s);
                PATH.lineTo(cx + .03f * s, cy + .22f * s);
                PATH.lineTo(cx + .09f * s, cy + .06f * s);
                PATH.close();
                c.drawPath(PATH, P);
                break;
            default:
                break;
        }
    }

    private static void stroke(int color, float width) {
        P.setStyle(Paint.Style.STROKE);
        P.setColor(color);
        P.setStrokeWidth(width);
        P.setStrokeCap(Paint.Cap.ROUND);
        P.setStrokeJoin(Paint.Join.ROUND);
    }

    private static void fill(int color) {
        P.setStyle(Paint.Style.FILL);
        P.setColor(color);
    }

    private static void triangle(Canvas c, float x0, float y0, float x1, float y1, float x2, float y2) {
        PATH.reset();
        PATH.moveTo(x0, y0);
        PATH.lineTo(x1, y1);
        PATH.lineTo(x2, y2);
        PATH.close();
        c.drawPath(PATH, P);
    }

    private static void sun(Canvas c, float x, float y, float r) {
        fill(0xFFFFC53D);
        c.drawCircle(x, y, r, P);
        stroke(0xFFFFC53D, r * .22f);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI / 4 * i;
            float dx = (float) Math.cos(a), dy = (float) Math.sin(a);
            c.drawLine(x + dx * 1.4f * r, y + dy * 1.4f * r, x + dx * 1.8f * r, y + dy * 1.8f * r, P);
        }
    }

    private static void moon(Canvas c, float x, float y, float r) {
        PATH.reset();
        PATH.addCircle(x, y, r, Path.Direction.CW);
        CUT.reset();
        CUT.addCircle(x + .45f * r, y - .35f * r, .85f * r, Path.Direction.CW);
        PATH.op(CUT, Path.Op.DIFFERENCE);
        fill(0xFFF6E7A6);
        c.drawPath(PATH, P);
    }

    private static void cloud(Canvas c, float x, float y, float k, int color) {
        fill(color);
        c.drawCircle(x - .2f * k, y + .04f * k, .2f * k, P);
        c.drawCircle(x + .03f * k, y - .08f * k, .27f * k, P);
        c.drawCircle(x + .25f * k, y + .07f * k, .18f * k, P);
        R.set(x - .4f * k, y, x + .43f * k, y + .25f * k);
        c.drawRoundRect(R, .125f * k, .125f * k, P);
    }
}
