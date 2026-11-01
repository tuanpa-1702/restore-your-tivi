package com.tivibox.home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.VideoView;

import java.util.List;

/**
 * Full-screen live TV for the VTV row: plays the channel's HLS stream with Android's own player (no libraries).
 * ▲ / CH+ next channel, ▼ / CH− previous, OK shows the channel name, Back returns Home.
 */
public class PlayerActivity extends Activity {
    static final String EXTRA_CHANNEL = "channel";
    private static final long LABEL_MS = 3000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private VideoView video;
    private TextView label;
    private List<Channels.Channel> channels;
    private int index;

    static Intent open(Context c, String channelId) {
        return new Intent(c, PlayerActivity.class).putExtra(EXTRA_CHANNEL, channelId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        float dp = getResources().getDisplayMetrics().density;
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        video = new VideoView(this);
        FrameLayout.LayoutParams vlp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER);
        root.addView(video, vlp);
        label = new TextView(this);
        label.setTextColor(Color.WHITE);
        label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        label.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD));
        int pad = Math.round(14 * dp);
        label.setPadding(pad, Math.round(8 * dp), pad, Math.round(8 * dp));
        label.setBackgroundColor(0x99000000);
        FrameLayout.LayoutParams llp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        llp.setMargins(Math.round(32 * dp), Math.round(24 * dp), 0, 0);
        root.addView(label, llp);
        setContentView(root);

        channels = Channels.shown(new Prefs(this));
        if (channels.isEmpty()) channels = java.util.Arrays.asList(Channels.ALL);
        String id = getIntent().getStringExtra(EXTRA_CHANNEL);
        for (int i = 0; i < channels.size(); i++) if (channels.get(i).id.equals(id)) index = i;

        video.setOnPreparedListener(mp -> {
            mp.setLooping(false);
            video.start();
            showLabel(channels.get(index).name, LABEL_MS);
        });
        video.setOnErrorListener((mp, what, extra) -> {
            showLabel("Không phát được " + channels.get(index).name
                    + " · kiểm tra mạng của TV hoặc thử kênh khác (▲ ▼)", 0);
            return true;
        });
        video.setOnCompletionListener(mp -> play()); // a live stream "ends" when the connection drops: reconnect
    }

    @Override
    protected void onResume() {
        super.onResume();
        play();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null);
        video.stopPlayback();
    }

    private void play() {
        Channels.Channel ch = channels.get(index);
        showLabel(ch.name + " · đang mở…", 0);
        video.stopPlayback();
        video.setVideoURI(Uri.parse(ch.url));
    }

    private void showLabel(String text, long hideAfterMs) {
        handler.removeCallbacksAndMessages(null);
        label.setText(text);
        label.setVisibility(View.VISIBLE);
        if (hideAfterMs > 0) handler.postDelayed(() -> label.setVisibility(View.GONE), hideAfterMs);
    }

    private void step(int delta) {
        index = (index + delta + channels.size()) % channels.size();
        play();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_CHANNEL_UP:
            case KeyEvent.KEYCODE_PAGE_UP:
                step(1);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_CHANNEL_DOWN:
            case KeyEvent.KEYCODE_PAGE_DOWN:
                step(-1);
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                showLabel(channels.get(index).name + " · ▲ ▼ đổi kênh · Back để thoát", LABEL_MS);
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }
}
