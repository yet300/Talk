import SwiftUI
import PhotosUI
import SharedLogic

/// A conversation (`Conversation`, `Chat-Mobile`): the bar with the person or group (their info behind it), the
/// messages from the bottom up with day dividers, and the composer with the reply bar and Add image, or the request
/// bar (Accept, Delete, Block) until a request is accepted. A tap on a message opens its sheet; a long press its menu.
/// Kept out of screenshots and recordings while Hide in screenshots is on. No read receipts, no typing indicator.
struct ConversationView: View {
    private let component: ConversationComponent
    @StateValue private var model: ConversationComponentModel
    @State private var actionsFor: ChatMessage?
    @State private var viewing: Data?
    @State private var blocking = false
    @State private var photo: PhotosPickerItem?
    @FocusState private var inputFocused: Bool
    @Environment(\.hideInScreenshots) private var hideInScreenshots
    @Environment(\.network) private var network
    @Environment(\.toast) private var toast

    init(_ component: ConversationComponent) {
        self.component = component
        _model = StateValue(component.model)
    }

    var body: some View {
        let strings = MR.strings.shared
        let title = chatTitle(name: model.name, members: model.members)
        VStack(spacing: 0) {
            ScreenCaptureProtected(enabled: hideInScreenshots) {
                MessageList(model: model, onActions: { actionsFor = $0 }, onShowImage: component.onShowImage,
                            onOpenImage: { viewing = $0 }, onRetry: component.onRetry, onReact: component.onReact)
            }
            ErrorRow(error: model.error, onDismiss: component.onErrorShown)
            if network.isOffline {
                Notice(icon: TalkIcons.offline, text: strings.chat_offline.text)
            } else if model.canReceive?.boolValue == false {
                Notice(icon: TalkIcons.info, text: strings.chat_member_cannot_receive.format(title))
            }
            if model.isRequest {
                requestBar(title)
            } else {
                composer
            }
        }
        .background(TalkColor.bg.ignoresSafeArea())
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) {
                Button(action: component.onOpenInfo) {
                    HStack(spacing: TalkSpacing.sm) {
                        ChatAvatar(members: model.members, size: 32)
                        VStack(alignment: .leading, spacing: 0) {
                            HStack(spacing: 4) {
                                Text(title).talkTextStyle(.bodyBold).foregroundStyle(TalkColor.textPrimary).lineLimit(1)
                                if model.isVerified { TalkIcon(icon: TalkIcons.check, size: 16).foregroundStyle(TalkColor.accent) }
                            }
                            Text(subtitle).talkTextStyle(.small).foregroundStyle(TalkColor.textSecondary).lineLimit(1)
                        }
                    }
                }
                .buttonStyle(.plain)
                .accessibilityLabel("\(title), \(strings.chat_info.text)")
            }
            ToolbarItem(placement: .primaryAction) {
                ToolbarIconButton(icon: TalkIcons.info, label: strings.chat_info.text, action: component.onOpenInfo)
            }
        }
        .sheet(item: Binding(get: { actionsFor.map(SheetMessage.init) }, set: { actionsFor = $0?.message })) { item in
            MessageSheet(message: item.message, reactions: model.reactions, onReply: {
                component.onReply(message: $0)
                inputFocused = true
            }, onReact: component.onReact, onCopy: { copyText($0.content, toast: toast, confirmation: strings.action_copied.text) },
               onDeleteForEveryone: component.onDeleteForEveryone, onDeleteForMe: component.onDeleteForMe) { actionsFor = nil }
        }
        .fullScreenCover(item: Binding(get: { viewing.map(ViewedData.init) }, set: { viewing = $0?.data })) { item in
            DataImageViewer(data: item.data) { viewing = nil }
        }
        .alert(strings.chat_block_title.format(title), isPresented: $blocking) {
            Button(strings.action_block.text, role: .destructive, action: component.onBlock)
            Button(strings.action_cancel.text, role: .cancel) {}
        } message: {
            Text(strings.chat_block_body.text)
        }
        .onChange(of: photo) { _, item in
            guard let item else { return }
            photo = nil
            Task {
                guard let data = try? await item.loadTransferable(type: Data.self) else { return }
                let mime = item.supportedContentTypes.first?.preferredMIMEType ?? "image/jpeg"
                await MainActor.run { component.onSendImage(file: BytesKt.imageFile(data: data, mimeType: mime)) }
            }
        }
    }

    private var subtitle: String {
        let strings = MR.strings.shared
        if model.isGroup { return strings.chat_you_and.format(model.members.map(\.displayLabel).joined(separator: ", ")) }
        if model.disappearing != nil { return strings.chat_disappearing_on.text }
        return model.members.first?.handle ?? ""
    }

    private var composer: some View {
        let strings = MR.strings.shared
        let off = model.canReceive?.boolValue == false
        return VStack(spacing: 0) {
            if let reply = model.replyingTo {
                HStack(spacing: TalkSpacing.sm) {
                    TalkIcon(icon: TalkIcons.reply, size: 18).foregroundStyle(TalkColor.accent)
                    VStack(alignment: .leading, spacing: 0) {
                        Text(strings.chat_replying_to.format(reply.isMine ? you : name(of: reply.authorKey)))
                            .talkTextStyle(.small).foregroundStyle(TalkColor.textSecondary)
                        Text(reply.content.isEmpty ? strings.chat_photo.text : reply.content)
                            .talkTextStyle(.body).foregroundStyle(TalkColor.textPrimary).lineLimit(1)
                    }
                    Spacer(minLength: 0)
                    IconButton(icon: TalkIcons.close, label: strings.chat_cancel_reply.text, tint: TalkColor.textSecondary, size: 18) {
                        component.onReply(message: nil)
                    }
                }
                .padding(.horizontal, TalkSpacing.lg)
                .padding(.vertical, TalkSpacing.sm)
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }
            if model.isUploading {
                HStack(spacing: TalkSpacing.sm) {
                    ProgressView().tint(TalkColor.accent)
                    Text(strings.chat_encrypting.text).talkTextStyle(.small).foregroundStyle(TalkColor.textSecondary)
                    Spacer()
                }
                .padding(.horizontal, TalkSpacing.lg)
            }
            HStack(spacing: TalkSpacing.xs) {
                PhotosPicker(selection: $photo, matching: .images) {
                    TalkIcon(icon: TalkIcons.image)
                        .foregroundStyle(TalkColor.textPrimary)
                        .frame(width: TalkSize.touchMin, height: TalkSize.touchMin)
                }
                .disabled(off || model.isUploading)
                .accessibilityLabel(strings.chat_add_image.text)
                TalkTextField(placeholder: strings.chat_message.text, text: model.text, onChange: component.onTextChanged,
                              pill: true, multiline: true, enabled: !off, submitLabel: .send) {
                    if model.canSend { component.onSend() }
                }
                .focused($inputFocused)
                IconButton(icon: TalkIcons.send, label: strings.chat_send.text, tint: TalkColor.accent, enabled: model.canSend, action: component.onSend)
                    .sensoryFeedback(.impact(weight: .light), trigger: model.messages.count)
            }
            .padding(.horizontal, TalkSpacing.sm)
            .padding(.vertical, TalkSpacing.sm)
        }
        .background(TalkColor.bg)
        .animation(TalkMotion.move, value: model.replyingTo?.id)
    }

    private func requestBar(_ name: String) -> some View {
        let strings = MR.strings.shared
        return VStack(alignment: .leading, spacing: TalkSpacing.md) {
            Text(strings.chat_request_note.format(name)).talkTextStyle(.body).foregroundStyle(TalkColor.textSecondary)
            HStack(spacing: TalkSpacing.sm) {
                TalkButton(strings.action_accept.text, action: component.onAccept)
                TalkButton(strings.action_delete.text, style: .secondary, action: component.onDelete)
                TalkButton(strings.action_block.text, style: .secondary) { blocking = true }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(TalkSpacing.lg)
        .background(TalkColor.bg)
    }

    private var you: String { MR.strings.shared.chats_you.format("").trimmingCharacters(in: CharacterSet(charactersIn: " :")) }

    private func name(of key: String) -> String {
        model.members.first { $0.key == key }?.displayLabel ?? ""
    }
}

private struct SheetMessage: Identifiable {
    let message: ChatMessage
    var id: String { message.id }
}

private struct ViewedData: Identifiable {
    let data: Data
    var id: Int { data.hashValue }
}

/// One row of the conversation: a day divider, or a message with where it sits in its sender's run.
private enum MessageRow: Identifiable {
    case day(Date)
    case message(ChatMessage, firstOfRun: Bool, lastOfRun: Bool)

    var id: String {
        switch self {
        case .day(let date): return "day-\(date.timeIntervalSince1970)"
        case .message(let message, _, _): return message.id
        }
    }

    /// Groups messages by day and by sender run (a gap before a new sender, the tail on the last of a run).
    static func rows(_ messages: [ChatMessage]) -> [MessageRow] {
        let calendar = Calendar.current
        func day(_ message: ChatMessage) -> Date {
            calendar.startOfDay(for: Date(timeIntervalSince1970: Double(message.createdAt.toEpochMilliseconds()) / 1000))
        }
        var rows: [MessageRow] = []
        for (index, message) in messages.enumerated() {
            let today = day(message)
            let previous = index > 0 ? messages[index - 1] : nil
            let next = index + 1 < messages.count ? messages[index + 1] : nil
            let newDay = previous.map { day($0) != today } ?? true
            if newDay { rows.append(.day(today)) }
            let samePrevious = !newDay && previous?.authorKey == message.authorKey
            let sameNext = next.map { $0.authorKey == message.authorKey && day($0) == today } ?? false
            rows.append(.message(message, firstOfRun: !samePrevious, lastOfRun: !sameNext))
        }
        return rows
    }
}

/// The messages, newest at the bottom, staying there as new ones arrive.
private struct MessageList: View {
    let model: ConversationComponentModel
    let onActions: (ChatMessage) -> Void
    let onShowImage: (ChatMessage) -> Void
    let onOpenImage: (Data) -> Void
    let onRetry: (ChatMessage) -> Void
    let onReact: (ChatMessage, String) -> Void

    var body: some View {
        let names = Dictionary(model.members.map { ($0.key, $0.displayLabel) }, uniquingKeysWith: { first, _ in first })
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(spacing: 0) {
                    ForEach(MessageRow.rows(model.messages)) { row in
                        switch row {
                        case .day(let date):
                            Text(Self.dayLabel(date))
                                .talkTextStyle(.small)
                                .foregroundStyle(TalkColor.textSecondary)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, TalkSpacing.md)
                        case .message(let message, let first, let last):
                            MessageBubble(message: message, firstOfRun: first, lastOfRun: last, isGroup: model.isGroup,
                                          names: names, image: model.images[message.id], reactions: model.reactions,
                                          onActions: { onActions(message) }, onShowImage: { onShowImage(message) },
                                          onOpenImage: onOpenImage, onRetry: { onRetry(message) }, onReact: { onReact(message, $0) })
                        }
                    }
                    Color.clear.frame(height: 1).id("end")
                }
                .padding(.vertical, TalkSpacing.sm)
            }
            .defaultScrollAnchor(.bottom)
            .scrollDismissesKeyboard(.interactively)
            .onChange(of: model.messages.last?.id) { _, _ in
                withAnimation(TalkMotion.move) { proxy.scrollTo("end", anchor: .bottom) }
            }
        }
    }

    /// "Today", "Yesterday", or the date.
    static func dayLabel(_ date: Date) -> String {
        let calendar = Calendar.current
        if calendar.isDateInToday(date) { return MR.strings.shared.chat_today.text }
        if calendar.isDateInYesterday(date) { return MR.strings.shared.chat_yesterday.text }
        return date.formatted(date: .abbreviated, time: .omitted)
    }
}

/// A message (`t-msg`): yours on the right in `bubble-out` (decision 22), theirs on the left; the sender's name in
/// groups; the reply quote, image and reactions inside; Sending… or Not sent with Retry under it.
private struct MessageBubble: View {
    let message: ChatMessage
    let firstOfRun: Bool
    let lastOfRun: Bool
    let isGroup: Bool
    let names: [String: String]
    let image: ConversationComponentImageState?
    let reactions: [String]
    let onActions: () -> Void
    let onShowImage: () -> Void
    let onOpenImage: (Data) -> Void
    let onRetry: () -> Void
    let onReact: (String) -> Void

    var body: some View {
        let strings = MR.strings.shared
        let mine = message.isMine
        let shape = UnevenRoundedRectangle(
            topLeadingRadius: 18, bottomLeadingRadius: !mine && lastOfRun ? 4 : 18,
            bottomTrailingRadius: mine && lastOfRun ? 4 : 18, topTrailingRadius: 18)
        VStack(alignment: mine ? .trailing : .leading, spacing: 2) {
            if isGroup && !mine && firstOfRun {
                Text(names[message.authorKey] ?? "").talkTextStyle(.small).foregroundStyle(TalkColor.textSecondary).padding(.leading, 12)
            }
            Group {
                if message.isExpired {
                    Text(strings.chat_expired.text)
                        .talkTextStyle(.body)
                        .foregroundStyle(TalkColor.textSecondary)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                        .background(TalkColor.surface, in: shape)
                } else if message.image != nil {
                    imageView(shape)
                        .onAppear(perform: onShowImage)
                } else {
                    textBubble(shape)
                        .onTapGesture(perform: onActions)
                }
            }
            .contextMenu { if !message.isExpired { menu } }
            if !message.reactions.isEmpty {
                HStack(spacing: 4) {
                    ForEach(message.reactions, id: \.emoji) { reaction in
                        Text(reaction.count > 1 ? "\(reaction.emoji) \(reaction.count)" : reaction.emoji)
                            .talkTextStyle(.small)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 2)
                            .background(reaction.isMine ? TalkColor.tintBlue : TalkColor.surface, in: Capsule())
                    }
                }
            }
            switch message.publishState {
            case .pending:
                Text(strings.post_pending.text).talkTextStyle(.tiny).foregroundStyle(TalkColor.textSecondary)
            case .failed:
                HStack(spacing: 8) {
                    Text(strings.post_failed.text).talkTextStyle(.small).foregroundStyle(TalkColor.error)
                    TalkButton(strings.action_retry.text, style: .secondary, action: onRetry)
                }
            case .published:
                EmptyView()
            }
        }
        .frame(maxWidth: .infinity, alignment: mine ? .trailing : .leading)
        .padding(.horizontal, TalkSpacing.md)
        .padding(.top, firstOfRun ? 10 : 2)
        .accessibilityElement(children: .combine)
        .accessibilityAction(named: Text(strings.chat_message_actions.text), onActions)
    }

    private func textBubble(_ shape: UnevenRoundedRectangle) -> some View {
        let strings = MR.strings.shared
        let mine = message.isMine
        return VStack(alignment: .leading, spacing: 6) {
            if let quote = message.replyTo {
                VStack(alignment: .leading, spacing: 0) {
                    if quote.text != nil || quote.isImage {
                        Text(names[quote.authorKey] ?? strings.chats_you.format("").trimmingCharacters(in: CharacterSet(charactersIn: " :")))
                            .talkTextStyle(.small)
                        Text(quote.text ?? strings.chat_photo.text).talkTextStyle(.small).opacity(0.85).lineLimit(2)
                    } else {
                        Text(strings.chat_reply_unavailable.text).talkTextStyle(.small)
                    }
                }
                .padding(8)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.black.opacity(0.12), in: RoundedRectangle(cornerRadius: 8))
            }
            Text(message.content).talkTextStyle(.body)
        }
        .foregroundStyle(mine ? Color.white : TalkColor.textPrimary)
        .padding(.horizontal, 14)
        .padding(.vertical, 8)
        .background(mine ? TalkColor.bubbleOut : TalkColor.bubbleTheirs, in: shape)
        .frame(maxWidth: 300, alignment: mine ? .trailing : .leading)
    }

    @ViewBuilder
    private func imageView(_ shape: UnevenRoundedRectangle) -> some View {
        let strings = MR.strings.shared
        let box = RoundedRectangle(cornerRadius: 18)
        if let loaded = image as? ConversationComponentImageStateLoaded {
            let data = BytesKt.toData(bytes: loaded.bytes)
            if let picture = UIImage(data: data) {
                Button { onOpenImage(data) } label: {
                    Image(uiImage: picture).resizable().scaledToFill().frame(width: 220, height: 220).clipShape(shape)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(strings.chat_open_image.text)
            } else {
                failed(box)
            }
        } else if image is ConversationComponentImageStateFailed {
            failed(box)
        } else {
            TalkIcon(icon: TalkIcons.image).foregroundStyle(TalkColor.textSecondary)
                .frame(width: 220, height: 220).background(TalkColor.surface, in: box)
        }
    }

    private func failed(_ box: RoundedRectangle) -> some View {
        Text(MR.strings.shared.chat_image_failed.text).talkTextStyle(.small).foregroundStyle(TalkColor.textSecondary)
            .frame(width: 220, height: 220).background(TalkColor.surface, in: box)
    }

    /// The long-press menu: the reactions, then what the sheet offers.
    @ViewBuilder
    private var menu: some View {
        ForEach(reactions, id: \.self) { emoji in
            Button(MR.strings.shared.chat_react.format(emoji)) { onReact(emoji) }
        }
        Button(MR.strings.shared.chat_message_actions.text, action: onActions)
    }
}

/// The message sheet: the reactions, Reply, Copy, Delete for everyone (yours, "Other apps may still show it"), Delete
/// for me.
private struct MessageSheet: View {
    let message: ChatMessage
    let reactions: [String]
    let onReply: (ChatMessage) -> Void
    let onReact: (ChatMessage, String) -> Void
    let onCopy: (ChatMessage) -> Void
    let onDeleteForEveryone: (ChatMessage) -> Void
    let onDeleteForMe: (ChatMessage) -> Void
    let onDone: () -> Void

    var body: some View {
        let strings = MR.strings.shared
        VStack(spacing: 0) {
            HStack {
                ForEach(reactions, id: \.self) { emoji in
                    Button {
                        onDone()
                        onReact(message, emoji)
                    } label: {
                        Text(emoji).font(.system(size: 22)).frame(width: 48, height: 48).background(TalkColor.surface, in: Circle())
                    }
                    .buttonStyle(PressStyle())
                    .accessibilityLabel(strings.chat_react.format(emoji))
                    if emoji != reactions.last { Spacer(minLength: 0) }
                }
            }
            .padding(.horizontal, TalkSpacing.xl)
            .padding(.vertical, TalkSpacing.sm)
            ActionRow(label: strings.chat_reply.text, icon: TalkIcons.reply) { onDone(); onReply(message) }
            if !message.content.isEmpty {
                ActionRow(label: strings.action_copy.text, icon: TalkIcons.copy) { onDone(); onCopy(message) }
            }
            if message.isMine {
                ActionRow(label: strings.chat_delete_everyone.text, icon: TalkIcons.trash, note: strings.chat_delete_everyone_note.text, danger: true) {
                    onDone()
                    onDeleteForEveryone(message)
                }
            }
            ActionRow(label: strings.chat_delete_me.text, icon: TalkIcons.trash, danger: true) { onDone(); onDeleteForMe(message) }
            Spacer(minLength: 0)
        }
        .padding(.top, TalkSpacing.xl)
        .presentationDetents([.medium])
        .presentationDragIndicator(.visible)
    }
}

/// A decrypted chat image full screen; a tap or Close dismisses it.
private struct DataImageViewer: View {
    let data: Data
    let onClose: () -> Void

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Color.black.ignoresSafeArea()
            if let image = UIImage(data: data) {
                Image(uiImage: image).resizable().scaledToFit().frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            IconButton(icon: TalkIcons.close, label: MR.strings.shared.action_close.text, tint: .white, action: onClose)
                .padding(TalkSpacing.md)
        }
        .onTapGesture(perform: onClose)
    }
}
