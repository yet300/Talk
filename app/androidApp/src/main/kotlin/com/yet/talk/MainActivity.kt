package com.yet.talk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arkivanov.decompose.retainedComponent
import com.yet.talk.ui.root.RootContent
import com.yet.talk.ui.theme.TalkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Retained across configuration changes; back presses reach the stack through its back handler.
        val root = retainedComponent { context ->
            (application as TalkApplication).graph.rootComponentFactory(context, deepLink = intent?.dataString)
        }

        setContent {
            TalkTheme {
                RootContent(root)
            }
        }
    }
}
