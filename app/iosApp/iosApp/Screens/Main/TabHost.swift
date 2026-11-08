import SwiftUI
import SharedLogic

/// One tab: its navigation stack, whose path is the router's pages while this tab is in front.
struct TabHost: View {
    @ObservedObject var router: MainRouter
    let main: MainComponent
    let tabs: TabsComponent
    let tab: TabsComponentTab
    let child: TabsComponentChild?
    let layout: MainLayout

    var body: some View {
        NavigationStack(path: router.path(for: tab)) {
            Group {
                if let child {
                    TabRoot(router: router, main: main, tabs: tabs, child: child, layout: layout)
                } else {
                    TalkColor.bg.ignoresSafeArea()
                }
            }
            .navigationDestination(for: Route.self) { route in
                RouteView(router: router, route: route)
            }
        }
    }
}

/// A tab's own screen; in two panes, the tab (or the open post's replies) beside the details panel.
private struct TabRoot: View {
    @ObservedObject var router: MainRouter
    let main: MainComponent
    let tabs: TabsComponent
    let child: TabsComponentChild
    let layout: MainLayout

    @StateValue private var panels: ChildPanels<AnyObject, TabsComponent, AnyObject, DetailsComponent, KotlinNothing, KotlinNothing>
    @StateValue private var reading: KotlinBoolean
    @Environment(\.back) private var back

    init(router: MainRouter, main: MainComponent, tabs: TabsComponent, child: TabsComponentChild, layout: MainLayout) {
        self.router = router
        self.main = main
        self.tabs = tabs
        self.child = child
        self.layout = layout
        _panels = StateValue(main.panels)
        _reading = StateValue(main.isReading)
    }

    var body: some View {
        let dual = panels.mode != .single
        let details = panels.details?.instance
        Group {
            if dual {
                TwoPanes {
                    if reading.boolValue, let details {
                        ReadingPane(details: details)
                    } else {
                        TabContent(router: router, tabs: tabs, child: child, layout: layout)
                    }
                } secondary: {
                    if let details {
                        DetailsPane(details: details)
                            .id(ObjectIdentifier(details))
                            .environment(\.isSecondaryPane, true)
                    } else {
                        DetailsPlaceholder()
                    }
                }
                .toolbar {
                    if details != nil {
                        ToolbarItem(placement: .navigation) {
                            ToolbarIconButton(icon: TalkIcons.back, label: MR.strings.shared.action_back.text) { back() }
                        }
                    }
                }
                .environment(\.selectedPost, details?.link.postKey)
                .environment(\.readerMode, ReaderMode(twoPane: true, onShowReplies: main.onShowReplies))
            } else {
                TabContent(router: router, tabs: tabs, child: child, layout: layout)
            }
        }
    }
}

/// The tab's screen.
private struct TabContent: View {
    @ObservedObject var router: MainRouter
    let tabs: TabsComponent
    let child: TabsComponentChild
    let layout: MainLayout

    var body: some View {
        switch onEnum(of: child) {
        case .home(let home):
            HomeView(home.component, tabs: tabs, onSettings: router.root.onOpenSettings, showsProfile: !layout.showsAllTabs)
        case .search(let search):
            SearchView(search.component)
        case .activity(let activity):
            ActivityView(activity.component)
        case .chats(let chats):
            ChatsView(chats.component, sideBySide: layout.isWide)
        case .videos(let videos):
            VideosView(videos.component)
        case .bookmarks(let bookmarks):
            BookmarksView(bookmarks.component, canGoBack: !layout.showsAllTabs)
        case .profile(let profile):
            ProfileView(profile.component, canGoBack: !layout.showsAllTabs)
        }
    }
}

/// Two panes split on the fold: with the iOS 27.1 SDK an `ArrangementView` (split, horizontal), which follows the
/// fold's division region; before it, the design's 415 | 476 split.
struct TwoPanes<Primary: View, Secondary: View>: View {
    @ViewBuilder let primary: () -> Primary
    @ViewBuilder let secondary: () -> Secondary

    var body: some View {
        if #available(iOS 27.1, *) {
            ArrangementView {
                primary()
            } secondary: {
                secondary()
                    .overlay(alignment: .leading) { Rectangle().fill(TalkColor.border).frame(width: TalkSize.hairline).ignoresSafeArea() }
            }
            .arrangementViewStyle(.split.axes(.horizontal))
        } else {
            GeometryReader { proxy in
                HStack(spacing: 0) {
                    primary().frame(width: proxy.size.width * 415 / 891)
                    Rectangle().fill(TalkColor.border).frame(width: TalkSize.hairline).ignoresSafeArea()
                    secondary().frame(maxWidth: .infinity)
                }
            }
        }
    }
}

/// The details panel next to the list: its top page (a post, an author, a video). Back pops it (side bar Back).
private struct DetailsPane: View {
    let details: DetailsComponent
    @StateValue private var stack: ChildStack<AnyObject, DetailsComponentChild>

    init(details: DetailsComponent) {
        self.details = details
        _stack = StateValue(details.stack)
    }

    var body: some View {
        let page = stack.active.instance
        DetailsPage(page: page)
            .id(ObjectIdentifier(page))
            .transition(.opacity)
            .animation(TalkMotion.fade, value: ObjectIdentifier(page))
    }
}

/// One page of the details panel.
struct DetailsPage: View {
    let page: DetailsComponentChild

    var body: some View {
        switch onEnum(of: page) {
        case .post(let post): PostView(post.component)
        case .profile(let profile): ProfileView(profile.component, canGoBack: true)
        case .video(let video): VideoView(video.component)
        }
    }
}

/// The left pane in the replies pose (Duo inner: replies | article): the open post's replies.
private struct ReadingPane: View {
    let details: DetailsComponent
    @StateValue private var stack: ChildStack<AnyObject, DetailsComponentChild>

    init(details: DetailsComponent) {
        self.details = details
        _stack = StateValue(details.stack)
    }

    var body: some View {
        if let post = stack.active.instance as? DetailsComponentChild.Post {
            RepliesPane(component: post.component)
        } else {
            TalkColor.bg
        }
    }
}

/// The right pane before a post is picked: the wordmark, faint.
private struct DetailsPlaceholder: View {
    var body: some View {
        ZStack {
            TalkColor.bg.ignoresSafeArea()
            Text(MR.strings.shared.app_name.text)
                .talkTextStyle(.wordmark)
                .foregroundStyle(TalkColor.textSecondary.opacity(0.4))
                .accessibilityHidden(true)
        }
    }
}

/// A pushed page.
private struct RouteView: View {
    @ObservedObject var router: MainRouter
    let route: Route

    var body: some View {
        Group {
            switch route.kind {
            case .above(let child):
                if let profile = child as? TabsComponentChild.Profile {
                    ProfileView(profile.component, canGoBack: true)
                } else if let bookmarks = child as? TabsComponentChild.Bookmarks {
                    BookmarksView(bookmarks.component, canGoBack: true)
                }
            case .details(let page, _):
                DetailsPage(page: page)
            case .chatPane(let chats):
                ChatPaneView(chats: chats)
            case .root(let child):
                RootDestination(child: child)
            }
        }
        .toolbar(.hidden, for: .tabBar)
    }
}

/// Compose, Settings, Revisions and Edit profile, pushed above Main.
private struct RootDestination: View {
    let child: RootComponentChild

    var body: some View {
        switch onEnum(of: child) {
        case .compose(let compose): ComposeView(compose.component)
        case .settings(let settings): ReadableWidth { SettingsView(settings.component) }
        case .revisions(let revisions): ReadableWidth { RevisionsView(revisions.component) }
        case .editProfile(let edit): ReadableWidth { EditProfileView(edit.component) }
        case .splash, .auth, .lock, .main: EmptyView()
        }
    }
}

/// A form or list on a large window: a centred column of a readable width instead of rows stretched across it.
struct ReadableWidth<Content: View>: View {
    @ViewBuilder let content: () -> Content

    var body: some View {
        content()
            .frame(maxWidth: 680)
            .frame(maxWidth: .infinity)
            .background(TalkColor.bg.ignoresSafeArea())
    }
}

extension EnvironmentValues {
    /// The page is the right pane of two: it leaves the navigation title to the left one.
    @Entry var isSecondaryPane = false
    /// Back through Decompose's dispatcher: the topmost handler wins (leaving the replies pose, then the details panel).
    @Entry var back: () -> Void = {}
}

extension View {
    /// The navigation title, unless this page is the right pane of two.
    func talkTitle(_ title: String) -> some View {
        modifier(TalkTitle(title: title))
    }
}

private struct TalkTitle: ViewModifier {
    let title: String
    @Environment(\.isSecondaryPane) private var isSecondaryPane

    func body(content: Content) -> some View {
        if isSecondaryPane {
            content
        } else {
            content.navigationTitle(title).navigationBarTitleDisplayMode(.inline)
        }
    }
}
