package io.github.aedev.flow.player.loxtube

/**
 * Operating mode for the video extraction engine.
 *
 * [FLOW] uses Flow's native InnerTube client extraction pipeline.
 * [LOXTUBE] uses the dedicated LoxTube phone multi-client extraction ladder.
 */
enum class LoxTubeEngineMode {
    FLOW,
    LOXTUBE;

    companion object {
        fun fromStorage(value: String?): LoxTubeEngineMode =
            entries.firstOrNull { it.name == value } ?: FLOW
    }
}
