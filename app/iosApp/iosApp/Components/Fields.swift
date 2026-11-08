import SwiftUI
import UIKit
import UniformTypeIdentifiers
import SharedLogic

/// The design's text field (`x-input`): a rounded control, or a pill for reply and message fields. The text belongs to
/// the component: the field shows `text` and reports every change.
struct TalkTextField: View {
    let placeholder: String
    let text: String
    let onChange: (String) -> Void
    var pill = false
    var secure = false
    var multiline = false
    var enabled = true
    var submitLabel: SubmitLabel = .done
    var keyboard: UIKeyboardType = .default
    var onSubmit: (() -> Void)? = nil

    var body: some View {
        let binding = Binding(get: { text }, set: onChange)
        Group {
            if secure {
                SecureField(placeholder, text: binding)
            } else if multiline {
                TextField(placeholder, text: binding, axis: .vertical).lineLimit(1...6)
            } else {
                TextField(placeholder, text: binding)
            }
        }
        .talkTextStyle(.inputText)
        .foregroundStyle(TalkColor.textPrimary)
        .tint(TalkColor.accent)
        .keyboardType(keyboard)
        .submitLabel(submitLabel)
        .onSubmit { onSubmit?() }
        .padding(.horizontal, TalkSpacing.md)
        .padding(.vertical, 10)
        .frame(minHeight: TalkSize.touchMin)
        .background(TalkColor.searchBg, in: RoundedRectangle(cornerRadius: pill ? 22 : TalkRadius.control))
        .disabled(!enabled)
        .opacity(enabled ? 1 : TalkSize.disabledOpacity)
    }
}

/// A form field with its label above and an optional caption below.
struct LabeledField<Field: View>: View {
    let label: String
    var caption: String? = nil
    @ViewBuilder let field: () -> Field

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label).talkTextStyle(.small).foregroundStyle(TalkColor.textSecondary)
            field()
            if let caption {
                Text(caption).talkTextStyle(.small).foregroundStyle(TalkColor.textSecondary)
            }
        }
    }
}

/// Copies a secret (the private key): only on this device, and gone from the clipboard after two minutes.
func copySecret(_ text: String) {
    UIPasteboard.general.setItems(
        [[UTType.utf8PlainText.identifier: text]],
        options: [.localOnly: true, .expirationDate: Date().addingTimeInterval(120)]
    )
}
