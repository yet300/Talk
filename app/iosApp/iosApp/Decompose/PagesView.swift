import SwiftUI
import SharedLogic

/// A Decompose `ChildPages` as pages to swipe between (Home's feeds, Profile's tabs, Chats · Requests): a horizontal
/// paging scroll view whose settled page selects that page in Kotlin, and which follows the component when a tab is
/// tapped. Each page keeps its own vertical scroll position while its neighbours are shown.
struct PagesView<T: AnyObject, Page: View>: View {
    let pages: ChildPages<AnyObject, T>
    let onSelect: (Int) -> Void
    @ViewBuilder let page: (T) -> Page

    @State private var position: Int?
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        ScrollView(.horizontal) {
            LazyHStack(spacing: 0) {
                ForEach(pages.items.indices, id: \.self) { index in
                    Group {
                        if let instance = pages.items[index].instance {
                            page(instance)
                        } else {
                            Color.clear
                        }
                    }
                    .containerRelativeFrame(.horizontal)
                    .id(index)
                }
            }
            .scrollTargetLayout()
        }
        .scrollTargetBehavior(.paging)
        .scrollIndicators(.hidden)
        .scrollBounceBehavior(.basedOnSize, axes: .horizontal)
        .scrollPosition(id: $position)
        // The neighbouring page must not peek out under a vertical bar (the Duo), while each page still runs under
        // the glass bars above and below it: clip the sides only.
        .mask { Rectangle().padding(.vertical, -2000) }
        .onAppear { position = Int(pages.selectedIndex) }
        .onChange(of: pages.selectedIndex) { _, selected in
            guard position != Int(selected) else { return }
            withAnimation(reduceMotion ? nil : TalkMotion.move) { position = Int(selected) }
        }
        .onChange(of: position) { _, settled in
            if let settled, settled != Int(pages.selectedIndex) { onSelect(settled) }
        }
    }
}
