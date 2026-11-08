import SwiftUI
import UIKit
import LocalAuthentication
import OSLog
import SharedLogic

/// Whether the device lock was passed in this run of Talk; going to the background clears it. Settings marks it
/// unlocked after its own check, so turning the lock on does not lock the person out of the screen they are on.
final class LockSession: ObservableObject {
    static let shared = LockSession()
    @Published var unlocked = false
    private var authenticating = false

    /// Face ID, Touch ID or the device passcode, whatever the device has; `done` runs on the main thread.
    func authenticate(_ done: ((Bool) -> Void)? = nil) {
        guard !authenticating else { return }
        authenticating = true
        LAContext().evaluatePolicy(.deviceOwnerAuthentication, localizedReason: MR.strings.shared.lock_title.text) { success, _ in
            DispatchQueue.main.async {
                self.authenticating = false
                if success { self.unlocked = true }
                done?(success)
            }
        }
    }

    /// The device has a passcode (and so maybe biometrics): the only case where locking cannot lock anybody out.
    static var deviceHasPasscode: Bool {
        LAContext().canEvaluatePolicy(.deviceOwnerAuthentication, error: nil)
    }
}

/// Opens Talk with the device lock (Settings, Security): Face ID, Touch ID or the device passcode. It applies only while
/// the device has a passcode, so nobody is locked out, and it locks again when Talk goes to the background — not when
/// it only goes inactive (rotation, the photo picker, the Face ID prompt itself, Control Center). The content stays
/// alive under the cover, so nothing is lost, but VoiceOver cannot reach it.
struct DeviceLockGate: ViewModifier {
    let enabled: Bool
    @ObservedObject private var session = LockSession.shared
    @State private var hasPasscode = LockSession.deviceHasPasscode
    @Environment(\.scenePhase) private var scenePhase

    func body(content: Content) -> some View {
        let locked = enabled && hasPasscode && !session.unlocked
        content
            .accessibilityHidden(locked)
            .overlay {
                if locked {
                    DeviceLockScreen { session.authenticate() }
                        .transition(.opacity)
                        .task { if scenePhase == .active { session.authenticate() } }
                }
            }
            .animation(TalkMotion.fade, value: locked)
            .onChange(of: scenePhase) { _, phase in
                switch phase {
                case .background: session.unlocked = false
                case .active:
                    hasPasscode = LockSession.deviceHasPasscode
                    // Back in front and still locked: ask at once. The cover's own `.task` ran while Talk was in the
                    // background (that is when it locks again), when no prompt can show.
                    if enabled && hasPasscode && !session.unlocked { session.authenticate() }
                default: break
                }
            }
    }
}

/// "Talk is locked" over everything, with Unlock (Face ID, Touch ID or the passcode, whatever the device has).
private struct DeviceLockScreen: View {
    let onUnlock: () -> Void

    var body: some View {
        let strings = MR.strings.shared
        ZStack {
            TalkColor.bg.ignoresSafeArea()
            VStack(spacing: TalkSpacing.lg) {
                TalkIcon(icon: TalkIcons.lock, size: TalkSize.iconEmpty).foregroundStyle(TalkColor.textSecondary)
                Text(strings.lock_title.text).talkTextStyle(.headingLarge).foregroundStyle(TalkColor.textPrimary)
                Text(strings.lock_device_body_ios.text)
                    .talkTextStyle(.body)
                    .foregroundStyle(TalkColor.textSecondary)
                    .multilineTextAlignment(.center)
                TalkButton(strings.lock_unlock.text, action: onUnlock)
            }
            .padding(TalkSpacing.xxl)
            .frame(maxWidth: 420)
        }
        .accessibilityElement(children: .contain)
        .accessibilityAddTraits(.isModal)
    }
}

/// Covers Talk in the app switcher (and whenever it is not in front) while Hide in screenshots is on, so chats and
/// keys do not show in the snapshot iOS keeps.
struct AppSwitcherCover: ViewModifier {
    let enabled: Bool
    @Environment(\.scenePhase) private var scenePhase

    func body(content: Content) -> some View {
        content.overlay {
            if enabled && scenePhase != .active {
                ZStack {
                    Rectangle().fill(.ultraThickMaterial).ignoresSafeArea()
                    Text(MR.strings.shared.app_name.text).talkTextStyle(.wordmark).foregroundStyle(TalkColor.textPrimary)
                }
                .accessibilityHidden(true)
            }
        }
    }
}

/// Keeps its content out of screenshots and screen recordings where iOS allows it: the content is drawn in the layer
/// iOS blanks for secure text entry. While the screen is being recorded or mirrored, it is covered as well. Used for
/// the private key and for chat messages when Hide in screenshots is on.
struct ScreenCaptureProtected<Content: View>: View {
    let enabled: Bool
    @ViewBuilder let content: () -> Content
    @State private var captured = Self.isCaptured

    var body: some View {
        Group {
            if enabled {
                SecureLayer(content: content())
                    .overlay {
                        if captured {
                            Rectangle().fill(TalkColor.bg)
                        }
                    }
            } else {
                content()
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: UIScreen.capturedDidChangeNotification)) { note in
            captured = Self.isCaptured
        }
    }

    /// The screen is being recorded, mirrored or shared.
    private static var isCaptured: Bool {
        UIApplication.shared.connectedScenes.contains { ($0 as? UIWindowScene)?.traitCollection.sceneCaptureState == .active }
    }
}

/// Hosts SwiftUI content inside a secure text field's canvas, which the system leaves out of captures.
///
/// iOS has no public API for this; the canvas is found by its class name first (`…CanvasView`), then as the field's
/// only drawing subview, after the field has laid itself out. If neither is there (a future iOS), the content is shown
/// unprotected and a fault is logged (visible in Console), so the change is noticed rather than silently shipped. The
/// hosting controller is attached to the screen's view controller, so sheets, menus and focus inside work.
private struct SecureLayer<Content: View>: UIViewRepresentable {
    let content: Content

    func makeUIView(context: Context) -> HostContainer {
        let host = UIHostingController(rootView: content)
        host.view.backgroundColor = .clear
        let container = HostContainer(host: host)
        let field = UITextField()
        field.isSecureTextEntry = true
        field.isUserInteractionEnabled = false
        field.layoutIfNeeded()
        if let canvas = Self.secureCanvas(in: field) {
            canvas.subviews.forEach { $0.removeFromSuperview() }
            canvas.isUserInteractionEnabled = true
            container.addSubview(canvas)
            pin(canvas, to: container)
            canvas.addSubview(host.view)
            pin(host.view, to: canvas)
        } else {
            Self.log.fault("The secure text canvas was not found: content is not protected from screen capture")
            container.addSubview(host.view)
            pin(host.view, to: container)
        }
        return container
    }

    func updateUIView(_ view: HostContainer, context: Context) {
        view.host.rootView = content
    }

    private static var log: Logger { Logger(subsystem: "com.yet.talk", category: "privacy") }

    private static func secureCanvas(in field: UITextField) -> UIView? {
        field.subviews.first { String(describing: type(of: $0)).contains("CanvasView") }
            ?? (field.subviews.count == 1 ? field.subviews.first : nil)
    }

    /// Adds its hosting controller to the nearest view controller once it is in a window.
    final class HostContainer: UIView {
        let host: UIHostingController<Content>

        init(host: UIHostingController<Content>) {
            self.host = host
            super.init(frame: .zero)
        }

        @available(*, unavailable)
        required init?(coder: NSCoder) { fatalError("init(coder:) is not used") }

        override func didMoveToWindow() {
            super.didMoveToWindow()
            if window == nil {
                host.willMove(toParent: nil)
                host.removeFromParent()
            } else if host.parent == nil, let parent = parentViewController {
                parent.addChild(host)
                host.didMove(toParent: parent)
            }
        }

        private var parentViewController: UIViewController? {
            var responder: UIResponder? = next
            while let current = responder {
                if let controller = current as? UIViewController { return controller }
                responder = current.next
            }
            return nil
        }
    }

    private func pin(_ view: UIView, to parent: UIView) {
        view.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            view.leadingAnchor.constraint(equalTo: parent.leadingAnchor),
            view.trailingAnchor.constraint(equalTo: parent.trailingAnchor),
            view.topAnchor.constraint(equalTo: parent.topAnchor),
            view.bottomAnchor.constraint(equalTo: parent.bottomAnchor),
        ])
    }
}
