package com.fuku856.povomanager.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class AppSettings(
    /** 自動解約日の何日前に通知するか(共通デフォルト) */
    val defaultNotifyDays: Set<Int> = DEFAULT_NOTIFY_DAYS,
    /** トッピング有効期限の何日前に通知するか */
    val toppingExpiryNotifyDays: Set<Int> = DEFAULT_TOPPING_NOTIFY_DAYS,
    /** 通知時刻(時、0-23) */
    val notifyHour: Int = DEFAULT_NOTIFY_HOUR,
    /** 通知時刻(分、0-59) */
    val notifyMinute: Int = DEFAULT_NOTIFY_MINUTE,
    /** 解約までの日数(povoの規約変更に備えて変更可能) */
    val expiryPeriodDays: Int = DEFAULT_EXPIRY_PERIOD_DAYS,
    /** ウィジェットを手動並び替え順で表示するか。false=期限の早い順(デフォルト) */
    val widgetManualOrder: Boolean = false,
) {
    companion object {
        val DEFAULT_NOTIFY_DAYS = setOf(30, 14, 7, 3, 1, 0)
        val DEFAULT_TOPPING_NOTIFY_DAYS = setOf(3, 1)
        const val DEFAULT_NOTIFY_HOUR = 9
        const val DEFAULT_NOTIFY_MINUTE = 0
        const val DEFAULT_EXPIRY_PERIOD_DAYS = 180

        /** 設定画面で選択可能な通知タイミング候補(日前) */
        val NOTIFY_DAY_CHOICES = listOf(60, 30, 14, 7, 3, 1, 0)
        val TOPPING_NOTIFY_DAY_CHOICES = listOf(7, 3, 1, 0)
    }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val DEFAULT_NOTIFY_DAYS = stringSetPreferencesKey("default_notify_days")
        val TOPPING_NOTIFY_DAYS = stringSetPreferencesKey("topping_notify_days")
        val NOTIFY_HOUR = intPreferencesKey("notify_hour")
        val NOTIFY_MINUTE = intPreferencesKey("notify_minute")
        val EXPIRY_PERIOD_DAYS = intPreferencesKey("expiry_period_days")
        val WIDGET_MANUAL_ORDER = booleanPreferencesKey("widget_manual_order")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            defaultNotifyDays = prefs[Keys.DEFAULT_NOTIFY_DAYS]
                ?.mapNotNull { it.toIntOrNull() }?.toSet()
                ?: AppSettings.DEFAULT_NOTIFY_DAYS,
            toppingExpiryNotifyDays = prefs[Keys.TOPPING_NOTIFY_DAYS]
                ?.mapNotNull { it.toIntOrNull() }?.toSet()
                ?: AppSettings.DEFAULT_TOPPING_NOTIFY_DAYS,
            notifyHour = prefs[Keys.NOTIFY_HOUR] ?: AppSettings.DEFAULT_NOTIFY_HOUR,
            notifyMinute = prefs[Keys.NOTIFY_MINUTE] ?: AppSettings.DEFAULT_NOTIFY_MINUTE,
            expiryPeriodDays = prefs[Keys.EXPIRY_PERIOD_DAYS] ?: AppSettings.DEFAULT_EXPIRY_PERIOD_DAYS,
            widgetManualOrder = prefs[Keys.WIDGET_MANUAL_ORDER] ?: false,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setDefaultNotifyDays(days: Set<Int>) {
        context.dataStore.edit { it[Keys.DEFAULT_NOTIFY_DAYS] = days.map(Int::toString).toSet() }
    }

    suspend fun setToppingExpiryNotifyDays(days: Set<Int>) {
        context.dataStore.edit { it[Keys.TOPPING_NOTIFY_DAYS] = days.map(Int::toString).toSet() }
    }

    suspend fun setNotifyTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.NOTIFY_HOUR] = hour
            it[Keys.NOTIFY_MINUTE] = minute
        }
    }

    suspend fun setExpiryPeriodDays(days: Int) {
        context.dataStore.edit { it[Keys.EXPIRY_PERIOD_DAYS] = days }
    }

    suspend fun setWidgetManualOrder(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WIDGET_MANUAL_ORDER] = enabled }
    }
}
