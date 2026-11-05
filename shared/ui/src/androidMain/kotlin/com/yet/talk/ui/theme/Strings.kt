package com.yet.talk.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.resources.toStringDesc
import dev.icerock.moko.resources.PluralsResource
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.desc.StringDesc
import androidx.compose.ui.res.stringResource as androidStringResource

/** A moko string (`MR.strings.*`) in the current locale. */
@Composable
@ReadOnlyComposable
fun stringResource(resource: StringResource, vararg args: Any): String =
    androidStringResource(resource.resourceId, *args)

/** A moko plural (`MR.plurals.*`) with [count] as its argument. */
@Composable
@ReadOnlyComposable
fun pluralResource(resource: PluralsResource, count: Int): String =
    pluralStringResource(resource.resourceId, count, count)

@Composable
fun StringDesc.text(): String = toString(LocalContext.current)

/** The user-facing message for an error from the domain layer. */
@Composable
fun DomainError.message(): String = toStringDesc().text()
