package com.example.peertut;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;

public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String tutorEmail = intent.getStringExtra("tutorEmail");
        String sessionId = intent.getStringExtra("sessionId");

        // Launch TutorHomeActivity when tapped
        Intent tapIntent = new Intent(context, TutorHomeActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        tapIntent.putExtra("sessionId", sessionId);
        tapIntent.putExtra("tutorEmail", tutorEmail);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, tapIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "session_channel")
                .setSmallIcon(R.drawable.ic_session_reminder) // Make sure you have a small icon
                .setContentTitle("Session Reminder")
                .setContentText("Your tutoring session with " + tutorEmail + " is starting soon.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
