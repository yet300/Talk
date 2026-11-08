import SwiftUI
import SharedLogic

/// Home (`Timeline · Mobile`): Following, Popular and Latest as pages to swipe (`ChildPages`), each edge to edge under
/// the glass bar, with the feed segments floating under it. The bar has your avatar (your profile, on a phone), the
/// wordmark and Settings. Compose floats above the tab bar on a phone; where bars stand vertically (the Duo) and on
/// wide windows it is the bar's prominent action instead.
struct HomeView: View {
    private let component: HomeComponent
    private let tabs: TabsComponent
    private let onSettings: () -> Void
    private let showsProfile: Bool
    @StateValue private var model: HomeComponentModel
    @StateValue private var pages: ChildPages<AnyObject, HomeComponentFeedPage>
    @StateValue private var me: TabsComponentMe
    @Environment(\.horizontalSizeClass) private var sizeClass

    init(_ component: HomeComponent, tabs: TabsComponent, onSettings: @escaping () -> Void, showsProfile: Bool) {
        self.component = component
        self.tabs = tabs
        self.onSettings = onSettings
        self.showsProfile = showsProfile
        _model = StateValue(component.model)
        _pages = StateValue(component.pages)
        _me = StateValue(tabs.me)
    }

    var body: some View {
        let strings = MR.strings.shared
        PagesView(pages: pages, onSelect: { component.onFeedSelected(feed: HomeComponentFeed.allCases[$0]) }) { page in
            FeedList(page: page, component: component)
                .id(ObjectIdentifier(page))
        }
        .background(TalkColor.bg.ignoresSafeArea())
        .safeAreaInset(edge: .top, spacing: 0) {
            VStack(spacing: 0) {
                NetworkBar()
                FeedSegments(selected: model.feed, onSelect: component.onFeedSelected)
                ErrorRow(error: model.error, onDismiss: component.onErrorShown)
            }
        }
        .overlay(alignment: .bottomTrailing) {
            if floatsCompose {
                ComposeFab(action: component.onCompose)
            }
        }
        .talkTitle(strings.tab_home.text)
        .toolbar {
            if showsProfile {
                ToolbarItem(placement: .topBarLeading) {
                    Avatar(user: me.user, size: 32) { tabs.onTabClicked(tab: .profile) }
                        .accessibilityLabel(strings.action_your_profile.text)
                }
            }
            ToolbarItem(placement: .principal) {
                Text(strings.app_name.text).talkTextStyle(.wordmark).foregroundStyle(TalkColor.textPrimary)
                    .accessibilityAddTraits(.isHeader)
            }
            if !floatsCompose {
                ToolbarItem(placement: .primaryAction) {
                    ToolbarIconButton(icon: TalkIcons.compose, label: strings.action_compose.text, action: component.onCompose)
                }
            }
            ToolbarItem(placement: .topBarTrailing) {
                ToolbarIconButton(icon: TalkIcons.settings, label: strings.action_settings.text, action: onSettings)
            }
        }
        .modifier(VerticalBarReader(isVertical: $barIsVertical))
    }

    @State private var barIsVertical = false

    /// The phone's floating Compose; where bars stand vertically or the window is wide, it joins the bar.
    private var floatsCompose: Bool { sizeClass == .compact && !barIsVertical }
}

/// Following · Popular · Latest, the feed capsule under the bar.
private struct FeedSegments: View {
    let selected: HomeComponentFeed
    let onSelect: (HomeComponentFeed) -> Void

    var body: some View {
        let strings = MR.strings.shared
        Picker(strings.tab_home.text, selection: Binding(get: { selected }, set: onSelect)) {
            ForEach(HomeComponentFeed.allCases, id: \.self) { feed in
                switch feed {
                case .following: Text(strings.feed_following.text).tag(feed)
                case .popular: Text(strings.feed_popular.text).tag(feed)
                case .latest: Text(strings.feed_latest.text).tag(feed)
                }
            }
        }
        .pickerStyle(.segmented)
        .frame(maxWidth: 360)
        .padding(.horizontal, TalkSpacing.lg)
        .padding(.vertical, TalkSpacing.sm)
        .sensoryFeedback(.selection, trigger: selected)
    }
}

/// One feed's posts.
private struct FeedList: View {
    let page: HomeComponentFeedPage
    let component: HomeComponent

    var body: some View {
        let strings = MR.strings.shared
        PagedPostList(
            page.pagingController(),
            onOpen: { component.onOpenPost(id: $0.id) },
            onOpenAuthor: { component.onOpenAuthor(id: $0.author.id) },
            actions: PostActionHandlers(onReply: component.onReply, onRepost: component.onRepost, onLike: component.onLike,
                                        onBookmark: component.onBookmark, onOpenRevisions: component.onOpenRevisions)
        ) {
            if page.feed == .popular {
                Caption(strings.feed_popular_caption.text)
            }
        } empty: {
            if page.feed == .following {
                EmptyState(strings.feed_following_empty_title.text, body: strings.feed_following_empty_body.text) {
                    TalkButton(strings.action_browse_latest.text) { component.onFeedSelected(feed: .latest) }
                }
            } else {
                EmptyState(strings.feed_empty_title.text, body: strings.feed_empty_body.text)
            }
        }
    }
}

/// The floating Compose button (56 pt, blue, above the tab bar).
struct ComposeFab: View {
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            TalkIcon(icon: TalkIcons.compose)
                .foregroundStyle(TalkColor.onAccent)
                .frame(width: TalkSize.fab, height: TalkSize.fab)
                .background(TalkColor.accent, in: Circle())
                .shadow(color: .black.opacity(0.25), radius: 8, y: 2)
        }
        .buttonStyle(PressStyle())
        .padding(.trailing, TalkSpacing.lg)
        .padding(.bottom, TalkSpacing.lg)
        .accessibilityLabel(MR.strings.shared.action_compose.text)
        .transition(.scale.combined(with: .opacity))
    }
}

/// Whether the bars here stand vertically (the iPhone Duo with the iOS 27.1 SDK): `toolbarVerticalEdge` names the
/// edge a vertical bar takes in this context, and is nil where bars stay horizontal.
struct VerticalBarReader: ViewModifier {
    @Binding var isVertical: Bool

    func body(content: Content) -> some View {
        if #available(iOS 27.1, *) {
            content.background(VerticalEdgeProbe(isVertical: $isVertical))
        } else {
            content
        }
    }
}

@available(iOS 27.1, *)
private struct VerticalEdgeProbe: View {
    @Binding var isVertical: Bool
    @Environment(\.toolbarVerticalEdge) private var edge

    var body: some View {
        Color.clear
            .onChange(of: edge, initial: true) { _, edge in isVertical = edge != nil }
    }
}
