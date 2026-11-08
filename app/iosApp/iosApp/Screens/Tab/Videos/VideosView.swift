import SwiftUI
import SharedLogic

/// Videos (`Videos-Mobile`, `VideoHome`): the list chips (Videos · Liked · Watch later · History); on Videos, For you as
/// a row to swipe, then Browse (category chips, Level and Type menus, the catalog 20 at a time); on a list, that list.
/// "Getting the catalog ready…" while the catalog loads into the database (decision 16).
struct VideosView: View {
    private let component: VideosComponent
    @StateValue private var model: VideosComponentModel

    init(_ component: VideosComponent) {
        self.component = component
        _model = StateValue(component.model)
    }

    var body: some View {
        let strings = MR.strings.shared
        Group {
            switch onEnum(of: model.catalog) {
            case .loading:
                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {
                        ListChips(selected: model.list, onSelect: component.onListSelected)
                        Caption(strings.videos_loading.text)
                        SkeletonCards(count: 3)
                    }
                }
            case .failed(let failed):
                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {
                        ListChips(selected: model.list, onSelect: component.onListSelected)
                        ErrorRow(error: failed.error, onDismiss: nil, onRetry: component.onRetry)
                    }
                }
            case .ready:
                if model.list == nil {
                    BrowseList(model: model, component: component)
                } else {
                    ListVideos(model: model, component: component)
                }
            }
        }
        .transition(.opacity)
        .background(TalkColor.bg.ignoresSafeArea())
        .safeAreaInset(edge: .top, spacing: 0) { NetworkBar() }
        .talkTitle(strings.tab_videos.text)
    }
}

/// For you and Browse.
private struct BrowseList: View {
    let model: VideosComponentModel
    let component: VideosComponent
    @StateObject private var browse: PagingModel<Video>
    @Environment(\.horizontalSizeClass) private var sizeClass

    init(model: VideosComponentModel, component: VideosComponent) {
        self.model = model
        self.component = component
        _browse = StateObject(wrappedValue: PagingModel(component.browseController()))
    }

    var body: some View {
        let strings = MR.strings.shared
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                ListChips(selected: model.list, onSelect: component.onListSelected)
                if !model.forYou.isEmpty {
                    SectionTitle(strings.videos_for_you.text)
                    ScrollView(.horizontal) {
                        LazyHStack(spacing: TalkSpacing.xs) {
                            ForEach(model.forYou, id: \.id) { video in
                                VideoCard(video: video, small: true) { component.onOpenVideo(id: video.id) }
                                    .frame(width: 260)
                            }
                        }
                        .padding(.horizontal, TalkSpacing.sm)
                    }
                    .scrollIndicators(.hidden)
                }
                BrowseFilters(model: model, component: component)
                    .padding(.top, TalkSpacing.md)
                // Two columns where there is room (the Duo inner screen, iPad): an even count for the fold.
                let columns = sizeClass == .regular ? 2 : 1
                if columns == 1 {
                    PagedRows(model: browse, id: { $0.id }) {
                        EmptyState(strings.videos_no_match.text)
                    } row: { video in
                        VideoCard(video: video) { component.onOpenVideo(id: video.id) }
                            .padding(.horizontal, TalkSpacing.sm)
                    }
                } else {
                    LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: TalkSpacing.sm), count: columns), spacing: TalkSpacing.sm) {
                        PagedRows(model: browse, id: { $0.id }) {
                            EmptyState(strings.videos_no_match.text)
                        } row: { video in
                            VideoCard(video: video) { component.onOpenVideo(id: video.id) }
                        }
                    }
                    .padding(.horizontal, TalkSpacing.sm)
                }
            }
            .padding(.bottom, TalkSpacing.xl)
        }
        .refreshable { await browse.refreshed() }
    }
}

/// Liked, Watch later or History.
private struct ListVideos: View {
    let model: VideosComponentModel
    let component: VideosComponent

    var body: some View {
        let strings = MR.strings.shared
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                ListChips(selected: model.list, onSelect: component.onListSelected)
                if model.listVideos.isEmpty {
                    switch model.list {
                    case .liked: EmptyState(strings.videos_empty_liked.text, icon: TalkIcons.like)
                    case .watchLater: EmptyState(strings.videos_empty_later.text, icon: TalkIcons.clock)
                    default: EmptyState(strings.videos_empty_history.text, icon: TalkIcons.history)
                    }
                }
                ForEach(model.listVideos, id: \.id) { video in
                    VideoCard(video: video) { component.onOpenVideo(id: video.id) }
                        .padding(.horizontal, TalkSpacing.sm)
                }
            }
        }
    }
}

/// Videos · Liked · Watch later · History (`t-chips`).
private struct ListChips: View {
    let selected: VideoList?
    let onSelect: (VideoList?) -> Void

    var body: some View {
        let strings = MR.strings.shared
        let lists: [(VideoList?, String)] = [(nil, strings.videos_all_videos.text), (.liked, strings.videos_liked.text),
                                             (.watchLater, strings.videos_later.text), (.history, strings.videos_history.text)]
        ScrollView(.horizontal) {
            HStack(spacing: TalkSpacing.sm) {
                ForEach(lists, id: \.1) { list, label in
                    Chip(text: label, on: list == selected) { onSelect(list) }
                }
            }
            .padding(.horizontal, TalkSpacing.lg)
            .padding(.vertical, TalkSpacing.sm)
        }
        .scrollIndicators(.hidden)
    }
}

/// Category chips (All, then the catalog's, largest first) in one row to swipe, then the Level and Type menus.
private struct BrowseFilters: View {
    let model: VideosComponentModel
    let component: VideosComponent

    var body: some View {
        let strings = MR.strings.shared
        let selected = model.filters.category
        VStack(alignment: .leading, spacing: 0) {
            ScrollView(.horizontal) {
                LazyHStack(spacing: TalkSpacing.sm) {
                    Chip(text: strings.videos_all.text, on: selected == nil) { component.onCategorySelected(category: nil) }
                    ForEach(model.categories, id: \.self) { category in
                        Chip(text: category, on: selected == category) { component.onCategorySelected(category: category) }
                    }
                }
                .padding(.horizontal, TalkSpacing.lg)
            }
            .scrollIndicators(.hidden)
            .frame(height: TalkSize.touchMin)
            HStack(spacing: TalkSpacing.sm) {
                FilterMenu(title: { strings.videos_level.format($0) }, value: model.filters.level,
                           options: VideoCatalog.shared.LEVELS, onSelect: component.onLevelSelected)
                FilterMenu(title: { strings.videos_type.format($0) }, value: model.filters.type,
                           options: VideoCatalog.shared.TYPES, onSelect: component.onTypeSelected)
            }
            .padding(.horizontal, TalkSpacing.lg)
            .padding(.vertical, TalkSpacing.sm)
        }
    }
}

/// `Level: Any` with a menu of Any and the values.
private struct FilterMenu: View {
    let title: (String) -> String
    let value: String?
    let options: [String]
    let onSelect: (String?) -> Void

    var body: some View {
        let any = MR.strings.shared.videos_any.text
        Menu {
            Picker(title(value?.capitalizedFirst ?? any), selection: Binding(get: { value }, set: onSelect)) {
                Text(any).tag(String?.none)
                ForEach(options, id: \.self) { Text($0.capitalizedFirst).tag(String?.some($0)) }
            }
        } label: {
            ChipLabel(text: title(value?.capitalizedFirst ?? any), on: value != nil, chevron: true)
        }
    }
}

/// A chip (`t-chip`): filled when on.
struct Chip: View {
    let text: String
    let on: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) { ChipLabel(text: text, on: on, chevron: false) }
            .buttonStyle(PressStyle())
            .accessibilityAddTraits(on ? .isSelected : [])
            .sensoryFeedback(.selection, trigger: on)
    }
}

private struct ChipLabel: View {
    let text: String
    let on: Bool
    let chevron: Bool

    var body: some View {
        HStack(spacing: 4) {
            Text(text).talkTextStyle(.bodyBold)
            if chevron { TalkIcon(icon: TalkIcons.chevron, size: 14).rotationEffect(.degrees(90)) }
        }
        .foregroundStyle(on ? TalkColor.bg : TalkColor.textPrimary)
        .padding(.horizontal, TalkSpacing.md)
        .frame(minHeight: 32)
        .background(on ? TalkColor.textPrimary : .clear, in: Capsule())
        .overlay(Capsule().strokeBorder(on ? .clear : TalkColor.controlBorder, lineWidth: 1))
        .animation(TalkMotion.settle, value: on)
    }
}

/// A catalog video (`VideoCard`): thumbnail, title, `channel · category · level`, why HN recommends it.
struct VideoCard: View {
    let video: Video
    var small = false
    let onOpen: () -> Void

    var body: some View {
        Button(action: onOpen) {
            VStack(alignment: .leading, spacing: 4) {
                Color.clear
                    .aspectRatio(16 / 9, contentMode: .fit)
                    .background(TalkColor.surface)
                    .overlay { if let url = URL(string: video.thumbnail(small: small)) { RemoteImage(url: url) } }
                    .overlay {
                        TalkIcon(icon: TalkIcons.play, size: 20)
                            .foregroundStyle(.white)
                            .frame(width: 40, height: 40)
                            .background(TalkColor.mediaScrim, in: Circle())
                    }
                    .clipShape(RoundedRectangle(cornerRadius: TalkRadius.card))
                Text(video.title).talkTextStyle(.bodyBold).foregroundStyle(TalkColor.textPrimary).lineLimit(2).multilineTextAlignment(.leading)
                Text([video.channel, video.categories.first ?? video.primaryTopic, video.level?.capitalizedFirst].compactMap { $0 }.joined(separator: " · "))
                    .talkTextStyle(.small).foregroundStyle(TalkColor.textSecondary).lineLimit(1)
                if !small, let why = video.whyHn {
                    Text(why).talkTextStyle(.small).foregroundStyle(TalkColor.textSecondary).lineLimit(2).multilineTextAlignment(.leading)
                }
            }
            .padding(TalkSpacing.sm)
            .contentShape(Rectangle())
        }
        .buttonStyle(PressStyle())
        .accessibilityLabel(MR.strings.shared.video_play.format(video.title))
    }
}

extension String {
    /// "intro" → "Intro": the catalog's levels and types, as the design writes them.
    var capitalizedFirst: String { prefix(1).uppercased() + dropFirst() }
}
