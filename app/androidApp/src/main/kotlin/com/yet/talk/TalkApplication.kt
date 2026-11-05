package com.yet.talk

import android.app.Application
import com.yet.talk.di.AppGraph
import com.yet.talk.di.createAndroidAppGraph

/** Owns the app graph for the process; activities take the root factory from it. */
class TalkApplication : Application() {
    val graph: AppGraph by lazy { createAndroidAppGraph(this) }
}
