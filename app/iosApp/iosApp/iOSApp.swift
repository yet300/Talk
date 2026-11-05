import SwiftUI
import SharedLogic

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    var body: some Scene {
        WindowGroup {
            RootView(root: appDelegate.root)
        }
    }
}

/// Owns the app graph and the root component for the life of the process.
final class AppDelegate: NSObject, UIApplicationDelegate {
    private let graph = IosAppGraphKt.createIosAppGraph()

    /// The swipe-back gesture of the UI phase's stack view sends its back events here.
    let backDispatcher = BackDispatcherKt.BackDispatcher()

    lazy var root: RootComponent = graph.rootComponentFactory.invoke(
        componentContext: DefaultComponentContext(
            lifecycle: ApplicationLifecycle(),
            stateKeeper: nil,
            instanceKeeper: nil,
            backHandler: backDispatcher
        ),
        deepLink: nil
    )
}
