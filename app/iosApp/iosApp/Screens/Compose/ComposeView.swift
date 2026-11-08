import SwiftUI
import PhotosUI
import UniformTypeIdentifiers
import SharedLogic

/// The editor (`Compose-Mobile`, decision 5): Close, Edit · Preview (and Split on a regular-width window, as the Duo
/// inner screen), Add image (the system photo picker, uploaded to Blossom), Publish or Save; the post it replies to,
/// quotes or edits, the attached images, then the markdown (monospace) or its preview. Split is an arrangement: side
/// by side on a wide window, and on the Duo half open the preview above the fold and the editor below it. The draft
/// lives in the component, so folding keeps it.
struct ComposeView: View {
    private let component: ComposeComponent
    @StateValue private var model: ComposeComponentModel
    @State private var photo: PhotosPickerItem?
    @Environment(\.horizontalSizeClass) private var sizeClass

    init(_ component: ComposeComponent) {
        self.component = component
        _model = StateValue(component.model)
    }

    var body: some View {
        let strings = MR.strings.shared
        let wide = sizeClass == .regular
        let isEdit = model.mode is ComposeModeEdit
        VStack(alignment: .leading, spacing: 0) {
            NetworkBar()
            if isEdit { Caption(strings.compose_edit_note.text) }
            ErrorRow(error: model.error, onDismiss: component.onErrorShown, onRetry: model.canPublish ? component.onPublish : nil)
            if let target = model.target { TargetPreview(mode: model.mode, post: target) }
            if !model.images.isEmpty { ImageStrip(images: model.images, onRemove: component.onRemoveImage) }
            Caption(model.isUploading ? strings.compose_uploading.text
                    : model.isPublishing ? strings.compose_publishing.text
                    : strings.compose_length.format(model.text.count, model.maxLength))
            content(view: wide ? model.view : (model.view == .split ? .edit : model.view))
                .animation(TalkMotion.fade, value: model.view)
        }
        .background(TalkColor.bg.ignoresSafeArea())
        .navigationBarBackButtonHidden()
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                ToolbarIconButton(icon: TalkIcons.close, label: strings.compose_close.text, action: component.onClose)
            }
            ToolbarItem(placement: .principal) {
                Picker(strings.compose_view.text, selection: Binding(get: { model.view }, set: component.onViewChanged)) {
                    Text(strings.compose_view_edit.text).tag(ComposeComponentView.edit)
                    if wide { Text(strings.compose_view_split.text).tag(ComposeComponentView.split) }
                    Text(strings.compose_view_preview.text).tag(ComposeComponentView.preview)
                }
                .pickerStyle(.segmented)
                .frame(maxWidth: wide ? 300 : 200)
            }
            ToolbarItemGroup(placement: .primaryAction) {
                PhotosPicker(selection: $photo, matching: .images) {
                    Label { Text(strings.compose_add_image.text) } icon: { TalkIcon(icon: TalkIcons.image) }
                }
                .disabled(model.isUploading)
                Button(isEdit ? strings.compose_save.text : strings.compose_publish.text, action: component.onPublish)
                    .disabled(!model.canPublish)
                    .fontWeight(.bold)
            }
        }
        .onChange(of: photo) { _, item in
            guard let item else { return }
            photo = nil
            Task { await attach(item) }
        }
        .onChange(of: wide, initial: true) { _, wide in
            if !wide && model.view == .split { component.onViewChanged(view: .edit) }
        }
    }

    @ViewBuilder
    private func content(view: ComposeComponentView) -> some View {
        switch view {
        case .edit:
            Editor(text: model.text, onChange: component.onTextChanged)
        case .preview:
            Preview(text: model.text)
        case .split:
            if #available(iOS 27.1, *) {
                ArrangementView {
                    Editor(text: model.text, onChange: component.onTextChanged)
                } secondary: {
                    Preview(text: model.text)
                }
                .arrangementViewStyle(.split)
            } else {
                HStack(spacing: 0) {
                    Editor(text: model.text, onChange: component.onTextChanged)
                    Rectangle().fill(TalkColor.border).frame(width: TalkSize.hairline)
                    Preview(text: model.text)
                }
            }
        }
    }

    /// The picked photo as bytes with its type, handed to the component to upload.
    private func attach(_ item: PhotosPickerItem) async {
        guard let data = try? await item.loadTransferable(type: Data.self) else { return }
        let mime = item.supportedContentTypes.first?.preferredMIMEType ?? "image/jpeg"
        await MainActor.run { component.onAttachImage(file: BytesKt.imageFile(data: data, mimeType: mime)) }
    }
}

/// The markdown body (monospace 15/24), focused when the editor opens.
private struct Editor: View {
    let text: String
    let onChange: (String) -> Void
    @FocusState private var focused: Bool

    var body: some View {
        ZStack(alignment: .topLeading) {
            if text.isEmpty {
                Text(MR.strings.shared.compose_placeholder.text)
                    .talkTextStyle(.codeBlock)
                    .foregroundStyle(TalkColor.textSecondary)
                    .padding(.horizontal, TalkSpacing.lg + 5)
                    .padding(.vertical, TalkSpacing.sm + 8)
                    .accessibilityHidden(true)
            }
            DraftText(text: text, onChange: onChange) { draft in
                TextEditor(text: draft)
                    .talkTextStyle(.codeBlock)
                    .foregroundStyle(TalkColor.textPrimary)
                    .scrollContentBackground(.hidden)
                    .tint(TalkColor.accent)
                    .focused($focused)
                    .padding(.horizontal, TalkSpacing.lg)
                    .padding(.vertical, TalkSpacing.sm)
                    .accessibilityLabel(MR.strings.shared.compose_body.text)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .onAppear { focused = true }
    }
}

private struct Preview: View {
    let text: String

    var body: some View {
        ScrollView {
            MarkdownView(markdown: text)
                .padding(.horizontal, TalkSpacing.lg)
                .padding(.vertical, TalkSpacing.sm)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

/// What the editor works on: "Reply" / "Quote post" / "Edit post" and that post's head and title.
private struct TargetPreview: View {
    let mode: ComposeMode
    let post: Post

    var body: some View {
        let strings = MR.strings.shared
        let heading: String? = switch onEnum(of: mode) {
        case .reply: strings.compose_reply.text
        case .quote: strings.compose_quote.text
        case .edit: strings.compose_edit.text
        case .new, .share: nil
        }
        if let heading {
            VStack(alignment: .leading, spacing: 4) {
                Text(heading).talkTextStyle(.small).foregroundStyle(TalkColor.textSecondary)
                if !(mode is ComposeModeEdit) {
                    VStack(alignment: .leading, spacing: 4) {
                        PostHead(post: post)
                        Text(post.summary.title).talkTextStyle(.bodyBold).foregroundStyle(TalkColor.textPrimary).lineLimit(2)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(TalkSpacing.md)
                    .overlay(RoundedRectangle(cornerRadius: TalkRadius.card).strokeBorder(TalkColor.border, lineWidth: 1))
                }
            }
            .padding(.horizontal, TalkSpacing.lg)
            .padding(.vertical, TalkSpacing.sm)
        }
    }
}

/// Attached images (also in the text as markdown); each can be removed.
private struct ImageStrip: View {
    let images: [SharedLogic.Image]
    let onRemove: (String) -> Void

    var body: some View {
        ScrollView(.horizontal) {
            HStack(spacing: TalkSpacing.sm) {
                ForEach(images, id: \.url) { image in
                    ZStack(alignment: .topTrailing) {
                        Color.clear.frame(width: 88, height: 88)
                            .overlay { if let url = URL(string: image.url) { RemoteImage(url: url) } }
                            .clipShape(RoundedRectangle(cornerRadius: TalkRadius.card))
                        IconButton(icon: TalkIcons.close, label: MR.strings.shared.compose_remove_image.text, tint: .white, size: 18) {
                            onRemove(image.url)
                        }
                    }
                }
            }
            .padding(.horizontal, TalkSpacing.lg)
        }
        .scrollIndicators(.hidden)
    }
}
