package com.example.civilink

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class NotificationService : Service() {
    override fun onCreate() {
        super.onCreate()
        Log.d("ExampleService", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ExampleService", "Service started")
        // Perform background tasks here

        // If service gets killed, it should be restarted
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("ExampleService", "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
