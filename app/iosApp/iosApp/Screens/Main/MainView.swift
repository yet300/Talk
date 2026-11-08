import SwiftUI
import SharedLogic

/// Main (`MainComponent.panels`, `ChildPanels`) in the system `TabView`: the Liquid Glass tab bar on a phone, which on
/// the iPhone Duo (iOS 27.1) stands vertically at the side as the design's side column. Each tab has its own
/// `NavigationStack`; the router pushes onto the one in front.
///
/// The window picks the panels' mode, as Android does from its window size: one pane on phones, the Duo outer screen
/// and the Duo inner screen upright; two panes (list | article, split on the fold) on a regular-width window that is
/// wider than tall, the Duo inner screen sideways. Chats splits itself, so it takes the whole width.
struct MainView: View {
    let root: RootComponent
    let main: MainComponent

    @StateObject private var router: MainRouter
    @StateValue private var panels: ChildPanels<AnyObject, TabsComponent, AnyObject, DetailsComponent, KotlinNothing, KotlinNothing>
    @Environment(\.horizontalSizeClass) private var sizeClass
    @State private var barIsVertical = false

    init(root: RootComponent, main: MainComponent) {
        self.root = root
        self.main = main
        _router = StateObject(wrappedValue: MainRouter(root: root, main: main))
        _panels = StateValue(main.panels)
    }

    var body: some View {
        GeometryReader { proxy in
            let layout = MainLayout(sizeClass: sizeClass, size: proxy.size, verticalBar: barIsVertical)
            content(layout)
                .onChange(of: layout, initial: true) { _, layout in apply(layout) }
                .onChange(of: router.baseTab) { _, _ in apply(layout) }
        }
        .ignoresSafeArea(.keyboard)
        .modifier(VerticalBarReader(isVertical: $barIsVertical))
    }

    @ViewBuilder
    private func content(_ layout: MainLayout) -> some View {
        MainTabs(router: router, main: main, tabs: panels.main.instance, layout: layout)
    }

    private func apply(_ layout: MainLayout) {
        router.profileIsTab = layout.showsAllTabs
        router.chatsSideBySide = layout.isWide
        let dual = layout.isTwoPane && router.baseTab != .chats
        main.onModeChanged(mode: dual ? .dual : .single)
    }
}

/// What the window allows: decided by size class and the shape of the space, never by device or orientation.
struct MainLayout: Equatable {
    /// Regular width: the Duo inner screen, iPad, a wide window. Your profile and Bookmarks are tabs of their own.
    let isWide: Bool
    /// Regular width and wider than tall: the Duo inner screen sideways. Two panes.
    let isTwoPane: Bool
    /// The bar has room for every destination: a wide window, or a bar standing at the side (the Duo's side column,
    /// with Bookmarks and Profile as on the design); a phone's bottom bar keeps five.
    let showsAllTabs: Bool

    init(sizeClass: UserInterfaceSizeClass?, size: CGSize, verticalBar: Bool) {
        isWide = sizeClass == .regular
        isTwoPane = isWide && size.width > size.height
        showsAllTabs = isWide || verticalBar
    }
}

/// The tab bar and each tab's navigation stack.
private struct MainTabs: View {
    @ObservedObject var router: MainRouter
    let main: MainComponent
    let tabs: TabsComponent
    let layout: MainLayout

    @StateValue private var stack: ChildStack<AnyObject, TabsComponentChild>
    @StateValue private var me: TabsComponentMe

    init(router: MainRouter, main: MainComponent, tabs: TabsComponent, layout: MainLayout) {
        self.router = router
        self.main = main
        self.tabs = tabs
        self.layout = layout
        _stack = StateValue(tabs.stack)
        _me = StateValue(tabs.me)
    }

    var body: some View {
        let selection = Binding<TabsComponentTab>(
            get: { router.baseTab },
            // A tap on the tab in front goes back to its root too: the details panel closes, as iOS tabs pop.
            set: { main.onTabSelected(tab: $0) }
        )
        TabView(selection: selection) {
            ForEach(visibleTabs, id: \.self) { tab in
                Tab(value: tab) {
                    TabHost(router: router, main: main, tabs: tabs, tab: tab, child: child(for: tab), layout: layout)
                } label: {
                    Label { Text(tab.label) } icon: { TalkIcon(icon: tab.icon(selected: tab == router.baseTab)) }
                }
                .badge(tab == .chats ? Int(me.unreadChats) : 0)
                .accessibilityLabel(tab.label)
            }
        }
        .modifier(TabBarBehavior())
        // The set of tabs changes once the bar is known to stand at the side, and in a different order. A TabView reused
        // across that keeps pages for the old positions and shows nothing for a moved tab (Chats, on the Duo).
        .id(layout.showsAllTabs)
    }

    /// Phone: Home, Search, Activity, Videos, Chats (decision 19); your profile and Bookmarks open from Home. Wide
    /// windows and the Duo's side bar add Bookmarks and Profile, as the side column does.
    private var visibleTabs: [TabsComponentTab] {
        layout.showsAllTabs
            ? [.home, .search, .activity, .chats, .videos, .bookmarks, .profile]
            : [.home, .search, .activity, .videos, .chats]
    }

    private func child(for tab: TabsComponentTab) -> TabsComponentChild? {
        stack.items.map { $0.instance }.first { $0.tab == tab }
    }
}

/// The tab bar shrinks while reading on iOS 26+.
private struct TabBarBehavior: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 26, *) {
            content.tabBarMinimizeBehavior(.onScrollDown)
        } else {
            content
        }
    }
}

extension TabsComponentTab {
    var label: String {
        let strings = MR.strings.shared
        switch self {
        case .home: return strings.tab_home.text
        case .search: return strings.tab_search.text
        case .activity: return strings.tab_activity.text
        case .chats: return strings.tab_chats.text
        case .videos: return strings.tab_videos.text
        case .bookmarks: return strings.tab_bookmarks.text
        case .profile: return strings.tab_profile.text
        }
    }

    func icon(selected: Bool) -> SharedLogic.ImageResource {
        switch self {
        case .home: return TalkIcons.home
        case .search: return TalkIcons.search
        case .activity: return TalkIcons.activity
        case .chats: return TalkIcons.chat
        case .videos: return TalkIcons.video
        case .bookmarks: return selected ? TalkIcons.bookmarkFilled : TalkIcons.bookmark
        case .profile: return TalkIcons.user
        }
    }
}
