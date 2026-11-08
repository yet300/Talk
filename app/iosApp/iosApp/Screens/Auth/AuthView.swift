import SwiftUI
import SharedLogic

/// Sign in (`AuthPanel`, decision 4): paste a private key (Use this key), or Create a new key and go through "Save your
/// key" first (Copy, then "I saved it" once copied or after 3 s). The key stays on this device (KSafe).
struct AuthView: View {
    private let component: AuthComponent
    @StateValue private var model: AuthComponentModel
    @Environment(\.hideInScreenshots) private var hideInScreenshots

    init(_ component: AuthComponent) {
        self.component = component
        _model = StateValue(component.model)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: TalkSpacing.lg) {
                Text(MR.strings.shared.app_name.text)
                    .talkTextStyle(.wordmark)
                    .foregroundStyle(TalkColor.textPrimary)
                    .accessibilityAddTraits(.isHeader)
                NetworkBar()
                Group {
                    if let newKey = model.newKey {
                        saveKey(newKey)
                    } else {
                        keyForm
                    }
                }
                .transition(.opacity)
                ErrorRow(error: model.error, onDismiss: component.onErrorShown)
            }
            .frame(maxWidth: 420)
            .padding(.horizontal, TalkSpacing.lg)
            .padding(.vertical, TalkSpacing.xxl)
            .frame(maxWidth: .infinity)
        }
        .scrollDismissesKeyboard(.interactively)
        .background(TalkColor.bg.ignoresSafeArea())
        .talkAnimation(TalkMotion.fade, value: model.newKey != nil)
    }

    private var keyForm: some View {
        let strings = MR.strings.shared
        return VStack(alignment: .leading, spacing: TalkSpacing.lg) {
            Text(strings.auth_title.text).talkTextStyle(.headingLarge).foregroundStyle(TalkColor.textPrimary)
            Text(strings.auth_subtitle.text).talkTextStyle(.body).foregroundStyle(TalkColor.textSecondary)
            LabeledField(label: strings.auth_key_label.text, caption: strings.auth_key_caption.text) {
                TalkTextField(placeholder: strings.auth_key_placeholder.text, text: model.key, onChange: component.onKeyChanged,
                              secure: true, submitLabel: .go) {
                    if model.canImport { component.onImportKey() }
                }
                .textContentType(.password)
                .autocorrectionDisabled()
                .textInputAutocapitalization(.never)
            }
            TalkButton(strings.auth_use_key.text, style: .post, enabled: model.canImport, busy: model.isWorking, fullWidth: true,
                       action: component.onImportKey)
            TalkButton(strings.auth_create_key.text, style: .secondary, enabled: !model.isWorking, fullWidth: true,
                       action: component.onCreateKey)
        }
    }

    /// "Save your key": the new `nsec` once, kept out of screenshots, with Copy and "I saved it".
    private func saveKey(_ key: String) -> some View {
        let strings = MR.strings.shared
        return VStack(alignment: .leading, spacing: TalkSpacing.lg) {
            Text(strings.auth_save_title.text).talkTextStyle(.headingLarge).foregroundStyle(TalkColor.textPrimary)
            Text(strings.auth_save_warning.text).talkTextStyle(.body).foregroundStyle(TalkColor.textSecondary)
            KeyText(key: key, protected: true)
            TalkButton(model.newKeyCopied ? strings.action_copied.text : strings.action_copy.text, style: .secondary, fullWidth: true) {
                copySecret(key)
                component.onNewKeyCopied()
            }
            .sensoryFeedback(.success, trigger: model.newKeyCopied)
            TalkButton(strings.auth_saved_it.text, style: .post, enabled: model.canConfirmSaved, busy: model.isWorking, fullWidth: true,
                       action: component.onNewKeySaved)
        }
    }
}

/// A private key on screen: monospaced on the surface, selectable, and drawn where screenshots and recordings leave it out.
struct KeyText: View {
    let key: String
    var protected = true

    var body: some View {
        ScreenCaptureProtected(enabled: protected) {
            Text(key)
                .talkTextStyle(.code)
                .foregroundStyle(TalkColor.textPrimary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(TalkSpacing.md)
                .background(TalkColor.surface, in: RoundedRectangle(cornerRadius: TalkRadius.control))
        }
        .frame(minHeight: 72)
        .accessibilityElement()
        .accessibilityLabel(MR.strings.shared.settings_private_key.text)
    }
}
