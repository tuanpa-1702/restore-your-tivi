package com.tivibox.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.KeyEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * QWERTY on-screen keyboard in the Tivi Home style, driven by the remote: ◀ ▶ ▲ ▼ pick a key, OK presses it.
 * Vietnamese is typed with Telex (KeyboardService + Telex), so there are no tone or vowel keys; the VI/ABC key
 * switches Telex off and ?123 swaps the letters for a page of symbols. The view only reports presses;
 * KeyboardService edits the text.
 */
public class KeyboardView extends View {
    static final int CHAR = 0, BACKSPACE = 1, SHIFT = 2, SPACE = 3, DONE = 4, MODE = 5, SYMBOLS = 6, MIC = 7;

    static final class Key {
        final int type;
        final String text, label;
        final float units;
        final RectF bounds = new RectF();

        Key(int type, String text, String label, float units) {
            this.type = type;
            this.text = text;
            this.label = label;
            this.units = units;
        }
    }

    interface Listener {
        void onKey(Key key);
    }

    // Key 58x36dp, 6dp apart; a row is 10 units wide.
    private static final float KEY_W = 58, KEY_H = 36, GAP = 6;

    private final float dp;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Typeface light = Typeface.create("sans-serif-light", Typeface.NORMAL);
    private final Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private final List<List<Key>> letters = new ArrayList<>();
    private final List<List<Key>> symbols = new ArrayList<>();
    private List<List<Key>> rows = letters;
    private int row = 2, col = 0;
    private boolean shift, vietnamese = true, listening;
    private Listener listener;

    public KeyboardView(Context context) {
        super(context);
        dp = context.getResources().getDisplayMetrics().density;
        setFocusable(true);
        letters.add(chars("1234567890"));
        letters.add(chars("qwertyuiop"));
        List<Key> r3 = chars("asdfghjkl");
        r3.add(new Key(CHAR, "-", null, 1));
        letters.add(r3);
        List<Key> r4 = new ArrayList<>();
        r4.add(new Key(SHIFT, "", "⇧", 1.5f));
        r4.addAll(chars("zxcvbnm"));
        r4.add(new Key(BACKSPACE, "", "⌫", 1.5f));
        letters.add(r4);
        letters.add(bottomRow("?123"));

        symbols.add(chars("1234567890"));
        symbols.add(chars("@#$%&*-+=/"));
        symbols.add(chars("()[]{}!?:;"));
        List<Key> s4 = chars("'\"_~^<>|");
        s4.add(new Key(BACKSPACE, "", "⌫", 2));
        symbols.add(s4);
        symbols.add(bottomRow("ABC"));
    }

    /** ?123 (or ABC on the symbol page), VI/ABC, voice, comma, full stop, space, Done: 10 units like every row. */
    private static List<Key> bottomRow(String pageLabel) {
        List<Key> r = new ArrayList<>();
        r.add(new Key(SYMBOLS, "", pageLabel, 1.5f));
        r.add(new Key(MODE, "", "VI", 1));
        r.add(new Key(MIC, "", "Nói", 1));
        r.addAll(chars(",."));
        r.add(new Key(SPACE, " ", "dấu cách", 3));
        r.add(new Key(DONE, "", "Xong", 1.5f));
        return r;
    }

    boolean isSymbols() {
        return rows == symbols;
    }

    /** Swaps letters and symbols, keeping the highlight on the same row and as close as possible. */
    void toggleSymbols() {
        rows = isSymbols() ? letters : symbols;
        col = Math.min(col, rows.get(row).size() - 1);
        layoutKeys();
        invalidate();
    }

    private static List<Key> chars(String s) {
        List<Key> r = new ArrayList<>();
        for (char c : s.toCharArray()) r.add(new Key(CHAR, String.valueOf(c), null, 1));
        return r;
    }

    boolean isVietnamese() {
        return vietnamese;
    }

    void setVietnamese(boolean vietnamese) {
        this.vietnamese = vietnamese;
        invalidate();
    }

    /** Amber "Nói" key while speech recognition is listening. */
    void setListening(boolean listening) {
        this.listening = listening;
        invalidate();
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    boolean isShift() {
        return shift;
    }

    void setShift(boolean shift) {
        this.shift = shift;
        invalidate();
    }

    private static float width(float units) {
        return units * KEY_W + (units - 1) * GAP;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        float w = width(10) * dp, h = (rows.size() * KEY_H + (rows.size() - 1) * GAP) * dp;
        setMeasuredDimension(Math.round(w + 8 * dp), Math.round(h + 8 * dp));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        layoutKeys();
    }

    private void layoutKeys() {
        float y = 4 * dp;
        for (List<Key> r : rows) {
            float x = 4 * dp;
            for (Key k : r) {
                k.bounds.set(x, y, x + width(k.units) * dp, y + KEY_H * dp);
                x += (width(k.units) + GAP) * dp;
            }
            y += (KEY_H + GAP) * dp;
        }
    }

    /** Handles remote navigation and OK; returns false for keys it does not use. */
    boolean handleKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                col = Math.max(0, col - 1);
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                col = Math.min(rows.get(row).size() - 1, col + 1);
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN: {
                int next = row + (keyCode == KeyEvent.KEYCODE_DPAD_UP ? -1 : 1);
                if (next < 0 || next >= rows.size()) return true;
                float cx = rows.get(row).get(col).bounds.centerX();
                List<Key> r = rows.get(next);
                int best = 0;
                for (int i = 1; i < r.size(); i++) {
                    if (Math.abs(r.get(i).bounds.centerX() - cx) < Math.abs(r.get(best).bounds.centerX() - cx)) best = i;
                }
                row = next;
                col = best;
                break;
            }
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                if (listener != null) listener.onKey(rows.get(row).get(col));
                break;
            default:
                return false;
        }
        invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas c) {
        paint.setTextAlign(Paint.Align.CENTER);
        for (int ri = 0; ri < rows.size(); ri++) {
            List<Key> r = rows.get(ri);
            for (int ci = 0; ci < r.size(); ci++) {
                Key k = r.get(ci);
                boolean at = ri == row && ci == col;
                RectF b = k.bounds;
                c.save();
                c.translate(b.left, b.top);
                // Same shape and border as every key; the selected key only changes its fill to cream.
                int fill = at ? 0xFFF3EEE8 : k.type == DONE ? 0x4D8EE08F
                        : (k.type == SHIFT && shift) || (k.type == MIC && listening) ? 0xFFFFC857 : 0x21FFFFFF;
                Glass.panel(c, paint, rect, 0, 0, b.width(), b.height(), 10 * dp, dp, fill, 0x3DFFFFFF);
                c.restore();
                String label = k.type == MODE ? (vietnamese ? "VI" : "ABC")
                        : k.label != null ? k.label : shift ? k.text.toUpperCase() : k.text;
                boolean word = label.length() > 1;
                paint.setTypeface(word ? medium : light);
                paint.setTextSize((word ? 13 : 20) * dp);
                paint.setColor(at || (k.type == SHIFT && shift) || (k.type == MIC && listening) ? 0xFF15151C : 0xFFFFFFFF);
                c.drawText(label, b.centerX(), Glass.baseline(paint, b.centerY()), paint);
            }
        }
    }
}
