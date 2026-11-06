package com.yet.talk.data.fake

import com.app.common.AppDispatchers
import com.yet.talk.database.driver.SqlDriverFactory
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

/** The data layer over an in-memory database and fake relays, wired by Metro. */
internal class TestData(factory: SqlDriverFactory, scope: CoroutineScope, io: CoroutineDispatcher) {
    val network = FakeNetwork()
    val clock = FixedClock(1_800_000_000)
    val vault = FakeKeyVault()

    private val graph = createGraphFactory<DataTestGraph.Factory>().create(
        driverFactory = factory,
        dispatchers = AppDispatchers(io = io),
        scope = scope,
        clock = clock,
        vault = vault,
        postRemote = network.posts,
        socialRemote = network.social,
        publishRemote = network.publisher,
        mediaRemote = network.media,
        nip05 = { identifier, _ -> identifier == "me@example.com" },
        github = network.github,
    )

    val accounts = graph.accounts
    val posts = graph.posts
    val reactions = graph.reactions
    val bookmarks = graph.bookmarks
    val follows = graph.follows
    val trending = graph.trending
    val profiles = graph.profiles
    val github = graph.github
    val syncDao = graph.syncDao
}
