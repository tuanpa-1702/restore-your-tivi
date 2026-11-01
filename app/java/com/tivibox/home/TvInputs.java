package com.tivibox.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * "Ngõ vào": the TV's own inputs (HDMI 1/2, AV, the tuner…) from TvInputManager. TV boxes usually have none, so the
 * Home button only shows when there is something to pick. Switching opens the input in the TV's Live TV app.
 */
final class TvInputs {
    private TvInputs() {}

    static List<TvInputInfo> list(Context c) {
        List<TvInputInfo> out = new ArrayList<>();
        TvInputManager tim = (TvInputManager) c.getSystemService(Context.TV_INPUT_SERVICE);
        if (tim == null) return out;
        try {
            for (TvInputInfo info : tim.getTvInputList()) {
                // HDMI/AV (passthrough) and the TV's built-in tuner only: apps like VLC register their own "tuner"
                // inputs, so a tuner counts only when it comes from a system app.
                boolean system = (info.getServiceInfo().applicationInfo.flags
                        & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0;
                boolean real = info.isPassthroughInput() || (info.getType() == TvInputInfo.TYPE_TUNER && system);
                if (real && !(Compat.atLeast(24) && info.isHidden(c))) out.add(info);
            }
        } catch (RuntimeException ignored) {
            // Some firmware ships a broken TV input service; treat it as "no inputs".
        }
        return out;
    }

    static String label(Context c, TvInputInfo info) {
        CharSequence custom = Compat.atLeast(24) ? info.loadCustomLabel(c) : null;
        CharSequence label = custom != null && custom.length() > 0 ? custom : info.loadLabel(c);
        return label == null || label.length() == 0 ? typeName(info.getType()) : label.toString();
    }

    private static String typeName(int type) {
        switch (type) {
            case TvInputInfo.TYPE_HDMI: return "HDMI";
            case TvInputInfo.TYPE_TUNER: return "Truyền hình";
            case TvInputInfo.TYPE_COMPOSITE: return "AV";
            case TvInputInfo.TYPE_COMPONENT: return "Component";
            case TvInputInfo.TYPE_VGA: return "VGA";
            default: return "Ngõ vào";
        }
    }

    static void pick(Activity a) {
        List<TvInputInfo> inputs = list(a);
        if (inputs.isEmpty()) {
            Toast.makeText(a, "Thiết bị này không có ngõ vào HDMI hay truyền hình.", Toast.LENGTH_LONG).show();
            return;
        }
        String[] labels = new String[inputs.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = label(a, inputs.get(i));
        new AlertDialog.Builder(a, DockEditor.DIALOG_THEME)
                .setTitle("Chọn ngõ vào")
                .setItems(labels, (d, which) -> open(a, inputs.get(which)))
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private static void open(Activity a, TvInputInfo info) {
        Intent intent = new Intent(Intent.ACTION_VIEW, info.isPassthroughInput()
                ? TvContract.buildChannelUriForPassthroughInput(info.getId())
                : TvContract.buildChannelsUriForInput(info.getId()));
        try {
            a.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Intent setup = info.createSetupIntent();
            try {
                if (setup != null) a.startActivity(setup);
                else throw e;
            } catch (ActivityNotFoundException e2) {
                Toast.makeText(a, "TV không có ứng dụng để mở ngõ vào này.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
