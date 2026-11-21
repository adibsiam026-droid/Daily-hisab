package com.example.core.i18n

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "daily_hisab_settings"
)

class LanguagePreferences(private val context: Context) {
    private val languageKey = stringPreferencesKey("app_language")
    private val guestModeKey = androidx.datastore.preferences.core.booleanPreferencesKey("guest_mode_active")

    val languageFlow: Flow<AppLanguage> = context.applicationContext.settingsDataStore.data
        .map { prefs -> AppLanguage.fromCode(prefs[languageKey]) }

    val isGuestModeFlow: Flow<Boolean> = context.applicationContext.settingsDataStore.data
        .map { prefs -> prefs[guestModeKey] ?: false }

    suspend fun setLanguage(language: AppLanguage) {
        context.applicationContext.settingsDataStore.edit { prefs ->
            prefs[languageKey] = language.code
        }
    }

    suspend fun setGuestMode(enabled: Boolean) {
        context.applicationContext.settingsDataStore.edit { prefs ->
            prefs[guestModeKey] = enabled
        }
    }
}
