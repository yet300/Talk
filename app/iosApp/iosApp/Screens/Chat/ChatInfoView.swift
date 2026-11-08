import SwiftUI
import CoreImage.CIFilterBuiltins
import SharedLogic

/// Chat info (`ChatInfo`): who is in it, disappearing messages, mute, Verify, the encryption explainer (decision 20),
/// and block and report (a person) or leave (a group), each asking first.
struct ChatInfoView: View {
    private let component: ChatInfoComponent
    @StateValue private var model: ChatInfoComponentModel
    @State private var explainer = false
    @State private var blocking = false
    @State private var leaving = false
    @State private var reporting = false

    init(_ component: ChatInfoComponent) {
        self.component = component
        _model = StateValue(component.model)
    }

    var body: some View {
        let strings = MR.strings.shared
        let title = chatTitle(name: model.name, members: model.members)
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                VStack(spacing: TalkSpacing.md) {
                    ChatAvatar(members: model.members, size: TalkSize.avatarLg * 1.5)
                    HStack(spacing: 6) {
                        Text(title).talkTextStyle(.headingLarge).foregroundStyle(TalkColor.textPrimary)
                        if model.isVerified {
                            TalkIcon(icon: TalkIcons.check, size: 22, label: strings.chat_verified.text).foregroundStyle(TalkColor.accent)
                        }
                    }
                }
                .frame(maxWidth: .infinity)
                .padding(TalkSpacing.lg)
                ErrorRow(error: model.error, onDismiss: component.onErrorShown)
                if model.isGroup {
                    SectionTitle(strings.chat_people.text)
                    ForEach(model.members, id: \.key) { user in
                        PersonRow(user: user) { component.onOpenMember(id: user.id) }
                    }
                }
                SectionTitle(strings.chat_disappearing.text)
                Picker(strings.chat_disappearing.text, selection: Binding(get: { model.disappearing }, set: component.onDisappearingChanged)) {
                    Text(strings.disappearing_off.text).tag(Disappearing.off)
                    Text(strings.disappearing_hour.text).tag(Disappearing.hour)
                    Text(strings.disappearing_day.text).tag(Disappearing.day)
                    Text(strings.disappearing_week.text).tag(Disappearing.week)
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, TalkSpacing.lg)
                .padding(.vertical, TalkSpacing.sm)
                Caption(strings.chat_disappearing_note.text)
                ToggleRow(title: strings.chat_mute.text, caption: strings.chat_mute_note.text, icon: TalkIcons.mute,
                          isOn: model.isMuted, onChange: component.onMutedChanged)
                if !model.isGroup {
                    ActionRow(label: strings.chat_verify.text, icon: TalkIcons.shield,
                              note: model.isVerified ? strings.chat_verified.text : nil, action: component.onVerify)
                }
                ActionRow(label: strings.chat_encryption_row.text, icon: TalkIcons.lock) { explainer = true }
                if model.isGroup {
                    ActionRow(label: strings.chat_leave.text, icon: TalkIcons.back, danger: true) { leaving = true }
                } else {
                    if model.isBlocked {
                        ActionRow(label: strings.action_unblock.text, icon: TalkIcons.block) { component.onBlockedChanged(blocked: false) }
                    } else {
                        ActionRow(label: strings.action_block.text, icon: TalkIcons.block, danger: true) { blocking = true }
                    }
                    if !model.members.isEmpty {
                        ActionRow(label: strings.action_report.text, icon: TalkIcons.report, danger: true) { reporting = true }
                    }
                }
            }
            .padding(.bottom, TalkSpacing.xl)
        }
        .background(TalkColor.bg.ignoresSafeArea())
        .talkTitle(strings.chat_info.text)
        .sheet(isPresented: $explainer) {
            VStack(alignment: .leading, spacing: TalkSpacing.md) {
                Text(strings.chat_encryption.text).talkTextStyle(.headingMedium).foregroundStyle(TalkColor.textPrimary)
                ForEach([strings.chat_encryption_1, strings.chat_encryption_2, strings.chat_encryption_3], id: \.self) {
                    Text($0.text).talkTextStyle(.body).foregroundStyle(TalkColor.textPrimary)
                }
                Spacer(minLength: 0)
            }
            .padding(TalkSpacing.xl)
            .presentationDetents([.medium])
            .presentationDragIndicator(.visible)
        }
        .sheet(isPresented: $reporting) {
            if let person = model.members.first {
                ReportSheet(target: ReportTargetProfile(author: person.id)) { reporting = false }
            }
        }
        .alert(strings.chat_block_title.format(title), isPresented: $blocking) {
            Button(strings.action_block.text, role: .destructive) { component.onBlockedChanged(blocked: true) }
            Button(strings.action_cancel.text, role: .cancel) {}
        } message: {
            Text(strings.chat_block_body.text)
        }
        .alert(strings.chat_leave_title.text, isPresented: $leaving) {
            Button(strings.chat_leave.text, role: .destructive, action: component.onLeave)
            Button(strings.action_cancel.text, role: .cancel) {}
        } message: {
            Text(strings.chat_leave_body.text)
        }
    }
}

/// Verify a contact (`VerifyContact`): the safety code (four groups of five digits, the same on both devices) and its QR
/// code, Scan code (the camera, where the device has one) and Mark as verified; once verified, the date and Remove
/// verification.
struct VerifyView: View {
    private let component: VerifyComponent
    @StateValue private var model: VerifyComponentModel
    @State private var scanning = false

    init(_ component: VerifyComponent) {
        self.component = component
        _model = StateValue(component.model)
    }

    var body: some View {
        let strings = MR.strings.shared
        let name = model.person.displayLabel
        ScrollView {
            VStack(spacing: TalkSpacing.lg) {
                ErrorRow(error: model.error, onDismiss: component.onErrorShown)
                Text(strings.verify_body.format(name)).talkTextStyle(.body).foregroundStyle(TalkColor.textPrimary)
                if let code = model.code {
                    HStack(spacing: TalkSpacing.md) {
                        ForEach(code.groups, id: \.self) { group in
                            Text(group).talkTextStyle(.code).font(.system(.title3, design: .monospaced)).foregroundStyle(TalkColor.textPrimary)
                        }
                    }
                    .accessibilityElement()
                    .accessibilityLabel("\(strings.verify_code.text): \(code.groups.joined(separator: " "))")
                    QrCodeView(payload: code.qrPayload)
                        .accessibilityLabel(strings.verify_qr.text)
                    if QrScanner.isAvailable {
                        Text(strings.verify_scan_hint.text).talkTextStyle(.small).foregroundStyle(TalkColor.textSecondary)
                        TalkButton(strings.verify_scan.text, style: .secondary) { scanning = true }
                    }
                }
                if model.scanMismatch {
                    Text(strings.verify_mismatch.text).talkTextStyle(.body).foregroundStyle(TalkColor.error)
                }
                if let verifiedAt = model.verifiedAt {
                    VStack(spacing: TalkSpacing.sm) {
                        HStack(spacing: TalkSpacing.md) {
                            TalkIcon(icon: TalkIcons.shield).foregroundStyle(TalkColor.accent)
                            VStack(alignment: .leading) {
                                Text(strings.chat_verified.text).talkTextStyle(.bodyBold).foregroundStyle(TalkColor.textPrimary)
                                Text(strings.verify_verified_on.format(name, verifiedAt.dateText())).talkTextStyle(.small).foregroundStyle(TalkColor.textSecondary)
                            }
                            Spacer(minLength: 0)
                        }
                        .padding(TalkSpacing.md)
                        .background(TalkColor.surface, in: RoundedRectangle(cornerRadius: 12))
                        TalkButton(strings.verify_unverify.text, style: .secondary, action: component.onUnverify)
                    }
                    .transition(.opacity)
                } else {
                    VStack(spacing: TalkSpacing.sm) {
                        TalkButton(strings.verify_mark.text, style: .post, enabled: model.code != nil, fullWidth: true, action: component.onMarkVerified)
                        Text(strings.verify_note.text).talkTextStyle(.small).foregroundStyle(TalkColor.textSecondary)
                    }
                    .transition(.opacity)
                }
            }
            .frame(maxWidth: 480)
            .padding(TalkSpacing.lg)
            .frame(maxWidth: .infinity)
            .animation(TalkMotion.fade, value: model.verifiedAt == nil)
        }
        .background(TalkColor.bg.ignoresSafeArea())
        .talkTitle(strings.verify_title.format(name))
        .sheet(isPresented: $scanning) {
            QrScanner { payload in
                scanning = false
                component.onScanned(payload: payload)
            }
            .ignoresSafeArea()
            .overlay(alignment: .top) {
                Text(strings.verify_scanning.text).talkTextStyle(.bodyBold).foregroundStyle(.white).padding(TalkSpacing.xl)
            }
        }
    }
}

/// A QR code drawn by Core Image, crisp at any size.
struct QrCodeView: View {
    let payload: String

    var body: some View {
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(payload.utf8)
        filter.correctionLevel = "M"
        let image = filter.outputImage.flatMap { CIContext().createCGImage($0, from: $0.extent) }
        return Group {
            if let image {
                Image(decorative: image, scale: 1).interpolation(.none).resizable().scaledToFit()
            }
        }
        .frame(width: 200, height: 200)
        .padding(TalkSpacing.md)
        .background(Color.white, in: RoundedRectangle(cornerRadius: TalkRadius.card))
    }
}

/// New chat (`NewChat`): search people and pick one, or several for a group (up to 10, a name from two); someone with no
/// inbox relays shows "Can't receive chats yet" and can't be picked.
struct NewChatView: View {
    private let component: NewChatComponent
    @StateValue private var model: NewChatComponentModel
    @FocusState private var searchFocused: Bool

    init(_ component: NewChatComponent) {
        self.component = component
        _model = StateValue(component.model)
    }

    var body: some View {
        let strings = MR.strings.shared
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                if !model.picked.isEmpty {
                    ScrollView(.horizontal) {
                        HStack(spacing: TalkSpacing.sm) {
                            ForEach(model.picked, id: \.key) { user in
                                Button { component.onPick(user: user) } label: {
                                    HStack(spacing: 6) {
                                        Avatar(user: user, size: 24)
                                        Text(user.displayLabel).talkTextStyle(.bodyBold)
                                        TalkIcon(icon: TalkIcons.close, size: 14)
                                    }
                                    .foregroundStyle(TalkColor.textPrimary)
                                    .padding(.horizontal, TalkSpacing.sm)
                                    .frame(minHeight: 36)
                                    .background(TalkColor.surface, in: Capsule())
                                }
                                .buttonStyle(PressStyle())
                                .accessibilityLabel(strings.chat_remove_pick.format(user.displayLabel))
                            }
                        }
                        .padding(.horizontal, TalkSpacing.lg)
                    }
                    .scrollIndicators(.hidden)
                }
                if model.picked.count > 1 {
                    TalkTextField(placeholder: strings.chat_group_name.text, text: model.groupName, onChange: component.onGroupNameChanged)
                        .padding(.horizontal, TalkSpacing.lg)
                        .padding(.vertical, TalkSpacing.sm)
                }
                TalkTextField(placeholder: strings.chat_new_search.text, text: model.query, onChange: component.onQueryChanged, pill: true,
                              submitLabel: .search)
                    .focused($searchFocused)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .padding(.horizontal, TalkSpacing.lg)
                    .padding(.vertical, TalkSpacing.sm)
                ErrorRow(error: model.error, onDismiss: component.onErrorShown)
                if model.isFull { Caption(strings.chat_group_full.format(Int32(ChatRepositoryCompanion.shared.MAX_GROUP_MEMBERS))) }
                if model.isSearching { Caption(strings.search_searching.text) }
                ForEach(model.people, id: \.key) { user in
                    let cannot = model.cannotReceive(user: user)
                    let picked = model.picked.contains { $0.key == user.key }
                    let checking = model.isChecking(user: user)
                    let enabled = !cannot && model.checking == nil && (!model.isFull || picked)
                    ListRow(title: user.displayLabel, caption: cannot ? strings.chat_new_cannot_receive.text : user.handle,
                            selected: picked, action: enabled ? { component.onPick(user: user) } : nil) {
                        Avatar(user: user)
                    } trailing: {
                        if checking {
                            ProgressView().tint(TalkColor.accent)
                        } else if picked {
                            TalkIcon(icon: TalkIcons.check).foregroundStyle(TalkColor.accent)
                        }
                    }
                    .opacity(cannot ? TalkSize.disabledOpacity : 1)
                }
            }
        }
        .scrollDismissesKeyboard(.interactively)
        .background(TalkColor.bg.ignoresSafeArea())
        .talkTitle(strings.chats_new.text)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button(strings.chat_start.text, action: component.onStart).disabled(!model.canStart)
            }
        }
        .onAppear { searchFocused = true }
    }
}
