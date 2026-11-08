import SwiftUI
import SharedLogic

/// Activity (`Activity-Mobile`, decision 1): who replied, quoted, reposted, liked or followed, newest first; a row opens
/// the reply or quote, the post, or the person. Read-only, paged.
struct ActivityView: View {
    private let component: ActivityComponent
    @StateObject private var model: PagingModel<ActivityItem>

    init(_ component: ActivityComponent) {
        self.component = component
        _model = StateObject(wrappedValue: PagingModel(component.pagingController()))
    }

    var body: some View {
        let strings = MR.strings.shared
        ScrollView {
            LazyVStack(spacing: 0) {
                PagedRows(model: model, id: { $0.key }) {
                    EmptyState(strings.activity_empty_title.text, body: strings.activity_empty_body.text, icon: TalkIcons.activity)
                } row: { item in
                    ActivityRow(item: item, component: component)
                }
            }
        }
        .refreshable { await model.refreshed() }
        .background(TalkColor.bg.ignoresSafeArea())
        .safeAreaInset(edge: .top, spacing: 0) { NetworkBar() }
        .talkTitle(strings.activity_title.text)
    }
}

/// "<name> liked your post" with the time and the post's title.
private struct ActivityRow: View {
    let item: ActivityItem
    let component: ActivityComponent
    @Environment(\.selectedPost) private var selectedPost

    var body: some View {
        let strings = MR.strings.shared
        let name = item.actor.displayLabel
        let title: String = switch item.type {
        case .reply: strings.activity_replied.format(name)
        case .quote: strings.activity_quoted.format(name)
        case .repost: strings.activity_reposted.format(name)
        case .like: strings.activity_liked.format(name)
        case .follow: strings.activity_followed.format(name)
        }
        let post = item.eventPost ?? item.subjectPost
        let caption = [item.createdAt.relativeText(), post?.summary.title].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: " · ")
        ListRow(title: title, caption: caption, selected: post != nil && selectedPost == post?.key, action: {
            if item.type == .follow || post == nil {
                component.onOpenProfile(id: item.actor.id)
            } else if let post {
                component.onOpenPost(id: post.id)
            }
        }) {
            Avatar(user: item.actor)
        } trailing: {
            EmptyView()
        }
    }
}
