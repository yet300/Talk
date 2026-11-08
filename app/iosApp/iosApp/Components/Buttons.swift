import SwiftUI
import SharedLogic

/// The design's buttons: Primary (blue pill), Secondary (outlined), Post (blue, compact), Danger (red text).
enum TalkButtonStyle {
    case primary, secondary, post, danger
}

struct TalkButton: View {
    let title: String
    var style: TalkButtonStyle = .primary
    var enabled = true
    var busy = false
    var fullWidth = false
    let action: () -> Void

    init(_ title: String, style: TalkButtonStyle = .primary, enabled: Bool = true, busy: Bool = false, fullWidth: Bool = false,
         action: @escaping () -> Void) {
        self.title = title
        self.style = style
        self.enabled = enabled
        self.busy = busy
        self.fullWidth = fullWidth
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            ZStack {
                Text(title).opacity(busy ? 0 : 1)
                if busy { ProgressView().tint(foreground) }
            }
            .talkTextStyle(style == .post ? .postButtonLabel : .bodyBold)
            .foregroundStyle(foreground)
            .padding(.horizontal, TalkSpacing.lg)
            .frame(maxWidth: fullWidth ? .infinity : nil)
            .frame(minHeight: style == .post ? TalkSize.buttonPostHeight : TalkSize.buttonHeight)
            .background(background, in: Capsule())
            .overlay(Capsule().strokeBorder(style == .secondary ? TalkColor.controlBorder : .clear, lineWidth: 1))
            .contentShape(Capsule())
        }
        .buttonStyle(PressStyle())
        .disabled(!enabled || busy)
        .opacity(enabled || busy ? 1 : TalkSize.disabledOpacity)
    }

    private var foreground: Color {
        switch style {
        case .primary, .post: return TalkColor.onAccent
        case .secondary: return TalkColor.textPrimary
        case .danger: return TalkColor.error
        }
    }

    private var background: Color {
        switch style {
        case .primary, .post: return TalkColor.accent
        case .secondary, .danger: return .clear
        }
    }
}

/// Follow / Following (Unfollow while pressed is the system's long-press menu, so a tap toggles).
struct FollowButton: View {
    let following: Bool
    let action: () -> Void

    var body: some View {
        let strings = MR.strings.shared
        TalkButton(following ? strings.action_following.text : strings.action_follow.text, style: following ? .secondary : .primary, action: action)
            .accessibilityValue(following ? strings.action_following.text : "")
            .sensoryFeedback(.selection, trigger: following)
    }
}

/// An icon button with a 44 pt target and its label for VoiceOver.
struct IconButton: View {
    let icon: SharedLogic.ImageResource
    let label: String
    var tint: Color = TalkColor.textPrimary
    var size: CGFloat = TalkSize.iconNav
    var enabled = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            TalkIcon(icon: icon, size: size)
                .foregroundStyle(tint)
                .frame(minWidth: TalkSize.touchMin, minHeight: TalkSize.touchMin)
                .contentShape(Rectangle())
        }
        .buttonStyle(PressStyle())
        .disabled(!enabled)
        .opacity(enabled ? 1 : TalkSize.disabledOpacity)
        .accessibilityLabel(label)
    }
}

/// Feedback on touch-down: a slight press, released with a spring.
struct PressStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.96 : 1)
            .opacity(configuration.isPressed ? 0.8 : 1)
            .animation(TalkMotion.settle, value: configuration.isPressed)
    }
}

/// A toolbar button with a title and an icon, so it fits horizontal and vertical (Duo) bars alike.
struct ToolbarIconButton: View {
    let icon: SharedLogic.ImageResource
    let label: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Label { Text(label) } icon: { TalkIcon(icon: icon) }
        }
        .accessibilityLabel(label)
    }
}
