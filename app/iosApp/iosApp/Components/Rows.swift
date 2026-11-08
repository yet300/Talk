import SwiftUI
import SharedLogic

/// A row (`t-row`): leading view, title with marks, caption, trailing view; highlighted when selected (list | detail).
struct ListRow<Leading: View, Marks: View, Trailing: View>: View {
    let title: String
    var caption: String? = nil
    var captionEmphasis = false
    var selected = false
    let action: (() -> Void)?
    @ViewBuilder var leading: () -> Leading
    @ViewBuilder var marks: () -> Marks
    @ViewBuilder var trailing: () -> Trailing

    var body: some View {
        let content = HStack(spacing: TalkSpacing.md) {
            leading()
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 4) {
                    Text(title)
                        .talkTextStyle(.bodyBold)
                        .foregroundStyle(TalkColor.textPrimary)
                        .lineLimit(1)
                    marks()
                }
                if let caption, !caption.isEmpty {
                    Text(caption)
                        .talkTextStyle(captionEmphasis ? .bodyBold : .body)
                        .foregroundStyle(captionEmphasis ? TalkColor.textPrimary : TalkColor.textSecondary)
                        .lineLimit(1)
                }
            }
            Spacer(minLength: 0)
            trailing()
        }
        .padding(.horizontal, TalkSpacing.lg)
        .padding(.vertical, TalkSpacing.md)
        .frame(minHeight: TalkSize.touchMin)
        .background(selected ? TalkColor.surface : .clear)
        .contentShape(Rectangle())

        if let action {
            Button(action: action) { content }
                .buttonStyle(RowPressStyle())
                .accessibilityAddTraits(selected ? .isSelected : [])
        } else {
            content
        }
    }
}

extension ListRow where Marks == EmptyView {
    init(title: String, caption: String? = nil, captionEmphasis: Bool = false, selected: Bool = false, action: (() -> Void)?,
         @ViewBuilder leading: @escaping () -> Leading, @ViewBuilder trailing: @escaping () -> Trailing) {
        self.init(title: title, caption: caption, captionEmphasis: captionEmphasis, selected: selected, action: action,
                  leading: leading, marks: { EmptyView() }, trailing: trailing)
    }
}

/// A person (`t-row` with an avatar): name and handle.
struct PersonRow<Trailing: View>: View {
    let user: User
    var caption: String? = nil
    var selected = false
    let action: () -> Void
    @ViewBuilder var trailing: () -> Trailing

    var body: some View {
        ListRow(title: user.displayLabel, caption: caption ?? user.handle, selected: selected, action: action) {
            Avatar(user: user)
        } trailing: {
            trailing()
        }
    }
}

extension PersonRow where Trailing == EmptyView {
    init(user: User, caption: String? = nil, selected: Bool = false, action: @escaping () -> Void) {
        self.init(user: user, caption: caption, selected: selected, action: action) { EmptyView() }
    }
}

/// A row's highlight on touch-down.
struct RowPressStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(configuration.isPressed ? TalkColor.postHover : .clear)
    }
}

/// A full-width action in a sheet or form (`t-menu-row`): icon, label, optional note; red when it destroys something.
struct ActionRow: View {
    let label: String
    var icon: SharedLogic.ImageResource? = nil
    var note: String? = nil
    var danger = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: TalkSpacing.lg) {
                if let icon { TalkIcon(icon: icon) }
                VStack(alignment: .leading, spacing: 2) {
                    Text(label).talkTextStyle(.bodyBold)
                    if let note {
                        Text(note).talkTextStyle(.small).foregroundStyle(TalkColor.textSecondary)
                    }
                }
                Spacer(minLength: 0)
            }
            .foregroundStyle(danger ? TalkColor.error : TalkColor.textPrimary)
            .padding(.horizontal, TalkSpacing.xl)
            .padding(.vertical, TalkSpacing.md)
            .frame(minHeight: TalkSize.touchMin + 8)
            .contentShape(Rectangle())
        }
        .buttonStyle(RowPressStyle())
    }
}
