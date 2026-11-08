import SwiftUI
import SharedLogic

/// Report (NIP-56): the public-report note and the reasons; a reason sends the report and confirms with the toast.
struct ReportSheet: View {
    let target: ReportTarget
    let onDone: () -> Void

    @Environment(\.moderationActions) private var moderation
    @Environment(\.toast) private var toast

    var body: some View {
        let strings = MR.strings.shared
        VStack(alignment: .leading, spacing: 0) {
            Text(strings.report_title.text)
                .talkTextStyle(.headingMedium)
                .foregroundStyle(TalkColor.textPrimary)
                .padding(.horizontal, TalkSpacing.xl)
                .padding(.top, TalkSpacing.xl)
                .accessibilityAddTraits(.isHeader)
            Text(strings.report_note.text)
                .talkTextStyle(.small)
                .foregroundStyle(TalkColor.textSecondary)
                .padding(.horizontal, TalkSpacing.xl)
                .padding(.vertical, TalkSpacing.sm)
            ForEach(ReportReason.allCases, id: \.self) { reason in
                ActionRow(label: label(reason)) {
                    moderation?.report(target: target, reason: reason)
                    toast.show(strings.toast_reported.text)
                    onDone()
                }
            }
            Spacer(minLength: 0)
        }
        .presentationDetents([.medium])
        .presentationDragIndicator(.visible)
    }

    private func label(_ reason: ReportReason) -> String {
        let strings = MR.strings.shared
        switch reason {
        case .nudity: return strings.report_nudity.text
        case .spam: return strings.report_spam.text
        case .illegal: return strings.report_illegal.text
        case .impersonation: return strings.report_impersonation.text
        case .other: return strings.report_other.text
        }
    }
}
