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
import dev.icerock.moko.resources.desc.RawStringDesc
import dev.icerock.moko.resources.desc.ResourceFormattedStringDesc
import dev.icerock.moko.resources.desc.ResourceStringDesc
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

/**
 * A moko string (`MR.strings.*`) in the browser's locale.
 *
 * Arguments are put in here rather than by moko: its JS formatter compiles each template with `new Function`,
 * which the page's Content-Security-Policy forbids (no `unsafe-eval`). Templates use ICU placeholders (`{0}`).
 */
@Composable
fun stringResource(resource: StringResource, vararg args: Any): String =
    IcuText.format(resource.localized(LocalStrings.current, null), args)

/** A moko plural (`MR.plurals.*`) with [count] as its argument, picked by the browser's plural rules. */
@Composable
fun pluralResource(resource: PluralsResource, count: Int): String {
    val provider = LocalStrings.current
    val key = IcuText.pluralKey(resource) ?: return resource.localized(provider, null, count, count)
    return IcuText.plural(provider.provideString(key, null), count)
}

@Composable
fun StringDesc.text(): String = when (this) {
    is ResourceStringDesc -> stringResource(stringRes)
    is ResourceFormattedStringDesc -> stringResource(stringRes, *args.toTypedArray())
    is RawStringDesc -> string
    else -> toLocalizedString(LocalStrings.current)
}

/** The bits of ICU MessageFormat our strings use, without compiling code. */
private object IcuText {
    private val placeholder = Regex("""\{(\d+)\}""")
    private val branch = Regex("""(zero|one|two|few|many|other|=\d+)\s*\{((?:[^{}]|\{\d+\})*)\}""")

    fun format(template: String, args: Array<out Any>): String =
        placeholder.replace(template) { match -> args.getOrNull(match.groupValues[1].toInt())?.toString() ?: match.value }
            .replace("''", "'")

    /** `{ PLURAL, plural, one {{0} reply} other {{0} replies} }` for [count]. */
    fun plural(template: String, count: Int): String {
        val branches = branch.findAll(template).associate { it.groupValues[1] to it.groupValues[2] }
        val category = js("new Intl.PluralRules(navigator.language)").select(count) as String
        val chosen = branches["=$count"] ?: branches[category] ?: branches["other"] ?: template
        return format(chosen, arrayOf(count))
    }

    /** The resource's key: moko keeps it private, and it is the object's only string field. */
    fun pluralKey(resource: PluralsResource): String? {
        val fields = js("Object.values")(resource.asDynamic()) as Array<Any?>
        return fields.firstOrNull { it is String } as String?
    }
}

/** The user-facing message for an error from the domain layer. */
@Composable
fun DomainError.message(): String = toStringDesc().text()
