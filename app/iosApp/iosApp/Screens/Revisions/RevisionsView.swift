import SwiftUI
import SharedLogic

/// Revisions (`Revisions-Mobile`): the versions on top ("Published", then the earlier ones with their date, title and
/// `+added −removed`), the selected one below, read-only, with Published version to go back to the latest. Picking a
/// version crossfades the text. Only the latest: "No earlier versions found".
struct RevisionsView: View {
    private let component: RevisionsComponent
    @StateValue private var model: RevisionsComponentModel

    init(_ component: RevisionsComponent) {
        self.component = component
        _model = StateValue(component.model)
    }

    var body: some View {
        let strings = MR.strings.shared
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                ErrorRow(error: model.error, onDismiss: component.onErrorShown)
                list
                Hairline()
                snapshot
                    .id(model.selected?.number ?? -1)
                    .transition(.opacity)
            }
            .animation(TalkMotion.fade, value: model.selected?.number ?? -1)
        }
        .refreshable { component.onRefresh() }
        .background(TalkColor.bg.ignoresSafeArea())
        .safeAreaInset(edge: .top, spacing: 0) { NetworkBar() }
        .talkTitle(strings.revisions_title.text)
    }

    @ViewBuilder
    private var list: some View {
        let strings = MR.strings.shared
        let revisions = model.revisions
        if let latest = revisions.first {
            Caption(strings.revisions_published.text)
            ListRow(title: strings.revisions_published_version.text,
                    caption: strings.revisions_latest_hint.format(latest.createdAt.dateText()),
                    selected: model.selected == latest,
                    action: { component.onSelect(number: latest.number) }) {
                EmptyView()
            } trailing: {
                EmptyView()
            }
            if revisions.count == 1 {
                EmptyState(strings.revisions_empty_title.text, body: strings.revisions_empty_body.text, icon: TalkIcons.history)
            } else {
                Caption(strings.revisions_earlier.text)
                ForEach(Array(revisions.dropFirst().enumerated()), id: \.element.number) { index, revision in
                    let older = index + 2 < revisions.count ? revisions[index + 2] : nil
                    ListRow(title: revision.createdAt.dateText(),
                            caption: PostSummaries.shared.of(markdown: revision.content).title,
                            selected: model.selected == revision,
                            action: { component.onSelect(number: revision.number) }) {
                        EmptyView()
                    } trailing: {
                        if let older {
                            let changes = RevisionDiff.shared.lineChanges(old: older.content, new: revision.content)
                            Text("+\(changes.added) −\(changes.removed)").talkTextStyle(.small).foregroundStyle(TalkColor.textSecondary)
                        }
                    }
                }
                Caption(strings.revisions_limited.text)
            }
        }
    }

    @ViewBuilder
    private var snapshot: some View {
        let strings = MR.strings.shared
        if let revision = model.selected {
            VStack(alignment: .leading, spacing: 0) {
                if !model.isLatest {
                    HStack(spacing: TalkSpacing.sm) {
                        Text(strings.revisions_snapshot.format(revision.createdAt.dateText()))
                            .talkTextStyle(.bodyBold)
                            .foregroundStyle(TalkColor.textPrimary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        TalkButton(strings.revisions_published_version.text, style: .secondary) {
                            if let latest = model.revisions.first { component.onSelect(number: latest.number) }
                        }
                    }
                    .padding(TalkSpacing.md)
                    .background(TalkColor.surface, in: RoundedRectangle(cornerRadius: TalkRadius.card))
                    .padding(TalkSpacing.lg)
                }
                MarkdownView(markdown: revision.content).padding(TalkSpacing.lg)
            }
        } else if model.isLoading {
            SkeletonCards(count: 1)
        }
    }
}
