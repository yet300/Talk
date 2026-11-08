import SwiftUI
import WebKit
import SharedLogic

/// The video page (`VideoPage-Mobile`): the player under the bar, the details (Like, Watch later, Share, Copy link, the
/// summary, why Hacker News recommends it, the credit), the comments (NIP-22) with a field, and Related. A related video
/// opens at the top.
struct VideoView: View {
    private let component: VideoComponent
    @StateValue private var model: VideoComponentModel

    init(_ component: VideoComponent) {
        self.component = component
        _model = StateValue(component.model)
    }

    var body: some View {
        let strings = MR.strings.shared
        VStack(spacing: 0) {
            NetworkBar()
            VideoPlayer(id: model.id, isPlaying: model.isPlaying, onPlay: component.onPlay)
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 0) {
                        Color.clear.frame(height: 0).id("top")
                        ErrorRow(error: model.error, onDismiss: component.onErrorShown)
                        VideoInfo(model: model, component: component)
                        SectionTitle(strings.video_comments.text)
                        HStack(spacing: TalkSpacing.sm) {
                            TalkTextField(placeholder: strings.video_comment_placeholder.text, text: model.comment,
                                          onChange: component.onCommentChanged, pill: true, submitLabel: .send) {
                                if model.canSend { component.onSendComment() }
                            }
                            TalkButton(strings.action_reply.text, style: .post, enabled: model.canSend, busy: model.isSending,
                                       action: component.onSendComment)
                        }
                        .padding(.horizontal, TalkSpacing.lg)
                        .padding(.vertical, TalkSpacing.sm)
                        if model.comments.isEmpty {
                            EmptyState(strings.video_comments_empty.text)
                        }
                        ForEach(model.comments, id: \.id) { comment in
                            CommentCard(comment: comment) { component.onOpenAuthor(id: comment.author.id) }
                        }
                        if !model.related.isEmpty {
                            SectionTitle(strings.video_related.text)
                            ForEach(model.related, id: \.id) { video in
                                VideoCard(video: video) { component.onOpenVideo(id: video.id) }
                                    .padding(.horizontal, TalkSpacing.sm)
                            }
                        }
                    }
                    .padding(.bottom, TalkSpacing.xl)
                }
                .scrollDismissesKeyboard(.interactively)
                .onChange(of: model.id) { _, _ in proxy.scrollTo("top") }
            }
        }
        .background(TalkColor.bg.ignoresSafeArea())
        .talkTitle(strings.video_title.text)
    }
}

/// The details (`VideoInfo`).
private struct VideoInfo: View {
    let model: VideoComponentModel
    let component: VideoComponent
    @Environment(\.toast) private var toast

    var body: some View {
        let strings = MR.strings.shared
        let video = model.video
        VStack(alignment: .leading, spacing: TalkSpacing.md) {
            Text(video?.title ?? model.url).talkTextStyle(.headingLarge).foregroundStyle(TalkColor.textPrimary)
            Text(strings.video_stats.format(video?.channel ?? "", "\(model.stats.likes)", "\(model.stats.comments)"))
                .talkTextStyle(.small).foregroundStyle(TalkColor.textSecondary)
            ScrollView(.horizontal) {
                HStack(spacing: TalkSpacing.sm) {
                    ToggleChip(icon: model.viewer.liked ? TalkIcons.likeFilled : TalkIcons.like, label: strings.video_like.text,
                               onColor: model.viewer.liked ? TalkColor.like : nil, action: component.onLike)
                    ToggleChip(icon: TalkIcons.clock, label: strings.video_later.text,
                               onColor: model.viewer.watchLater ? TalkColor.accent : nil, action: component.onWatchLater)
                    ToggleChip(icon: TalkIcons.share, label: strings.video_share.text, onColor: nil, action: component.onShare)
                    ToggleChip(icon: TalkIcons.link, label: strings.video_copy_link.text, onColor: nil) {
                        copyText(model.url, toast: toast)
                    }
                }
            }
            .scrollIndicators(.hidden)
            if let video {
                if let summary = video.summary {
                    Text(summary).talkTextStyle(.body).foregroundStyle(TalkColor.textPrimary)
                }
                if let why = video.whyHn {
                    VStack(alignment: .leading, spacing: TalkSpacing.sm) {
                        Text(strings.video_why.text).talkTextStyle(.bodyBold).foregroundStyle(TalkColor.textPrimary)
                        Text(why).talkTextStyle(.body).foregroundStyle(TalkColor.textPrimary)
                        if let quote = video.quote {
                            Text("“\(quote)”").talkTextStyle(.body).foregroundStyle(TalkColor.textSecondary)
                        }
                        if video.recommenders > 0 {
                            Text(strings.video_recommenders.format("\(video.recommenders)")).talkTextStyle(.small).foregroundStyle(TalkColor.textSecondary)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(TalkSpacing.md)
                    .background(TalkColor.surface, in: RoundedRectangle(cornerRadius: 12))
                }
                let labels = video.categories + [video.type?.capitalizedFirst, video.level?.capitalizedFirst].compactMap { $0 }
                if !labels.isEmpty {
                    Text(labels.joined(separator: "  ·  ")).talkTextStyle(.small).foregroundStyle(TalkColor.accent)
                }
                if video.whyHn != nil {
                    Text(strings.videos_credit.text).talkTextStyle(.small).foregroundStyle(TalkColor.textSecondary)
                }
            }
        }
        .padding(TalkSpacing.lg)
    }
}

/// An action that turns on (Like, Watch later): its icon and label, the on colour when on.
private struct ToggleChip: View {
    let icon: SharedLogic.ImageResource
    let label: String
    let onColor: Color?
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                TalkIcon(icon: icon, size: 18)
                Text(label).talkTextStyle(.bodyBold)
            }
            .foregroundStyle(onColor ?? TalkColor.textPrimary)
            .padding(.horizontal, TalkSpacing.md)
            .frame(minHeight: 36)
            .overlay(Capsule().strokeBorder(TalkColor.controlBorder, lineWidth: 1))
        }
        .buttonStyle(PressStyle())
        .animation(TalkMotion.settle, value: onColor != nil)
        .accessibilityAddTraits(onColor != nil ? .isSelected : [])
    }
}

/// A NIP-22 comment: top-level only, no actions (decision 16).
private struct CommentCard: View {
    let comment: VideoComment
    let onOpenAuthor: () -> Void

    var body: some View {
        let strings = MR.strings.shared
        HStack(alignment: .top, spacing: TalkSpacing.md) {
            Avatar(user: comment.author, onTap: onOpenAuthor)
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 4) {
                    Text(comment.author.displayLabel).talkTextStyle(.bodyBold).foregroundStyle(TalkColor.textPrimary)
                    Text("\(comment.author.handle) · \(comment.createdAt.relativeText())").talkTextStyle(.body)
                        .foregroundStyle(TalkColor.textSecondary).lineLimit(1)
                }
                switch comment.publishState {
                case .pending: Text(strings.post_pending.text).talkTextStyle(.small).foregroundStyle(TalkColor.textSecondary)
                case .failed: Text(strings.post_failed.text).talkTextStyle(.small).foregroundStyle(TalkColor.error)
                case .published: EmptyView()
                }
                MarkdownView(markdown: comment.content, style: .body)
            }
        }
        .padding(.horizontal, TalkSpacing.lg)
        .padding(.vertical, TalkSpacing.md)
        .overlay(alignment: .bottom) { Hairline() }
    }
}

/// The player (`VideoPlayer`): the thumbnail with Play, then YouTube's IFrame player in its privacy-enhanced mode
/// (`youtube-nocookie.com`, decision 15) in a `WKWebView`, inline (`playsinline`), crossfading from the thumbnail. Its
/// fullscreen button opens the system's fullscreen player.
struct VideoPlayer: View {
    let id: String
    let isPlaying: Bool
    let onPlay: () -> Void

    var body: some View {
        ZStack {
            Color.black
            if isPlaying {
                YouTubeEmbed(id: id).transition(.opacity)
            } else {
                Button(action: onPlay) {
                    Color.clear
                        .overlay { if let url = URL(string: "https://i.ytimg.com/vi/\(id)/hqdefault.jpg") { RemoteImage(url: url) } }
                        .overlay {
                            TalkIcon(icon: TalkIcons.play)
                                .foregroundStyle(.white)
                                .frame(width: 64, height: 64)
                                .background(TalkColor.mediaScrim, in: Circle())
                        }
                        .clipped()
                }
                .buttonStyle(.plain)
                .accessibilityLabel(MR.strings.shared.video_play_video.text)
            }
        }
        .aspectRatio(16 / 9, contentMode: .fit)
        .frame(maxWidth: .infinity)
        .animation(TalkMotion.fade, value: isPlaying)
    }
}

private struct YouTubeEmbed: UIViewRepresentable {
    let id: String

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.allowsInlineMediaPlayback = true
        configuration.mediaTypesRequiringUserActionForPlayback = []
        configuration.preferences.isElementFullscreenEnabled = true
        // Nothing persists: no cookies or storage from the player outlive the page.
        configuration.websiteDataStore = .nonPersistent()
        let view = WKWebView(frame: .zero, configuration: configuration)
        view.isOpaque = false
        view.backgroundColor = .black
        view.scrollView.isScrollEnabled = false
        view.navigationDelegate = context.coordinator
        view.uiDelegate = context.coordinator
        load(view)
        return view
    }

    func updateUIView(_ view: WKWebView, context: Context) {
        if context.coordinator.loaded != id { load(view) }
        context.coordinator.loaded = id
    }

    func makeCoordinator() -> Coordinator { Coordinator(loaded: id) }

    /// The embed stays the embed: links out of it (the YouTube logo, "Watch on YouTube", end screens) open in the
    /// YouTube app or Safari instead of replacing the player.
    final class Coordinator: NSObject, WKNavigationDelegate, WKUIDelegate {
        var loaded: String
        init(loaded: String) { self.loaded = loaded }

        func webView(_ webView: WKWebView, decidePolicyFor action: WKNavigationAction,
                     decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
            let url = action.request.url
            let isPlayer = url?.host == Self.embedHost || url?.host == Self.pageHost || url?.scheme == "about"
            // Sub-frame loads (the player's own requests) and the player itself stay; a top-level link leaves.
            if isPlayer || action.targetFrame?.isMainFrame == false {
                decisionHandler(.allow)
            } else {
                if let url { UIApplication.shared.open(url) }
                decisionHandler(.cancel)
            }
        }

        /// `target=_blank` links: no new web view, the system opens them.
        func webView(_ webView: WKWebView, createWebViewWith configuration: WKWebViewConfiguration,
                     for action: WKNavigationAction, windowFeatures: WKWindowFeatures) -> WKWebView? {
            if let url = action.request.url { UIApplication.shared.open(url) }
            return nil
        }

        static let embedHost = "www.youtube-nocookie.com"
        static let pageHost = "talk.invalid"
    }

    static func dismantleUIView(_ view: WKWebView, coordinator: Coordinator) {
        view.loadHTMLString("", baseURL: nil)
    }

    private func load(_ view: WKWebView) {
        let video = id.addingPercentEncoding(withAllowedCharacters: .alphanumerics) ?? id
        let html = """
        <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
        <style>html,body{margin:0;height:100%;background:#000}iframe{position:absolute;inset:0;width:100%;height:100%;border:0}</style>
        </head><body>
        <iframe src="https://www.youtube-nocookie.com/embed/\(video)?playsinline=1&autoplay=1&rel=0&iv_load_policy=3&fs=1"
          allow="autoplay; encrypted-media; fullscreen; picture-in-picture" allowfullscreen
          referrerpolicy="strict-origin-when-cross-origin"></iframe>
        </body></html>
        """
        view.loadHTMLString(html, baseURL: URL(string: "https://talk.invalid"))
    }
}
