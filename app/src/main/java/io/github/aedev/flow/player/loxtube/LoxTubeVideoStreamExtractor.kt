package io.github.aedev.flow.player.loxtube

import io.github.aedev.flow.player.stream.InnerTubeVideoStreamExtractor
import io.github.aedev.flow.player.stream.InnerTubeVideoStreamExtractor.VideoExtractionResult

/**
 * LoxTube high-performance video extraction engine.
 *
 * Implements LoxTube's measured phone playback route planner (PhoneSourcePlanner):
 * 1. Fast direct path: VISIONOS (token-free, direct adaptive streams)
 * 2. SmartTV bypass path: TV_TIZEN (Samsung Tizen identity, botwall-resilient)
 * 3. Embedded player path: TVHTML5_SIMPLY_EMBEDDED_PLAYER (restricted content fallback)
 * 4. Android VR path: ANDROID_VR_1_65_10 / ANDROID_VR_1_61_48
 * 5. iOS path: IOS / IPADOS
 * 6. Durable web path: MWEB / WEB with BotGuard PoToken + SABR
 */
object LoxTubeVideoStreamExtractor {
    suspend fun extract(
        videoId: String,
        forceSabr: Boolean = false,
        audioOnly: Boolean = false,
    ): VideoExtractionResult? =
        InnerTubeVideoStreamExtractor.extractLoxTube(videoId, forceSabr, audioOnly)
}
