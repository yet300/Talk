package com.yet.talk.feature.root.moderation

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.moderation.Moderation
import com.yet.talk.domain.model.moderation.ReportReason
import com.yet.talk.domain.model.moderation.ReportTarget
import com.yet.talk.domain.repository.ModerationRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

/**
 * What every screen needs to moderate (`Moderation`): who is in the viewer's network (avatars and banners of
 * others are blurred) and which posts are hidden (reported by the network), and Report (NIP-56).
 */
interface ModerationComponent {
    val model: Value<Moderation>

    /** Publishes a public report; the UI says "Reported. Your report is public." */
    fun report(target: ReportTarget, reason: ReportReason)
}

/** For UI previews: nothing blurred or hidden. */
class PreviewModerationComponent(model: Moderation = Moderation()) : ModerationComponent {
    override val model: Value<Moderation> = MutableValue(model)

    override fun report(target: ReportTarget, reason: ReportReason) = Unit
}

/** App-wide: fetches the network's reports whenever the viewer changes (sign-in). */
@SingleIn(AppScope::class)
@Inject
internal class DefaultModerationComponent(
    private val moderation: ModerationRepository,
    private val scope: CoroutineScope,
) : ModerationComponent {
    private val value = MutableValue(Moderation())
    override val model: Value<Moderation> = value

    init {
        scope.launch {
            moderation.moderation.collect { value.value = it }
        }
        scope.launch {
            moderation.moderation.distinctUntilChangedBy { it.me }.collect { if (it.me != null) moderation.refresh() }
        }
    }

    override fun report(target: ReportTarget, reason: ReportReason) {
        scope.launch { moderation.report(target, reason) }
    }
}
