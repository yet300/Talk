package com.app.common.decompose

import com.arkivanov.decompose.Cancellation
import com.arkivanov.decompose.value.Value

/** A [Value] computed from two others, updated whenever either changes (a model from a store and a navigation). */
fun <A : Any, B : Any, R : Any> combine(a: Value<A>, b: Value<B>, transform: (A, B) -> R): Value<R> =
    object : Value<R>() {
        override val value: R get() = transform(a.value, b.value)

        override fun subscribe(observer: (R) -> Unit): Cancellation {
            var subscribed = false
            val first = a.subscribe { if (subscribed) observer(value) }
            val second = b.subscribe { if (subscribed) observer(value) }
            subscribed = true
            observer(value)
            return Cancellation {
                first.cancel()
                second.cancel()
            }
        }
    }
