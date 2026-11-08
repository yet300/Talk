import SwiftUI
import UIKit
import SharedLogic

/// Short confirmations ("Link copied", "Reported. Your report is public."): one at a time, at the bottom, for two
/// seconds, read out by VoiceOver.
final class ToastPresenter: ObservableObject {
    @Published private(set) var message: String?
    private var hide: DispatchWorkItem?

    func show(_ text: String) {
        hide?.cancel()
        message = text
        UIAccessibility.post(notification: .announcement, argument: text)
        let work = DispatchWorkItem { [weak self] in self?.message = nil }
        hide = work
        DispatchQueue.main.asyncAfter(deadline: .now() + 2, execute: work)
    }
}

/// Shows the presenter's toast over the content.
struct ToastHost: ViewModifier {
    @ObservedObject var presenter: ToastPresenter

    func body(content: Content) -> some View {
        content
            .overlay(alignment: .bottom) {
                if let message = presenter.message {
                    Text(message)
                        .talkTextStyle(.bodyBold)
                        .foregroundStyle(TalkColor.textPrimary)
                        .padding(.horizontal, TalkSpacing.lg)
                        .padding(.vertical, TalkSpacing.md)
                        .talkGlass(in: Capsule())
                        .padding(.bottom, 96)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                        .accessibilityHidden(true)
                }
            }
            .talkAnimation(TalkMotion.move, value: presenter.message)
            .environment(\.toast, presenter)
    }
}

/// Copies text to the clipboard and confirms it with a toast.
@MainActor
func copyText(_ text: String, toast: ToastPresenter, confirmation: String = MR.strings.shared.toast_link_copied.text) {
    UIPasteboard.general.string = text
    toast.show(confirmation)
}
