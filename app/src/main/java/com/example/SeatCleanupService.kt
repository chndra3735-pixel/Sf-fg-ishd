package com.example

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

/**
 * Bound local cleanup service used strictly to detect task swipe-removal from Recents
 * (onTaskRemoved) and promptly remove the user's ID from their occupied voice seat.
 */
class SeatCleanupService : Service() {
  private val binder = Binder()

  override fun onBind(intent: Intent?): IBinder = binder

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    return START_NOT_STICKY
  }

  override fun onTaskRemoved(rootIntent: Intent?) {
    SeatPresenceTracker.cleanupSynchronously(applicationContext, timeoutMs = 1500L)
    stopSelf()
    super.onTaskRemoved(rootIntent)
  }
}
