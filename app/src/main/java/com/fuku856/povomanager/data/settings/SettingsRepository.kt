package com.fuku856.povomanager.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
    /** 解約期限の何日前に通知するか(共通デフォルト) */
    val defaultNotifyDays: Set<Int> = DEFAULT_NOTIFY_DAYS,
    /** トッピング有効期限の何日前に通知するか */
    val toppingExpiryNotifyDays: Set<Int> = DEFAULT_TOPPING_NOTIFY_DAYS,
    /** 通知時刻(時、0-23) */
    val notifyHour: Int = DEFAULT_NOTIFY_HOUR,
    /** 解約までの日数(povoの規約変更に備えて変更可能) */
    val expiryPeriodDays: Int = DEFAULT_EXPIRY_PERIOD_DAYS,
) {
    companion object {
        val DEFAULT_NOTIFY_DAYS = setOf(30, 14, 7, 3, 1)
        val DEFAULT_TOPPING_NOTIFY_DAYS = setOf(3, 1)
        const val DEFAULT_NOTIFY_HOUR = 9
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
        val EXPIRY_PERIOD_DAYS = intPreferencesKey("expiry_period_days")
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
            expiryPeriodDays = prefs[Keys.EXPIRY_PERIOD_DAYS] ?: AppSettings.DEFAULT_EXPIRY_PERIOD_DAYS,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setDefaultNotifyDays(days: Set<Int>) {
        context.dataStore.edit { it[Keys.DEFAULT_NOTIFY_DAYS] = days.map(Int::toString).toSet() }
    }

    suspend fun setToppingExpiryNotifyDays(days: Set<Int>) {
        context.dataStore.edit { it[Keys.TOPPING_NOTIFY_DAYS] = days.map(Int::toString).toSet() }
    }

    suspend fun setNotifyHour(hour: Int) {
        context.dataStore.edit { it[Keys.NOTIFY_HOUR] = hour }
    }

    suspend fun setExpiryPeriodDays(days: Int) {
        context.dataStore.edit { it[Keys.EXPIRY_PERIOD_DAYS] = days }
    }
}
