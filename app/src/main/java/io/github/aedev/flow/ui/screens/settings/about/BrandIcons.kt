package io.github.aedev.flow.ui.screens.settings.about

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Brand marks have no Material symbol. The fill is a placeholder: Icon tints the path.
internal val IconReddit: ImageVector by lazy {
    ImageVector
        .Builder(
            name = "Reddit",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.NonZero) {
                moveTo(12f, 0f)
                curveTo(5.373f, 0f, 0f, 5.373f, 0f, 12f)
                curveToRelative(0f, 3.314f, 1.343f, 6.314f, 3.515f, 8.485f)
                lineToRelative(-2.286f, 2.286f)
                curveTo(0.775f, 23.225f, 1.097f, 24f, 1.738f, 24f)
                lineTo(12f, 24f)
                curveToRelative(6.627f, 0f, 12f, -5.373f, 12f, -12f)
                reflectiveCurveTo(18.627f, 0f, 12f, 0f)
                close()
                moveTo(16.388f, 3.199f)
                curveToRelative(1.104f, 0f, 1.999f, 0.895f, 1.999f, 1.999f)
                curveToRelative(0f, 1.105f, -0.895f, 2f, -1.999f, 2f)
                curveToRelative(-0.946f, 0f, -1.739f, -0.657f, -1.947f, -1.539f)
                verticalLineToRelative(0.002f)
                curveToRelative(-1.147f, 0.162f, -2.032f, 1.15f, -2.032f, 2.341f)
                verticalLineToRelative(0.007f)
                curveToRelative(1.776f, 0.067f, 3.4f, 0.567f, 4.686f, 1.363f)
                curveToRelative(0.473f, -0.363f, 1.064f, -0.58f, 1.707f, -0.58f)
                curveToRelative(1.547f, 0f, 2.802f, 1.254f, 2.802f, 2.802f)
                curveToRelative(0f, 1.117f, -0.655f, 2.081f, -1.601f, 2.531f)
                curveToRelative(-0.088f, 3.256f, -3.637f, 5.876f, -7.997f, 5.876f)
                curveToRelative(-4.361f, 0f, -7.905f, -2.617f, -7.998f, -5.87f)
                curveToRelative(-0.954f, -0.447f, -1.614f, -1.415f, -1.614f, -2.538f)
                curveToRelative(0f, -1.548f, 1.255f, -2.802f, 2.803f, -2.802f)
                curveToRelative(0.645f, 0f, 1.239f, 0.218f, 1.712f, 0.585f)
                curveToRelative(1.275f, -0.79f, 2.881f, -1.291f, 4.64f, -1.365f)
                verticalLineToRelative(-0.01f)
                curveToRelative(0f, -1.663f, 1.263f, -3.034f, 2.88f, -3.207f)
                curveToRelative(0.188f, -0.911f, 0.993f, -1.595f, 1.959f, -1.595f)
                close()
                moveTo(8.303f, 11.575f)
                curveToRelative(-0.784f, 0f, -1.459f, 0.78f, -1.506f, 1.797f)
                curveToRelative(-0.047f, 1.016f, 0.64f, 1.429f, 1.426f, 1.429f)
                curveToRelative(0.786f, 0f, 1.371f, -0.369f, 1.418f, -1.385f)
                curveToRelative(0.047f, -1.017f, -0.553f, -1.841f, -1.338f, -1.841f)
                close()
                moveTo(15.709f, 11.575f)
                curveToRelative(-0.786f, 0f, -1.385f, 0.824f, -1.338f, 1.841f)
                curveToRelative(0.047f, 1.017f, 0.634f, 1.385f, 1.418f, 1.385f)
                curveToRelative(0.785f, 0f, 1.473f, -0.413f, 1.426f, -1.429f)
                curveToRelative(-0.046f, -1.017f, -0.721f, -1.797f, -1.506f, -1.797f)
                close()
                moveTo(12.006f, 15.588f)
                curveToRelative(-0.974f, 0f, -1.907f, 0.048f, -2.77f, 0.135f)
                curveToRelative(-0.147f, 0.015f, -0.241f, 0.168f, -0.183f, 0.305f)
                curveToRelative(0.483f, 1.154f, 1.622f, 1.964f, 2.953f, 1.964f)
                curveToRelative(1.33f, 0f, 2.47f, -0.81f, 2.953f, -1.964f)
                curveToRelative(0.057f, -0.137f, -0.037f, -0.29f, -0.184f, -0.305f)
                curveToRelative(-0.863f, -0.087f, -1.795f, -0.135f, -2.769f, -0.135f)
                close()
            }
        }.build()
}

// Ported from Mihon.
internal val IconPatreon: ImageVector by lazy {
    ImageVector
        .Builder(
            name = "Patreon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(22.957f, 7.21f)
                curveToRelative(-0.004f, -3.064f, -2.391f, -5.576f, -5.191f, -6.482f)
                curveToRelative(-3.478f, -1.125f, -8.064f, -0.962f, -11.384f, 0.604f)
                curveTo(2.357f, 3.231f, 1.093f, 7.391f, 1.046f, 11.54f)
                curveToRelative(-0.039f, 3.411f, 0.302f, 12.396f, 5.369f, 12.46f)
                curveToRelative(3.765f, 0.047f, 4.326f, -4.804f, 6.068f, -7.141f)
                curveToRelative(1.24f, -1.662f, 2.836f, -2.132f, 4.801f, -2.618f)
                curveToRelative(3.376f, -0.836f, 5.678f, -3.501f, 5.673f, -7.031f)
                close()
            }
        }.build()
}

// Simple Icons' Open Collective mark.
internal val IconOpenCollective: ImageVector by lazy {
    ImageVector
        .Builder(
            name = "OpenCollective",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 0f)
                curveTo(5.373f, 0f, 0f, 5.373f, 0f, 12f)
                reflectiveCurveToRelative(5.373f, 12f, 12f, 12f)
                curveToRelative(2.54f, 0f, 4.894f, -0.79f, 6.834f, -2.135f)
                lineToRelative(-3.107f, -3.109f)
                arcToRelative(7.715f, 7.715f, 0f, isMoreThanHalf = true, isPositiveArc = true, 0f, -13.512f)
                lineToRelative(3.107f, -3.109f)
                arcTo(11.943f, 11.943f, 0f, isMoreThanHalf = false, isPositiveArc = false, 12f, 0f)
                close()
                moveToRelative(9.865f, 5.166f)
                lineToRelative(-3.109f, 3.107f)
                arcTo(7.67f, 7.67f, 0f, isMoreThanHalf = false, isPositiveArc = true, 19.715f, 12f)
                arcToRelative(7.682f, 7.682f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.959f, 3.727f)
                lineToRelative(3.109f, 3.107f)
                arcTo(11.943f, 11.943f, 0f, isMoreThanHalf = false, isPositiveArc = false, 24f, 12f)
                curveToRelative(0f, -2.54f, -0.79f, -4.894f, -2.135f, -6.834f)
                close()
            }
        }.build()
}

// Telegram brand mark.
internal val IconTelegram: ImageVector by lazy {
    ImageVector
        .Builder(
            name = "Telegram",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 0f)
                curveTo(5.373f, 0f, 0f, 5.373f, 0f, 12f)
                reflectiveCurveToRelative(5.373f, 12f, 12f, 12f)
                reflectiveCurveToRelative(12f, -5.373f, 12f, -12f)
                reflectiveCurveTo(18.627f, 0f, 12f, 0f)
                close()
                moveToRelative(5.894f, 8.221f)
                lineToRelative(-1.97f, 9.28f)
                curveToRelative(-0.145f, 0.658f, -0.537f, 0.818f, -1.084f, 0.508f)
                lineToRelative(-3f, -2.21f)
                lineToRelative(-1.446f, 1.394f)
                curveToRelative(-0.14f, 0.18f, -0.357f, 0.295f, -0.6f, 0.295f)
                curveToRelative(-0.002f, 0f, -0.003f, 0f, -0.005f, 0f)
                lineToRelative(0.213f, -3.054f)
                lineToRelative(5.56f, -5.022f)
                curveToRelative(0.24f, -0.213f, -0.054f, -0.334f, -0.373f, -0.121f)
                lineToRelative(-6.869f, 4.326f)
                lineToRelative(-2.96f, -0.924f)
                curveToRelative(-0.643f, -0.204f, -0.657f, -0.643f, 0.136f, -0.953f)
                lineToRelative(11.57f, -4.461f)
                curveToRelative(0.537f, -0.194f, 1.006f, 0.131f, 0.828f, 0.946f)
                close()
            }
        }.build()
}

// Facebook brand mark.
internal val IconFacebook: ImageVector by lazy {
    ImageVector
        .Builder(
            name = "Facebook",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(24f, 12.073f)
                curveToRelative(0f, -6.627f, -5.373f, -12f, -12f, -12f)
                reflectiveCurveToRelative(-12f, 5.373f, -12f, 12f)
                curveToRelative(0f, 5.99f, 4.388f, 10.954f, 10.125f, 11.854f)
                verticalLineToRelative(-8.385f)
                horizontalLineTo(7.078f)
                verticalLineToRelative(-3.47f)
                horizontalLineToRelative(3.047f)
                verticalLineTo(9.43f)
                curveToRelative(0f, -3.007f, 1.792f, -4.669f, 4.533f, -4.669f)
                curveToRelative(1.312f, 0f, 2.686f, 0.235f, 2.686f, 0.235f)
                verticalLineToRelative(2.953f)
                horizontalLineTo(15.83f)
                curveToRelative(-1.491f, 0f, -1.956f, 0.925f, -1.956f, 1.874f)
                verticalLineToRelative(2.25f)
                horizontalLineToRelative(3.328f)
                lineToRelative(-0.532f, 3.47f)
                horizontalLineToRelative(-2.796f)
                verticalLineToRelative(8.385f)
                curveTo(19.612f, 23.027f, 24f, 18.062f, 24f, 12.073f)
                close()
            }
        }.build()
}
