import SwiftUI
import SharedLogic

/// Settings (`Settings-Mobile`, decision 7): Identity (with the private key behind a confirmation), Relays, Theme and
/// Security (the device lock with Face ID, Touch ID or the passcode; hide in screenshots; erase chats; erase
/// everything).
struct SettingsView: View {
    private let component: SettingsComponent
    @StateValue private var model: SettingsComponentModel

    init(_ component: SettingsComponent) {
        self.component = component
        _model = StateValue(component.model)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                ErrorRow(error: model.error, onDismiss: component.onErrorShown)
                IdentitySection(model: model, component: component)
                RelaysSection(model: model, component: component)
                ThemeSection(theme: model.theme, onSelect: component.onThemeSelected)
                SecuritySection(model: model, component: component)
            }
            .padding(.bottom, TalkSpacing.xl)
        }
        .scrollDismissesKeyboard(.interactively)
        .background(TalkColor.bg.ignoresSafeArea())
        .safeAreaInset(edge: .top, spacing: 0) { NetworkBar() }
        .talkTitle(MR.strings.shared.settings_title.text)
        // Leaving Settings hides the key again.
        .onDisappear(perform: component.onHideKey)
    }
}

/// Who is signed in, Sign out (which asks first, offering the key), Edit profile, Show private key.
private struct IdentitySection: View {
    let model: SettingsComponentModel
    let component: SettingsComponent
    @State private var confirmsShow = false
    @State private var signingOut = false
    @Environment(\.hideInScreenshots) private var hideInScreenshots

    var body: some View {
        let strings = MR.strings.shared
        let npub = model.account?.npub.shortNpub()
        SectionTitle(strings.settings_identity.text)
        ListRow(title: model.me?.displayLabel ?? npub ?? "",
                caption: [npub, strings.settings_signer_local.text].compactMap { $0 }.joined(separator: " · "),
                action: nil) {
            Avatar(user: model.me)
        } trailing: {
            TalkButton(strings.settings_sign_out.text, style: .secondary, enabled: !model.isWorking) { signingOut = true }
        }
        ActionRow(label: strings.action_edit_profile.text, icon: TalkIcons.user, action: component.onEditProfile)
        if let key = model.exportedKey, !signingOut {
            VStack(alignment: .leading, spacing: TalkSpacing.md) {
                Text(strings.settings_key_warning.text).talkTextStyle(.body).foregroundStyle(TalkColor.error)
                KeyText(key: key, protected: hideInScreenshots)
                HStack(spacing: TalkSpacing.sm) {
                    TalkButton(strings.action_copy.text, style: .secondary) { copySecret(key) }
                    TalkButton(strings.settings_hide_key.text, style: .secondary, action: component.onHideKey)
                }
            }
            .padding(TalkSpacing.lg)
            .transition(.opacity)
        } else {
            ListRow(title: strings.settings_private_key.text, action: nil) {
                EmptyView()
            } trailing: {
                TalkButton(strings.settings_show_key.text, style: .secondary) { confirmsShow = true }
            }
        }
        EmptyView()
            .alert(strings.settings_show_key.text, isPresented: $confirmsShow) {
                Button(strings.settings_show_key.text, action: component.onExportKey)
                Button(strings.action_cancel.text, role: .cancel) {}
            } message: {
                Text(strings.settings_key_warning.text)
            }
            .sheet(isPresented: $signingOut, onDismiss: component.onHideKey) {
                KeyStepSheet(title: strings.settings_sign_out_title.text, message: strings.settings_sign_out_body.text,
                             confirm: strings.settings_sign_out.text, exportedKey: model.exportedKey,
                             onShowKey: component.onExportKey, onConfirm: { signingOut = false; component.onSignOut() },
                             onCancel: { signingOut = false })
            }
    }
}

/// "Sign out?" and "Erase everything?" (`ConfirmDialog` with the key): without the key the account can't be signed back
/// in to, so it offers Show private key and Copy before the red confirm.
struct KeyStepSheet: View {
    let title: String
    let message: String
    let confirm: String
    let exportedKey: String?
    let onShowKey: () -> Void
    let onConfirm: () -> Void
    let onCancel: () -> Void
    @Environment(\.hideInScreenshots) private var hideInScreenshots

    var body: some View {
        let strings = MR.strings.shared
        VStack(alignment: .leading, spacing: TalkSpacing.lg) {
            Text(title).talkTextStyle(.headingLarge).foregroundStyle(TalkColor.textPrimary).accessibilityAddTraits(.isHeader)
            Text(message).talkTextStyle(.body).foregroundStyle(TalkColor.textSecondary)
            if let exportedKey {
                KeyText(key: exportedKey, protected: hideInScreenshots)
                TalkButton(strings.action_copy.text, style: .secondary) { copySecret(exportedKey) }
            } else {
                TalkButton(strings.settings_show_key.text, style: .secondary, action: onShowKey)
            }
            Spacer(minLength: 0)
            HStack(spacing: TalkSpacing.sm) {
                TalkButton(strings.action_cancel.text, style: .secondary, fullWidth: true, action: onCancel)
                Button(action: onConfirm) {
                    Text(confirm)
                        .talkTextStyle(.bodyBold)
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity, minHeight: TalkSize.buttonHeight)
                        .background(TalkColor.error, in: Capsule())
                }
                .buttonStyle(PressStyle())
            }
        }
        .padding(TalkSpacing.xl)
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
    }
}

/// A row per relay (address, state, access, remove) and the `wss://` field with Add.
private struct RelaysSection: View {
    let model: SettingsComponentModel
    let component: SettingsComponent

    var body: some View {
        let strings = MR.strings.shared
        SectionTitle(strings.settings_relays.text)
        ForEach(model.relays, id: \.url) { relay in
            let state = model.relayStates[relay.url] ?? .notUsedYet
            ListRow(title: relay.url, caption: "\(Self.label(state)) · \(Self.access(relay))", action: nil) {
                ZStack(alignment: .bottomTrailing) {
                    TalkIcon(icon: TalkIcons.relay, size: 20).foregroundStyle(TalkColor.textSecondary)
                    Circle().fill(Self.color(state)).frame(width: 8, height: 8)
                }
                .animation(TalkMotion.settle, value: state)
            } trailing: {
                IconButton(icon: TalkIcons.close, label: strings.settings_remove_relay.format(relay.url), tint: TalkColor.textSecondary, size: 18) {
                    component.onRemoveRelay(url: relay.url)
                }
            }
        }
        HStack(spacing: TalkSpacing.sm) {
            TalkTextField(placeholder: strings.settings_relay_placeholder.text, text: model.relayInput, onChange: component.onRelayInputChanged,
                          keyboard: .URL, onSubmit: component.onAddRelay)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .accessibilityLabel(strings.settings_relay_address.text)
            TalkButton(strings.settings_add_relay.text, style: .secondary, enabled: !model.isWorking, action: component.onAddRelay)
        }
        .padding(.horizontal, TalkSpacing.lg)
        .padding(.vertical, TalkSpacing.sm)
    }

    static func label(_ state: RelayState) -> String {
        let strings = MR.strings.shared
        switch state {
        case .connected: return strings.relay_connected.text
        case .connecting: return strings.relay_connecting.text
        case .unreachable: return strings.relay_unreachable.text
        case .notUsedYet: return strings.relay_not_used.text
        }
    }

    static func color(_ state: RelayState) -> Color {
        switch state {
        case .connected: return TalkColor.repost
        case .connecting: return TalkColor.warning
        case .unreachable: return TalkColor.error
        case .notUsedYet: return TalkColor.textSecondary
        }
    }

    static func access(_ relay: Relay) -> String {
        let strings = MR.strings.shared
        if relay.read && relay.write { return strings.relay_read_write.text }
        return relay.read ? strings.relay_read_only.text : strings.relay_write_only.text
    }
}

/// Theme: Dark · Light · System (the design's order).
private struct ThemeSection: View {
    let theme: ThemeMode
    let onSelect: (ThemeMode) -> Void

    var body: some View {
        let strings = MR.strings.shared
        SectionTitle(strings.settings_theme.text)
        Picker(strings.settings_theme.text, selection: Binding(get: { theme }, set: onSelect)) {
            Text(strings.theme_dark.text).tag(ThemeMode.dark)
            Text(strings.theme_light.text).tag(ThemeMode.light)
            Text(strings.theme_system.text).tag(ThemeMode.system)
        }
        .pickerStyle(.segmented)
        .padding(.horizontal, TalkSpacing.lg)
        .padding(.vertical, TalkSpacing.sm)
    }
}

/// Security (decision 24 on iOS): Device lock (turning it on asks for Face ID, Touch ID or the passcode once, so nobody
/// locks themselves out; it needs a device passcode), Hide in screenshots, Erase chats, Erase everything.
private struct SecuritySection: View {
    let model: SettingsComponentModel
    let component: SettingsComponent
    @State private var erasingChats = false
    @State private var erasingAll = false
    @Environment(\.toast) private var toast

    var body: some View {
        let strings = MR.strings.shared
        let available = LockSession.deviceHasPasscode
        SectionTitle(strings.settings_security.text)
        ToggleRow(title: strings.security_device_lock.text,
                  caption: available || model.privacy.deviceLock ? strings.security_device_lock_caption_ios.text : strings.security_device_lock_unavailable_ios.text,
                  icon: TalkIcons.lock,
                  isOn: model.privacy.deviceLock,
                  enabled: available || model.privacy.deviceLock) { on in
            if on {
                LockSession.shared.authenticate { ok in if ok { component.onDeviceLockChanged(enabled: true) } }
            } else {
                component.onDeviceLockChanged(enabled: false)
            }
        }
        ToggleRow(title: strings.security_hide_screenshots.text, caption: strings.security_hide_screenshots_caption_ios.text,
                  icon: TalkIcons.eyeOff, isOn: model.privacy.hideInScreenshots, enabled: true,
                  onChange: component.onHideInScreenshotsChanged)
        ActionRow(label: strings.security_erase_chats.text, icon: TalkIcons.trash, danger: true) { erasingChats = true }
        ActionRow(label: strings.security_erase_all.text, icon: TalkIcons.trash, danger: true) { erasingAll = true }
            .alert(strings.security_erase_chats_title.text, isPresented: $erasingChats) {
                Button(strings.security_erase_chats_confirm.text, role: .destructive) {
                    component.onEraseChats()
                    toast.show(strings.toast_chats_erased.text)
                }
                Button(strings.action_cancel.text, role: .cancel) {}
            } message: {
                Text(strings.security_erase_chats_body.text)
            }
            .sheet(isPresented: $erasingAll, onDismiss: component.onHideKey) {
                KeyStepSheet(title: strings.security_erase_all_title.text, message: strings.security_erase_all_body.text,
                             confirm: strings.security_erase_all.text, exportedKey: model.exportedKey,
                             onShowKey: component.onExportKey, onConfirm: { erasingAll = false; component.onEraseEverything() },
                             onCancel: { erasingAll = false })
            }
    }
}

/// A setting with a switch: icon, title, caption.
struct ToggleRow: View {
    let title: String
    let caption: String?
    var icon: SharedLogic.ImageResource? = nil
    let isOn: Bool
    var enabled = true
    let onChange: (Bool) -> Void

    var body: some View {
        Toggle(isOn: Binding(get: { isOn }, set: onChange)) {
            HStack(spacing: TalkSpacing.lg) {
                if let icon { TalkIcon(icon: icon).foregroundStyle(TalkColor.textPrimary) }
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).talkTextStyle(.bodyBold).foregroundStyle(TalkColor.textPrimary)
                    if let caption {
                        Text(caption).talkTextStyle(.small).foregroundStyle(TalkColor.textSecondary)
                    }
                }
            }
        }
        .tint(TalkColor.accent)
        .disabled(!enabled)
        .padding(.horizontal, TalkSpacing.xl)
        .padding(.vertical, TalkSpacing.md)
    }
}
