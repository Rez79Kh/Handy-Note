package com.application.noteapp.receivers

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.application.noteapp.R
import com.application.noteapp.activities.MainActivity
import com.application.noteapp.database.NoteDatabase

class NotificationReceiver : BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context?, intent: Intent?) {
        val channelID = "note_app"
        val i = Intent(context, MainActivity::class.java)
        intent!!.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val title = intent.getStringExtra("title")
        val content = intent.getStringExtra("content")
        val noteId = intent.getIntExtra("note_id", -1)

        val pendingIntent = PendingIntent.getActivity(
            context,
            noteId,
            i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = Notification.Builder(context, channelID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(Notification.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = NotificationManagerCompat.from(context!!)
        notificationManager.notify(123, builder.build())

        // Update alarm in database
        NoteDatabase.invoke(context).getNoteDao().updateAlarmState(noteId, false, "")

    }
}