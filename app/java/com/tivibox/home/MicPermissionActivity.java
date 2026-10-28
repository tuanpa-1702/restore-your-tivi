package com.tivibox.home;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;

/** A keyboard cannot ask for permissions itself; this invisible screen asks for the microphone, then closes. */
public class MicPermissionActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        finish();
    }
}
