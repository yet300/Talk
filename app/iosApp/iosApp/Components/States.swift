import SwiftUI
import SharedLogic

/// The design's empty state (`x-empty`): an optional 48 pt icon, title, body, one action.
struct EmptyState<Action: View>: View {
    let title: String
    var body_: String? = nil
    var icon: SharedLogic.ImageResource? = nil
    @ViewBuilder var action: () -> Action

    init(_ title: String, body: String? = nil, icon: SharedLogic.ImageResource? = nil, @ViewBuilder action: @escaping () -> Action) {
        self.title = title
        self.body_ = body
        self.icon = icon
        self.action = action
    }

    var body: some View {
        VStack(spacing: TalkSpacing.md) {
            if let icon {
                TalkIcon(icon: icon, size: TalkSize.iconEmpty).foregroundStyle(TalkColor.textSecondary)
            }
            Text(title)
                .talkTextStyle(.headingLarge)
                .foregroundStyle(TalkColor.textPrimary)
                .multilineTextAlignment(.center)
            if let body_ {
                Text(body_)
                    .talkTextStyle(.body)
                    .foregroundStyle(TalkColor.textSecondary)
                    .multilineTextAlignment(.center)
            }
            action()
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, TalkSpacing.xxl)
        .padding(.vertical, TalkSpacing.xxxl)
        .accessibilityElement(children: .contain)
    }
}

extension EmptyState where Action == EmptyView {
    init(_ title: String, body: String? = nil, icon: SharedLogic.ImageResource? = nil) {
        self.init(title, body: body, icon: icon) { EmptyView() }
    }
}

/// Skeleton cards (`x-skel`): a list's first load. They breathe, unless Reduce Motion is on.
struct SkeletonCards: View {
    var count = 4
    @State private var dim = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        VStack(spacing: 0) {
            ForEach(0..<count, id: \.self) { _ in
                HStack(alignment: .top, spacing: TalkSpacing.md) {
                    Circle().fill(TalkColor.elevated).frame(width: TalkSize.avatarMd, height: TalkSize.avatarMd)
                    VStack(alignment: .leading, spacing: TalkSpacing.sm) {
                        ForEach([0.4, 0.8, 1, 0.92], id: \.self) { width in
                            GeometryReader { proxy in
                                RoundedRectangle(cornerRadius: 6).fill(TalkColor.elevated).frame(width: proxy.size.width * width)
                            }
                            .frame(height: 12)
                        }
                    }
                }
                .padding(.horizontal, TalkSpacing.lg)
                .padding(.vertical, TalkSpacing.md)
            }
        }
        .opacity(dim ? 0.55 : 1)
        .onAppear {
            guard !reduceMotion else { return }
            withAnimation(.easeInOut(duration: 0.9).repeatForever(autoreverses: true)) { dim = true }
        }
        .accessibilityElement()
        .accessibilityLabel(MR.strings.shared.state_loading.text)
    }
}

/// An error the user can dismiss (`t-alert-row`, the message in red), with Retry where it helps.
struct ErrorRow: View {
    let error: DomainError?
    var onDismiss: (() -> Void)?
    var onRetry: (() -> Void)? = nil

    var body: some View {
        if let error {
            HStack(spacing: TalkSpacing.sm) {
                Text(error.text)
                    .talkTextStyle(.body)
                    .foregroundStyle(TalkColor.error)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if let onRetry {
                    TalkButton(MR.strings.shared.action_retry.text, style: .secondary, action: onRetry)
                }
                if let onDismiss {
                    IconButton(icon: TalkIcons.close, label: MR.strings.shared.action_dismiss.text, tint: TalkColor.textSecondary, size: 18, action: onDismiss)
                }
            }
            .padding(.horizontal, TalkSpacing.lg)
            .padding(.vertical, TalkSpacing.sm)
            .transition(.opacity.combined(with: .move(edge: .top)))
            .onAppear { UIAccessibility.post(notification: .announcement, argument: error.text) }
        }
    }
}

/// A status line above a list (`t-notice`): offline, "public and not kept".
struct Notice: View {
    let icon: SharedLogic.ImageResource
    let text: String

    var body: some View {
        HStack(spacing: TalkSpacing.sm) {
            TalkIcon(icon: icon, size: 18).foregroundStyle(TalkColor.textSecondary)
            Text(text).talkTextStyle(.body).foregroundStyle(TalkColor.textSecondary)
            Spacer(minLength: 0)
        }
        .padding(TalkSpacing.md)
        .background(TalkColor.surface, in: RoundedRectangle(cornerRadius: TalkRadius.card))
        .padding(.horizontal, TalkSpacing.lg)
        .padding(.vertical, TalkSpacing.sm)
        .accessibilityElement(children: .combine)
    }
}

/// A caption line under a section or list (`t-caption`).
struct Caption: View {
    let text: String

    init(_ text: String) { self.text = text }

    var body: some View {
        Text(text)
            .talkTextStyle(.small)
            .foregroundStyle(TalkColor.textSecondary)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, TalkSpacing.lg)
            .padding(.vertical, TalkSpacing.sm)
    }
}

/// A section title in forms and lists (`t-section-title`).
struct SectionTitle: View {
    let text: String

    init(_ text: String) { self.text = text }

    var body: some View {
        Text(text)
            .talkTextStyle(.headingMedium)
            .foregroundStyle(TalkColor.textPrimary)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, TalkSpacing.lg)
            .padding(.top, TalkSpacing.lg)
            .padding(.bottom, TalkSpacing.sm)
            .accessibilityAddTraits(.isHeader)
    }
}

/// The hairline between rows.
struct Hairline: View {
    var body: some View {
        Rectangle().fill(TalkColor.border).frame(height: TalkSize.hairline)
    }
}

/// The offline bar on every screen (`NetworkBar`): offline, or every relay unreachable.
struct NetworkBar: View {
    @Environment(\.network) private var network

    var body: some View {
        if network.isOffline {
            HStack(spacing: TalkSpacing.sm) {
                TalkIcon(icon: TalkIcons.offline, size: 16)
                Text(network.deviceOnline ? MR.strings.shared.network_unreachable.text : MR.strings.shared.network_offline.text)
                    .talkTextStyle(.small)
                Spacer(minLength: 0)
            }
            .foregroundStyle(TalkColor.textPrimary)
            .padding(.horizontal, TalkSpacing.lg)
            .padding(.vertical, TalkSpacing.sm)
            .background(TalkColor.surface)
            .accessibilityElement(children: .combine)
            .accessibilityAddTraits(.updatesFrequently)
            .transition(.move(edge: .top).combined(with: .opacity))
        }
    }
}
