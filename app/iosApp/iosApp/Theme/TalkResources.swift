import SwiftUI
import SharedLogic

extension StringResource {
    /// The string in the current locale: `MR.strings.shared.app_name.text`.
    var text: String { desc().localized() }

    /// The string with format arguments: `MR.strings.shared.post_read_time.format(6)`.
    func format(_ args: Any...) -> String { format(args_: args).localized() }
}

extension PluralsResource {
    /// The plural for `count`, with `count` as its argument.
    func format(count: Int) -> String {
        format(number: Int32(count), args_: [count]).localized()
    }
}

extension DomainError {
    /// The user-facing message for an error from the domain layer.
    var text: String { toStringDesc().localized() }
}

/// The design's 24×24 stroke icon set (moko `images/ic_*.svg`). Filled variants mark an active state.
enum TalkIcons {
    private static let mr = MR.images.shared
    static let home = mr.ic_home
    static let search = mr.ic_search
    static let activity = mr.ic_activity
    static let bookmark = mr.ic_bookmark
    static let bookmarkFilled = mr.ic_bookmark_filled
    static let user = mr.ic_user
    static let compose = mr.ic_compose
    static let reply = mr.ic_reply
    static let repost = mr.ic_repost
    static let like = mr.ic_like
    static let likeFilled = mr.ic_like_filled
    static let share = mr.ic_share
    static let history = mr.ic_history
    static let back = mr.ic_back
    static let more = mr.ic_more
    static let settings = mr.ic_settings
    static let close = mr.ic_close
    static let relay = mr.ic_relay
    static let key = mr.ic_key
    static let copy = mr.ic_copy
    static let check = mr.ic_check
    static let plus = mr.ic_plus
    static let offline = mr.ic_offline
    static let image = mr.ic_image
    static let trash = mr.ic_trash
}

/// One icon, tinted with the foreground style; pass `label` unless it is decorative.
struct TalkIcon: View {
    let icon: SharedLogic.ImageResource
    var size: CGFloat = TalkSize.iconNav
    var label: String? = nil

    var body: some View {
        let image = Image(uiImage: icon.toUIImage() ?? UIImage())
            .renderingMode(.template)
            .resizable()
            .scaledToFit()
            .frame(width: size, height: size)
        if let label {
            image.accessibilityLabel(label)
        } else {
            image.accessibilityHidden(true)
        }
    }
}
