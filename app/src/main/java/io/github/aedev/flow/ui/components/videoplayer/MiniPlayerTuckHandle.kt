package io.github.aedev.flow.ui.components.videoplayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.R
import kotlin.math.roundToInt

private val VisualHandleWidth = 6.dp
private val VisualHandleHeight = 48.dp
private val TouchWidth = 28.dp
private val TouchHeight = 56.dp

/**
 * The tab left at the screen edge while the mini player is tucked past it. It rides at the mini
 * player's height, read in the layout phase, and a tap or a drag on it brings the player back.
 * Styled with the legacy sapphire-blue edge handle.
 */
@Composable
internal fun BoxScope.MiniPlayerTuckHandle(
    state: PlayerDraggableState,
    miniY: () -> Float,
    miniHeight: Float,
    onUntuck: () -> Unit,
) {
    val side = state.tuckedSide
    val motion = MaterialTheme.motionScheme
    AnimatedVisibility(
        visible = side != null,
        enter = fadeIn(motion.defaultEffectsSpec()),
        exit = ExitTransition.None,
        modifier =
            Modifier
                .align(if (side == MiniPlayerTuckSide.Left) Alignment.TopStart else Alignment.TopEnd)
                .offset {
                    val handleHeight = VisualHandleHeight.toPx()
                    IntOffset(0, (miniY() + (miniHeight - handleHeight) / 2f).roundToInt())
                },
    ) {
        val onLeft = side == MiniPlayerTuckSide.Left
        val handleShape =
            if (onLeft) {
                RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 4.dp, bottomEnd = 4.dp)
            } else {
                RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 0.dp, bottomEnd = 0.dp)
            }
        val gradientBrush =
            if (onLeft) {
                Brush.horizontalGradient(listOf(Color(0xFF0D47A1), Color(0xFF1E88E5)))
            } else {
                Brush.horizontalGradient(listOf(Color(0xFF1E88E5), Color(0xFF0D47A1)))
            }

        Box(
            modifier =
                Modifier
                    .size(TouchWidth, TouchHeight)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.mini_player_show),
                        onClick = onUntuck,
                    ).draggable(
                        state = rememberDraggableState { },
                        orientation = Orientation.Horizontal,
                        onDragStarted = { onUntuck() },
                    ),
            contentAlignment = if (onLeft) Alignment.CenterStart else Alignment.CenterEnd,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(VisualHandleWidth, VisualHandleHeight)
                        .background(brush = gradientBrush, shape = handleShape)
                        .border(width = 0.75.dp, color = Color(0x80FFFFFF), shape = handleShape),
            )
        }
    }
}
