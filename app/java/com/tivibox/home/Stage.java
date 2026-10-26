package com.tivibox.home;

import android.view.View;

/** Screens are designed on a 960x540 dp stage; this scales the stage view to whatever the TV reports. */
final class Stage {
    private Stage() {}

    static void fitOnLayout(View root, View stage) {
        root.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> fit(stage, v.getWidth(), v.getHeight()));
    }

    static void fit(View stage, int width, int height) {
        int sw = stage.getWidth(), sh = stage.getHeight();
        if (sw == 0 || sh == 0 || width == 0 || height == 0) return;
        float s = Math.min(width / (float) sw, height / (float) sh);
        stage.setPivotX(0);
        stage.setPivotY(0);
        stage.setScaleX(s);
        stage.setScaleY(s);
        stage.setTranslationX((width - sw * s) / 2);
        stage.setTranslationY((height - sh * s) / 2);
    }
}
