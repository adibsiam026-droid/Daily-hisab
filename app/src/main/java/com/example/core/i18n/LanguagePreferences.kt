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

    val languageFlow: Flow<AppLanguage> = context.applicationContext.settingsDataStore.data
        .map { prefs -> AppLanguage.fromCode(prefs[languageKey]) }

    suspend fun setLanguage(language: AppLanguage) {
        context.applicationContext.settingsDataStore.edit { prefs ->
            prefs[languageKey] = language.code
        }
    }
}
