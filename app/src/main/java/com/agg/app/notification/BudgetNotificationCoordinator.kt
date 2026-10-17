package com.agg.app.notification

import com.agg.app.domain.model.BudgetAlert

/** Platform boundary kept small so permission and notification policy can be tested without Android. */
interface BudgetNotificationPort {
    val apiLevel: Int
    fun hasPostPermission(): Boolean
    fun notificationsEnabled(): Boolean
    fun requestPostPermission(onResult: (Boolean) -> Unit)
    fun post(alert: BudgetAlert)
}

/** Delivers only metadata from an already successful save; it never owns or retries financial writes. */
class BudgetNotificationCoordinator(private val port: BudgetNotificationPort) {
    fun onSaved(alerts: List<BudgetAlert>, evaluationWarning: Boolean, onWarning: () -> Unit) {
        if (alerts.isEmpty() && !evaluationWarning) return

        // Keep this notice independent from Android permission and notification outcomes.
        onWarning()
        if (alerts.isEmpty()) return

        try {
            if (port.apiLevel >= ANDROID_13_API) {
                val permissionGranted = port.hasPostPermission()
                val enabled = port.notificationsEnabled()
                if (!permissionGranted) {
                    port.requestPostPermission { granted ->
                        if (granted) postIfAllowed(alerts)
                    }
                    return
                }
                if (!enabled) return
                postIfAllowed(alerts)
            } else if (port.notificationsEnabled()) {
                alerts.forEach(port::post)
            }
        } catch (_: Exception) {
            // Notification failures must never change the successful financial result.
        }
    }

    private fun postIfAllowed(alerts: List<BudgetAlert>) {
        try {
            if (port.hasPostPermission() && port.notificationsEnabled()) {
                alerts.forEach(port::post)
            }
        } catch (_: Exception) {
            // Includes permission revocation and platform races between the callback and posting.
        }
    }

    private companion object {
        const val ANDROID_13_API = 33
    }
}
