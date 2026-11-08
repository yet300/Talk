import SwiftUI

/// The design's motion vocabulary, as Apple ships it: critically damped springs for anything moved or swapped, a short
/// ease-out for content that only changes, a little bounce only where a finger carried momentum (the like pop).
/// Views pass `nil` instead when Reduce Motion is on, so slides become instant and fades stay.
enum TalkMotion {
    /// Screens, panes and pages moving (damping 1, response 0.4).
    static let move = Animation.spring(response: 0.4, dampingFraction: 1)
    /// Colours and small state changes.
    static let settle = Animation.spring(response: 0.3, dampingFraction: 1)
    /// A toggle the finger just flipped.
    static let pop = Animation.spring(response: 0.3, dampingFraction: 0.5)
    /// Content changing in place (loading → content → empty).
    static let fade = Animation.easeOut(duration: 0.2)
}

extension View {
    /// `withAnimation`'s value form that honours Reduce Motion: no movement, only the change.
    func talkAnimation<V: Equatable>(_ animation: Animation, value: V) -> some View {
        modifier(ReducibleAnimation(animation: animation, value: value))
    }
}

private struct ReducibleAnimation<V: Equatable>: ViewModifier {
    let animation: Animation
    let value: V
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func body(content: Content) -> some View {
        content.animation(reduceMotion ? TalkMotion.fade : animation, value: value)
    }
}
