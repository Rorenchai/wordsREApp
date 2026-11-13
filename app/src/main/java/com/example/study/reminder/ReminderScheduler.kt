package com.example.study.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** 每日提醒调度：按「时:分」计算下一次触发延迟，交给 WorkManager 做 24h 周期任务。 */
class ReminderScheduler(private val context: Context) {

    fun schedule(hour: Int, minute: Int) {
        val now = LocalDateTime.now()
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val initialDelayMs = Duration.between(now, next).toMillis().coerceAtLeast(0)

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
            .addTag(TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /** 立即发一条测试提醒，便于用户确认通知权限与样式。 */
    fun postTestNotification(): Boolean = ReminderWorker.postReminder(context)

    companion object {
        const val WORK_NAME = "daily_learning_reminder"
        private const val TAG = "learning_reminder"
    }
}
