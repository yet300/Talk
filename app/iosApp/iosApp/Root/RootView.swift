import SwiftUI
import SharedLogic

/// The root stack. Screens are placeholders until the UI phase replaces them.
struct RootView: View {
    private let root: RootComponent
    @StateValue private var stack: ChildStack<AnyObject, RootComponentChild>

    init(root: RootComponent) {
        self.root = root
        _stack = StateValue(root.stack)
    }

    var body: some View {
        ZStack {
            TalkColor.bg.ignoresSafeArea()
            Text(title(for: stack.active.instance))
                .talkTextStyle(.wordmark)
                .foregroundStyle(TalkColor.textPrimary)
        }
    }

    private func title(for child: RootComponentChild) -> String {
        let strings = MR.strings.shared
        switch child {
        case is RootComponentChild.Auth: return strings.auth_title.text
        case is RootComponentChild.Main: return strings.tab_home.text
        case is RootComponentChild.Revisions: return strings.revisions_title.text
        case is RootComponentChild.Compose: return strings.compose_new.text
        case is RootComponentChild.Settings: return strings.settings_title.text
        default: return strings.app_name.text
        }
    }
}
