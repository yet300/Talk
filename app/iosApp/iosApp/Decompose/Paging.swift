import SwiftUI
import SharedLogic

/// A `PagingController` (Kotlin, `shared/logic` iosMain) for the life of a list: the loaded rows and load states,
/// `loadAround` as rows appear, `refresh` and `retry`. The controller stops collecting when the list goes away.
final class PagingModel<T: AnyObject>: ObservableObject {
    @Published private(set) var snapshot: PagingSnapshot<T>

    private let controller: PagingController<T>
    private var cancellation: Cancellation?

    init(_ controller: PagingController<T>) {
        self.controller = controller
        snapshot = controller.snapshot.value
        cancellation = controller.snapshot.subscribe { [weak self] in self?.snapshot = $0 }
    }

    var items: [T] { snapshot.items }

    func onAppear(index: Int) { controller.loadAround(index: Int32(index)) }

    func refresh() { controller.refresh() }

    func retry() { controller.retry() }

    deinit {
        cancellation?.cancel()
        controller.close()
    }
}

/// A paged list's rows inside a `LazyVStack` or `List`: skeleton rows on the first load, `empty` when there is nothing,
/// an error row with Retry, a skeleton row while the next page loads, and the next page once the last row appears.
struct PagedRows<T: AnyObject, Row: View, Empty: View>: View {
    @ObservedObject var model: PagingModel<T>
    let id: (T) -> String
    @ViewBuilder let empty: () -> Empty
    @ViewBuilder let row: (T) -> Row

    var body: some View {
        let snapshot = model.snapshot
        if snapshot.isFirstLoad {
            SkeletonCards()
        } else if snapshot.isEmpty {
            empty()
        }
        ForEach(rows(snapshot.items)) { entry in
            row(entry.item)
                .onAppear { model.onAppear(index: entry.index) }
        }
        if let error = snapshot.error {
            ErrorRow(error: error, onDismiss: nil, onRetry: model.retry)
        }
        if !snapshot.items.isEmpty && snapshot.append == .loading {
            SkeletonCards(count: 1)
        }
    }

    private struct Entry: Identifiable {
        let id: String
        let index: Int
        let item: T
    }

    /// Rows keyed by the item's id; a repeated id (the same post twice in a page) keeps its first place.
    private func rows(_ items: [T]) -> [Entry] {
        var seen = Set<String>()
        return items.enumerated().compactMap { index, item in
            let key = id(item)
            return seen.insert(key).inserted ? Entry(id: key, index: index, item: item) : nil
        }
    }
}
