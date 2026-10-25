package com.tivibox.home;

import android.inputmethodservice.InputMethodService;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * "Bàn phím Tivi": a QWERTY system keyboard in the Tivi Home style for any app on the TV. The remote's D-pad and OK
 * drive KeyboardView; number keys on the remote still go straight to the app. In VI mode letters go through Telex,
 * which rewrites the word before the cursor (s f r x j tones, aa/aw/dd…); password, e-mail and URL fields start in ABC.
 */
public class KeyboardService extends InputMethodService {
    private KeyboardView keyboard;
    private MeshBackgroundView background;

    @Override
    public View onCreateInputView() {
        float dp = getResources().getDisplayMetrics().density;
        LinearLayout column = new LinearLayout(this);
        // The panel is exactly as tall as the keys; otherwise the background would fill the whole screen.
        FrameLayout panel = new FrameLayout(this) {
            @Override
            protected void onMeasure(int widthSpec, int heightSpec) {
                column.measure(widthSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(column.getMeasuredHeight(), MeasureSpec.EXACTLY));
            }
        };
        background = new MeshBackgroundView(this, null);
        panel.addView(background, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        View line = new View(this);
        line.setBackgroundColor(0x33FFFFFF);
        panel.addView(line, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, Math.max(1, Math.round(dp))));

        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = Math.round(12 * dp);
        column.setPadding(pad, pad, pad, pad);
        TextView hint = new TextView(this);
        hint.setGravity(Gravity.CENTER);
        hint.setText("◀ ▶ ▲ ▼ chọn phím · OK để gõ · Telex: s sắc · f huyền · r hỏi · x ngã · j nặng · "
                + "aa â · aw ă · ow ơ · w ư · dd đ · Back để đóng");
        hint.setTextColor(0x99FFFFFF);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        column.addView(hint);
        keyboard = new KeyboardView(this);
        keyboard.setListener(this::onKey);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Math.round(6 * dp);
        column.addView(keyboard, lp);
        panel.addView(column, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));
        return panel;
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return false; // keep the app visible above the keyboard instead of Android's plain extract editor
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        if (background == null || keyboard == null) return;
        background.setTheme(new Prefs(this).theme());
        keyboard.setVietnamese(!isPlainField(info));
        updateShift();
    }

    /** Fields where Telex would mangle the text: passwords, e-mail addresses, links, numbers. */
    private static boolean isPlainField(EditorInfo info) {
        if (info == null) return false;
        int cls = info.inputType & InputType.TYPE_MASK_CLASS;
        if (cls != InputType.TYPE_CLASS_TEXT) return true;
        int v = info.inputType & InputType.TYPE_MASK_VARIATION;
        return v == InputType.TYPE_TEXT_VARIATION_PASSWORD || v == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                || v == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD || v == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                || v == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS || v == InputType.TYPE_TEXT_VARIATION_URI;
    }

    /** Capital letter at the start of a field that asks for it (names, sentences). */
    private void updateShift() {
        EditorInfo info = getCurrentInputEditorInfo();
        InputConnection ic = getCurrentInputConnection();
        boolean caps = false;
        if (info != null && ic != null && (info.inputType & InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT) {
            caps = ic.getCursorCapsMode(info.inputType) != 0;
        }
        keyboard.setShift(caps);
    }

    private void onKey(KeyboardView.Key key) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        switch (key.type) {
            case KeyboardView.CHAR: {
                String text = keyboard.isShift() ? key.text.toUpperCase() : key.text;
                if (keyboard.isVietnamese() && Character.isLetter(text.charAt(0))) telex(ic, text.charAt(0));
                else ic.commitText(text, 1);
                updateShift();
                break;
            }
            case KeyboardView.MODE:
                keyboard.setVietnamese(!keyboard.isVietnamese());
                break;
            case KeyboardView.SYMBOLS:
                keyboard.toggleSymbols();
                break;
            case KeyboardView.SPACE:
                ic.commitText(" ", 1);
                updateShift();
                break;
            case KeyboardView.BACKSPACE: {
                CharSequence selected = ic.getSelectedText(0);
                if (selected != null && selected.length() > 0) ic.commitText("", 1);
                else ic.deleteSurroundingText(1, 0);
                updateShift();
                break;
            }
            case KeyboardView.SHIFT:
                keyboard.setShift(!keyboard.isShift());
                break;
            case KeyboardView.DONE:
                done(ic);
                break;
            default:
                break;
        }
    }

    /** Types `c` through Telex: rewrites the word before the cursor when a tone or vowel mark applies. */
    private void telex(InputConnection ic, char c) {
        CharSequence before = ic.getTextBeforeCursor(20, 0);
        CharSequence selected = ic.getSelectedText(0);
        if (before == null || (selected != null && selected.length() > 0)) {
            ic.commitText(String.valueOf(c), 1);
            return;
        }
        int start = before.length();
        while (start > 0 && Character.isLetter(before.charAt(start - 1))) start--;
        String word = before.subSequence(start, before.length()).toString();
        String typed = Telex.type(word, c);
        if (typed.equals(word + c)) {
            ic.commitText(String.valueOf(c), 1);
            return;
        }
        ic.beginBatchEdit();
        ic.deleteSurroundingText(word.length(), 0);
        ic.commitText(typed, 1);
        ic.endBatchEdit();
    }

    private void done(InputConnection ic) {
        EditorInfo info = getCurrentInputEditorInfo();
        int action = info == null ? EditorInfo.IME_ACTION_NONE : info.imeOptions & EditorInfo.IME_MASK_ACTION;
        boolean multiLine = info != null && (info.inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0;
        if (multiLine) {
            ic.commitText("\n", 1);
            return;
        }
        if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(action);
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER);
        }
        requestHideSelf(0);
    }

    // The remote's D-pad and OK go to the keyboard while it is on screen; everything else (Back, numbers) to the app.
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isInputViewShown() && keyboard != null && keyboard.handleKey(keyCode)) return true;
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (isInputViewShown() && keyboard != null && isKeyboardKey(keyCode)) return true;
        return super.onKeyUp(keyCode, event);
    }

    private static boolean isKeyboardKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                return true;
            default:
                return false;
        }
    }
}
