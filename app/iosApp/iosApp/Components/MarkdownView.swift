import SwiftUI

/// A post's markdown as native text: Foundation's CommonMark parser (`AttributedString(markdown:)`, full syntax) gives
/// the blocks (headings, paragraphs, lists, quotes, code blocks, rules) and the inline styles (bold, italic, code,
/// links), drawn with the design's reading style. Images in the text show under their paragraph. Nothing is rendered
/// as HTML, so there is nothing to sanitize.
struct MarkdownView: View {
    let markdown: String
    var style: TalkTextStyle = .reading

    var body: some View {
        let blocks = MarkdownBlocks.blocks(for: markdown)
        VStack(alignment: .leading, spacing: TalkSpacing.md) {
            ForEach(blocks) { block in
                view(for: block)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .tint(TalkColor.accent)
    }

    @ViewBuilder
    private func view(for block: MarkdownBlock) -> some View {
        switch block.kind {
        case .heading(let level):
            Text(block.text)
                .talkTextStyle(level <= 1 ? .headingLarge : .headingMedium)
                .foregroundStyle(TalkColor.textPrimary)
                .accessibilityAddTraits(.isHeader)
        case .paragraph:
            Text(block.text)
                .talkTextStyle(style)
                .foregroundStyle(TalkColor.textPrimary)
        case .listItem(let marker, let depth):
            HStack(alignment: .firstTextBaseline, spacing: TalkSpacing.sm) {
                Text(marker).talkTextStyle(style).foregroundStyle(TalkColor.textSecondary)
                Text(block.text).talkTextStyle(style).foregroundStyle(TalkColor.textPrimary)
            }
            .padding(.leading, CGFloat(depth) * TalkSpacing.lg)
        case .quote:
            Text(block.text)
                .talkTextStyle(style)
                .foregroundStyle(TalkColor.textSecondary)
                .padding(.leading, TalkSpacing.md)
                .overlay(alignment: .leading) { Rectangle().fill(TalkColor.border).frame(width: 3) }
        case .code:
            ScrollView(.horizontal) {
                Text(block.text)
                    .talkTextStyle(.codeBlock)
                    .foregroundStyle(TalkColor.textPrimary)
                    .textSelection(.enabled)
                    .padding(TalkSpacing.md)
            }
            .background(TalkColor.surface, in: RoundedRectangle(cornerRadius: TalkRadius.card))
        case .rule:
            Hairline()
        case .image(let url, let alt):
            RemoteImage(url: url, contentMode: .fit)
                .frame(maxWidth: .infinity, maxHeight: TalkSize.mediaMax)
                .clipShape(RoundedRectangle(cornerRadius: TalkRadius.card))
                .accessibilityLabel(alt)
        }
    }
}

struct MarkdownBlock: Identifiable {
    enum Kind {
        case heading(Int)
        case paragraph
        case listItem(marker: String, depth: Int)
        case quote
        case code
        case rule
        case image(URL, alt: String)
    }

    let id: Int
    let kind: Kind
    let text: AttributedString
}

/// Parses markdown into blocks once per text (lists scroll the same posts in and out).
enum MarkdownBlocks {
    private static let cache = NSCache<NSString, Box>()

    private final class Box {
        let blocks: [MarkdownBlock]
        init(_ blocks: [MarkdownBlock]) { self.blocks = blocks }
    }

    static func blocks(for markdown: String) -> [MarkdownBlock] {
        if let cached = cache.object(forKey: markdown as NSString) { return cached.blocks }
        let blocks = parse(markdown)
        cache.setObject(Box(blocks), forKey: markdown as NSString)
        return blocks
    }

    private static func parse(_ markdown: String) -> [MarkdownBlock] {
        let options = AttributedString.MarkdownParsingOptions(interpretedSyntax: .full, failurePolicy: .returnPartiallyParsedIfPossible)
        guard let parsed = try? AttributedString(markdown: markdown, options: options) else {
            return [MarkdownBlock(id: 0, kind: .paragraph, text: AttributedString(markdown))]
        }
        var blocks: [MarkdownBlock] = []
        var images: [(URL, String)] = []

        func flushImages() {
            for (url, alt) in images { blocks.append(MarkdownBlock(id: blocks.count, kind: .image(url, alt: alt), text: AttributedString())) }
            images.removeAll()
        }

        for (intent, range) in parsed.runs[\.presentationIntent] {
            var text = AttributedString()
            for run in parsed[range].runs {
                if let url = run.imageURL {
                    images.append((url, String(parsed[run.range].characters)))
                } else {
                    text.append(parsed[run.range])
                }
            }
            let kind = kind(of: intent)
            if case .rule = kind {
                blocks.append(MarkdownBlock(id: blocks.count, kind: .rule, text: AttributedString()))
            } else if !text.characters.allSatisfy(\.isWhitespace) {
                if case .code = kind {
                    // Code keeps its line breaks but not the trailing one.
                    while text.characters.last == "\n" { text.characters.removeLast() }
                }
                blocks.append(MarkdownBlock(id: blocks.count, kind: kind, text: text))
            }
            flushImages()
        }
        return blocks
    }

    private static func kind(of intent: PresentationIntent?) -> MarkdownBlock.Kind {
        guard let intent else { return .paragraph }
        // Components run from the innermost block outwards: the first list item and list are this block's own.
        var depth = -1
        var ordinal: Int?
        var ordered: Bool?
        for component in intent.components {
            switch component.kind {
            case .header(let level): return .heading(level)
            case .codeBlock: return .code
            case .thematicBreak: return .rule
            case .blockQuote where ordinal == nil: return .quote
            case .listItem(let number):
                if ordinal == nil { ordinal = number }
                depth += 1
            case .orderedList:
                if ordered == nil { ordered = true }
            case .unorderedList:
                if ordered == nil { ordered = false }
            default:
                break
            }
        }
        guard let ordinal else { return .paragraph }
        return .listItem(marker: ordered == true ? "\(ordinal)." : "•", depth: max(depth, 0))
    }
}
