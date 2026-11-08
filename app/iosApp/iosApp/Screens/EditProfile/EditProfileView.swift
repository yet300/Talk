import SwiftUI
import SharedLogic

/// The profile editor (`EditProfile-Mobile`): Cancel · Edit profile · Save; the banner and avatar (each asks for a GIPHY
/// or Unsplash link, decision 13), the fields, NIP-05 with Verified, and Link GitHub (NIP-39, decision 14).
struct EditProfileView: View {
    private let component: EditProfileComponent
    @StateValue private var model: EditProfileComponentModel
    @State private var linkFor: ImageKind?

    init(_ component: EditProfileComponent) {
        self.component = component
        _model = StateValue(component.model)
    }

    var body: some View {
        let strings = MR.strings.shared
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                imageFields
                Spacer().frame(height: 64)
                Caption(strings.edit_profile_images.text)
                ErrorRow(error: model.error, onDismiss: component.onErrorShown)
                if model.isLoading {
                    SkeletonCards(count: 2)
                } else {
                    fields
                    GitHubSection(github: model.github, component: component)
                }
            }
            .padding(.bottom, TalkSpacing.xl)
        }
        .scrollDismissesKeyboard(.interactively)
        .background(TalkColor.bg.ignoresSafeArea())
        .talkTitle(strings.action_edit_profile.text)
        .navigationBarBackButtonHidden()
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button(strings.action_cancel.text, action: component.onCancel)
            }
            ToolbarItem(placement: .confirmationAction) {
                Button(strings.action_save.text, action: component.onSave)
                    .disabled(!model.canSave || model.isSaving)
            }
        }
        .alert(linkFor == .avatar ? strings.edit_profile_avatar_link.text : strings.edit_profile_banner_link.text,
               isPresented: Binding(get: { linkFor != nil }, set: { if !$0 { linkFor = nil } })) {
            ImageLinkFields(kind: linkFor ?? .avatar) { link in
                component.onImageLinkSubmitted(kind: (linkFor ?? .avatar).kotlin, link: link)
                linkFor = nil
            }
        } message: {
            Text(strings.edit_profile_link_body.text)
        }
    }

    /// The banner (3:1) and the avatar over its edge, each with the image mark; a tap asks for a link.
    private var imageFields: some View {
        let strings = MR.strings.shared
        return ZStack(alignment: .bottomLeading) {
            Button { linkFor = .banner } label: {
                Color.clear
                    .aspectRatio(3, contentMode: .fit)
                    .background(TalkColor.surface)
                    .overlay {
                        if let banner = model.banner, let url = URL(string: banner) { RemoteImage(url: url) }
                    }
                    .overlay { EditMark() }
                    .clipped()
            }
            .buttonStyle(.plain)
            .accessibilityLabel(strings.edit_profile_change_banner.text)
            Button { linkFor = .avatar } label: {
                ZStack {
                    TalkColor.elevated
                    if let avatar = model.avatar, let url = URL(string: avatar) { RemoteImage(url: url) }
                    EditMark()
                }
                .frame(width: 88, height: 88)
                .clipShape(Circle())
                .overlay(Circle().stroke(TalkColor.bg, lineWidth: 4))
            }
            .buttonStyle(.plain)
            .padding(.leading, TalkSpacing.lg)
            .offset(y: 60)
            .accessibilityLabel(strings.edit_profile_change_avatar.text)
        }
    }

    private var fields: some View {
        let strings = MR.strings.shared
        let enabled = !model.isSaving
        return VStack(alignment: .leading, spacing: TalkSpacing.md) {
            field(strings.edit_profile_name.text, model.name, .name, enabled: enabled)
            field(strings.edit_profile_display_name.text, model.displayName, .displayName, enabled: enabled)
            LabeledField(label: strings.edit_profile_about.text, caption: strings.compose_length.format(model.aboutLength, model.aboutMax)) {
                TalkTextField(placeholder: "", text: model.about, onChange: { component.onFieldChanged(field: .about, text: $0) },
                              multiline: true, enabled: enabled)
            }
            .foregroundStyle(model.aboutLength > model.aboutMax ? TalkColor.error : TalkColor.textSecondary)
            LabeledField(label: strings.edit_profile_website.text) {
                TalkTextField(placeholder: "", text: model.website, onChange: { component.onFieldChanged(field: .website, text: $0) },
                              enabled: enabled, keyboard: .URL)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
            }
            LabeledField(label: strings.edit_profile_nip05.text) {
                TalkTextField(placeholder: strings.edit_profile_nip05_placeholder.text, text: model.nip05,
                              onChange: { component.onFieldChanged(field: .nip05, text: $0) }, enabled: enabled, keyboard: .emailAddress)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
            }
            if model.nip05Status == .verified {
                HStack(spacing: 4) {
                    TalkIcon(icon: TalkIcons.check, size: 18).foregroundStyle(TalkColor.accent)
                    Text(strings.edit_profile_verified.text).talkTextStyle(.small).foregroundStyle(TalkColor.textSecondary)
                }
                .transition(.opacity)
            }
        }
        .padding(.horizontal, TalkSpacing.lg)
        .animation(TalkMotion.fade, value: model.nip05Status == .verified)
    }

    private func field(_ label: String, _ text: String, _ field: EditProfileComponentField, enabled: Bool) -> some View {
        LabeledField(label: label) {
            TalkTextField(placeholder: "", text: text, onChange: { component.onFieldChanged(field: field, text: $0) }, enabled: enabled,
                          submitLabel: .next)
        }
    }
}

enum ImageKind {
    case avatar, banner

    var kotlin: ProfileImage {
        switch self {
        case .avatar: return .avatar
        case .banner: return .banner
        }
    }
}

/// The link field in the alert, with Open GIPHY and Open Unsplash; Use checks the link (only those sites are accepted).
private struct ImageLinkFields: View {
    let kind: ImageKind
    let onUse: (String) -> Void
    @State private var link = ""
    @Environment(\.openURL) private var openURL

    var body: some View {
        let strings = MR.strings.shared
        TextField(strings.edit_profile_link_placeholder.text, text: $link)
            .keyboardType(.URL)
            .textInputAutocapitalization(.never)
        Button(strings.edit_profile_open_giphy.text) { openURL(URL(string: "https://giphy.com")!) }
        Button(strings.edit_profile_open_unsplash.text) { openURL(URL(string: "https://unsplash.com")!) }
        Button(strings.action_use.text) { onUse(link) }
        Button(strings.action_cancel.text, role: .cancel) {}
    }
}

private struct EditMark: View {
    var body: some View {
        TalkIcon(icon: TalkIcons.image, size: 20)
            .foregroundStyle(.white)
            .frame(width: 40, height: 40)
            .background(TalkColor.mediaScrim, in: Circle())
    }
}

/// Link GitHub (`GitHubLink`): the username and Link GitHub; then the proof text for a public gist, Copy, the gist link
/// and Verify; then the verified login with Use GitHub avatar.
private struct GitHubSection: View {
    let github: EditProfileComponentGitHub
    let component: EditProfileComponent
    @Environment(\.toast) private var toast

    var body: some View {
        let strings = MR.strings.shared
        VStack(alignment: .leading, spacing: TalkSpacing.sm) {
            HStack(spacing: 6) {
                TalkIcon(icon: TalkIcons.github, size: 18)
                Text(strings.github_title.text).talkTextStyle(.bodyBold)
            }
            .foregroundStyle(TalkColor.textPrimary)
            switch onEnum(of: github) {
            case .notLinked(let state):
                LabeledField(label: strings.github_username.text) {
                    TalkTextField(placeholder: strings.github_username_placeholder.text, text: state.login,
                                  onChange: component.onGitHubLoginChanged, onSubmit: component.onLinkGitHub)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                }
                TalkButton(strings.github_link.text, style: .secondary, enabled: !state.login.isEmpty, action: component.onLinkGitHub)
            case .waiting(let state):
                Text(strings.github_gist_hint.text).talkTextStyle(.small).foregroundStyle(TalkColor.textSecondary)
                Text(state.proofText)
                    .talkTextStyle(.code)
                    .foregroundStyle(TalkColor.textPrimary)
                    .textSelection(.enabled)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(TalkSpacing.md)
                    .background(TalkColor.surface, in: RoundedRectangle(cornerRadius: TalkRadius.control))
                TalkButton(strings.action_copy.text, style: .secondary) {
                    copyText(state.proofText, toast: toast, confirmation: strings.action_copied.text)
                }
                if state.failed {
                    Text(strings.github_proof_missing.text).talkTextStyle(.body).foregroundStyle(TalkColor.error)
                }
                LabeledField(label: strings.github_gist_link.text) {
                    TalkTextField(placeholder: strings.github_gist_placeholder.text, text: state.gistLink,
                                  onChange: component.onGistLinkChanged, keyboard: .URL, onSubmit: component.onVerifyGitHub)
                        .textInputAutocapitalization(.never)
                }
                TalkButton(state.isChecking ? strings.github_verifying.text : strings.github_verify.text,
                           enabled: !state.gistLink.isEmpty && !state.isChecking, action: component.onVerifyGitHub)
            case .linked(let state):
                HStack(spacing: 6) {
                    Text(state.login).talkTextStyle(.bodyBold).foregroundStyle(TalkColor.textPrimary)
                    TalkIcon(icon: TalkIcons.check, size: 18).foregroundStyle(TalkColor.accent)
                    Text(strings.github_verified.text).talkTextStyle(.small).foregroundStyle(TalkColor.textSecondary)
                }
                if !state.usesAvatar {
                    TalkButton(strings.github_use_avatar.text, style: .secondary, action: component.onUseGitHubAvatar)
                }
            }
        }
        .padding(TalkSpacing.lg)
    }
}
