package com.yet.talk.web.ui.screens.tab.videos.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yet.talk.domain.model.video.VideoCatalog
import com.yet.talk.domain.model.video.VideoList
import com.yet.talk.feature.tab.videos.VideosComponent
import com.yet.talk.resources.*
import com.yet.talk.web.ui.components.video.label
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text

/** Videos · Liked · Watch later · History (`t-chips`, a tab list). */
@Composable
fun ListChips(selected: VideoList?, onSelect: (VideoList?) -> Unit) {
    val lists = listOf(null, VideoList.Liked, VideoList.WatchLater, VideoList.History)
    val label = stringResource(MR.strings.videos_lists)
    Div({ classes("t-chips"); attr("role", "tablist"); attr("aria-label", label) }) {
        lists.forEach { list ->
            Chip(
                text = stringResource(
                    when (list) {
                        null -> MR.strings.videos_all_videos
                        VideoList.Liked -> MR.strings.videos_liked
                        VideoList.WatchLater -> MR.strings.videos_later
                        VideoList.History -> MR.strings.videos_history
                    },
                ),
                on = list == selected,
                role = "tab",
            ) { onSelect(list) }
        }
    }
}

/**
 * Category chips (All and the catalog's 150 categories, the largest first), then the Level and Type menus. On
 * the phone the chips are one row to swipe; wider, they wrap, the [COLLAPSED] largest first with More.
 */
@Composable
fun BrowseFilters(model: VideosComponent.Model, component: VideosComponent, phone: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    val selected = model.filters.category
    val shown = when {
        phone || expanded -> model.categories
        // A category chosen beyond the first ones stays visible while collapsed.
        else -> model.categories.take(COLLAPSED) + listOfNotNull(selected?.takeIf { it !in model.categories.take(COLLAPSED) })
    }
    val categoryLabel = stringResource(MR.strings.videos_category)
    Div({ classes("t-chips", if (phone) "t-chips-scroll" else "t-chips-wrap"); attr("role", "group"); attr("aria-label", categoryLabel) }) {
        Chip(stringResource(MR.strings.videos_all), selected == null, pressed = true) { component.onCategorySelected(null) }
        shown.forEach { category ->
            Chip(category, selected == category, pressed = true) { component.onCategorySelected(category) }
        }
        if (!phone && model.categories.size > COLLAPSED) {
            Button({
                classes("t-chip", "t-chip-more")
                type(ButtonType.Button)
                attr("aria-expanded", expanded.toString())
                onClick { expanded = !expanded }
            }) { Text(stringResource(if (expanded) MR.strings.videos_fewer_categories else MR.strings.videos_more_categories)) }
        }
    }
    Div({ classes("t-filter-row") }) {
        FilterMenu(MR.strings.videos_level, model.filters.level, VideoCatalog.LEVELS, component::onLevelSelected)
        FilterMenu(MR.strings.videos_type, model.filters.type, VideoCatalog.TYPES, component::onTypeSelected)
    }
}

/** `Level: Any` with a menu of Any and the values (`x-menu`, radio rows). */
@Composable
private fun FilterMenu(title: dev.icerock.moko.resources.StringResource, value: String?, options: List<String>, onSelect: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val any = stringResource(MR.strings.videos_any)
    Div({ classes("t-filter") }) {
        Button({
            classes("t-chip")
            if (value != null) classes("t-chip-on")
            type(ButtonType.Button)
            attr("aria-haspopup", "menu")
            attr("aria-expanded", open.toString())
            onClick { open = !open }
        }) { Text(stringResource(title, value?.label() ?: any)) }
        if (open) {
            Div({ classes("t-menu-backdrop"); onClick { open = false } })
            Div({ classes("x-menu", "t-filter-menu"); attr("role", "menu") }) {
                (listOf<String?>(null) + options).forEach { option ->
                    Button({
                        classes("t-menu-row")
                        type(ButtonType.Button)
                        attr("role", "menuitemradio")
                        attr("aria-checked", (option == value).toString())
                        onClick { open = false; onSelect(option) }
                    }) { Text(option?.label() ?: any) }
                }
            }
        }
    }
}

private const val COLLAPSED = 12

@Composable
private fun Chip(text: String, on: Boolean, role: String? = null, pressed: Boolean = false, onClick: () -> Unit) {
    Button({
        classes("t-chip")
        if (on) classes("t-chip-on")
        type(ButtonType.Button)
        role?.let { attr("role", it); attr("aria-selected", on.toString()) }
        if (pressed) attr("aria-pressed", on.toString())
        onClick { onClick() }
    }) { Text(text) }
}
