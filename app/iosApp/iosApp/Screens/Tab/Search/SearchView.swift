import SwiftUI
import SharedLogic

/// Search (`Search · Mobile`): the system search field in the bar, then People and Posts; the idle hint, Searching…
/// and "No results" in between. Local matches show on every keystroke, NIP-50 relays answer after a pause.
struct SearchView: View {
    private let component: SearchComponent
    @StateValue private var model: SearchComponentModel
    @Environment(\.selectedPost) private var selectedPost

    init(_ component: SearchComponent) {
        self.component = component
        _model = StateValue(component.model)
    }

    var body: some View {
        let strings = MR.strings.shared
        let actions = PostActionHandlers(onReply: component.onReply, onRepost: component.onRepost, onLike: component.onLike,
                                         onBookmark: component.onBookmark, onOpenRevisions: component.onOpenRevisions)
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                ErrorRow(error: model.error, onDismiss: component.onErrorShown)
                if model.isIdle {
                    Caption(strings.search_idle.text)
                } else if model.users.isEmpty && model.posts.isEmpty {
                    if model.isSearching {
                        Caption(strings.search_searching.text)
                    } else {
                        EmptyState(strings.search_empty.format(model.query.trimmingCharacters(in: .whitespaces)), icon: TalkIcons.search)
                    }
                }
                if !model.users.isEmpty {
                    SectionTitle(strings.search_people.text)
                    ForEach(model.users, id: \.key) { user in
                        PersonRow(user: user) { component.onOpenProfile(id: user.id) }
                    }
                }
                if !model.posts.isEmpty {
                    SectionTitle(strings.search_posts.text)
                    ForEach(model.posts, id: \.key) { post in
                        PostCard(post: post, onOpen: { component.onOpenPost(id: $0.id) },
                                 onOpenAuthor: { component.onOpenProfile(id: $0.author.id) }, actions: actions)
                    }
                }
            }
            .animation(TalkMotion.fade, value: model.users.count + model.posts.count)
        }
        .scrollDismissesKeyboard(.immediately)
        .background(TalkColor.bg.ignoresSafeArea())
        .safeAreaInset(edge: .top, spacing: 0) { NetworkBar() }
        .talkTitle(strings.tab_search.text)
        .modifier(DraftSearchable(text: model.query, prompt: strings.search_placeholder.text, onChange: component.onQueryChanged))
    }
}
