package com.yet.talk.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yet.talk.resources.TalkTokens

/** `TalkTokens.Spacing` in dp. */
@Immutable
data class TalkSpacing(
    val xs: Dp = TalkTokens.Spacing.xs.dp,
    val sm: Dp = TalkTokens.Spacing.sm.dp,
    val md: Dp = TalkTokens.Spacing.md.dp,
    val lg: Dp = TalkTokens.Spacing.lg.dp,
    val xl: Dp = TalkTokens.Spacing.xl.dp,
    val xxl: Dp = TalkTokens.Spacing.xxl.dp,
    val xxxl: Dp = TalkTokens.Spacing.xxxl.dp,
    val xxxxl: Dp = TalkTokens.Spacing.xxxxl.dp,
)

/** `TalkTokens.Size` in dp: avatars, icons, buttons, bars, the navigation capsule. */
@Immutable
data class TalkSizes(
    val avatarMd: Dp = TalkTokens.Size.avatarMd.dp,
    val avatarLg: Dp = TalkTokens.Size.avatarLg.dp,
    val avatarProfile: Dp = TalkTokens.Size.avatarProfile.dp,
    val actionTarget: Dp = TalkTokens.Size.actionTarget.dp,
    val iconAction: Dp = TalkTokens.Size.iconAction.dp,
    val iconNav: Dp = TalkTokens.Size.iconNav.dp,
    val iconEmpty: Dp = TalkTokens.Size.iconEmpty.dp,
    val logo: Dp = TalkTokens.Size.logo.dp,
    val buttonHeight: Dp = TalkTokens.Size.buttonHeight.dp,
    val buttonPostHeight: Dp = TalkTokens.Size.buttonPostHeight.dp,
    val fab: Dp = TalkTokens.Size.fab.dp,
    val barHeight: Dp = TalkTokens.Size.barHeight.dp,
    val touchMin: Dp = TalkTokens.Size.touchMin.dp,
    val bannerHeight: Dp = TalkTokens.Size.bannerHeight.dp,
    val mediaMax: Dp = TalkTokens.Size.mediaMax.dp,
    val capsuleHeight: Dp = TalkTokens.Size.capsuleHeight.dp,
    val capsuleItemWidth: Dp = TalkTokens.Size.capsuleItemWidth.dp,
    val capsuleItemHeight: Dp = TalkTokens.Size.capsuleItemHeight.dp,
    val capsuleOffset: Dp = TalkTokens.Size.capsuleOffset.dp,
    val hairline: Dp = TalkTokens.Size.hairline.dp,
    val contentMaxWidth: Dp = TalkTokens.Layout.colMain.dp,
)

/** `TalkTokens.Radius` as shapes. */
@Immutable
data class TalkShapes(
    val control: RoundedCornerShape = RoundedCornerShape(TalkTokens.Radius.control.dp),
    val menu: RoundedCornerShape = RoundedCornerShape(TalkTokens.Radius.menu.dp),
    val card: RoundedCornerShape = RoundedCornerShape(TalkTokens.Radius.card.dp),
    val pill: RoundedCornerShape = RoundedCornerShape(percent = 50),
)

internal fun TalkShapes.toMaterial() = Shapes(
    extraSmall = control,
    small = control,
    medium = menu,
    large = card,
    extraLarge = card,
)
