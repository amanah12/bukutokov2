package com.bukutoko.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Calendar;

/** Menjadwalkan alarm riwayat 23.59 dan pengingat. */
public class NoteScheduler {
    private static final int SNAP_CODE = 1;
    private static final int FLAGS = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

    public static void ensure(Context c) {
        scheduleSnapshot(c);
        JSONArray r = NoteStore.reminders(c);
        long now = System.currentTimeMillis();
        for (int i = 0; i < r.length(); i++) {
            JSONObject o = r.optJSONObject(i);
            if (o == null) continue;
            if (o.optLong("time") > now) schedule(c, o.optInt("id"), o.optLong("time"), o.optString("text"));
            else NoteStore.removeReminder(c, o.optInt("id"));
        }
    }

    public static void scheduleSnapshot(Context c) {
        Calendar t = Calendar.getInstance();
        t.set(Calendar.HOUR_OF_DAY, 23);
        t.set(Calendar.MINUTE, 59);
        t.set(Calendar.SECOND, 0);
        t.set(Calendar.MILLISECOND, 0);
        if (t.getTimeInMillis() <= System.currentTimeMillis()) t.add(Calendar.DAY_OF_YEAR, 1);
        PendingIntent pi = PendingIntent.getBroadcast(c, SNAP_CODE, new Intent(c, SnapshotReceiver.class), FLAGS);
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.getTimeInMillis(), pi);
    }

    public static int addReminder(Context c, long time, String text) {
        int id = NoteStore.addReminder(c, time, text);
        schedule(c, id, time, text);
        return id;
    }

    static void schedule(Context c, int id, long time, String text) {
        Intent in = new Intent(c, ReminderReceiver.class).putExtra("id", id).putExtra("text", text);
        PendingIntent pi = PendingIntent.getBroadcast(c, id, in, FLAGS);
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
        if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi);
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi);
    }

    public static void cancelReminder(Context c, int id) {
        PendingIntent pi = PendingIntent.getBroadcast(c, id, new Intent(c, ReminderReceiver.class), FLAGS);
        ((AlarmManager) c.getSystemService(Context.ALARM_SERVICE)).cancel(pi);
        NoteStore.removeReminder(c, id);
    }
}
