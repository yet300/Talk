import SwiftUI
import SharedLogic

/// Chats (`Chats-Mobile`, `Chats-Inner`): the list with Chats · Requests as pages to swipe (`ChildPages`) and the
/// Location channels row; the open conversation, its info, Verify, New chat or Location channels are the `pane` slot
/// (`ChildSlot`). On a phone the router pushes the pane over the list (Back and the swipe close it); on a regular-width
/// window (the Duo inner screen, iPad) the list and the pane sit side by side.
struct ChatsView: View {
    private let component: ChatsComponent
    private let sideBySide: Bool
    @StateValue private var pane: ChildSlot<AnyObject, ChatsComponentPane>

    init(_ component: ChatsComponent, sideBySide: Bool) {
        self.component = component
        self.sideBySide = sideBySide
        _pane = StateValue(component.pane)
    }

    var body: some View {
        let open = pane.child?.instance
        if sideBySide {
            HStack(spacing: 0) {
                ChatList(component: component, open: open)
                    .frame(width: 360)
                Rectangle().fill(TalkColor.border).frame(width: TalkSize.hairline).ignoresSafeArea()
                Group {
                    if let open {
                        PaneContent(pane: open, showsRoom: true)
                            .id(ObjectIdentifier(open))
                            .transition(.opacity)
                    } else {
                        Caption(MR.strings.shared.chats_pick.text)
                            .multilineTextAlignment(.center)
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                    }
                }
                .frame(maxWidth: .infinity)
                .animation(TalkMotion.fade, value: open.map(ObjectIdentifier.init))
            }
        } else {
            ChatList(component: component, open: open)
        }
    }
}

/// The pane pushed over the Chats list on a phone; it follows the slot (the conversation, then its info…).
struct ChatPaneView: View {
    let chats: ChatsComponent

    var body: some View {
        ValueReader(chats.pane) { slot in
            if let pane = slot.child?.instance {
                PaneContent(pane: pane, showsRoom: false)
                    .id(ObjectIdentifier(pane))
            } else {
                TalkColor.bg
            }
        }
    }
}

/// One pane of Chats. Each takes its own Back to where its component goes (info back to the conversation).
struct PaneContent: View {
    let pane: ChatsComponentPane
    /// Wide windows show a channel's room in the pane; on a phone the router pushes it.
    let showsRoom: Bool

    var body: some View {
        switch onEnum(of: pane) {
        case .conversation(let it): ConversationView(it.component)
        case .newChat(let it): NewChatView(it.component)
        case .info(let it): ChatInfoView(it.component)
        case .verify(let it): VerifyView(it.component)
        case .channels(let it): ChannelsView(it.component, showsRoom: showsRoom)
        }
    }
}

/// The list: Chats · Requests (with the count), the Location channels row, the chats, empty states.
private struct ChatList: View {
    let component: ChatsComponent
    let open: ChatsComponentPane?
    @StateValue private var model: ChatsComponentModel
    @StateValue private var pages: ChildPages<AnyObject, ChatsComponentListPage>
    @Environment(\.hideInScreenshots) private var hideInScreenshots

    init(component: ChatsComponent, open: ChatsComponentPane?) {
        self.component = component
        self.open = open
        _model = StateValue(component.model)
        _pages = StateValue(component.pages)
    }

    var body: some View {
        let strings = MR.strings.shared
        PagesView(pages: pages, onSelect: { component.onTabSelected(tab: ChatsComponentTab.allCases[$0]) }) { page in
            ValueReader(page.chats) { chats in
                list(tab: page.tab, chats: chats as? [Chat] ?? [])
            }
            .id(ObjectIdentifier(page))
        }
        .background(TalkColor.bg.ignoresSafeArea())
        .safeAreaInset(edge: .top, spacing: 0) {
            VStack(spacing: 0) {
                NetworkBar()
                Picker(strings.tab_chats.text, selection: Binding(get: { model.tab }, set: { component.onTabSelected(tab: $0) })) {
                    Text(strings.chats_tab_chats.text).tag(ChatsComponentTab.chats)
                    Text(model.requestCount > 0 ? strings.chats_tab_requests_count.format(model.requestCount) : strings.chats_tab_requests.text)
                        .tag(ChatsComponentTab.requests)
                }
                .pickerStyle(.segmented)
                .frame(maxWidth: 300)
                .padding(.horizontal, TalkSpacing.lg)
                .padding(.vertical, TalkSpacing.sm)
                ErrorRow(error: model.error, onDismiss: component.onErrorShown)
            }
        }
        .talkTitle(strings.tab_chats.text)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                ToolbarIconButton(icon: TalkIcons.plus, label: strings.chats_new.text, action: component.onNewChat)
            }
        }
        .modifier(CaptureCover(enabled: hideInScreenshots))
    }

    private func list(tab: ChatsComponentTab, chats: [Chat]) -> some View {
        let strings = MR.strings.shared
        let selected = open?.chatKey
        return ScrollView {
            LazyVStack(spacing: 0) {
                if tab == .chats {
                    ListRow(title: strings.channels_title.text, caption: strings.channels_row_caption.text,
                            selected: open is ChatsComponentPane.Channels, action: component.onOpenChannels) {
                        RowIcon(icon: TalkIcons.pin)
                    } trailing: {
                        TalkIcon(icon: TalkIcons.chevron, size: 18).foregroundStyle(TalkColor.textSecondary)
                    }
                } else {
                    Caption(strings.chats_blocked_note.text)
                }
                if model.isLoading {
                    SkeletonCards(count: 3)
                } else if chats.isEmpty {
                    if tab == .chats {
                        EmptyState(strings.chats_empty_title.text, body: strings.chats_empty_body.text, icon: TalkIcons.chat) {
                            TalkButton(strings.chats_new.text, action: component.onNewChat)
                        }
                    } else {
                        EmptyState(strings.chats_requests_empty_title.text, body: strings.chats_requests_empty_body.text)
                    }
                }
                ForEach(chats, id: \.key) { chat in
                    ChatRow(chat: chat, selected: chat.key == selected) { component.onOpenChat(id: chat.id) }
                }
            }
            .animation(TalkMotion.settle, value: chats.map(\.key))
        }
    }
}

/// A chat in the list: avatar, name with verified / disappearing / muted marks, last message, time, the unread dot.
private struct ChatRow: View {
    let chat: Chat
    let selected: Bool
    let action: () -> Void

    var body: some View {
        let strings = MR.strings.shared
        let name = chatTitle(name: chat.name, members: chat.members)
        let last = chat.lastIsImage ? strings.chat_photo.text : chat.lastMessage
        let preview: String = last.map { message in
            if chat.lastState == .failed { return strings.post_failed.text }
            return chat.lastIsMine ? strings.chats_you.format(message) : message
        } ?? ""
        let unread = chat.unread > 0
        ListRow(title: name, caption: preview, captionEmphasis: unread, selected: selected, action: action) {
            ChatAvatar(members: chat.members)
        } marks: {
            if chat.isVerified { TalkIcon(icon: TalkIcons.check, size: 16).foregroundStyle(TalkColor.accent) }
            if chat.disappearing != nil { TalkIcon(icon: TalkIcons.timer, size: 14).foregroundStyle(TalkColor.textSecondary) }
            if chat.isMuted { TalkIcon(icon: TalkIcons.mute, size: 14).foregroundStyle(TalkColor.textSecondary) }
        } trailing: {
            VStack(alignment: .trailing, spacing: 6) {
                Text(chat.lastAt.relativeText()).talkTextStyle(.small).foregroundStyle(TalkColor.textSecondary)
                if unread { Circle().fill(TalkColor.accent).frame(width: 10, height: 10) }
            }
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel([unread ? strings.chats_unread.format(name) : name, preview].joined(separator: ", "))
    }
}

/// A chat's title: a group's name, else its members' names.
func chatTitle(name: String?, members: [User]) -> String {
    if let name, !name.trimmingCharacters(in: .whitespaces).isEmpty { return name }
    return members.map(\.displayLabel).joined(separator: ", ")
}

/// Covers chats while the screen is recorded or mirrored, when Hide in screenshots is on.
struct CaptureCover: ViewModifier {
    let enabled: Bool
    @State private var captured = false

    func body(content: Content) -> some View {
        content
            .overlay {
                if enabled && captured {
                    TalkColor.bg.ignoresSafeArea()
                }
            }
            .onReceive(NotificationCenter.default.publisher(for: UIScreen.capturedDidChangeNotification).prepend(Notification(name: .init("start")))) { _ in
                captured = UIApplication.shared.connectedScenes.contains { ($0 as? UIWindowScene)?.traitCollection.sceneCaptureState == .active }
            }
    }
}
