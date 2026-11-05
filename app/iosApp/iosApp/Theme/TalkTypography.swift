import SwiftUI
import SharedLogic

/// The design's type scale (`TalkTokens.Type`) in the system font (SF Pro), scaled with Dynamic Type.
enum TalkTextStyle {
    case display, headingLarge, headingMedium, body, bodyBold, caption, small, tiny
    case reading, code, codeBlock, navLabel, tabLabel, inputText, postButtonLabel, wordmark

    fileprivate var token: TalkTokens.TextStyle {
        let t = TalkTokens.Type_.shared
        switch self {
        case .display: return t.display
        case .headingLarge: return t.headingLarge
        case .headingMedium: return t.headingMedium
        case .body: return t.body
        case .bodyBold: return t.bodyBold
        case .caption: return t.caption
        case .small: return t.small
        case .tiny: return t.tiny
        case .reading: return t.reading
        case .code: return t.code
        case .codeBlock: return t.codeBlock
        case .navLabel: return t.navLabel
        case .tabLabel: return t.tabLabel
        case .inputText: return t.inputText
        case .postButtonLabel: return t.postButtonLabel
        case .wordmark: return t.wordmark
        }
    }

    fileprivate var isMonospaced: Bool { self == .code || self == .codeBlock }

    /// The Dynamic Type style the size scales with.
    fileprivate var relativeTo: UIFont.TextStyle {
        switch self {
        case .display: return .largeTitle
        case .headingLarge, .wordmark: return .title2
        case .headingMedium: return .title3
        case .caption, .small, .tiny: return .caption1
        default: return .body
        }
    }
}

private struct TalkTextModifier: ViewModifier {
    let style: TalkTextStyle
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    func body(content: Content) -> some View {
        let token = style.token
        let metrics = UIFontMetrics(forTextStyle: style.relativeTo)
        let size = metrics.scaledValue(for: CGFloat(token.size))
        let lineHeight = metrics.scaledValue(for: CGFloat(token.lineHeight))
        let font = UIFont.systemFont(ofSize: size, weight: Self.weight(token.weight))
        return content
            .font(.system(size: size, weight: Self.fontWeight(token.weight), design: style.isMonospaced ? .monospaced : .default))
            .tracking(CGFloat(token.letterSpacing))
            .lineSpacing(max(0, lineHeight - font.lineHeight))
    }

    private static func weight(_ css: Int32) -> UIFont.Weight {
        switch css {
        case ..<500: return .regular
        case 500..<600: return .medium
        case 600..<700: return .semibold
        case 700..<800: return .bold
        default: return .heavy
        }
    }

    private static func fontWeight(_ css: Int32) -> Font.Weight {
        switch css {
        case ..<500: return .regular
        case 500..<600: return .medium
        case 600..<700: return .semibold
        case 700..<800: return .bold
        default: return .heavy
        }
    }
}

extension View {
    /// Applies one of the design's text styles: size, weight, line height and tracking.
    func talkTextStyle(_ style: TalkTextStyle) -> some View {
        modifier(TalkTextModifier(style: style))
    }
}
