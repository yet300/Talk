package com.yet.talk.web.ui.screens.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.yet.talk.feature.auth.AuthComponent
import com.yet.talk.resources.*
import com.yet.talk.web.decompose.subscribeAsState
import com.yet.talk.web.ui.screens.auth.component.KeySection
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Section
import org.jetbrains.compose.web.dom.Text

/**
 * Sign in (`Sign in · Web`, `AuthPanel`): a centred 420 px panel. Signers come later, so the private key
 * section is open by default (decided), with Create a new key next to it.
 */
@Composable
fun AuthContent(component: AuthComponent) {
    val model by component.model.subscribeAsState()
    Div({ classes("talk", "t-auth-page") }) {
        Section({ classes("t-part", "t-auth") }) {
            Div({ classes("t-wordmark") }) { Text(stringResource(MR.strings.app_name)) }
            Div({ classes("t-auth-head") }) {
                H1({ classes("t-auth-title") }) { Text(stringResource(MR.strings.auth_title)) }
                P({ classes("t-muted", "t-flush") }) { Text(stringResource(MR.strings.auth_subtitle)) }
            }
            KeySection(model, component)
        }
    }
}
