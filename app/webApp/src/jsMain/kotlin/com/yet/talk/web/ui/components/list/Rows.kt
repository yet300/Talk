package com.yet.talk.web.ui.components.list

import androidx.compose.runtime.Composable
import com.yet.talk.domain.model.user.User
import com.yet.talk.resources.displayLabel
import com.yet.talk.resources.handle
import com.yet.talk.web.ui.components.Avatar
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/** A section heading inside a column (`t-section`): "People", "Relays", "Identity". */
@Composable
fun SectionTitle(text: String) {
    H2({ classes("t-section", "t-flush") }) { Text(text) }
}

/** A person (`t-row`): avatar, name and [caption] (the handle by default). Search people, Activity. */
@Composable
fun PersonRow(user: User, onClick: () -> Unit, caption: String = user.handle, title: String = user.displayLabel) {
    A(href = "#", attrs = {
        classes("t-row")
        onClick { it.preventDefault(); onClick() }
    }) {
        Avatar(user)
        Div({ classes("t-row-text") }) {
            Span({ classes("x-name", "t-block") }) { Text(title) }
            Span({ classes("t-caption") }) { Text(caption) }
        }
    }
}
