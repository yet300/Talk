import SwiftUI
import SharedLogic

/// Bookmarks (`Bookmarks-Mobile`): saved posts, removed with the card's bookmark; Back returns Home (decision 17). On a
/// phone it is your profile's third tab, so this page shows as a tab on the Duo and wide windows, or from a link.
struct BookmarksView: View {
    private let component: BookmarksComponent
    private let canGoBack: Bool
    @StateValue private var model: BookmarksComponentModel

    init(_ component: BookmarksComponent, canGoBack: Bool) {
        self.component = component
        self.canGoBack = canGoBack
        _model = StateValue(component.model)
    }

    var body: some View {
        let strings = MR.strings.shared
        PagedPostList(
            component.pagingController(),
            onOpen: { component.onOpenPost(id: $0.id) },
            onOpenAuthor: { component.onOpenAuthor(id: $0.author.id) },
            actions: PostActionHandlers(onReply: component.onReply, onRepost: component.onRepost, onLike: component.onLike,
                                        onBookmark: component.onBookmark, onOpenRevisions: component.onOpenRevisions)
        ) {
            ErrorRow(error: model.error, onDismiss: component.onErrorShown)
        } empty: {
            EmptyState(strings.bookmarks_empty_title.text, body: strings.bookmarks_empty_body.text, icon: TalkIcons.bookmark) {
                TalkButton(strings.action_back_home.text, action: component.onBrowseHome)
            }
        }
        .safeAreaInset(edge: .top, spacing: 0) { NetworkBar() }
        .talkTitle(strings.bookmarks_title.text)
        .navigationBarBackButtonHidden(!canGoBack)
    }
}
