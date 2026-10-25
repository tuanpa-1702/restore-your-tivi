package com.tivibox.home;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * Foreground service that runs while a child-lock timer is set. When it runs out it covers every app with
 * LockScreenView (needs "display over other apps"), pauses and mutes playback, and stays until the PIN is entered.
 */
public class LockService extends Service {
    private static final String CHANNEL = "child_lock";
    private static final long WARN_MS = 60_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Prefs prefs;
    private WindowManager windows;
    private AudioManager audio;
    private AudioFocusRequest focus;
    private LockScreenView overlay;
    private boolean warned, muted;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            check();
            handler.postDelayed(this, 1000);
        }
    };

    /** Starts the service while a timer or lock is set, stops it otherwise. Call from a visible activity. */
    static void sync(Context c) {
        Intent i = new Intent(c, LockService.class);
        if (ChildLock.active(new Prefs(c))) {
            try {
                c.startForegroundService(i);
            } catch (IllegalStateException | SecurityException e) {
                // Not allowed from the background; MainActivity starts it again on its next resume.
            }
        } else {
            c.stopService(i);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = new Prefs(this);
        windows = getSystemService(WindowManager.class);
        audio = getSystemService(AudioManager.class);
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Khoá trẻ em", NotificationManager.IMPORTANCE_MIN));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification n = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Khoá trẻ em đang bật")
                .setOngoing(true)
                .build();
        startForeground(1, n);
        handler.removeCallbacks(tick);
        handler.post(tick);
        return START_STICKY;
    }

    private void check() {
        if (prefs.isLocked()) {
            showOverlay();
            return;
        }
        long deadline = prefs.lockDeadline();
        if (deadline <= 0) {
            stopSelf();
            return;
        }
        long left = deadline - System.currentTimeMillis();
        if (left <= 0) {
            prefs.setLocked(true);
            showOverlay();
        } else if (left <= WARN_MS && !warned) {
            warned = true;
            Toast.makeText(this, "Còn 1 phút nữa TV sẽ khoá", Toast.LENGTH_LONG).show();
        }
    }

    private void showOverlay() {
        if (overlay != null) return;
        if (!ChildLock.canCoverApps(this)) return; // MainActivity shows its own lock when it comes to the front
        overlay = new LockScreenView(this, prefs.theme(), this::tryUnlock);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_FULLSCREEN,
                PixelFormat.OPAQUE);
        try {
            windows.addView(overlay, lp);
        } catch (RuntimeException e) {
            overlay = null;
            return;
        }
        overlay.focusPin();
        silence();
    }

    private boolean tryUnlock(String pin) {
        if (!ChildLock.pinMatches(pin)) return false;
        prefs.clearLock();
        stopSelf();
        return true;
    }

    private void silence() {
        long now = SystemClock.uptimeMillis();
        audio.dispatchMediaKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE, 0));
        audio.dispatchMediaKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE, 0));
        focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
                .build();
        audio.requestAudioFocus(focus);
        try {
            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
            muted = true;
        } catch (SecurityException ignored) {
            // Do Not Disturb can refuse; the pause and the audio focus still apply.
        }
    }

    private void restoreAudio() {
        if (focus != null) {
            audio.abandonAudioFocusRequest(focus);
            focus = null;
        }
        if (muted) {
            try {
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
            } catch (SecurityException ignored) {
                // Nothing more to restore.
            }
            muted = false;
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(tick);
        if (overlay != null) {
            try {
                windows.removeView(overlay);
            } catch (RuntimeException ignored) {
                // Already gone.
            }
            overlay = null;
        }
        restoreAudio();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
