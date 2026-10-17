package com.agg.app.notification

import com.agg.app.domain.model.BudgetAlert
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetNotificationCoordinatorTest {
    private val alert = BudgetAlert(42, YearMonth.of(2026, 5), 80)

    @Test fun `no alert and no evaluation warning has no platform effects`() {
        val port = FakeNotificationPort(apiLevel = 35)
        var warnings = 0
        BudgetNotificationCoordinator(port).onSaved(emptyList(), false) { warnings++ }
        assertEquals(0, warnings)
        assertEquals(emptyList<String>(), port.effects)
    }

    @Test fun `evaluation warning is surfaced without a threshold notification`() {
        val port = FakeNotificationPort(apiLevel = 35)
        var warnings = 0
        BudgetNotificationCoordinator(port).onSaved(emptyList(), true) { warnings++ }
        assertEquals(1, warnings)
        assertEquals(emptyList<String>(), port.effects)
    }

    @Test fun `crossing and evaluation warning produce one in-app notice`() {
        val port = FakeNotificationPort(apiLevel = 32)
        var warnings = 0
        BudgetNotificationCoordinator(port).onSaved(listOf(alert), true) { warnings++ }
        assertEquals(1, warnings)
        assertEquals(listOf("enabled-check", "post:42:80"), port.effects)
    }

    @Test fun `API 33 and later request permission contextually and post only after grant`() {
        val port = FakeNotificationPort(apiLevel = 33, granted = false, enabled = false)
        var warnings = 0
        val coordinator = BudgetNotificationCoordinator(port)
        coordinator.onSaved(listOf(alert), false) { warnings++ }
        assertEquals(1, warnings)
        assertEquals(listOf("permission-check", "enabled-check", "request"), port.effects)
        port.respondToPermission(granted = true)
        assertEquals(listOf("permission-check", "enabled-check", "request", "permission-check", "enabled-check", "post:${alert.budgetId}:${alert.thresholdPercent}"), port.effects)
        assertEquals(1, warnings)
    }

    @Test fun `permission denial preserves in-app warning and does not post or retry`() {
        val port = FakeNotificationPort(apiLevel = 35, granted = false)
        var warnings = 0
        BudgetNotificationCoordinator(port).onSaved(listOf(alert), false) { warnings++ }
        port.respondToPermission(granted = false)
        assertEquals(1, warnings)
        assertEquals(listOf("permission-check", "enabled-check", "request"), port.effects)
    }

    @Test fun `permission callback grant does not bypass a still denied permission check`() {
        val port = FakeNotificationPort(apiLevel = 35, granted = false, enabled = false)
        var warnings = 0
        BudgetNotificationCoordinator(port).onSaved(listOf(alert), false) { warnings++ }
        port.respondToPermission(granted = true, permissionActuallyGranted = false)
        assertEquals(1, warnings)
        assertEquals(listOf("permission-check", "enabled-check", "request", "permission-check"), port.effects)
    }

    @Test fun `already granted and pre 33 paths post without requesting runtime permission`() {
        val granted = FakeNotificationPort(apiLevel = 35, granted = true)
        BudgetNotificationCoordinator(granted).onSaved(listOf(alert), false) {}
        assertEquals(listOf("permission-check", "enabled-check", "permission-check", "enabled-check", "post:42:80"), granted.effects)

        val legacy = FakeNotificationPort(apiLevel = 32, granted = false)
        BudgetNotificationCoordinator(legacy).onSaved(listOf(alert), false) {}
        assertEquals(listOf("enabled-check", "post:42:80"), legacy.effects)

        val disabledLegacy = FakeNotificationPort(apiLevel = 32, granted = false, enabled = false)
        BudgetNotificationCoordinator(disabledLegacy).onSaved(listOf(alert), false) {}
        assertEquals(listOf("enabled-check"), disabledLegacy.effects)
    }

    @Test fun `request and query failures degrade to warning without retrying save`() {
        var warnings = 0
        val requestFailure = FakeNotificationPort(apiLevel = 35, granted = false, enabled = false, requestFailure = true)
        BudgetNotificationCoordinator(requestFailure).onSaved(listOf(alert), false) { warnings++ }
        assertEquals(1, warnings)
        assertEquals(listOf("permission-check", "enabled-check", "request"), requestFailure.effects)

        val queryFailure = FakeNotificationPort(apiLevel = 35, granted = true, enabledFailure = true)
        BudgetNotificationCoordinator(queryFailure).onSaved(listOf(alert), false) { warnings++ }
        assertEquals(2, warnings)
        assertEquals(listOf("permission-check", "enabled-check"), queryFailure.effects)
    }

    @Test fun `disabled notifications and platform failures degrade to warning`() {
        val disabled = FakeNotificationPort(apiLevel = 35, granted = true, enabled = false)
        var warnings = 0
        BudgetNotificationCoordinator(disabled).onSaved(listOf(alert), false) { warnings++ }
        assertEquals(1, warnings)
        assertEquals(listOf("permission-check", "enabled-check"), disabled.effects)

        val throwing = FakeNotificationPort(apiLevel = 35, granted = true, postFailure = true)
        BudgetNotificationCoordinator(throwing).onSaved(listOf(alert), false) { warnings++ }
        assertEquals(2, warnings)
        assertEquals(listOf("permission-check", "enabled-check", "permission-check", "enabled-check", "post:42:80"), throwing.effects)
    }

    private class FakeNotificationPort(
        override val apiLevel: Int,
        granted: Boolean = true,
        enabled: Boolean = true,
        private val postFailure: Boolean = false,
        private val requestFailure: Boolean = false,
        private val enabledFailure: Boolean = false,
    ) : BudgetNotificationPort {
        val effects = mutableListOf<String>()
        var granted = granted
        var enabled = enabled
        private var permissionResult: ((Boolean) -> Unit)? = null

        fun respondToPermission(granted: Boolean, permissionActuallyGranted: Boolean = granted) {
            this.granted = permissionActuallyGranted
            enabled = granted
            permissionResult?.invoke(granted)
        }

        override fun notificationsEnabled(): Boolean {
            effects += "enabled-check"
            if (enabledFailure) error("query failed")
            if (!enabled) return false
            return true
        }

        override fun hasPostPermission(): Boolean {
            effects += "permission-check"
            return granted
        }

        override fun requestPostPermission(onResult: (Boolean) -> Unit) {
            effects += "request"
            if (requestFailure) error("request failed")
            permissionResult = onResult
        }

        override fun post(alert: BudgetAlert) {
            effects += "post:${alert.budgetId}:${alert.thresholdPercent}"
            if (postFailure) error("post failed")
        }
    }
}
