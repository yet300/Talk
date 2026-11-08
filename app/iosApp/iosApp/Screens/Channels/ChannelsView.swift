import SwiftUI
import SharedLogic

/// Location channels (`LocationChannels`, `Channels-Intro`): the first run (what they are, the location step with Core
/// Location's "while using" permission, a nickname) stepping forward, then the five levels with people counts,
/// Teleport and the nickname. The open room (`ChildSlot`) is pushed over it on a phone and shown in place when wide.
struct ChannelsView: View {
    private let component: ChannelsComponent
    private let showsRoom: Bool
    @StateValue private var model: ChannelsComponentModel
    @StateValue private var room: ChildSlot<AnyObject, RoomComponent>

    init(_ component: ChannelsComponent, showsRoom: Bool) {
        self.component = component
        self.showsRoom = showsRoom
        _model = StateValue(component.model)
        _room = StateValue(component.room)
    }

    var body: some View {
        if showsRoom, let open = room.child?.instance {
            RoomView(open)
                .id(ObjectIdentifier(open))
                .transition(.move(edge: .trailing))
        } else {
            steps
        }
    }

    private var steps: some View {
        let strings = MR.strings.shared
        return VStack(spacing: 0) {
            NetworkBar()
            ErrorRow(error: model.error, onDismiss: component.onErrorShown)
            Group {
                switch model.step {
                case .loading:
                    Color.clear
                case .intro:
                    IntroStep(icon: TalkIcons.pin, title: strings.channels_title.text) {
                        body(strings.channels_intro_body.text)
                        body(strings.channels_not_kept.text, muted: true)
                        TalkButton(strings.action_continue.text, style: .post, fullWidth: true, action: component.onIntroContinue)
                        TalkButton(strings.action_not_now.text, style: .secondary, fullWidth: true, action: component.onBack)
                    }
                case .locationStep:
                    IntroStep(icon: TalkIcons.shield, title: strings.channels_location_title.text) {
                        body(strings.channels_location_body.text)
                        TalkButton(strings.channels_allow_location.text, style: .post, fullWidth: true, action: component.onAllowLocation)
                        TalkButton(strings.channels_teleport_instead.text, style: .secondary, fullWidth: true, action: component.onTeleportInstead)
                    }
                case .nickname:
                    IntroStep(icon: TalkIcons.user, title: strings.channels_nickname_title.text) {
                        LabeledField(label: strings.channels_nickname.text) {
                            TalkTextField(placeholder: "anon", text: model.nicknameDraft, onChange: component.onNicknameChanged,
                                          submitLabel: .join, onSubmit: component.onSaveNickname)
                                .textInputAutocapitalization(.never)
                        }
                        body(strings.channels_nickname_body.text, muted: true)
                        TalkButton(strings.channels_join.text, style: .post, fullWidth: true, action: component.onSaveNickname)
                    }
                case .list:
                    ChannelList(model: model, component: component)
                }
            }
            .id(model.step)
            .transition(.asymmetric(insertion: .move(edge: .trailing).combined(with: .opacity), removal: .opacity))
        }
        .animation(TalkMotion.move, value: model.step)
        .background(TalkColor.bg.ignoresSafeArea())
        .talkTitle(strings.channels_title.text)
        .toolbar {
            if model.step == .list {
                ToolbarItem(placement: .primaryAction) {
                    ToolbarIconButton(icon: TalkIcons.teleport, label: strings.channels_teleport.text) { component.onShowTeleport(shown: true) }
                }
            }
        }
        .sheet(isPresented: Binding(get: { model.isTeleportOpen }, set: { component.onShowTeleport(shown: $0) })) {
            TeleportSheet(onOpen: { component.onTeleport(code: $0, level: $1) }, onCancel: { component.onShowTeleport(shown: false) })
        }
    }

    private func body(_ text: String, muted: Bool = false) -> some View {
        Text(text)
            .talkTextStyle(.body)
            .foregroundStyle(muted ? TalkColor.textSecondary : TalkColor.textPrimary)
            .multilineTextAlignment(.center)
    }
}

private struct IntroStep<Content: View>: View {
    let icon: SharedLogic.ImageResource
    let title: String
    @ViewBuilder let content: () -> Content

    var body: some View {
        ScrollView {
            VStack(spacing: TalkSpacing.lg) {
                TalkIcon(icon: icon, size: TalkSize.iconEmpty).foregroundStyle(TalkColor.textSecondary)
                Text(title).talkTextStyle(.headingLarge).foregroundStyle(TalkColor.textPrimary).accessibilityAddTraits(.isHeader)
                content()
            }
            .frame(maxWidth: 420)
            .padding(TalkSpacing.xl)
            .frame(maxWidth: .infinity)
        }
    }
}

/// The five levels where you are, with people counts ("? people" where nobody announces), Teleport, the nickname.
private struct ChannelList: View {
    let model: ChannelsComponentModel
    let component: ChannelsComponent

    var body: some View {
        let strings = MR.strings.shared
        ScrollView {
            LazyVStack(spacing: 0) {
                Caption(strings.channels_caption.text)
                switch model.location {
                case .locating:
                    SkeletonCards(count: 2)
                case .unavailable:
                    Caption(strings.channels_location_denied.text)
                case .unknown:
                    EmptyView()
                case .known:
                    ForEach(model.here, id: \.geohash) { channel in
                        let count = model.peopleIn(channel: channel)
                        ListRow(title: channel.level.label, caption: "#\(channel.geohash)", action: { component.onOpen(channel: channel) }) {
                            RowIcon(icon: TalkIcons.pin)
                        } trailing: {
                            Text(channel.level.sendsPresence
                                 ? count.map { MR.plurals.shared.channels_people.format(count: Int($0.int32Value)) } ?? ""
                                 : strings.channels_people_unknown.text)
                                .talkTextStyle(.small)
                                .foregroundStyle(TalkColor.textSecondary)
                        }
                    }
                }
                ListRow(title: strings.channels_teleport.text, caption: strings.channels_teleport_caption.text,
                        action: { component.onShowTeleport(shown: true) }) {
                    RowIcon(icon: TalkIcons.teleport)
                } trailing: {
                    EmptyView()
                }
                ListRow(title: strings.channels_nickname_here.format(model.nickname), caption: strings.channels_nickname_caption.text, action: nil) {
                    EmptyView()
                } trailing: {
                    TalkButton(strings.action_change.text, style: .secondary, action: component.onEditNickname)
                }
            }
        }
    }
}

extension ChannelLevel {
    var label: String {
        let strings = MR.strings.shared
        switch self {
        case .region: return strings.level_region.text
        case .province: return strings.level_province.text
        case .city: return strings.level_city.text
        case .neighborhood: return strings.level_neighborhood.text
        case .block: return strings.level_block.text
        }
    }
}

/// Teleport (`Teleport`): an area code and a level; an area typed finer than the level is cut to it.
private struct TeleportSheet: View {
    let onOpen: (String, ChannelLevel) -> Void
    let onCancel: () -> Void
    @State private var code = ""
    @State private var level: ChannelLevel = .city

    var body: some View {
        let strings = MR.strings.shared
        NavigationStack {
            VStack(alignment: .leading, spacing: TalkSpacing.lg) {
                Text(strings.channels_teleport_body.text).talkTextStyle(.body).foregroundStyle(TalkColor.textSecondary)
                LabeledField(label: strings.channels_area_code.text) {
                    TalkTextField(placeholder: "u4pru", text: code, onChange: { code = $0 }, submitLabel: .go) {
                        if !code.isEmpty { onOpen(code, level) }
                    }
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                }
                Text(strings.channels_level.text).talkTextStyle(.small).foregroundStyle(TalkColor.textSecondary)
                ScrollView(.horizontal) {
                    HStack(spacing: TalkSpacing.sm) {
                        ForEach(ChannelLevel.allCases, id: \.self) { option in
                            Chip(text: option.label, on: option == level) {
                                level = option
                                let area = Geohash.shared.normalize(input: code)
                                if Geohash.shared.isValid(geohash: area) && area.count > Int(option.precision) {
                                    code = String(area.prefix(Int(option.precision)))
                                }
                            }
                        }
                    }
                }
                .scrollIndicators(.hidden)
                Spacer(minLength: 0)
            }
            .padding(TalkSpacing.xl)
            .navigationTitle(strings.channels_teleport.text)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(strings.action_cancel.text, action: onCancel) }
                ToolbarItem(placement: .confirmationAction) {
                    Button(strings.channels_open.text) { onOpen(code, level) }.disabled(code.isEmpty)
                }
            }
        }
        .presentationDetents([.medium])
    }
}

/// A channel room (`Channel-Room-Mobile`): `#area · Level` with the people count, "public and not kept", a plain
/// timeline of `nick#abcd` lines with the teleport mark, and the composer. At Block level, decision 21's warning.
struct RoomView: View {
    private let component: RoomComponent
    @StateValue private var model: RoomComponentModel

    init(_ component: RoomComponent) {
        self.component = component
        _model = StateValue(component.model)
    }

    var body: some View {
        let strings = MR.strings.shared
        let channel = model.channel
        let people = model.people.map { MR.plurals.shared.channels_people.format(count: Int($0.int32Value)) } ?? strings.channels_people_unknown.text
        VStack(spacing: 0) {
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 0) {
                        Notice(icon: TalkIcons.info, text: strings.channels_not_kept.text)
                        if channel.level == .block && !channel.isTeleport {
                            Notice(icon: TalkIcons.pin, text: strings.channels_block_warning.text)
                        }
                        ErrorRow(error: model.error, onDismiss: component.onErrorShown)
                        ForEach(model.messages, id: \.id) { message in
                            VStack(alignment: .leading, spacing: 2) {
                                HStack(spacing: 4) {
                                    Text(message.displayName).talkTextStyle(.bodyBold)
                                        .foregroundStyle(message.isMine ? TalkColor.accent : TalkColor.textPrimary)
                                    if message.isTeleported {
                                        TalkIcon(icon: TalkIcons.teleport, size: 14, label: strings.channels_teleported.text).foregroundStyle(TalkColor.accent)
                                    }
                                    Text("· " + Date(timeIntervalSince1970: Double(message.createdAt.toEpochMilliseconds()) / 1000)
                                        .formatted(date: .omitted, time: .shortened))
                                        .talkTextStyle(.small).foregroundStyle(TalkColor.textSecondary)
                                }
                                Text(message.content).talkTextStyle(.body).foregroundStyle(TalkColor.textPrimary)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, TalkSpacing.lg)
                            .padding(.vertical, 4)
                        }
                        Color.clear.frame(height: 1).id("end")
                    }
                }
                .defaultScrollAnchor(.bottom)
                .scrollDismissesKeyboard(.interactively)
                .onChange(of: model.messages.count) { _, _ in withAnimation(TalkMotion.move) { proxy.scrollTo("end") } }
            }
            HStack(spacing: TalkSpacing.xs) {
                TalkTextField(placeholder: strings.channels_message_as.format(channel.geohash, model.nickname), text: model.text,
                              onChange: component.onTextChanged, pill: true, submitLabel: .send) {
                    if model.canSend { component.onSend() }
                }
                IconButton(icon: TalkIcons.send, label: strings.chat_send.text, tint: TalkColor.accent, enabled: model.canSend, action: component.onSend)
            }
            .padding(TalkSpacing.sm)
            .background(TalkColor.bg)
        }
        .background(TalkColor.bg.ignoresSafeArea())
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) {
                VStack(spacing: 0) {
                    Text(strings.channels_room_title.format(channel.geohash, channel.level.label)).talkTextStyle(.bodyBold)
                        .foregroundStyle(TalkColor.textPrimary).lineLimit(1)
                    Text(channel.isTeleport ? "\(people) · \(strings.channels_teleported_here.text)" : people)
                        .talkTextStyle(.small).foregroundStyle(TalkColor.textSecondary).lineLimit(1)
                }
            }
        }
    }
}
