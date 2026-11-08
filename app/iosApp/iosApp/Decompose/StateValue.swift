import SwiftUI
import SharedLogic

/// Holds an `ObservableValue` for the life of a view: `@StateValue var stack: ChildStack<…>`. A `StateObject`, so a
/// parent redrawing the view does not subscribe again (or drop the view's state); a view for another component gets a
/// new identity (`.id`) and with it a new subscription.
@propertyWrapper struct StateValue<T : AnyObject>: DynamicProperty {
    @StateObject
    private var obj: ObservableValue<T>

    var wrappedValue: T { obj.value }

    init(_ value: Value<T>) {
        _obj = StateObject(wrappedValue: ObservableValue(value))
    }
}
