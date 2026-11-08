package com.yet.talk.ui.screens.main.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yet.talk.feature.tab.main.component.details.DetailsComponent
import com.yet.talk.feature.tab.main.component.tabs.TabsComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.TalkIconButton
import com.yet.talk.ui.components.glass.icon
import com.yet.talk.ui.components.glass.label
import com.yet.talk.ui.theme.TalkIcon
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.TalkMotion
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors

/**
 * The Duo side column (`SideColumn`, 60 dp, on the camera side): Back while something is open, Compose, and the
 * tabs at the bottom with the active one on a glass pill; Chats carries a dot for unread messages (decision 19).
 */
@Composable
fun SideColumn(tabs: TabsComponent, details: DetailsComponent?, isReading: Boolean, onBack: () -> Unit, onTab: (TabsComponent.Tab) -> Unit) {
    val stack by tabs.stack.subscribeAsState()
    val me by tabs.me.subscribeAsState()
    val active = stack.active.instance.tab.takeIf { details == null }
    val home = stack.items.firstNotNullOfOrNull { (it.instance as? TabsComponent.Child.Home)?.component }
    Column(
        Modifier.width(SideWidth).fillMaxHeight().background(MaterialTheme.talkColors.bg)
            .windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AnimatedVisibility(details != null || isReading, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
            TalkIconButton(TalkIcons.Back, stringResource(MR.strings.action_back), onBack)
        }
        if (home != null) TalkIconButton(TalkIcons.Compose, stringResource(MR.strings.action_compose), home::onCompose, tint = MaterialTheme.talkColors.accent)
        Spacer(Modifier.weight(1f))
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SideTabs.forEach { tab ->
                val selected = tab == active
                val pill by animateColorAsState(if (selected) MaterialTheme.talkColors.glassPill else Color.Transparent, TalkMotion.settle(), label = "pill")
                val label = tab.label()
                Box(
                    Modifier.size(MaterialTheme.sizes.touchMin + 4.dp).clip(CircleShape).drawBehind { drawRect(pill) }
                        .selectable(selected = selected, role = Role.Tab) { onTab(tab) }
                        .semantics { contentDescription = label },
                    contentAlignment = Alignment.Center,
                ) {
                    TalkIcon(tab.icon(selected), contentDescription = null, tint = if (selected) MaterialTheme.talkColors.textPrimary else MaterialTheme.talkColors.glassIcon)
                    if (tab == TabsComponent.Tab.Chats && me.unreadChats > 0) {
                        Box(Modifier.align(Alignment.TopEnd).offset(x = (-8).dp, y = 8.dp).size(8.dp).clip(CircleShape).background(MaterialTheme.talkColors.navBadge))
                    }
                }
            }
        }
    }
}

private val SideTabs = listOf(
    TabsComponent.Tab.Home, TabsComponent.Tab.Search, TabsComponent.Tab.Activity, TabsComponent.Tab.Chats,
    TabsComponent.Tab.Videos, TabsComponent.Tab.Bookmarks, TabsComponent.Tab.Profile,
)

private val SideWidth = 60.dp
