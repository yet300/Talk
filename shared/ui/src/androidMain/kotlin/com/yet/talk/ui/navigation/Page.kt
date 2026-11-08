package com.yet.talk.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import com.yet.talk.ui.theme.talkColors

/**
 * One screen in a navigation container (a stack, panel, slot or page): opaque on the page background and clipped, so
 * while it slides, shrinks with the back gesture or fades, the screen under it never shows through or past its edges.
 */
@Composable
fun Page(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxSize().clipToBounds().background(MaterialTheme.talkColors.bg)) { content() }
}
