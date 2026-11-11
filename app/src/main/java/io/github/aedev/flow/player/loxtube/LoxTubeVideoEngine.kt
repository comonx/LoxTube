package io.github.aedev.flow.player.loxtube

import android.content.Context
import android.util.Log
import io.github.aedev.flow.player.error.PlayerDiagnostics
import io.github.aedev.flow.player.stream.InnerTubeVideoStreamExtractor
import io.github.aedev.flow.player.stream.InnerTubeVideoStreamExtractor.VideoExtractionResult
import io.github.aedev.flow.player.stream.PlaybackBlock
import kotlinx.coroutines.flow.first

/**
 * Dual-engine video extraction coordinator.
 *
 * Routes video stream requests between Flow's native extraction engine and
 * the LoxTube phone multi-client engine according to user preference, with
 * automatic intelligent fallback when the active engine encounters a refusal or error.
 */
object LoxTubeVideoEngine {
    private const val TAG = "LoxTubeVideoEngine"

    suspend fun extract(
        videoId: String,
        context: Context,
        forceSabr: Boolean = false,
        audioOnly: Boolean = false,
    ): VideoExtractionResult? {
        val prefs = LoxTubeEnginePreferences(context)
        val mode = prefs.engineMode.first()
        val autoFallback = prefs.autoFallbackEnabled.first()

        return when (mode) {
            LoxTubeEngineMode.FLOW -> {
                Log.d(TAG, "Extracting $videoId with primary engine: FLOW")
                val flowResult = InnerTubeVideoStreamExtractor.extract(videoId, forceSabr, audioOnly)
                if (flowResult != null) {
                    flowResult
                } else if (autoFallback) {
                    Log.w(TAG, "Flow engine extraction failed for $videoId. Auto-switching to LoxTube engine fallback...")
                    PlayerDiagnostics.logWarning(TAG, "auto-switch: FLOW failed for $videoId, trying LOXTUBE fallback")
                    val loxResult = LoxTubeVideoStreamExtractor.extract(videoId, forceSabr, audioOnly)
                    if (loxResult != null) {
                        PlayerDiagnostics.logWarning(TAG, "auto-switch: LOXTUBE fallback SUCCEEDED for $videoId")
                    }
                    loxResult
                } else {
                    null
                }
            }
            LoxTubeEngineMode.LOXTUBE -> {
                Log.d(TAG, "Extracting $videoId with primary engine: LOXTUBE")
                val loxResult = LoxTubeVideoStreamExtractor.extract(videoId, forceSabr, audioOnly)
                if (loxResult != null) {
                    loxResult
                } else if (autoFallback) {
                    Log.w(TAG, "LoxTube engine extraction failed for $videoId. Auto-switching to Flow engine fallback...")
                    PlayerDiagnostics.logWarning(TAG, "auto-switch: LOXTUBE failed for $videoId, trying FLOW fallback")
                    val flowResult = InnerTubeVideoStreamExtractor.extract(videoId, forceSabr, audioOnly)
                    if (flowResult != null) {
                        PlayerDiagnostics.logWarning(TAG, "auto-switch: FLOW fallback SUCCEEDED for $videoId")
                    }
                    flowResult
                } else {
                    null
                }
            }
        }
    }

    fun isGone(videoId: String): Boolean =
        InnerTubeVideoStreamExtractor.isGone(videoId)

    fun blockOf(videoId: String): PlaybackBlock? =
        InnerTubeVideoStreamExtractor.blockOf(videoId)
}
