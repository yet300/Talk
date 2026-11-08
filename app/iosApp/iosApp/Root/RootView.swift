import SwiftUI
import SharedLogic

/// The root (`RootComponent.stack`). Splash, Auth, Lock and Main swap with a crossfade; Compose, Settings, Revisions
/// and Edit profile are pushed over Main by its navigation stack (`MainView`), so they get the system back button and
/// the interactive swipe back. The root also provides what every screen reads (moderation, post cards, the offline
/// state, the toast), applies the theme, and puts the device lock and the app-switcher cover over everything.
struct RootView: View {
    private let root: RootComponent
    @StateValue private var stack: ChildStack<AnyObject, RootComponentChild>
    @StateValue private var moderation: Moderation
    @StateValue private var network: NetworkStatus
    @StateValue private var privacy: Privacy
    @StateObject private var theme: ThemeObserver
    @StateObject private var toast = ToastPresenter()
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    init(root: RootComponent) {
        self.root = root
        _stack = StateValue(root.stack)
        _moderation = StateValue(root.moderation.model)
        _network = StateValue(root.network)
        _privacy = StateValue(root.privacy)
        _theme = StateObject(wrappedValue: ThemeObserver(root.theme))
    }

    var body: some View {
        let signedIn = stack.items.contains { $0.instance is RootComponentChild.Main }
        let base = stack.items.first { Self.isTopLevel($0.instance) }?.instance ?? stack.active.instance
        ZStack {
            TalkColor.bg.ignoresSafeArea()
            topLevel(base)
                .id(ObjectIdentifier(base))
                .transition(.opacity)
        }
        .animation(reduceMotion ? nil : TalkMotion.fade, value: ObjectIdentifier(base))
        .environment(\.moderation, moderation)
        .environment(\.moderationActions, root.moderation)
        .environment(\.postCards, root.postCards)
        .environment(\.network, network)
        .environment(\.hideInScreenshots, privacy.hideInScreenshots)
        .modifier(ToastHost(presenter: toast))
        .modifier(DeviceLockGate(enabled: privacy.deviceLock && signedIn))
        .modifier(AppSwitcherCover(enabled: privacy.hideInScreenshots && signedIn))
        .onChange(of: theme.mode, initial: true) { _, mode in Self.apply(mode) }
    }

    @ViewBuilder
    private func topLevel(_ child: RootComponentChild) -> some View {
        switch onEnum(of: child) {
        case .splash:
            Text(MR.strings.shared.app_name.text)
                .talkTextStyle(.wordmark)
                .foregroundStyle(TalkColor.textPrimary)
        case .auth(let auth):
            AuthView(auth.component)
        case .lock(let lock):
            // Talk's own passcode is the web's (NIP-49); iOS locks with the device, so this shows only for an
            // account carried over with a passcode.
            LockView(lock.component)
        case .main(let main):
            MainView(root: root, main: main.component)
        case .revisions, .compose, .settings, .editProfile:
            // Destinations above Main are pushed by MainView; they never stand alone.
            EmptyView()
        }
    }

    /// Destinations that replace each other rather than stack.
    static func isTopLevel(_ child: RootComponentChild) -> Bool {
        child is RootComponentChild.Splash || child is RootComponentChild.Auth || child is RootComponentChild.Lock || child is RootComponentChild.Main
    }

    /// The theme chosen in Settings, on every window (sheets and alerts included); System follows the device.
    private static func apply(_ mode: ThemeMode) {
        let style: UIUserInterfaceStyle
        switch mode {
        case .system: style = .unspecified
        case .light: style = .light
        case .dark: style = .dark
        }
        for scene in UIApplication.shared.connectedScenes {
            (scene as? UIWindowScene)?.windows.forEach { $0.overrideUserInterfaceStyle = style }
        }
    }
}

/// `root.theme` as a Swift enum.
final class ThemeObserver: ObservableObject {
    @Published private(set) var mode: ThemeMode
    private var cancellation: Cancellation?

    init(_ value: Value<__ThemeMode>) {
        mode = ThemeMode.from(value.value)
        cancellation = value.subscribe { [weak self] in self?.mode = ThemeMode.from($0) }
    }

    deinit { cancellation?.cancel() }
}

private extension ThemeMode {
    static func from(_ kotlin: __ThemeMode) -> ThemeMode { kotlin as ThemeMode }
}
