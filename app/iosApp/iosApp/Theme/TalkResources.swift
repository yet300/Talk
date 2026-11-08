import SwiftUI
import SharedLogic

extension StringResource {
    /// The string in the current locale: `MR.strings.shared.app_name.text`.
    var text: String { desc().localized() }

    /// The string with format arguments: `MR.strings.shared.post_read_time.format(6)`.
    func format(_ args: Any...) -> String { format(args: args).localized() }
}

extension PluralsResource {
    /// The plural for `count`, with `count` as its argument.
    func format(count: Int) -> String {
        format(number: Int32(count), args: [count]).localized()
    }
}

extension DomainError {
    /// The user-facing message for an error from the domain layer.
    var text: String { toStringDesc().localized() }
}

/// The design's 24×24 stroke icon set (moko `images/ic_*.svg`). Filled variants mark an active state.
enum TalkIcons {
    private static let mr = MR.images.shared
    static let activity = mr.ic_activity
    static let back = mr.ic_back
    static let block = mr.ic_block
    static let bookmark = mr.ic_bookmark
    static let bookmarkFilled = mr.ic_bookmark_filled
    static let chat = mr.ic_chat
    static let check = mr.ic_check
    static let chevron = mr.ic_chevron
    static let clock = mr.ic_clock
    static let close = mr.ic_close
    static let compose = mr.ic_compose
    static let copy = mr.ic_copy
    static let eyeOff = mr.ic_eye_off
    static let fork = mr.ic_fork
    static let github = mr.ic_github
    static let globe = mr.ic_globe
    static let history = mr.ic_history
    static let home = mr.ic_home
    static let image = mr.ic_image
    static let info = mr.ic_info
    static let key = mr.ic_key
    static let like = mr.ic_like
    static let likeFilled = mr.ic_like_filled
    static let link = mr.ic_link
    static let lock = mr.ic_lock
    static let more = mr.ic_more
    static let mute = mr.ic_mute
    static let offline = mr.ic_offline
    static let pin = mr.ic_pin
    static let play = mr.ic_play
    static let plus = mr.ic_plus
    static let qr = mr.ic_qr
    static let relay = mr.ic_relay
    static let reply = mr.ic_reply
    static let report = mr.ic_report
    static let repost = mr.ic_repost
    static let search = mr.ic_search
    static let send = mr.ic_send
    static let settings = mr.ic_settings
    static let share = mr.ic_share
    static let shield = mr.ic_shield
    static let star = mr.ic_star
    static let teleport = mr.ic_teleport
    static let timer = mr.ic_timer
    static let trash = mr.ic_trash
    static let user = mr.ic_user
    static let users = mr.ic_users
    static let video = mr.ic_video
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

extension String {
    /// `npub1w7r…4xze`: the prefix and four characters on each side.
    func shortNpub() -> String { UserTextKt.shortNpub(self) }
}
