import SwiftUI
import SharedLogic

/// One page pushed on the navigation stack of the tab in front. Its identity is the Kotlin component's, so the stack
/// pushes and pops exactly when Kotlin does.
struct Route: Hashable {
    enum Kind {
        /// Your profile or Bookmarks, pages above the tabs on a phone (decision 17).
        case above(TabsComponentChild)
        /// A page of the details panel in one pane: a post, an author, a video.
        case details(DetailsComponentChild, DetailsComponent)
        /// The open conversation, its info, Verify, New chat or Location channels over the Chats list on a phone.
        case chatPane(ChatsComponent)
        /// A location channel's room over the channels.
        case room(RoomComponent)
        /// Compose, Settings, Revisions, Edit profile above Main.
        case root(RootComponentChild)
    }

    let key: String
    let kind: Kind

    static func == (a: Route, b: Route) -> Bool { a.key == b.key }
    func hash(into hasher: inout Hasher) { hasher.combine(key) }
}

/// Flattens the component tree below the root into the pages the tab in front pushes, so one native `NavigationStack`
/// (system back button, interactive swipe back, vertical bars on the Duo) renders several Decompose containers: the
/// root stack above Main, the details panel in one pane, your profile and Bookmarks above the tabs, the Chats pane
/// and a channel's room. Popping a page (Back or the swipe) tells the component that owns it.
final class MainRouter: ObservableObject {
    @Published private(set) var routes: [Route] = []
    /// The tab whose navigation stack is in front: the active tab, or the tab under your profile or Bookmarks.
    @Published private(set) var baseTab: TabsComponentTab = .home

    /// Profile and Bookmarks are tabs of their own (wide windows, the Duo's vertical bar) rather than pages above Home.
    var profileIsTab = false { didSet { if oldValue != profileIsTab { rebuild() } } }
    /// Chats shows the list and the conversation side by side, so its pane is not a page.
    var chatsSideBySide = false { didSet { if oldValue != chatsSideBySide { rebuild() } } }

    let root: RootComponent
    let main: MainComponent
    private lazy var watcher = ValueWatcher { [weak self] in self?.rebuild() }

    init(root: RootComponent, main: MainComponent) {
        self.root = root
        self.main = main
        rebuild()
    }

    var tabs: TabsComponent { main.panels.value.main.instance }

    /// The navigation path of `tab`: the pages when it is in front, nothing otherwise. Setting a shorter path pops.
    func path(for tab: TabsComponentTab) -> Binding<[Route]> {
        Binding(
            get: { [weak self] in
                guard let self, self.baseTab == tab else { return [] }
                return self.routes
            },
            set: { [weak self] path in
                guard let self, self.baseTab == tab, path.count < self.routes.count else { return }
                for route in self.routes[path.count...].reversed() { self.pop(route) }
            }
        )
    }

    private func pop(_ route: Route) {
        switch route.kind {
        case .above(let child):
            if let profile = child as? TabsComponentChild.Profile { profile.component.onBack() }
            if let bookmarks = child as? TabsComponentChild.Bookmarks { bookmarks.component.onBrowseHome() }
        case .details(_, let details):
            details.onBack()
        case .chatPane(let chats):
            if let pane = chats.pane.value.child?.instance { Self.close(pane) }
        case .room(let room):
            room.onBack()
        case .root:
            root.onBack()
        }
    }

    /// Back from a Chats pane: each goes where its own Back goes (info back to the conversation, the rest to the list).
    static func close(_ pane: ChatsComponentPane) {
        switch onEnum(of: pane) {
        case .conversation(let it): it.component.onBack()
        case .newChat(let it): it.component.onBack()
        case .info(let it): it.component.onBack()
        case .verify(let it): it.component.onBack()
        case .channels(let it): it.component.onBack()
        }
    }

    func rebuild() {
        var watched: Set<String> = ["root", "panels"]
        watcher.watch("root", root.stack)
        watcher.watch("panels", main.panels)
        var routes: [Route] = []
        let panels = main.panels.value

        var base: TabsComponentTab = .home
        do {
            let tabs = panels.main.instance
            watched.insert("tabs")
            watcher.watch("tabs", tabs.stack)
            let stack = tabs.stack.value
            let active = stack.active.instance
            if !profileIsTab, Self.isAbove(active) {
                routes.append(Route(key: "above:\(ObjectIdentifier(active))", kind: .above(active)))
                base = stack.items.reversed().map { $0.instance }.first { !Self.isAbove($0) }.map(Self.tab) ?? .home
            } else {
                base = Self.tab(active)
            }

            if panels.mode == .single, let details = panels.details?.instance {
                watched.insert("details")
                watcher.watch("details", details.stack)
                for item in details.stack.value.items {
                    let page = item.instance
                    routes.append(Route(key: "details:\(ObjectIdentifier(page))", kind: .details(page, details)))
                }
            }

            if base == .chats, !chatsSideBySide,
               let chats = stack.items.compactMap({ $0.instance as? TabsComponentChild.Chats }).first?.component {
                watched.insert("pane")
                watcher.watch("pane", chats.pane)
                if let pane = chats.pane.value.child?.instance {
                    routes.append(Route(key: "pane:\(ObjectIdentifier(chats))", kind: .chatPane(chats)))
                    if let channels = pane as? ChatsComponentPane.Channels {
                        watched.insert("room")
                        watcher.watch("room", channels.component.room)
                        if let room = channels.component.room.value.child?.instance {
                            routes.append(Route(key: "room:\(ObjectIdentifier(room))", kind: .room(room)))
                        }
                    }
                }
            }
        }

        for item in root.stack.value.items {
            let child = item.instance
            guard !RootView.isTopLevel(child) else { continue }
            routes.append(Route(key: "root:\(ObjectIdentifier(child))", kind: .root(child)))
        }
        watcher.keep(only: watched)

        if base != baseTab { baseTab = base }
        if routes.map(\.key) != self.routes.map(\.key) { self.routes = routes }
    }

    /// Your profile and Bookmarks, pages above the tabs on a phone.
    static func isAbove(_ child: TabsComponentChild) -> Bool {
        child is TabsComponentChild.Profile || child is TabsComponentChild.Bookmarks
    }

    static func tab(_ child: TabsComponentChild) -> TabsComponentTab { child.tab }
}
