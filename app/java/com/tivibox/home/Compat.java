package com.tivibox.home;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;
import android.view.WindowManager;

/**
 * Android 5.0–7.1 support (minSdk 21). Before Android 6 every permission in the manifest is granted at install time
 * and there is no "display over other apps" or "modify system settings" switch; before Android 8 overlays use
 * TYPE_PHONE. Everything newer than 21 that the app calls goes through here or sits behind an SDK_INT check.
 */
final class Compat {
    private Compat() {}

    static boolean atLeast(int sdk) {
        return Build.VERSION.SDK_INT >= sdk;
    }

    /** Works on every API level (Context.checkSelfPermission is API 23). */
    static boolean has(Context c, String permission) {
        return c.checkPermission(permission, Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED;
    }

    /** Runtime permission prompt on Android 6+; before that the permission is already granted. */
    static void request(Activity a, String permission, int requestCode) {
        if (atLeast(23)) a.requestPermissions(new String[]{permission}, requestCode);
    }

    static boolean canDrawOverlays(Context c) {
        return !atLeast(23) || Settings.canDrawOverlays(c);
    }

    static boolean canWriteSettings(Context c) {
        return !atLeast(23) || Settings.System.canWrite(c);
    }

    @SuppressWarnings("deprecation")
    static int overlayWindowType() {
        return atLeast(26) ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
    }

    /** Material dialogs on Android 5; the TV's own dialog style from Android 6. */
    static int dialogTheme() {
        return atLeast(23) ? android.R.style.Theme_DeviceDefault_Dialog_Alert : android.R.style.Theme_Material_Dialog_Alert;
    }

    static String join(String sep, Iterable<String> parts) {
        StringBuilder b = new StringBuilder();
        for (String p : parts) {
            if (b.length() > 0) b.append(sep);
            b.append(p);
        }
        return b.toString();
    }
}
