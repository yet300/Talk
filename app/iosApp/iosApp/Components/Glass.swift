import SwiftUI

extension View {
    /// Liquid Glass on iOS 26+, the system material before it, and the design's solid glass with Reduce Transparency.
    /// For the few floating pieces Talk draws itself (the toast, the compose button, the feed capsule); bars and sheets
    /// get glass from their system containers.
    func talkGlass<S: Shape>(in shape: S, interactive: Bool = false) -> some View {
        modifier(TalkGlass(shape: shape, interactive: interactive))
    }

    /// The system's glass button style where it exists, bordered before it.
    @ViewBuilder
    func talkGlassButton(prominent: Bool = false) -> some View {
        if #available(iOS 26, *) {
            if prominent { buttonStyle(.glassProminent) } else { buttonStyle(.glass) }
        } else {
            if prominent { buttonStyle(.borderedProminent) } else { buttonStyle(.bordered) }
        }
    }
}

private struct TalkGlass<S: Shape>: ViewModifier {
    let shape: S
    let interactive: Bool
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency

    func body(content: Content) -> some View {
        if reduceTransparency {
            content.background(TalkColor.glassSolid, in: shape)
        } else if #available(iOS 26, *) {
            content.glassEffect(interactive ? .regular.interactive() : .regular, in: shape)
        } else {
            content.background(.regularMaterial, in: shape)
        }
    }
}
