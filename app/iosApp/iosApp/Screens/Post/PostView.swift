import SwiftUI
import SharedLogic

/// The reader (`Reader · Mobile`): the article edge to edge under the glass bar with More (a sheet, decision 3): Show
/// the parent for a reply, the title, the author with Follow, the actions, Share and Revisions, the quoted post, the
/// markdown, tags and the post's address; then the replies with a reply field. In two panes (Duo inner) the replies
/// move to the left pane and Reply shows them there, without moving or reloading the article.
struct PostView: View {
    private let component: PostComponent
    @StateValue private var model: PostComponentModel
    @Environment(\.readerMode) private var reader
    @State private var showsMore = false
    @State private var reporting = false
    @State private var confirmsDelete = false
    @FocusState private var replyFocused: Bool

    init(_ component: PostComponent) {
        self.component = component
        _model = StateValue(component.model)
    }

    var body: some View {
        let strings = MR.strings.shared
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                ErrorRow(error: model.error, onDismiss: component.onErrorShown)
                if let post = model.post {
                    ArticleView(post: post, model: model, component: component) {
                        if reader.twoPane { reader.onShowReplies() } else { replyFocused = true }
                    }
                    if !reader.twoPane {
                        RepliesHeader(model: model, component: component, focused: $replyFocused)
                        ForEach(model.replies, id: \.post.key) { reply in
                            ReplyCard(reply: reply, component: component)
                        }
                    }
                } else if model.isLoading {
                    SkeletonCards(count: 1)
                } else {
                    EmptyState(strings.post_unavailable.text)
                }
            }
            .padding(.bottom, TalkSpacing.xl)
        }
        .scrollDismissesKeyboard(.interactively)
        .refreshable { component.onRefresh() }
        .background(TalkColor.bg.ignoresSafeArea())
        .safeAreaInset(edge: .top, spacing: 0) { NetworkBar() }
        .talkAnimation(TalkMotion.fade, value: model.post == nil)
        .talkTitle(strings.post_title.text)
        .toolbar {
            if model.post != nil {
                ToolbarItem(placement: .primaryAction) {
                    ToolbarIconButton(icon: TalkIcons.more, label: strings.action_more.text) { showsMore = true }
                }
            }
        }
        .sheet(isPresented: $showsMore) {
            if let post = model.post {
                MoreActionsSheet(post: post, model: model, component: component,
                                 onDelete: { showsMore = false; confirmsDelete = true },
                                 onReport: { showsMore = false; reporting = true },
                                 onDone: { showsMore = false })
            }
        }
        .sheet(isPresented: $reporting) {
            if let post = model.post {
                ReportSheet(target: ReportTargetPost(id: post.id, author: post.author.id)) { reporting = false }
            }
        }
        .alert(strings.post_delete_title.text, isPresented: $confirmsDelete) {
            Button(strings.action_delete.text, role: .destructive, action: component.onDelete)
            Button(strings.action_cancel.text, role: .cancel) {}
        } message: {
            Text(strings.post_delete_body.text)
        }
    }
}

/// The article (`Article`).
private struct ArticleView: View {
    let post: Post
    let model: PostComponentModel
    let component: PostComponent
    let onReply: () -> Void
    @Environment(\.toast) private var toast

    var body: some View {
        let strings = MR.strings.shared
        let parts = Self.splitTitle(post.content)
        VStack(alignment: .leading, spacing: TalkSpacing.md) {
            if let thread = post.thread {
                Button(strings.post_show_parent.text) { component.onOpenPost(id: thread.parentId) }
                    .talkTextStyle(.body)
                    .foregroundStyle(TalkColor.accent)
                    .buttonStyle(.plain)
            }
            if let title = parts.title {
                Text(title)
                    .talkTextStyle(.display)
                    .foregroundStyle(TalkColor.textPrimary)
                    .accessibilityAddTraits(.isHeader)
            }
            HStack(spacing: TalkSpacing.md) {
                Avatar(user: post.author) { component.onOpenAuthor(id: post.author.id) }
                VStack(alignment: .leading, spacing: 2) {
                    Text(post.author.displayLabel).talkTextStyle(.bodyBold).foregroundStyle(TalkColor.textPrimary)
                    Text([post.author.handle, post.createdAt.dateText(), strings.post_read_time.format(post.summary.readMinutes)].joined(separator: " · "))
                        .talkTextStyle(.small)
                        .foregroundStyle(TalkColor.textSecondary)
                    if post.editedAt != nil {
                        Button(strings.post_edited.text, action: component.onOpenRevisions)
                            .talkTextStyle(.small)
                            .foregroundStyle(TalkColor.accent)
                            .buttonStyle(.plain)
                    }
                }
                Spacer(minLength: 0)
                if !model.isMine {
                    FollowButton(following: model.isFollowingAuthor, action: component.onFollowAuthor)
                }
            }
            PostActions(post: post, handlers: PostActionHandlers(
                onReply: { _ in onReply() }, onRepost: component.onRepost, onLike: component.onLike,
                onBookmark: component.onBookmark, onOpenRevisions: { _ in component.onOpenRevisions() }))
            HStack(spacing: TalkSpacing.sm) {
                ShareLink(item: model.shareLink) {
                    Text(strings.action_share.text)
                        .talkTextStyle(.bodyBold)
                        .foregroundStyle(TalkColor.textPrimary)
                        .padding(.horizontal, TalkSpacing.lg)
                        .frame(minHeight: TalkSize.buttonHeight)
                        .overlay(Capsule().strokeBorder(TalkColor.controlBorder, lineWidth: 1))
                }
                TalkButton(strings.action_revisions.text, style: .secondary, action: component.onOpenRevisions)
            }
            if let quoted = post.quoteOf {
                QuotedCard(id: quoted) { component.onOpenPost(id: $0.id) }
            }
            MarkdownView(markdown: parts.body)
            if !post.summary.tags.isEmpty {
                Text(post.summary.tags.map { "#\($0)" }.joined(separator: "  "))
                    .talkTextStyle(.body)
                    .foregroundStyle(TalkColor.accent)
            }
            Button {
                copyText(model.shareLink, toast: toast)
            } label: {
                HStack(spacing: 6) {
                    TalkIcon(icon: TalkIcons.copy, size: 18)
                    Text(model.shareLink.shortNpub()).talkTextStyle(.small)
                }
                .foregroundStyle(TalkColor.textSecondary)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(strings.post_copy_address.text)
        }
        .padding(.horizontal, TalkSpacing.lg)
        .padding(.vertical, TalkSpacing.md)
        .overlay(alignment: .bottom) { Hairline() }
    }

    /// A leading `# Title` line, and the markdown after it; no title when the post does not start with one.
    static func splitTitle(_ content: String) -> (title: String?, body: String) {
        let trimmed = content.drop { $0.isWhitespace }
        let first = trimmed.prefix { $0 != "\n" }
        guard first.hasPrefix("# ") else { return (nil, content) }
        let title = first.dropFirst(2).trimmingCharacters(in: .whitespaces)
        let body = trimmed.dropFirst(first.count).drop { $0 == "\n" }
        return (title, String(body))
    }
}

/// "N replies" and the reply field (`t-reply-box`); Reply stays off until there is text.
private struct RepliesHeader: View {
    let model: PostComponentModel
    let component: PostComponent
    var focused: FocusState<Bool>.Binding

    var body: some View {
        let strings = MR.strings.shared
        VStack(alignment: .leading, spacing: TalkSpacing.md) {
            Text(MR.plurals.shared.replies_count.format(count: model.replies.count))
                .talkTextStyle(.headingMedium)
                .foregroundStyle(TalkColor.textPrimary)
                .accessibilityAddTraits(.isHeader)
            HStack(spacing: TalkSpacing.sm) {
                TalkTextField(placeholder: strings.post_write_reply.text, text: model.replyText, onChange: component.onReplyTextChanged,
                              pill: true, multiline: true, submitLabel: .send) {
                    if model.canSendReply { component.onSendReply() }
                }
                .focused(focused)
                TalkButton(strings.action_reply.text, style: .post, enabled: model.canSendReply, busy: model.isReplying,
                           action: component.onSendReply)
            }
        }
        .padding(TalkSpacing.lg)
        .overlay(alignment: .bottom) { Hairline() }
    }
}

/// A reply card: avatar, head, the markdown, Reply (opens the editor) and Like; deeper replies step in.
private struct ReplyCard: View {
    let reply: ThreadReply
    let component: PostComponent
    @Environment(\.moderation) private var moderation
    @State private var revealed = false

    var body: some View {
        let post = reply.post
        if moderation.hiddenReason(post: post.id, author: post.author.id) != nil && !revealed {
            Button { revealed = true } label: {
                Text(MR.strings.shared.moderation_hidden.text)
                    .talkTextStyle(.body)
                    .foregroundStyle(TalkColor.textSecondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(TalkSpacing.lg)
            }
            .buttonStyle(.plain)
        } else {
            HStack(alignment: .top, spacing: TalkSpacing.md) {
                Avatar(user: post.author) { component.onOpenAuthor(id: post.author.id) }
                VStack(alignment: .leading, spacing: 4) {
                    PostHead(post: post)
                    MarkdownView(markdown: post.content, style: .body)
                    PostActions(post: post, handlers: PostActionHandlers(
                        onReply: component.onReplyInEditor, onRepost: component.onRepost, onLike: component.onLike,
                        onBookmark: component.onBookmark, onOpenRevisions: { _ in }))
                }
            }
            .padding(.leading, TalkSpacing.lg + CGFloat(min(Int(reply.depth), 4) * 16))
            .padding(.trailing, TalkSpacing.lg)
            .padding(.top, TalkSpacing.md)
            .padding(.bottom, TalkSpacing.sm)
            .contentShape(Rectangle())
            .onTapGesture { component.onOpenPost(id: post.id) }
            .overlay(alignment: .bottom) { Hairline() }
        }
    }
}

/// The replies alone, for the left pane in two panes (Duo inner: replies | article).
struct RepliesPane: View {
    let component: PostComponent
    @StateValue private var model: PostComponentModel
    @FocusState private var focused: Bool

    init(component: PostComponent) {
        self.component = component
        _model = StateValue(component.model)
    }

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                RepliesHeader(model: model, component: component, focused: $focused)
                ForEach(model.replies, id: \.post.key) { reply in
                    ReplyCard(reply: reply, component: component)
                }
            }
        }
        .scrollDismissesKeyboard(.interactively)
        .background(TalkColor.bg.ignoresSafeArea())
        .talkTitle(MR.strings.shared.profile_replies.text)
    }
}

/// More actions (`MoreFlow`, decision 3, a sheet on the phone and the Duo): Quote, Copy link, Share; Edit and Delete on
/// your own post, Report on someone else's.
private struct MoreActionsSheet: View {
    let post: Post
    let model: PostComponentModel
    let component: PostComponent
    let onDelete: () -> Void
    let onReport: () -> Void
    let onDone: () -> Void
    @Environment(\.toast) private var toast

    var body: some View {
        let strings = MR.strings.shared
        VStack(spacing: 0) {
            ActionRow(label: strings.action_quote.text, icon: TalkIcons.repost) {
                onDone()
                component.onQuote(post: post)
            }
            ActionRow(label: strings.post_copy_link.text, icon: TalkIcons.link) {
                onDone()
                copyText(model.shareLink, toast: toast)
            }
            ShareLink(item: model.shareLink) {
                HStack(spacing: TalkSpacing.lg) {
                    TalkIcon(icon: TalkIcons.share)
                    Text(strings.action_share.text).talkTextStyle(.bodyBold)
                    Spacer(minLength: 0)
                }
                .foregroundStyle(TalkColor.textPrimary)
                .padding(.horizontal, TalkSpacing.xl)
                .padding(.vertical, TalkSpacing.md)
                .frame(minHeight: TalkSize.touchMin + 8)
                .contentShape(Rectangle())
            }
            .buttonStyle(RowPressStyle())
            if model.isMine {
                ActionRow(label: strings.action_edit.text, icon: TalkIcons.compose) {
                    onDone()
                    component.onEdit()
                }
                ActionRow(label: strings.action_delete.text, icon: TalkIcons.trash, danger: true, action: onDelete)
            } else {
                ActionRow(label: strings.action_report.text, icon: TalkIcons.report, danger: true, action: onReport)
            }
            Spacer(minLength: 0)
        }
        .padding(.top, TalkSpacing.xl)
        .presentationDetents([.height(model.isMine ? 340 : 290), .medium])
        .presentationDragIndicator(.visible)
    }
}
