import SwiftUI
import SharedLogic

/// A paged list of post cards, edge to edge under the glass bars: the first load's skeletons, `empty`, an error row
/// with Retry, the next page when the end appears, and pull to refresh. Keeps its own scroll position.
struct PagedPostList<Header: View, Empty: View>: View {
    @StateObject private var model: PagingModel<Post>
    let onOpen: (Post) -> Void
    let onOpenAuthor: (Post) -> Void
    let actions: PostActionHandlers
    var topInset: CGFloat = 0
    var tracking: ScrollTracking? = nil
    @ViewBuilder let header: () -> Header
    @ViewBuilder let empty: () -> Empty

    init(_ controller: @autoclosure @escaping () -> PagingController<Post>, onOpen: @escaping (Post) -> Void,
         onOpenAuthor: @escaping (Post) -> Void, actions: PostActionHandlers, topInset: CGFloat = 0, tracking: ScrollTracking? = nil,
         @ViewBuilder header: @escaping () -> Header, @ViewBuilder empty: @escaping () -> Empty) {
        _model = StateObject(wrappedValue: PagingModel(controller()))
        self.onOpen = onOpen
        self.onOpenAuthor = onOpenAuthor
        self.actions = actions
        self.topInset = topInset
        self.tracking = tracking
        self.header = header
        self.empty = empty
    }

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 0) {
                Color.clear.frame(height: topInset)
                header()
                PagedRows(model: model, id: { $0.key }, empty: empty) { post in
                    PostCard(post: post, onOpen: onOpen, onOpenAuthor: onOpenAuthor, actions: actions)
                }
            }
            .padding(.bottom, TalkSpacing.xl)
        }
        .refreshable { await model.refreshed() }
        .modifier(ScrollTrackingModifier(tracking: tracking))
        .background(TalkColor.bg)
    }
}

/// A list's scroll offset reported to its screen, and its position for the screen to move (Profile's header).
struct ScrollTracking {
    let position: Binding<ScrollPosition>
    let onScroll: (CGFloat) -> Void
}

private struct ScrollTrackingModifier: ViewModifier {
    let tracking: ScrollTracking?

    func body(content: Content) -> some View {
        if let tracking {
            content
                .scrollPosition(tracking.position)
                .onScrollGeometryChange(for: CGFloat.self) { $0.contentOffset.y + $0.contentInsets.top } action: { _, offset in
                    tracking.onScroll(offset)
                }
        } else {
            content
        }
    }
}

extension PagingModel {
    /// Refreshes and waits (up to ten seconds) until the first page is back, for pull to refresh.
    @MainActor
    func refreshed() async {
        refresh()
        try? await Task.sleep(for: .milliseconds(300))
        var waited = 0
        while snapshot.refresh == .loading && waited < 100 {
            try? await Task.sleep(for: .milliseconds(100))
            waited += 1
        }
    }
}
