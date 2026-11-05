package com.yet.talk.web.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.settings.ThemeMode
import com.yet.talk.resources.toStringDesc
import dev.icerock.moko.resources.PluralsResource
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.desc.StringDesc
import dev.icerock.moko.resources.provider.JsStringProvider
import kotlinx.browser.document

private val LocalStrings = staticCompositionLocalOf<JsStringProvider> { error("Strings are provided by TalkTheme") }

/**
 * Installs the token stylesheet, applies [theme] (`data-theme` on `<html>`; System leaves it to the browser)
 * and provides the loaded strings. Component styles are the design's CSS (`resources/styles`).
 */
@Composable
fun TalkTheme(strings: JsStringProvider, theme: ThemeMode, content: @Composable () -> Unit) {
    TalkCss.install()
    DisposableEffect(theme) {
        val root = document.documentElement
        when (theme) {
            ThemeMode.System -> root?.removeAttribute("data-theme")
            ThemeMode.Light -> root?.setAttribute("data-theme", "light")
            ThemeMode.Dark -> root?.setAttribute("data-theme", "dark")
        }
        onDispose { }
    }
    CompositionLocalProvider(LocalStrings provides strings, content = content)
}

/** A moko string (`MR.strings.*`) in the browser's locale. */
@Composable
fun stringResource(resource: StringResource, vararg args: Any): String =
    if (args.isEmpty()) resource.localized(LocalStrings.current, null) else resource.localized(LocalStrings.current, null, *args)

/** A moko plural (`MR.plurals.*`) with [count] as its argument. */
@Composable
fun pluralResource(resource: PluralsResource, count: Int): String =
    resource.localized(LocalStrings.current, null, count, count)

@Composable
fun StringDesc.text(): String = toLocalizedString(LocalStrings.current)

/** The user-facing message for an error from the domain layer. */
@Composable
fun DomainError.message(): String = toStringDesc().text()
