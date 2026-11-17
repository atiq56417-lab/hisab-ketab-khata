package com.hisabkitab.khata.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
    private val shopKey = longPreferencesKey("selected_shop")
    private val themeKey = stringPreferencesKey("theme")

    /** 0 means "all shops". */
    val selectedShopId: Flow<Long> = context.dataStore.data.map { it[shopKey] ?: 0L }

    /** "system", "light" or "dark". */
    val theme: Flow<String> = context.dataStore.data.map { it[themeKey] ?: "system" }

    suspend fun setSelectedShop(id: Long) {
        context.dataStore.edit { it[shopKey] = id }
    }

    suspend fun setTheme(value: String) {
        context.dataStore.edit { it[themeKey] = value }
    }
}
