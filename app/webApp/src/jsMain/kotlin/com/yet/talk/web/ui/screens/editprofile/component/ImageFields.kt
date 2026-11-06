package com.yet.talk.web.ui.screens.editprofile.component

import androidx.compose.runtime.Composable
import com.yet.talk.domain.rule.ProfileImage
import com.yet.talk.feature.editprofile.EditProfileComponent
import com.yet.talk.resources.*
import com.yet.talk.web.ui.theme.TalkIcon
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Span

/** The banner (3:1) and the avatar over its edge: the current image (if any) under the image icon. */
@Composable
fun ImageFields(model: EditProfileComponent.Model, onPick: (ProfileImage) -> Unit) {
    val bannerLabel = stringResource(MR.strings.edit_profile_change_banner)
    val avatarLabel = stringResource(MR.strings.edit_profile_change_avatar)
    Button({
        classes("t-edit-banner")
        type(ButtonType.Button)
        attr("aria-label", bannerLabel)
        onClick { onPick(ProfileImage.Banner) }
    }) {
        model.banner?.let { Img(src = it, alt = "") { classes("t-edit-img") } }
        Span({ classes("t-edit-icon") }) { TalkIcon(TalkIcons.Image) }
    }
    Button({
        classes("t-edit-avatar")
        type(ButtonType.Button)
        attr("aria-label", avatarLabel)
        onClick { onPick(ProfileImage.Avatar) }
    }) {
        model.avatar?.let { Img(src = it, alt = "") { classes("t-edit-img") } }
        Span({ classes("t-edit-icon") }) { TalkIcon(TalkIcons.Image) }
    }
}
