import SwiftUI
import SharedLogic

/// A post's images (`x-media`): one at its own shape (between 4:3 tall and 2:1 wide), several in a two-column grid.
/// A tap opens the viewer.
struct MediaGrid: View {
    let images: [SharedLogic.Image]
    @State private var viewing: ViewedImage?

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: TalkRadius.card)
        Group {
            if images.count == 1, let image = images.first {
                picture(image, index: 0)
                    .aspectRatio(ratio(image), contentMode: .fit)
            } else {
                let rows = stride(from: 0, to: min(images.count, 4), by: 2).map { Array(images[$0..<min($0 + 2, images.count)]) }
                VStack(spacing: 2) {
                    ForEach(rows.indices, id: \.self) { row in
                        HStack(spacing: 2) {
                            ForEach(rows[row].indices, id: \.self) { column in
                                picture(rows[row][column], index: row * 2 + column).aspectRatio(1, contentMode: .fit)
                            }
                        }
                    }
                }
            }
        }
        .clipShape(shape)
        .overlay(shape.strokeBorder(TalkColor.border, lineWidth: 1))
        .fullScreenCover(item: $viewing) { viewed in
            ImageViewer(urls: images.compactMap { URL(string: $0.url) }, start: viewed.index) { viewing = nil }
        }
    }

    private func picture(_ image: SharedLogic.Image, index: Int) -> some View {
        Button {
            viewing = ViewedImage(index: index)
        } label: {
            // The shape comes from a clear box; the picture fills it, so a wide image never widens the card.
            Color.clear
                .overlay {
                    if let url = URL(string: image.url) { RemoteImage(url: url) } else { TalkColor.surface }
                }
                .frame(maxWidth: .infinity)
                .clipped()
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(image.alt ?? MR.strings.shared.media_open.format(Int32(index + 1), Int32(images.count)))
    }

    private func ratio(_ image: SharedLogic.Image) -> CGFloat {
        guard let width = image.width?.doubleValue, let height = image.height?.doubleValue, height > 0 else { return 16 / 9 }
        return min(max(width / height, 0.75), 2)
    }
}

private struct ViewedImage: Identifiable {
    let index: Int
    var id: Int { index }
}

/// Images full screen, swiped between, pinch to zoom; Close and a swipe down dismiss it.
struct ImageViewer: View {
    let urls: [URL]
    let start: Int
    let onClose: () -> Void
    @State private var page: Int?

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Color.black.ignoresSafeArea()
            ScrollView(.horizontal) {
                LazyHStack(spacing: 0) {
                    ForEach(urls.indices, id: \.self) { index in
                        ZoomableImage(url: urls[index])
                            .containerRelativeFrame([.horizontal, .vertical])
                            .id(index)
                            .accessibilityLabel(MR.strings.shared.media_position.format(Int32(index + 1), Int32(urls.count)))
                    }
                }
                .scrollTargetLayout()
            }
            .scrollTargetBehavior(.paging)
            .scrollPosition(id: $page)
            .ignoresSafeArea()
            IconButton(icon: TalkIcons.close, label: MR.strings.shared.action_close.text, tint: .white, action: onClose)
                .padding(TalkSpacing.md)
        }
        .onAppear { page = start }
        .gesture(DragGesture(minimumDistance: 40).onEnded { if $0.translation.height > 120 { onClose() } })
        .accessibilityLabel(MR.strings.shared.media_viewer.text)
    }
}

private struct ZoomableImage: View {
    let url: URL
    @State private var scale: CGFloat = 1

    var body: some View {
        RemoteImage(url: url, contentMode: .fit)
            .scaleEffect(scale)
            .gesture(MagnifyGesture().onChanged { scale = max(1, $0.magnification) }.onEnded { _ in withAnimation(TalkMotion.settle) { scale = 1 } })
    }
}

/// A quoted post (`x-quote`): its author, three lines of its text or title, an image mark when it has images.
struct QuotedCard: View {
    let id: Any
    let onOpen: (Post) -> Void
    @Environment(\.postCards) private var cards
    @State private var post: Post?

    var body: some View {
        // A stack, not a Group: `.task` on an empty Group never runs, and the post only arrives through it.
        VStack(spacing: 0) {
            if let post {
                Button { onOpen(post) } label: {
                    VStack(alignment: .leading, spacing: 4) {
                        PostHead(post: post)
                        Text(post.summary.isArticle ? post.summary.title : post.summary.text)
                            .talkTextStyle(.body)
                            .foregroundStyle(TalkColor.textPrimary)
                            .lineLimit(3)
                            .multilineTextAlignment(.leading)
                        if !post.images.isEmpty {
                            TalkIcon(icon: TalkIcons.image).foregroundStyle(TalkColor.textSecondary)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(TalkSpacing.md)
                    .overlay(RoundedRectangle(cornerRadius: TalkRadius.card).strokeBorder(TalkColor.border, lineWidth: 1))
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
        }
        .task(id: String(describing: id)) {
            for await quoted in cards.quote(id: id) {
                post = quoted
            }
        }
    }
}

/// The link below a post: a YouTube thumbnail that opens the video page, else a row with the address that opens the
/// browser.
struct LinkCard: View {
    let link: PostLink
    @Environment(\.postCards) private var cards
    @Environment(\.openURL) private var openURL

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: TalkRadius.card)
        switch onEnum(of: link) {
        case .youTube(let video):
            Button { cards.openVideo(id: video.videoId) } label: {
                Color.clear
                    .aspectRatio(16 / 9, contentMode: .fit)
                    .overlay {
                        if let url = URL(string: video.thumbnail(small: false)) { RemoteImage(url: url) }
                    }
                    .overlay {
                        TalkIcon(icon: TalkIcons.play)
                            .foregroundStyle(.white)
                            .frame(width: 56, height: 56)
                            .background(TalkColor.mediaScrim, in: Circle())
                    }
                .clipShape(shape)
                .overlay(shape.strokeBorder(TalkColor.border, lineWidth: 1))
                .contentShape(shape)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(MR.strings.shared.video_play_video.text)
        case .gitHub, .web:
            Button {
                if let url = URL(string: link.url) { openURL(url) }
            } label: {
                HStack(spacing: TalkSpacing.sm) {
                    TalkIcon(icon: link is PostLinkGitHub ? TalkIcons.github : TalkIcons.link)
                        .foregroundStyle(TalkColor.textSecondary)
                    Text(link.url.displayAddress)
                        .talkTextStyle(.body)
                        .foregroundStyle(TalkColor.textSecondary)
                        .lineLimit(1)
                    Spacer(minLength: 0)
                }
                .padding(TalkSpacing.md)
                .overlay(shape.strokeBorder(TalkColor.border, lineWidth: 1))
                .contentShape(shape)
            }
            .buttonStyle(.plain)
            .accessibilityAddTraits(.isLink)
        }
    }
}

extension String {
    /// A link without its scheme and trailing slash, as rows show it.
    var displayAddress: String {
        var text = self
        for prefix in ["https://", "http://"] where text.hasPrefix(prefix) { text.removeFirst(prefix.count) }
        if text.hasSuffix("/") { text.removeLast() }
        return text
    }
}
