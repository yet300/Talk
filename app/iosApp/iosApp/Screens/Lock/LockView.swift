import SwiftUI
import SharedLogic

/// The passcode lock screen (`Lock-Mobile`): "Talk is locked", the passcode and Unlock; Forgot passcode? erases the
/// account here after a warning. iOS locks with the device instead (`DeviceLockGate`), so the root shows this only
/// for a stored key encrypted with a passcode.
struct LockView: View {
    private let component: LockComponent
    @StateValue private var model: LockComponentModel
    @State private var forgot = false

    init(_ component: LockComponent) {
        self.component = component
        _model = StateValue(component.model)
    }

    var body: some View {
        let strings = MR.strings.shared
        VStack(spacing: 0) {
            NetworkBar()
            Spacer()
            VStack(spacing: TalkSpacing.lg) {
                Text(strings.app_name.text).talkTextStyle(.wordmark).foregroundStyle(TalkColor.textPrimary)
                TalkIcon(icon: TalkIcons.lock, size: TalkSize.iconEmpty).foregroundStyle(TalkColor.textSecondary)
                Text(strings.lock_title.text)
                    .talkTextStyle(.headingLarge)
                    .foregroundStyle(TalkColor.textPrimary)
                    .accessibilityAddTraits(.isHeader)
                ErrorRow(error: model.error, onDismiss: component.onErrorShown)
                TalkTextField(placeholder: strings.lock_passcode.text, text: model.passcode, onChange: component.onPasscodeChanged,
                              secure: true, submitLabel: .go, onSubmit: component.onUnlock)
                    .textContentType(.password)
                TalkButton(model.isUnlocking ? strings.lock_unlocking.text : strings.lock_unlock.text, style: .post,
                           enabled: !model.isUnlocking && !model.passcode.isEmpty, fullWidth: true, action: component.onUnlock)
                Button(strings.lock_forgot.text) { forgot = true }
                    .talkTextStyle(.body)
                    .foregroundStyle(TalkColor.accent)
            }
            .frame(maxWidth: 420)
            .padding(TalkSpacing.xl)
            Spacer()
        }
        .background(TalkColor.bg.ignoresSafeArea())
        .alert(strings.lock_forgot_title.text, isPresented: $forgot) {
            Button(strings.lock_forgot_confirm.text, role: .destructive, action: component.onForgot)
            Button(strings.action_cancel.text, role: .cancel) {}
        } message: {
            Text(strings.lock_forgot_body.text)
        }
    }
}
