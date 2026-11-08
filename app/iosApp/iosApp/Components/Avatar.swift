import SwiftUI
import SharedLogic

/// The user's picture over their initials on the elevated ground. Outside the viewer's network (decision 18) the
/// picture is blurred and a GIF shows its still frame, unless `shown`.
struct Avatar: View {
    let user: User?
    var size: CGFloat = TalkSize.avatarMd
    var shown = false
    var onTap: (() -> Void)? = nil

    @Environment(\.moderation) private var moderation

    var body: some View {
        let outside = user.map { !shown && !moderation.isInNetwork(id: $0.id) } ?? false
        let picture = user?.picture.flatMap { $0.isEmpty ? nil : $0 }.map { outside ? ProfileImageHosts.shared.still(url: $0) : $0 }
        let face = ZStack {
            Circle().fill(TalkColor.elevated)
            Text(user?.initials ?? "")
                .talkTextStyle(.small)
                .foregroundStyle(TalkColor.textPrimary)
            if let picture, let url = URL(string: picture) {
                RemoteImage(url: url)
                    .blur(radius: outside ? 8 : 0)
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())

        if let onTap {
            Button(action: onTap) { face }
                .buttonStyle(.plain)
                .accessibilityLabel(user?.displayLabel ?? "")
        } else {
            face.accessibilityHidden(true)
        }
    }
}

/// An image from the network, filling its frame (cropped), the surface colour until it arrives.
struct RemoteImage: View {
    let url: URL
    var contentMode: ContentMode = .fill

    var body: some View {
        AsyncImage(url: url, transaction: Transaction(animation: TalkMotion.fade)) { phase in
            switch phase {
            case .success(let image):
                image.resizable().aspectRatio(contentMode: contentMode)
            default:
                TalkColor.surface
            }
        }
    }
}

/// A chat's avatar: the person's, or two overlapping ones for a group.
struct ChatAvatar: View {
    let members: [User]
    var size: CGFloat = TalkSize.avatarMd

    var body: some View {
        if members.count > 1 {
            ZStack(alignment: .bottomTrailing) {
                Avatar(user: members[0], size: size * 0.72)
                    .frame(width: size, height: size, alignment: .topLeading)
                Avatar(user: members[1], size: size * 0.72)
                    .overlay(Circle().stroke(TalkColor.bg, lineWidth: 2))
            }
            .frame(width: size, height: size)
        } else {
            Avatar(user: members.first, size: size)
        }
    }
}
