import SwiftUI
import UIKit
import UniformTypeIdentifiers
import SharedLogic

/// Typing into text whose value lives in a Kotlin store. The system field keeps reading its binding between keystrokes,
/// and the store's new value only reaches SwiftUI on a later update, so a binding straight to the store hands the field
/// old text: characters get lost and the caret jumps. Here the field owns a draft and reports every change. Text the
/// component sets by itself (a restored draft, cleared after Send) still replaces the draft; its echoes of what was
/// typed don't.
struct DraftText<Content: View>: View {
    let text: String
    let onChange: (String) -> Void
    @ViewBuilder let content: (Binding<String>) -> Content

    @State private var draft: String
    /// Sent to the component, its echo not seen yet.
    @State private var pending: [String] = []

    init(text: String, onChange: @escaping (String) -> Void, @ViewBuilder content: @escaping (Binding<String>) -> Content) {
        self.text = text
        self.onChange = onChange
        self.content = content
        _draft = State(initialValue: text)
    }

    var body: some View {
        content($draft)
            .onChange(of: draft) { _, typed in
                if typed == text && pending.isEmpty { return }
                pending.append(typed)
                onChange(typed)
            }
            .onChange(of: text) { _, value in
                if let echo = pending.firstIndex(of: value) {
                    pending.removeFirst(echo + 1)
                } else {
                    pending = []
                    if draft != value { draft = value }
                }
            }
    }
}

/// The system search field over text that lives in a Kotlin store (see `DraftText`).
struct DraftSearchable: ViewModifier {
    let text: String
    let prompt: String
    let onChange: (String) -> Void

    func body(content: Content) -> some View {
        DraftText(text: text, onChange: onChange) { draft in
            content.searchable(text: draft, placement: .navigationBarDrawer(displayMode: .always), prompt: prompt)
        }
    }
}

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
        DraftText(text: text, onChange: onChange) { binding in
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
