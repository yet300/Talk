package com.yet.talk.ui.components.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.yet.talk.feature.tab.main.component.tabs.TabsComponent
import com.yet.talk.resources.MR
import com.yet.talk.resources.chats_unread_count
import com.yet.talk.resources.tab_activity
import com.yet.talk.resources.tab_bookmarks
import com.yet.talk.resources.tab_chats
import com.yet.talk.resources.tab_home
import com.yet.talk.resources.tab_profile
import com.yet.talk.resources.tab_search
import com.yet.talk.resources.tab_videos
import com.yet.talk.ui.theme.TalkIcon
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.pluralResource
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.hazeGlass
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** The tabs in the phone's capsule (decision 19): Profile opens from the home header, Bookmarks from Profile. */
val PhoneTabs = listOf(TabsComponent.Tab.Home, TabsComponent.Tab.Search, TabsComponent.Tab.Activity, TabsComponent.Tab.Videos, TabsComponent.Tab.Chats)

/**
 * The phone's navigation (`MobileNav`), as iOS 26 draws it: a floating Liquid Glass capsule whose selection is a glass
 * pill that slides between tabs, stretching as it moves. Press and drag along the capsule: the pill becomes a lens that
 * follows the finger, the tab under it lights up, and letting go selects it. A tap selects as usual, and every tab
 * stays a plain selectable tab for TalkBack.
 */
@Composable
fun GlassTabBar(
    selected: TabsComponent.Tab?,
    unreadChats: Int,
    onSelect: (TabsComponent.Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.talkColors
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val select by rememberUpdatedState(onSelect)

    val itemWidth = with(density) { ItemWidth.toPx() }
    val step = with(density) { (ItemWidth + ItemGap).toPx() }
    val selectedIndex = PhoneTabs.indexOf(selected).coerceAtLeast(0)

    // The pill's left edge, from the capsule's inner edge; it springs to the selected tab and follows a drag.
    val pill = remember { Animatable(selectedIndex * step) }
    val lens = remember { Animatable(1f) }
    var dragging by remember { mutableStateOf(false) }
    var hovered by remember { mutableStateOf(selectedIndex) }

    LaunchedEffect(selectedIndex) {
        if (!dragging) {
            hovered = selectedIndex
            pill.animateTo(selectedIndex * step, PillSpring)
        }
    }

    fun indexAt(x: Float) = ((x - itemWidth / 2) / step).roundToInt().coerceIn(0, PhoneTabs.lastIndex)

    Box(
        modifier = modifier
            .height(MaterialTheme.sizes.capsuleHeight)
            .hazeGlass(input = HazeInput.Backdrop(LocalGlassSource.current), style = pillGlass())
            .padding(CapsulePadding)
            .pointerInput(step) {
                detectHorizontalDragGestures(
                    onDragStart = { start ->
                        dragging = true
                        scope.launch { lens.animateTo(LensScale, PillSpring) }
                        scope.launch { pill.animateTo((start.x - itemWidth / 2).coerceIn(0f, PhoneTabs.lastIndex * step), DragSpring) }
                        hovered = indexAt(start.x)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        scope.launch { pill.snapTo((change.position.x - itemWidth / 2).coerceIn(0f, PhoneTabs.lastIndex * step)) }
                        val under = indexAt(change.position.x)
                        if (under != hovered) {
                            hovered = under
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                        }
                    },
                    onDragEnd = {
                        dragging = false
                        val target = hovered
                        scope.launch { lens.animateTo(1f, PillSpring) }
                        scope.launch { pill.animateTo(target * step, PillSpring) }
                        select(PhoneTabs[target])
                    },
                    onDragCancel = {
                        dragging = false
                        hovered = selectedIndex
                        scope.launch { lens.animateTo(1f, PillSpring) }
                        scope.launch { pill.animateTo(selectedIndex * step, PillSpring) }
                    },
                )
            },
    ) {
        // The selection: a glass pill behind the icons. Moving, it stretches along its path, like a drop of liquid.
        val stretch = 1f + (abs(pill.velocity) / with(density) { 4000.dp.toPx() }).coerceAtMost(MaxStretch)
        Box(
            modifier = Modifier
                .offset { IntOffset(pill.value.roundToInt(), 0) }
                .width(ItemWidth)
                .height(MaterialTheme.sizes.capsuleItemHeight)
                .graphicsLayer {
                    scaleX = lens.value * stretch
                    scaleY = lens.value / stretch.coerceAtLeast(1f).let { 1f + (it - 1f) / 2 }
                }
                .then(
                    if (dragging) {
                        Modifier.hazeGlass(input = HazeInput.Backdrop(LocalGlassSource.current), style = LensGlass)
                    } else {
                        Modifier.clip(RoundedCornerShape(percent = 50)).background(colors.glassPill)
                    },
                ),
        )
        Row(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(ItemGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PhoneTabs.forEachIndexed { index, tab ->
                val lit = index == (if (dragging) hovered else selectedIndex)
                val badge = if (tab == TabsComponent.Tab.Chats) unreadChats else 0
                val label = tab.label()
                val description = if (badge > 0) "$label, ${pluralResource(MR.plurals.chats_unread_count, badge)}" else label
                Box(
                    modifier = Modifier
                        .width(ItemWidth)
                        .height(MaterialTheme.sizes.capsuleItemHeight)
                        .clip(RoundedCornerShape(percent = 50))
                        .selectable(selected = tab == selected, role = Role.Tab, indication = null, interactionSource = null) { select(tab) }
                        .semantics { contentDescription = description },
                    contentAlignment = Alignment.Center,
                ) {
                    TalkIcon(
                        icon = if (tab == TabsComponent.Tab.Videos) TalkIcons.Play else tab.icon(lit),
                        contentDescription = null,
                        tint = if (lit) colors.textPrimary else colors.glassIcon,
                        modifier = Modifier.graphicsLayer {
                            val scale = if (lit && dragging) 1.12f else 1f
                            scaleX = scale
                            scaleY = scale
                        },
                    )
                    if (badge > 0) NavBadge(badge, Modifier.align(Alignment.TopCenter).offset(x = 14.dp, y = 4.dp))
                }
            }
        }
    }
}

/** The unread count on a navigation item (`t-nav-badge`). */
@Composable
fun NavBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .widthIn(min = 20.dp)
            .height(20.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.talkColors.navBadge)
            .padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            style = MaterialTheme.talkTypography.small,
            color = Color.White,
        )
    }
}

internal fun TabsComponent.Tab.icon(active: Boolean) = when (this) {
    TabsComponent.Tab.Home -> TalkIcons.Home
    TabsComponent.Tab.Search -> TalkIcons.Search
    TabsComponent.Tab.Activity -> TalkIcons.Activity
    TabsComponent.Tab.Chats -> TalkIcons.Chat
    TabsComponent.Tab.Videos -> TalkIcons.Video
    TabsComponent.Tab.Bookmarks -> if (active) TalkIcons.BookmarkFilled else TalkIcons.Bookmark
    TabsComponent.Tab.Profile -> TalkIcons.User
}

@Composable
internal fun TabsComponent.Tab.label(): String = stringResource(
    when (this) {
        TabsComponent.Tab.Home -> MR.strings.tab_home
        TabsComponent.Tab.Search -> MR.strings.tab_search
        TabsComponent.Tab.Activity -> MR.strings.tab_activity
        TabsComponent.Tab.Chats -> MR.strings.tab_chats
        TabsComponent.Tab.Videos -> MR.strings.tab_videos
        TabsComponent.Tab.Bookmarks -> MR.strings.tab_bookmarks
        TabsComponent.Tab.Profile -> MR.strings.tab_profile
    },
)

/** Five items (decision 19) at the design's four-item width would not fit a phone. */
private val ItemWidth = 64.dp
private val ItemGap = 2.dp
private val CapsulePadding = 4.dp

/** How much the lens grows while held, and how far the moving pill may stretch. */
private const val LensScale = 1.18f
private const val MaxStretch = 0.35f

/** A springy settle with a little overshoot, as iOS 26's tab selection. */
private val PillSpring = spring<Float>(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)
private val DragSpring = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)

/** Clear glass for the lens: it shows the icons and content beneath it, refracted at the edge. */
private val LensGlass: GlassStyle = GlassStyle.clear.then { shape(RoundedCornerShape(percent = 50)) }
