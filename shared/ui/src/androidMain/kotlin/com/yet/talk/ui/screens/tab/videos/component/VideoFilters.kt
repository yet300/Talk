package com.yet.talk.ui.screens.tab.videos.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import com.yet.talk.domain.model.video.VideoCatalog
import com.yet.talk.domain.model.video.VideoList
import com.yet.talk.feature.tab.videos.VideosComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.video.label
import com.yet.talk.ui.theme.TalkIcon
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography
import dev.icerock.moko.resources.StringResource

/** Videos · Liked · Watch later · History (`t-chips`). */
@Composable
fun ListChips(selected: VideoList?, onSelect: (VideoList?) -> Unit) {
    val lists = listOf(null, VideoList.Liked, VideoList.WatchLater, VideoList.History)
    LazyRow(contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.lg), horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
        items(lists, key = { it?.name ?: "all" }) { list ->
            Chip(
                stringResource(
                    when (list) {
                        null -> MR.strings.videos_all_videos
                        VideoList.Liked -> MR.strings.videos_liked
                        VideoList.WatchLater -> MR.strings.videos_later
                        VideoList.History -> MR.strings.videos_history
                    },
                ),
                on = list == selected,
            ) { onSelect(list) }
        }
    }
}

/**
 * Category chips (All and the catalog's categories, the largest first) as one row to swipe on the phone (decision
 * 16), then the Level and Type menus.
 */
@Composable
fun BrowseFilters(model: VideosComponent.Model, component: VideosComponent) {
    val selected = model.filters.category
    LazyRow(contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.lg), horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
        item(key = "all") { Chip(stringResource(MR.strings.videos_all), selected == null) { component.onCategorySelected(null) } }
        items(model.categories, key = { it }) { category -> Chip(category, selected == category, Modifier.animateItem()) { component.onCategorySelected(category) } }
    }
    Row(Modifier.padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.sm), horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
        FilterMenu(MR.strings.videos_level, model.filters.level, VideoCatalog.LEVELS, component::onLevelSelected)
        FilterMenu(MR.strings.videos_type, model.filters.type, VideoCatalog.TYPES, component::onTypeSelected)
    }
}

/** `Level: Any` with a menu of Any and the values; back closes the menu (Material's popup). */
@Composable
private fun FilterMenu(title: StringResource, value: String?, options: List<String>, onSelect: (String?) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    val any = stringResource(MR.strings.videos_any)
    Box {
        Chip(stringResource(title, value?.label() ?: any), on = value != null, trailingChevron = true) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = MaterialTheme.talkColors.elevated) {
            (listOf<String?>(null) + options).forEach { option ->
                DropdownMenuItem(
                    text = { Text(option?.label() ?: any, style = MaterialTheme.talkTypography.body) },
                    trailingIcon = if (option == value) ({ TalkIcon(TalkIcons.Check, contentDescription = null, tint = MaterialTheme.talkColors.accent) }) else null,
                    onClick = { open = false; onSelect(option) },
                )
            }
        }
    }
}

@Composable
fun Chip(text: String, on: Boolean, modifier: Modifier = Modifier, trailingChevron: Boolean = false, onClick: () -> Unit) {
    val colors = MaterialTheme.talkColors
    FilterChip(
        selected = on,
        onClick = onClick,
        modifier = modifier,
        label = { Text(text, style = MaterialTheme.talkTypography.bodyBold) },
        trailingIcon = if (trailingChevron) ({ TalkIcon(TalkIcons.Chevron, contentDescription = null, size = FilterChipDefaults.IconSize, modifier = Modifier.rotate(90f)) }) else null,
        shape = MaterialTheme.shapes.extraLarge,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = colors.textPrimary,
            selectedLabelColor = colors.bg,
            selectedTrailingIconColor = colors.bg,
            labelColor = colors.textPrimary,
        ),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = on, borderColor = colors.controlBorder),
    )
}
