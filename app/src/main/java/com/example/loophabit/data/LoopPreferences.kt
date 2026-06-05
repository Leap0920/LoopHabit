package com.example.loophabit.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "loop_preferences")

class LoopPreferences(private val context: Context) {
    companion object {
        val LOOP_INDEX_KEY = intPreferencesKey("loop_index")
    }

    val loopIndexFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[LOOP_INDEX_KEY] ?: 0
        }

    suspend fun setLoopIndex(index: Int) {
        context.dataStore.edit { preferences ->
            preferences[LOOP_INDEX_KEY] = index
        }
    }
}
