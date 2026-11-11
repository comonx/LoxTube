/*
 * Copyright (C) 2025-2026 Flow | A-EDev
 *
 * This file is part of Flow (https://github.com/Flow-Tube/Flow).
 */

package io.github.aedev.flow.ui.components.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Full-surface loading state. Only ever composed while a surface has nothing else to show, so the
 * indicator runs while it is on screen and leaves the tree with it.
 */
@Composable
fun FlowLoadingIndicator(
    modifier: Modifier = Modifier,
    indicatorSize: Dp = 48.dp,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        LoxNeonLoader(size = indicatorSize)
    }
}

/**
 * Inline loading row for a feed continuation or a paged list tail.
 */
@Composable
fun FlowFeedProgress(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        LoxNeonLoader(size = 38.dp)
    }
}
