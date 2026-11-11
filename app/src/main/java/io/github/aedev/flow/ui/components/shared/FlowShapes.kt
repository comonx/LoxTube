/*
 * Copyright (C) 2025-2026 Flow | A-EDev
 *
 * This file is part of Flow (https://github.com/Flow-Tube/Flow).
 */

package io.github.aedev.flow.ui.components.shared

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Shape
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star

/**
 * Every artist portrait wears this shape, so an artist is recognisable as
 * one before the label is read. Albums and playlists keep the theme's rounded rectangles.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun flowArtistShape(): Shape = MaterialShapes.Cookie9Sided.toShape()

/**
 * The shape behind a shuffle or radio action, distinct from both artists and collections.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun flowActionShape(): Shape = MaterialShapes.Cookie12Sided.toShape()

/**
 * YouTube channel profile picture avatar shape: Scalloped Circular Badge / Fluted Wavy Avatar.
 * Outer perimeter features 28 subtle, smooth fluted waves (depth ~2.5dp) with smooth rounded curves,
 * ensuring inner avatar content remains clearly visible.
 */
private val ChannelAvatarPolygon: RoundedPolygon by lazy {
    RoundedPolygon.star(
        numVerticesPerRadius = 28,
        radius = 1f,
        innerRadius = 0.93f,
        rounding = CornerRounding(radius = 0.5f, smoothing = 0.5f),
        innerRounding = CornerRounding(radius = 0.5f, smoothing = 0.5f),
    ).normalized()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun flowChannelAvatarShape(): Shape = remember { ChannelAvatarPolygon }.toShape()

