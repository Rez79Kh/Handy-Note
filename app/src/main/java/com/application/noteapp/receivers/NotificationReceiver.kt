package com.application.noteapp.receivers

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.activityViewModels
import com.application.noteapp.R
import com.application.noteapp.activities.MainActivity
import com.application.noteapp.database.NoteDatabase
import com.application.noteapp.repository.NoteRepository
import com.application.noteapp.viewmodel.NoteViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class NotificationReceiver:BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context?, intent: Intent?) {
        val channelID = "note_app"
        val i = Intent(context,MainActivity::class.java)
        intent!!.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val title = intent.getStringExtra("title")
        val content = intent.getStringExtra("content")
        val note_id = intent.getIntExtra("note_id",-1)

        val pendingIntent = PendingIntent.getActivity(context,note_id,i,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = Notification.Builder(context,channelID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(Notification.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = NotificationManagerCompat.from(context!!)
        notificationManager.notify(123,builder.build())

        // Update alarm in database
        NoteDatabase.invoke(context).getNoteDao().updateAlarmState(note_id,false,"")

    }
}