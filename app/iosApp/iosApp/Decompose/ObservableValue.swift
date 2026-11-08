import SwiftUI
import SharedLogic

/// A Decompose `Value` as an `ObservableObject`, for SwiftUI.
final class ObservableValue<T: AnyObject>: ObservableObject {
    @Published private(set) var value: T

    private var cancellation: Cancellation?

    init(_ value: Value<T>) {
        self.value = value.value
        cancellation = value.subscribe { [weak self] newValue in self?.value = newValue }
    }

    deinit {
        cancellation?.cancel()
    }
}

/// Observes a `Value` that belongs to a child shown inside the same view (a pane, a page), so the view redraws with it.
/// Like `StateValue` it subscribes once per view identity: when the child (and so the `Value`) changes, give the view a
/// new identity with `.id(ObjectIdentifier(child))`, or it keeps showing the old child's state.
struct ValueReader<T: AnyObject, Content: View>: View {
    @StateObject private var object: ObservableValue<T>
    private let content: (T) -> Content

    init(_ value: Value<T>, @ViewBuilder content: @escaping (T) -> Content) {
        _object = StateObject(wrappedValue: ObservableValue(value))
        self.content = content
    }

    var body: some View { content(object.value) }
}

/// A Kotlin `Value<Boolean>` read as `Bool`.
extension ObservableValue where T == KotlinBoolean {
    var bool: Bool { value.boolValue }
}

/// Watches several `Value`s that may come and go (the router's stacks and slots) and calls `onChange` once per change.
final class ValueWatcher {
    private var watched: [String: (source: AnyObject, cancellation: Cancellation)] = [:]
    private let onChange: () -> Void
    private var isNotifying = false

    init(onChange: @escaping () -> Void) {
        self.onChange = onChange
    }

    /// Subscribes to `value` under `key`, replacing what was watched there; `nil` stops watching.
    func watch<T: AnyObject>(_ key: String, _ value: Value<T>?) {
        if let value, let current = watched[key], current.source === value { return }
        watched.removeValue(forKey: key)?.cancellation.cancel()
        guard let value else { return }
        var initial = true
        let cancellation = value.subscribe { [weak self] _ in
            if initial { return }
            self?.notify()
        }
        initial = false
        watched[key] = (value, cancellation)
    }

    /// Stops watching every key not in `keys`.
    func keep(only keys: Set<String>) {
        for key in watched.keys where !keys.contains(key) {
            watched.removeValue(forKey: key)?.cancellation.cancel()
        }
    }

    private func notify() {
        guard !isNotifying else { return }
        isNotifying = true
        onChange()
        isNotifying = false
    }

    deinit {
        watched.values.forEach { $0.cancellation.cancel() }
    }
}
