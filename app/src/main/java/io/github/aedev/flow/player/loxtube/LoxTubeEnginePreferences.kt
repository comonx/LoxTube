package io.github.aedev.flow.player.loxtube

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.aedev.flow.data.local.safePreferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.loxtubeEngineDataStore: DataStore<Preferences> by safePreferencesDataStore(name = "loxtube_engine_preferences")

@Singleton
class LoxTubeEnginePreferences
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val appContext = context.applicationContext

        val engineMode: Flow<LoxTubeEngineMode> =
            appContext.loxtubeEngineDataStore.data.map { preferences ->
                LoxTubeEngineMode.fromStorage(preferences[KEY_ENGINE_MODE])
            }

        val autoFallbackEnabled: Flow<Boolean> =
            appContext.loxtubeEngineDataStore.data.map { preferences ->
                preferences[KEY_AUTO_FALLBACK] ?: true
            }

        suspend fun setEngineMode(mode: LoxTubeEngineMode) {
            appContext.loxtubeEngineDataStore.edit { preferences ->
                preferences[KEY_ENGINE_MODE] = mode.name
            }
        }

        suspend fun setAutoFallbackEnabled(enabled: Boolean) {
            appContext.loxtubeEngineDataStore.edit { preferences ->
                preferences[KEY_AUTO_FALLBACK] = enabled
            }
        }

        private companion object {
            val KEY_ENGINE_MODE = stringPreferencesKey("loxtube_video_engine_mode")
            val KEY_AUTO_FALLBACK = booleanPreferencesKey("loxtube_video_engine_auto_fallback")
        }
    }
