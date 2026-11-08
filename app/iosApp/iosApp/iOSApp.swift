import SwiftUI
import SharedLogic

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    var body: some Scene {
        WindowGroup {
            RootView(root: appDelegate.root)
                .environment(\.back) { [backDispatcher = appDelegate.backDispatcher] in _ = backDispatcher.back() }
        }
    }
}

/// Owns the app graph and the root component for the life of the process.
final class AppDelegate: NSObject, UIApplicationDelegate {
    private var stateKeeper = StateKeeperDispatcherKt.StateKeeperDispatcher(savedState: nil)
    private let graph = IosAppGraphKt.createIosAppGraph()

    /// Back events from the UI (the Duo side bar's Back in two panes) go through Decompose's dispatcher, so the
    /// topmost handler wins; the navigation stacks' own back and swipe tell the components directly.
    let backDispatcher = BackDispatcherKt.BackDispatcher()

    lazy var root: RootComponent = graph.rootComponentFactory.invoke(
        componentContext: DefaultComponentContext(
            lifecycle: ApplicationLifecycle(),
            stateKeeper: stateKeeper,
            instanceKeeper: nil,
            backHandler: backDispatcher
        ),
        deepLink: nil
    )

    func application(_ application: UIApplication, shouldSaveSecureApplicationState coder: NSCoder) -> Bool {
        StateKeeperUtilsKt.save(coder: coder, state: stateKeeper.save())
        return true
    }

    func application(_ application: UIApplication, shouldRestoreSecureApplicationState coder: NSCoder) -> Bool {
        stateKeeper = StateKeeperDispatcherKt.StateKeeperDispatcher(savedState: StateKeeperUtilsKt.restore(coder: coder))
            return true
    }
}
