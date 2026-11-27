package com.bukutoko.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.util.Calendar;

public class SnapshotReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        // alarm bisa telat melewati tengah malam; pakai tanggal kemarin kalau sudah lewat
        boolean late = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 12;
        NoteStore.snapshot(c, late ? NoteStore.yesterday() : NoteStore.today());
        NoteScheduler.scheduleSnapshot(c);
    }
}
