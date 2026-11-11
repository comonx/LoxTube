package io.github.aedev.flow.ui.components.layout.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBarDefaults
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.ui.utils.LocalWindowSizeClass
import io.github.aedev.flow.ui.utils.isExpandedWidth
import io.github.aedev.flow.ui.utils.isMediumHeight

const val FLOW_NAV_BAR_TAG = "flow_nav_bar"
const val FLOW_NAV_RAIL_TAG = "flow_nav_rail"

object FlowNavigationDefaults {
    /**
     * The bar's height above the system navigation inset: slim and compact for icon-only bar.
     */
    val BarHeight: Dp = 40.dp
}

/**
 * Whether this window is wide enough to navigate from the start edge instead of the bottom. The
 * decision belongs to the window's size class, so a phone in a narrow split window keeps the bar
 * even on a large display.
 */
@Composable
fun flowUsesNavigationRail(): Boolean = LocalWindowSizeClass.current.let { it.isExpandedWidth && it.isMediumHeight }

/**
 * The app's primary navigation around [content]: the bottom bar over it on compact and medium
 * windows, the wide navigation rail beside it once the window is expanded.
 *
 * The bar overlays the content, so callers reserve [onBarHeightChanged] themselves. The rail takes
 * its own column; [onRailWidthChanged] is only for overlays drawn outside this layout.
 */
@Composable
fun FlowNavigationChrome(
    tabs: List<FlowTab>,
    selectedTab: FlowTab?,
    onTabSelected: (FlowTab) -> Unit,
    barVisible: Boolean,
    railVisible: Boolean,
    modifier: Modifier = Modifier,
    onBarHeightChanged: (Dp) -> Unit = {},
    onRailWidthChanged: (Dp) -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    val usesRail = flowUsesNavigationRail()
    val showRail = usesRail && railVisible
    val density = LocalDensity.current
    val railInsets = WideNavigationRailDefaults.windowInsets

    Row(modifier = modifier.fillMaxSize()) {
        if (showRail) {
            FlowNavigationRail(
                tabs = tabs,
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                modifier =
                    Modifier
                        .testTag(FLOW_NAV_RAIL_TAG)
                        .onSizeChanged { onRailWidthChanged(with(density) { it.width.toDp() }) },
            )
        }
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (showRail) {
                            Modifier.consumeWindowInsets(railInsets.only(WindowInsetsSides.Start))
                        } else {
                            Modifier
                        },
                    ),
        ) {
            content()
            if (!usesRail) {
                NavigationBarSlot(
                    tabs = tabs,
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    visible = barVisible,
                    onHeightChanged = onBarHeightChanged,
                )
            }
        }
    }
}

@Composable
private fun BoxScope.NavigationBarSlot(
    tabs: List<FlowTab>,
    selectedTab: FlowTab?,
    onTabSelected: (FlowTab) -> Unit,
    visible: Boolean,
    onHeightChanged: (Dp) -> Unit,
) {
    val density = LocalDensity.current
    val insets = ShortNavigationBarDefaults.windowInsets
    val bottomInsetPx = insets.getBottom(density)
    val compactBottomInsetPx = if (bottomInsetPx > 0) (bottomInsetPx * 0.55f).toInt() else 0
    val compactBottomInsetDp = with(density) { compactBottomInsetPx.toDp() }
    val effectiveInsets =
        WindowInsets(
            left = insets.getLeft(density, androidx.compose.ui.unit.LayoutDirection.Ltr),
            top = insets.getTop(density),
            right = insets.getRight(density, androidx.compose.ui.unit.LayoutDirection.Ltr),
            bottom = compactBottomInsetPx,
        )
    val motion = MaterialTheme.motionScheme
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(Alignment.BottomCenter),
        enter = slideInVertically(motion.defaultSpatialSpec()) { it } + fadeIn(motion.defaultEffectsSpec()),
        exit = slideOutVertically(motion.fastSpatialSpec()) { it } + fadeOut(motion.fastEffectsSpec()),
    ) {
        FlowNavigationBar(
            tabs = tabs,
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            windowInsets = effectiveInsets,
            modifier =
                Modifier
                    .testTag(FLOW_NAV_BAR_TAG)
                    .height(FlowNavigationDefaults.BarHeight + compactBottomInsetDp)
                    .onSizeChanged { onHeightChanged(with(density) { (it.height - compactBottomInsetPx).toDp() }) },
        )
    }
}
