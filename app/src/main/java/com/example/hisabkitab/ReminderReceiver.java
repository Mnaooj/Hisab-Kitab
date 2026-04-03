package com.example.hisabkitab;

import android.app.*;
import android.content.*;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d("ReminderReceiver", "Notification triggered!");

        String channelId = "reminder_channel";

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create notification channel for Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Daily Reminder",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Daily reminder to add transactions");
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with a proper small icon
                        .setContentTitle("HisabKitab Reminder")
                        .setContentText("Don't forget to add today's transactions 💰")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true);

        manager.notify(1, builder.build());
    }
}