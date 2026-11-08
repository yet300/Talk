import SwiftUI
import SharedLogic

/// What every screen reads from the root, as Android's composition locals: the viewer's network and Report, the post
/// cards' extras, the offline state, the post open next to a list, and how the reader lays out.
extension EnvironmentValues {
    /// Blurring and hidden posts (decision 18); nothing blurred until the viewer is known.
    @Entry var moderation: Moderation = EnvironmentDefaults.moderation
    /// Report (NIP-56).
    @Entry var moderationActions: ModerationComponent? = nil
    /// Quotes, link cards and Retry for every post card.
    @Entry var postCards: PostCardsComponent = EnvironmentDefaults.postCards
    /// Offline, the relays' states and what waits to be sent.
    @Entry var network: NetworkStatus = EnvironmentDefaults.network
    /// The post open next to a list (two panes): its card is highlighted.
    @Entry var selectedPost: String? = nil
    /// Two panes: Reply in the article shows the replies in the left pane (decision 6).
    @Entry var readerMode: ReaderMode = ReaderMode(twoPane: false, onShowReplies: {})
    /// Chats and the shown key stay out of screenshots and the app switcher (Settings, Security).
    @Entry var hideInScreenshots: Bool = true
    /// A short confirmation at the bottom ("Link copied").
    @Entry var toast: ToastPresenter = EnvironmentDefaults.toast
}

/// The defaults above, created once: an `@Entry` default is evaluated on every read, and a new object each time would
/// make every reader redraw.
private enum EnvironmentDefaults {
    static let moderation = Moderation(me: nil, following: Set(), hiddenPosts: Set(), blocked: Set())
    static let postCards: PostCardsComponent = PreviewPostCardsComponent()
    static let network = NetworkStatus(deviceOnline: true, relays: [:], waitingToSend: 0)
    static let toast = ToastPresenter()
}

struct ReaderMode {
    let twoPane: Bool
    let onShowReplies: () -> Void
}
