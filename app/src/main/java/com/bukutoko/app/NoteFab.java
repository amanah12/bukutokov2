package com.bukutoko.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

/** Ikon catatan melayang. Panggil NoteFab.attach(this) dari onResume() Activity. Aman dipanggil berulang. */
public class NoteFab {
    static int dp(Context c, int v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }

    public static void attach(final Activity a) {
        final ViewGroup root = (ViewGroup) a.findViewById(android.R.id.content);
        if (root == null || root.findViewWithTag("note_fab") != null) return;
        NoteStore.catchUp(a);
        NoteScheduler.ensure(a);

        final TextView fab = new TextView(a);
        fab.setTag("note_fab");
        fab.setText("\uD83D\uDCDD");
        fab.setTextSize(24);
        fab.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.parseColor("#FFD84D"));
        bg.setStroke(dp(a, 2), Color.BLACK);
        fab.setBackground(bg);
        if (Build.VERSION.SDK_INT >= 21) fab.setElevation(dp(a, 8));

        int s = dp(a, 56);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(s, s, Gravity.BOTTOM | Gravity.END);
        lp.setMargins(0, 0, dp(a, 16), dp(a, 96));
        root.addView(fab, lp);

        final float[] st = new float[4];
        final int slop = ViewConfiguration.get(a).getScaledTouchSlop();
        fab.setOnTouchListener((v, e) -> {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    st[0] = e.getRawX(); st[1] = e.getRawY();
                    st[2] = v.getTranslationX(); st[3] = v.getTranslationY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    v.setTranslationX(st[2] + e.getRawX() - st[0]);
                    v.setTranslationY(st[3] + e.getRawY() - st[1]);
                    return true;
                case MotionEvent.ACTION_UP:
                    if (Math.abs(e.getRawX() - st[0]) < slop && Math.abs(e.getRawY() - st[1]) < slop) NoteDialog.show(a);
                    return true;
            }
            return false;
        });
    }
}
