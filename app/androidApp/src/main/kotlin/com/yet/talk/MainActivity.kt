package com.yet.talk

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arkivanov.decompose.retainedComponent
import com.yet.talk.ui.TalkApp

/** A FragmentActivity so BiometricPrompt (the device lock) can show over it. */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Retained across configuration changes; back presses reach the stack through its back handler.
        val root = retainedComponent { context ->
            (application as TalkApplication).graph.rootComponentFactory(context, deepLink = intent?.dataString)
        }

        setContent { TalkApp(root) }
    }
}
