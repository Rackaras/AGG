package com.agg.app.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.agg.app.MainActivity
import com.agg.app.R
import com.agg.app.domain.model.BudgetAlert

/** Android implementation; runtime permission UI remains owned by the ActivityResult host. */
class AndroidBudgetNotificationPort(
    private val context: Context,
    private val permissionRequester: ((Boolean) -> Unit) -> Unit,
) : BudgetNotificationPort {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    override val apiLevel: Int get() = Build.VERSION.SDK_INT

    override fun hasPostPermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    override fun notificationsEnabled(): Boolean = notificationManager.areNotificationsEnabled()

    override fun requestPostPermission(onResult: (Boolean) -> Unit) = permissionRequester(onResult)

    override fun post(alert: BudgetAlert) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPostPermission()) return
        if (!notificationsEnabled()) return

        createChannelIfNeeded()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val message = context.getString(
            R.string.budget_notification_message,
            alert.thresholdPercent,
            alert.month.toString(),
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_budget_notification)
            .setContentTitle(context.getString(R.string.budget_notification_title))
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        val id = listOf(alert.budgetId, alert.month.hashCode().toLong(), alert.thresholdPercent.toLong())
            .fold(17) { result, value -> 31 * result + value.hashCode() }
        notificationManager.notify(id, notification)
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.budget_notification_channel),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
    }

    private companion object {
        const val CHANNEL_ID = "budget_alerts"
        const val REQUEST_CODE = 806
    }
}
