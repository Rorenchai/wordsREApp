package com.example.study.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.study.MainActivity
import com.example.study.R

/** 每日学习提醒通知：由 WorkManager 每日调度触发，点击打开学习页。 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return if (postReminder(applicationContext)) Result.success() else Result.retry()
    }

    companion object {
        const val CHANNEL_ID = "daily_learning_reminder"
        const val NOTIFICATION_ID = 1001

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(NotificationManager::class.java) ?: return
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "每日学习提醒",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "每天提醒完成背单词目标"
                }
                manager.createNotificationChannel(channel)
            }
        }

        /** @return true 通知已发出；false 表示缺少通知权限或系统拒绝。 */
        fun postReminder(context: Context): Boolean {
            ensureChannel(context)
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("该背单词啦")
                .setContentText("今天的目标还没完成？点击继续学习")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            return try {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
                true
            } catch (error: SecurityException) {
                false
            }
        }
    }
}
