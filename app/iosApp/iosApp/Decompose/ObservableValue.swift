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

/// Holds an `ObservableValue` for the life of a view: `@StateValue var stack: ChildStack<…>`.
@propertyWrapper
struct StateValue<T: AnyObject>: DynamicProperty {
    @StateObject private var object: ObservableValue<T>

    var wrappedValue: T { object.value }

    init(_ value: Value<T>) {
        _object = StateObject(wrappedValue: ObservableValue(value))
    }
}
