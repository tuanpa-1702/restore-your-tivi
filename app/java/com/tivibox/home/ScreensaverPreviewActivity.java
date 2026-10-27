package com.tivibox.home;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;

/** Full-screen preview of a screensaver template; any key closes it, like the real screensaver. */
public class ScreensaverPreviewActivity extends Activity {
    static final String EXTRA_TEMPLATE = "template";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new ScreensaverView(this, getIntent().getIntExtra(EXTRA_TEMPLATE, new Prefs(this).saverTemplate())));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        finish();
        return true;
    }
}
