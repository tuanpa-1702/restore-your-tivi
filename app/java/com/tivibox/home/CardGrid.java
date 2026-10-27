package com.tivibox.home;

import android.app.Activity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Setting cards laid out two per line inside a vertical container, remembering which card had focus. */
final class CardGrid {
    private final Activity activity;
    private final LinearLayout container;
    private final List<View> cards = new ArrayList<>();
    private LinearLayout line;

    CardGrid(Activity activity, LinearLayout container) {
        this.activity = activity;
        this.container = container;
    }

    /** Index of the focused card, for restoring focus after a rebuild. */
    int focusedIndex() {
        return Math.max(0, cards.indexOf(activity.getCurrentFocus()));
    }

    void clear() {
        container.removeAllViews();
        cards.clear();
        line = null;
    }

    void add(String title, String value, Runnable click) {
        if (line == null || line.getChildCount() == 2) {
            line = new LinearLayout(activity);
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setBaselineAligned(false);
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(-1, -2);
            llp.bottomMargin = Math.round(12 * activity.getResources().getDisplayMetrics().density);
            container.addView(line, llp);
        }
        View v = activity.getLayoutInflater().inflate(R.layout.item_setting, line, false);
        ((TextView) v.findViewById(R.id.title)).setText(title);
        TextView valueView = v.findViewById(R.id.value);
        valueView.setText(value);
        valueView.setLines(1); // equal card heights in a line without MATCH_PARENT; five lines fit one screen
        valueView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        if (click != null) v.setOnClickListener(x -> click.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        float dp = activity.getResources().getDisplayMetrics().density;
        if (line.getChildCount() == 1) lp.setMarginStart(Math.round(14 * dp));
        line.addView(v, lp);
        cards.add(v);
    }

    /** Pads an odd last line so its single card keeps half width, then focuses card `index`. */
    void finish(int index) {
        if (line != null && line.getChildCount() == 1) {
            View spacer = new View(activity);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 1, 1f);
            lp.setMarginStart(Math.round(14 * activity.getResources().getDisplayMetrics().density));
            line.addView(spacer, lp);
        }
        if (!cards.isEmpty()) cards.get(Math.min(index, cards.size() - 1)).requestFocus();
    }
}
