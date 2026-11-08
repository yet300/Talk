import SwiftUI
import SharedLogic

/// The design's post card (`PostCard`): the repost line in Following, avatar, name, `handle · time`, "Replying to";
/// an article card (title, excerpt, `N min read #tags`) for a post starting with `# heading`, else a text card
/// (6 lines, Show more); images, the link, the quoted post; then the actions, or Sending… / Not sent with Retry.
/// Reported or blocked posts sit behind a row with Show.
struct PostCard: View {
    let post: Post
    let onOpen: (Post) -> Void
    let onOpenAuthor: (Post) -> Void
    let actions: PostActionHandlers

    @Environment(\.moderation) private var moderation
    @Environment(\.selectedPost) private var selectedPost
    @State private var revealed = false

    var body: some View {
        if let hidden = moderation.hiddenReason(post: post.id, author: post.author.id), !revealed {
            HiddenRow(reason: hidden) { revealed = true }
        } else {
            card
        }
    }

    private var card: some View {
        let summary = post.summary
        let strings = MR.strings.shared
        let selected = selectedPost == post.key
        return VStack(alignment: .leading, spacing: 0) {
            if let reposter = post.repostedBy {
                HStack(spacing: 6) {
                    TalkIcon(icon: TalkIcons.repost, size: 16)
                    Text(strings.post_reposted.format(reposter.displayLabel)).talkTextStyle(.small)
                }
                .foregroundStyle(TalkColor.textSecondary)
                .padding(.leading, 24)
                .padding(.bottom, 4)
            }
            HStack(alignment: .top, spacing: TalkSpacing.md) {
                Avatar(user: post.author) { onOpenAuthor(post) }
                VStack(alignment: .leading, spacing: 6) {
                    PostHead(post: post) { actions.onOpenRevisions(post) }
                    if let replyTo = post.replyTo {
                        Text(strings.post_replying_to.format(replyTo.handle))
                            .talkTextStyle(.body)
                            .foregroundStyle(TalkColor.textSecondary)
                    }
                    if summary.isArticle {
                        Text(summary.title)
                            .talkTextStyle(.headingMedium)
                            .foregroundStyle(TalkColor.textPrimary)
                        if !summary.excerpt.isEmpty {
                            Text(summary.excerpt)
                                .talkTextStyle(.body)
                                .foregroundStyle(TalkColor.textSecondary)
                                .lineLimit(3)
                        }
                        Text(([strings.post_read_time.format(summary.readMinutes)] + summary.tags.map { "#\($0)" }).joined(separator: "  "))
                            .talkTextStyle(.small)
                            .foregroundStyle(TalkColor.textSecondary)
                    } else if !summary.text.isEmpty {
                        TextBody(text: summary.text)
                    }
                    if !post.images.isEmpty { MediaGrid(images: post.images) }
                    if let link = summary.link { LinkCard(link: link) }
                    if let quoted = post.quoteOf { QuotedCard(id: quoted, onOpen: onOpen) }
                    switch post.publishState {
                    case .pending:
                        Text(strings.post_pending.text).talkTextStyle(.small).foregroundStyle(TalkColor.textSecondary)
                    case .failed:
                        FailedRow(post: post)
                    case .published:
                        PostActions(post: post, handlers: actions)
                    }
                }
            }
        }
        .padding(.horizontal, TalkSpacing.lg)
        .padding(.vertical, TalkSpacing.md)
        .background(selected ? TalkColor.surface : .clear)
        .animation(TalkMotion.settle, value: selected)
        .contentShape(Rectangle())
        .onTapGesture { onOpen(post) }
        .accessibilityElement(children: .contain)
        .accessibilityAction(named: Text(strings.post_title.text)) { onOpen(post) }
        .accessibilityAddTraits(selected ? .isSelected : [])
        .overlay(alignment: .bottom) { Hairline() }
    }
}

/// A text card's body: up to six lines, then Show more expands it in place.
private struct TextBody: View {
    let text: String
    @State private var expanded = false

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(text)
                .talkTextStyle(.body)
                .foregroundStyle(TalkColor.textPrimary)
                .lineLimit(expanded ? nil : 6)
            if !expanded && (text.count > 420 || text.filter { $0 == "\n" }.count >= 6) {
                Button(MR.strings.shared.post_show_more.text) {
                    withAnimation(TalkMotion.settle) { expanded = true }
                }
                .talkTextStyle(.body)
                .foregroundStyle(TalkColor.accent)
                .buttonStyle(.plain)
            }
        }
    }
}

/// Name, `handle · time`, and Edited (opens the revisions) when the post was edited.
struct PostHead: View {
    let post: Post
    var onEdited: (() -> Void)? = nil

    var body: some View {
        HStack(spacing: 4) {
            Text(post.author.displayLabel)
                .talkTextStyle(.bodyBold)
                .foregroundStyle(TalkColor.textPrimary)
                .lineLimit(1)
            Text("\(post.author.handle) · \(post.createdAt.relativeText())")
                .talkTextStyle(.body)
                .foregroundStyle(TalkColor.textSecondary)
                .lineLimit(1)
            if post.editedAt != nil, let onEdited {
                Button(MR.strings.shared.post_edited.text, action: onEdited)
                    .talkTextStyle(.body)
                    .foregroundStyle(TalkColor.textSecondary)
                    .buttonStyle(.plain)
            }
        }
    }
}

/// Not sent, with Retry: publishes the stored event again (to the relays still owed it).
private struct FailedRow: View {
    let post: Post
    @Environment(\.postCards) private var cards

    var body: some View {
        HStack(spacing: TalkSpacing.md) {
            Text(MR.strings.shared.post_failed.text).talkTextStyle(.bodyBold).foregroundStyle(TalkColor.error)
            TalkButton(MR.strings.shared.action_retry.text, style: .secondary) { cards.retry(post: post) }
        }
    }
}

/// A post the viewer's network reported, or whose author they blocked: the row, with Show to reveal it.
struct HiddenRow: View {
    let reason: HiddenReason
    let onShow: () -> Void

    var body: some View {
        let strings = MR.strings.shared
        HStack(spacing: TalkSpacing.sm) {
            TalkIcon(icon: TalkIcons.eyeOff, size: 18).foregroundStyle(TalkColor.textSecondary)
            Text(reason == .blocked ? strings.moderation_hidden_blocked.text : strings.moderation_hidden.text)
                .talkTextStyle(.body)
                .foregroundStyle(TalkColor.textSecondary)
                .frame(maxWidth: .infinity, alignment: .leading)
            Button(strings.action_show.text, action: onShow)
                .talkTextStyle(.bodyBold)
                .foregroundStyle(TalkColor.accent)
                .buttonStyle(.plain)
        }
        .padding(.horizontal, TalkSpacing.lg)
        .padding(.vertical, TalkSpacing.md)
        .background(TalkColor.surface)
        .overlay(alignment: .bottom) { Hairline() }
    }
}
