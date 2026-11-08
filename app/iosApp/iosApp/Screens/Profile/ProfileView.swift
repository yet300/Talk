import SwiftUI
import SharedLogic

/// A profile (`Profile · Mobile`): the banner reaches the top edge under a clear bar; once it scrolls away the bar turns
/// to glass and the name appears. Posts · Replies (· Bookmarks on your own) are pages to swipe (`ChildPages`), each with
/// its own scroll; the header and the tabs stay in place above them and collapse with the page's scroll until the tabs
/// pin under the bar. Switching pages lines the next one up with the header, so it never jumps.
struct ProfileView: View {
    private let component: ProfileComponent
    private let canGoBack: Bool
    @StateValue private var model: ProfileComponentModel
    @StateValue private var pages: ChildPages<AnyObject, ProfileComponentTabPage>
    @State private var headerHeight: CGFloat = 320
    @State private var offsets: [ProfileComponentTab: CGFloat] = [:]
    @State private var positions: [ProfileComponentTab: ScrollPosition] = [:]
    @State private var reporting = false

    init(_ component: ProfileComponent, canGoBack: Bool) {
        self.component = component
        self.canGoBack = canGoBack
        _model = StateValue(component.model)
        _pages = StateValue(component.pages)
    }

    var body: some View {
        GeometryReader { proxy in
            let safeTop = proxy.safeAreaInsets.top
            let pinned = safeTop + Self.tabsHeight
            let range = max(headerHeight - pinned, 0)
            let collapsed = min(max(offsets[model.tab] ?? 0, 0), range)
            let bannerPast = collapsed > TalkSize.bannerHeight * 0.66 - 20
            ZStack(alignment: .top) {
                PagesView(pages: pages, onSelect: select) { page in
                    list(page, topInset: headerHeight)
                        .id(ObjectIdentifier(page))
                }
                VStack(spacing: 0) {
                    ProfileHeader(model: model, component: component, topInset: safeTop)
                    tabs
                    ErrorRow(error: model.error, onDismiss: component.onErrorShown)
                }
                .background(TalkColor.bg)
                .onGeometryChange(for: CGFloat.self) { $0.size.height } action: { headerHeight = $0 }
                .offset(y: -collapsed)
            }
            .ignoresSafeArea(edges: .top)
            .onChange(of: model.tab) { _, tab in lineUp(tab, collapsed: collapsed, range: range) }
            .toolbarBackground(bannerPast ? .automatic : .hidden, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .principal) {
                    Text(model.user?.displayLabel ?? "")
                        .talkTextStyle(.headingMedium)
                        .foregroundStyle(TalkColor.textPrimary)
                        .lineLimit(1)
                        .opacity(bannerPast ? 1 : 0)
                        .animation(TalkMotion.fade, value: bannerPast)
                }
                if model.isMe {
                    ToolbarItem(placement: .primaryAction) {
                        ToolbarIconButton(icon: TalkIcons.settings, label: MR.strings.shared.action_settings.text, action: component.onOpenSettings)
                    }
                } else if model.user != nil {
                    ToolbarItem(placement: .primaryAction) {
                        ToolbarIconButton(icon: TalkIcons.more, label: MR.strings.shared.action_more.text) { reporting = true }
                    }
                }
            }
        }
        .background(TalkColor.bg.ignoresSafeArea())
        .navigationBarBackButtonHidden(!(canGoBack || model.canGoBack))
        .navigationBarTitleDisplayMode(.inline)
        .safeAreaInset(edge: .top, spacing: 0) { NetworkBar() }
        .sheet(isPresented: $reporting) {
            if let user = model.user {
                ReportSheet(target: ReportTargetProfile(author: user.id)) { reporting = false }
            }
        }
    }

    private static let tabsHeight: CGFloat = 56

    private var tabs: some View {
        let strings = MR.strings.shared
        let available = pages.items.compactMap { $0.configuration as? __ProfileComponentTab }.map { $0 as ProfileComponentTab }
        return Picker(strings.tab_profile.text, selection: Binding(get: { model.tab }, set: { component.onTabSelected(tab: $0) })) {
            ForEach(available, id: \.self) { tab in
                switch tab {
                case .posts: Text(strings.profile_posts.text).tag(tab)
                case .replies: Text(strings.profile_replies.text).tag(tab)
                case .bookmarks: Text(strings.profile_bookmarks.text).tag(tab)
                }
            }
        }
        .pickerStyle(.segmented)
        .frame(maxWidth: 360)
        .padding(.horizontal, TalkSpacing.lg)
        .frame(height: Self.tabsHeight)
        .sensoryFeedback(.selection, trigger: model.tab)
    }

    private func select(_ index: Int) {
        guard let tab = pages.items[index].configuration as? __ProfileComponentTab else { return }
        component.onTabSelected(tab: tab as ProfileComponentTab)
    }

    /// The page shown next starts where the header is: under the pinned tabs when collapsed, not under a gap.
    private func lineUp(_ tab: ProfileComponentTab, collapsed: CGFloat, range: CGFloat) {
        let offset = offsets[tab] ?? 0
        if offset < collapsed || (offset > collapsed && collapsed < range) {
            positions[tab, default: ScrollPosition()].scrollTo(y: collapsed)
        }
    }

    private func list(_ page: ProfileComponentTabPage, topInset: CGFloat) -> some View {
        let strings = MR.strings.shared
        let tab = page.tab
        return PagedPostList(
            page.pagingController(),
            onOpen: { component.onOpenPost(id: $0.id) },
            onOpenAuthor: { component.onOpenAuthor(id: $0.author.id) },
            actions: PostActionHandlers(onReply: component.onReply, onRepost: component.onRepost, onLike: component.onLike,
                                        onBookmark: component.onBookmark, onOpenRevisions: component.onOpenRevisions),
            topInset: topInset,
            tracking: ScrollTracking(
                position: Binding(get: { positions[tab] ?? ScrollPosition() }, set: { positions[tab] = $0 }),
                onScroll: { offsets[tab] = $0 }
            )
        ) {
            EmptyView()
        } empty: {
            switch tab {
            case .posts: EmptyState(strings.profile_empty_title.text)
            case .replies: EmptyState(strings.profile_empty_replies_title.text)
            case .bookmarks: EmptyState(strings.bookmarks_empty_title.text, body: strings.bookmarks_empty_body.text, icon: TalkIcons.bookmark)
            }
        }
    }
}

/// The banner (or the surface), the avatar rising over it, Follow or Edit profile, name, `nip05 · npub`, about, links,
/// counts. Outside your network the banner and avatar are blurred until Show (decision 18).
private struct ProfileHeader: View {
    let model: ProfileComponentModel
    let component: ProfileComponent
    let topInset: CGFloat
    @Environment(\.moderation) private var moderation
    @Environment(\.openURL) private var openURL
    @State private var shown = false

    private let avatarSize: CGFloat = 96

    var body: some View {
        let strings = MR.strings.shared
        let user = model.user
        let outside = user.map { !shown && !moderation.isInNetwork(id: $0.id) } ?? false
        VStack(alignment: .leading, spacing: 0) {
            ZStack {
                TalkColor.surface
                if let banner = user?.banner, !banner.isEmpty, let url = URL(string: banner) {
                    RemoteImage(url: url).blur(radius: outside ? 24 : 0).clipped()
                }
                if outside && (user?.banner != nil || user?.picture != nil) {
                    TalkButton(strings.action_show.text, style: .secondary) { withAnimation(TalkMotion.fade) { shown = true } }
                        .background(TalkColor.bg, in: Capsule())
                        .accessibilityLabel(strings.moderation_show_profile.text)
                }
            }
            .frame(height: topInset + TalkSize.bannerHeight * 0.66)
            .clipped()

            HStack(alignment: .top) {
                Avatar(user: user, size: avatarSize, shown: shown)
                    .overlay(Circle().stroke(TalkColor.bg, lineWidth: 4))
                    .offset(y: -avatarSize / 2)
                    .frame(height: avatarSize / 2 + TalkSpacing.sm, alignment: .top)
                Spacer()
                Group {
                    if model.isMe {
                        TalkButton(strings.action_edit_profile.text, style: .secondary, action: component.onEditProfile)
                    } else if user != nil {
                        FollowButton(following: model.isFollowing, action: component.onFollowToggle)
                    }
                }
                .padding(.top, TalkSpacing.md)
            }
            .padding(.horizontal, TalkSpacing.lg)

            VStack(alignment: .leading, spacing: TalkSpacing.sm) {
                Text(user?.displayLabel ?? "")
                    .talkTextStyle(.display)
                    .foregroundStyle(TalkColor.textPrimary)
                    .accessibilityAddTraits(.isHeader)
                Text([user?.nip05.flatMap { $0.isEmpty ? nil : $0 }, user?.npub.shortNpub()].compactMap { $0 }.joined(separator: " · "))
                    .talkTextStyle(.body)
                    .foregroundStyle(TalkColor.textSecondary)
                    .textSelection(.enabled)
                if let about = user?.about, !about.isEmpty {
                    Text(about).talkTextStyle(.body).foregroundStyle(TalkColor.textPrimary)
                }
                if let user { links(user) }
                stats
            }
            .padding(.horizontal, TalkSpacing.lg)
            .padding(.bottom, TalkSpacing.md)
        }
    }

    /// The website, and the GitHub account when its gist proof holds (NIP-39).
    @ViewBuilder
    private func links(_ user: User) -> some View {
        HStack(spacing: TalkSpacing.lg) {
            if let site = user.website, !site.isEmpty {
                Button {
                    if let url = URL(string: site.hasPrefix("http") ? site : "https://\(site)") { openURL(url) }
                } label: {
                    HStack(spacing: 4) {
                        TalkIcon(icon: TalkIcons.link, size: 18).foregroundStyle(TalkColor.textSecondary)
                        Text(site.displayAddress).talkTextStyle(.body).foregroundStyle(TalkColor.accent).lineLimit(1)
                    }
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(.isLink)
            }
            if let github = user.github {
                Button {
                    if let url = URL(string: "https://github.com/\(github.login)") { openURL(url) }
                } label: {
                    HStack(spacing: 4) {
                        TalkIcon(icon: TalkIcons.github, size: 18).foregroundStyle(TalkColor.textSecondary)
                        Text(github.login).talkTextStyle(.body).foregroundStyle(TalkColor.textPrimary)
                        TalkIcon(icon: TalkIcons.check, size: 18).foregroundStyle(TalkColor.accent)
                    }
                }
                .buttonStyle(.plain)
                .accessibilityLabel(MR.strings.shared.github_badge.format(github.login))
            }
        }
    }

    /// `posts · following · followers`.
    private var stats: some View {
        let strings = MR.strings.shared
        var parts: [(Int64, String)] = []
        if let posts = model.stats.posts { parts.append((posts.int64Value, strings.profile_articles.text)) }
        parts.append((Int64(model.stats.following), strings.profile_following.text))
        if let followers = model.stats.followers { parts.append((followers.int64Value, strings.profile_followers.text)) }
        return HStack(spacing: TalkSpacing.lg) {
            ForEach(parts, id: \.1) { count, label in
                (Text("\(count) ").foregroundStyle(TalkColor.textPrimary).bold() + Text(label).foregroundStyle(TalkColor.textSecondary))
                    .talkTextStyle(.body)
            }
        }
    }
}
