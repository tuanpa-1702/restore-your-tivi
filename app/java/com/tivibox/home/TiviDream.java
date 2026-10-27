package com.tivibox.home;

import android.service.dreams.DreamService;

/** System screensaver ("Màn hình chờ Tivi Home"): shows the template chosen in the screensaver settings. */
public class TiviDream extends DreamService {
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false); // any key wakes the TV
        setFullscreen(true);
        setScreenBright(true);
        setContentView(new ScreensaverView(this, new Prefs(this).saverTemplate()));
    }
}
