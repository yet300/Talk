import SwiftUI
import SharedLogic

/// What a card's actions do; each screen routes these to its component.
struct PostActionHandlers {
    let onReply: (Post) -> Void
    let onRepost: (Post) -> Void
    let onLike: (Post) -> Void
    let onBookmark: (Post) -> Void
    /// "Edited" next to the time.
    let onOpenRevisions: (Post) -> Void
}

/// The card's action row (`x-actions`): Reply, Repost (green when reposted), Like (pink, filled), Bookmark (blue, filled).
struct PostActions: View {
    let post: Post
    let handlers: PostActionHandlers

    var body: some View {
        let strings = MR.strings.shared
        HStack {
            PostAction(icon: TalkIcons.reply, count: post.stats.replies, label: strings.a11y_replies.format(Int32(clamping: post.stats.replies)), onColor: nil) {
                handlers.onReply(post)
            }
            Spacer(minLength: 0)
            PostAction(icon: TalkIcons.repost, count: post.stats.reposts, label: strings.a11y_reposts.format(Int32(clamping: post.stats.reposts)),
                       onColor: post.viewer.reposted ? TalkColor.repost : nil) {
                handlers.onRepost(post)
            }
            Spacer(minLength: 0)
            PostAction(icon: post.viewer.liked ? TalkIcons.likeFilled : TalkIcons.like, count: post.stats.likes,
                       label: strings.a11y_likes.format(Int32(clamping: post.stats.likes)), onColor: post.viewer.liked ? TalkColor.like : nil) {
                handlers.onLike(post)
            }
            Spacer(minLength: 0)
            PostAction(icon: post.viewer.bookmarked ? TalkIcons.bookmarkFilled : TalkIcons.bookmark, count: nil,
                       label: post.viewer.bookmarked ? strings.action_remove_bookmark.text : strings.action_bookmark.text,
                       onColor: post.viewer.bookmarked ? TalkColor.accent : nil) {
                handlers.onBookmark(post)
            }
        }
        .padding(.top, 4)
    }
}

/// One action (`x-action`): an 18 pt icon with its count in a 44 pt target. Its colour eases to the on colour, and
/// turning it on with a tap pops the icon with a small spring and a light haptic.
struct PostAction: View {
    let icon: SharedLogic.ImageResource
    let count: Int64?
    let label: String
    let onColor: Color?
    let action: () -> Void

    @State private var popped = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        let on = onColor != nil
        Button {
            if !on && !reduceMotion {
                popped = true
                withAnimation(TalkMotion.pop) { popped = false }
            }
            action()
        } label: {
            HStack(spacing: 4) {
                TalkIcon(icon: icon, size: TalkSize.iconAction)
                    .scaleEffect(popped ? 0.6 : 1)
                if let count, count > 0 {
                    Text("\(count)")
                        .talkTextStyle(.small)
                        .contentTransition(.numericText(value: Double(count)))
                }
            }
            .foregroundStyle(onColor ?? TalkColor.textSecondary)
            .padding(.horizontal, 8)
            .frame(minWidth: TalkSize.touchMin, minHeight: TalkSize.touchMin)
            .contentShape(Rectangle())
        }
        .buttonStyle(PressStyle())
        .animation(TalkMotion.settle, value: on)
        .animation(TalkMotion.settle, value: count)
        .sensoryFeedback(.impact(weight: .light), trigger: on) { _, new in new }
        .accessibilityLabel(label)
        .accessibilityAddTraits(on ? .isSelected : [])
    }
}
