package com.yet.talk.web.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.yet.talk.web.browser.ResizeObserver
import kotlinx.browser.window
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.dom.Span
import org.w3c.dom.HTMLElement
import org.w3c.dom.asList
import org.w3c.dom.events.Event
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The selection of a navigation (the rail, the phone's capsule) as one pill that slides from item to item and
 * stretches along its path while it travels, as the Android capsule does (after iOS 26). The container gets
 * [navPillContainer] and holds a [NavPill]; the selected item carries `aria-current="page"` and the pill follows it,
 * also when the container resizes (the rail collapses to icons at 1224 px). With [draggable], pressing and sliding
 * along the capsule turns the pill into a lens that follows the finger; letting go selects the item under it.
 */
@Stable
class NavPillState internal constructor(private val draggable: Boolean) {
    internal var onSelect: (Int) -> Unit = {}
    private var container: HTMLElement? = null
    private var dragging = false

    internal fun attach(element: HTMLElement): () -> Unit {
        container = element
        val observer = ResizeObserver { _, _ -> place(animate = false) }
        observer.observe(element)
        val stopDrag = if (draggable) dragToSelect(element) else ({})
        place(animate = false)
        return {
            stopDrag()
            observer.disconnect()
            container = null
        }
    }

    /** Puts the pill on the selected item, or hides it when no item is selected (Settings, a post). */
    internal fun place(animate: Boolean = true) {
        val container = container ?: return
        if (dragging) return
        val pill = container.pill() ?: return
        val item = container.querySelector("[aria-current=page]") as? HTMLElement
        if (item == null) {
            pill.classList.remove(SHOWN)
            return
        }
        val wasShown = pill.classList.contains(SHOWN)
        val fromX = pill.style.getPropertyValue("--x").removeSuffix("px").toDoubleOrNull() ?: 0.0
        val fromY = pill.style.getPropertyValue("--y").removeSuffix("px").toDoubleOrNull() ?: 0.0
        // The first placement, and one from hidden, appears in place instead of flying in from a corner.
        pill.classList.toggle(INSTANT, !animate || !wasShown)
        pill.style.setProperty("--x", "${item.offsetLeft}px")
        pill.style.setProperty("--y", "${item.offsetTop}px")
        pill.style.width = "${item.offsetWidth}px"
        pill.style.height = "${item.offsetHeight}px"
        pill.classList.add(SHOWN)
        val dx = abs(fromX - item.offsetLeft)
        val dy = abs(fromY - item.offsetTop)
        if (animate && wasShown && (dx > 1 || dy > 1)) {
            pill.classList.remove(STRETCH_X, STRETCH_Y)
            pill.offsetWidth // restarts the animation
            pill.classList.add(if (dx >= dy) STRETCH_X else STRETCH_Y)
        }
    }

    /** Press and slide; the click that ends a drag is swallowed, a plain tap selects as usual. */
    private fun dragToSelect(container: HTMLElement): () -> Unit {
        var pointer: Int? = null
        var startX = 0.0
        var hovered = -1
        var swallowClick = false
        fun items() = container.querySelectorAll("a").asList().map { it as HTMLElement }
        fun indexAt(x: Double): Int {
            val list = items()
            return list.indices.minByOrNull { i -> abs(list[i].offsetLeft + list[i].offsetWidth / 2.0 - x) } ?: -1
        }
        val down: (Event) -> Unit = { event ->
            val e = event.asDynamic()
            if (e.pointerType != "mouse" || e.button == 0) {
                pointer = e.pointerId as Int
                startX = e.clientX as Double
            }
        }
        val move: (Event) -> Unit = move@{ event ->
            val e = event.asDynamic()
            if (pointer == null || pointer != e.pointerId as Int) return@move
            val pill = container.pill() ?: return@move
            val clientX = e.clientX as Double
            if (!dragging) {
                if (abs(clientX - startX) < DRAG_SLOP) return@move
                dragging = true
                // Keeps the drag when the finger leaves the capsule; throws for a pointer that is already gone.
                runCatching { container.asDynamic().setPointerCapture(pointer) }
                container.classList.add(DRAGGING)
                pill.classList.remove(INSTANT, STRETCH_X, STRETCH_Y)
                pill.classList.add(SHOWN)
            }
            val x = clientX - container.getBoundingClientRect().left
            val left = (x - pill.offsetWidth / 2.0).coerceIn(0.0, (container.clientWidth - pill.offsetWidth).toDouble())
            pill.style.setProperty("--x", "${left.roundToInt()}px")
            val under = indexAt(x)
            if (under != hovered) {
                items().forEachIndexed { i, item -> item.classList.toggle(LIT, i == under) }
                if (hovered != -1) window.navigator.asDynamic().vibrate?.call(window.navigator, 6)
                hovered = under
            }
            event.preventDefault()
        }
        val up: (Event) -> Unit = up@{ event ->
            val e = event.asDynamic()
            if (pointer == null || pointer != e.pointerId as Int) return@up
            pointer = null
            if (!dragging) return@up
            dragging = false
            swallowClick = true
            container.classList.remove(DRAGGING)
            items().forEach { it.classList.remove(LIT) }
            // Where the finger let go, not the last item a move event reported: a quick flick sends few moves.
            val target = indexAt((e.clientX as Double) - container.getBoundingClientRect().left)
            hovered = -1
            if (target >= 0) onSelect(target)
            // Back onto the selection if nothing changed; a new selection moves it again once the DOM updates.
            place()
        }
        val click: (Event) -> Unit = { event ->
            if (swallowClick) {
                swallowClick = false
                event.preventDefault()
                event.stopPropagation()
            }
        }
        container.addEventListener("pointerdown", down)
        container.addEventListener("pointermove", move)
        container.addEventListener("pointerup", up)
        container.addEventListener("pointercancel", up)
        container.addEventListener("click", click, true)
        return {
            container.removeEventListener("pointerdown", down)
            container.removeEventListener("pointermove", move)
            container.removeEventListener("pointerup", up)
            container.removeEventListener("pointercancel", up)
            container.removeEventListener("click", click, true)
        }
    }

    private fun HTMLElement.pill() = querySelector(".t-nav-pill") as? HTMLElement

    private companion object {
        const val SHOWN = "is-shown"
        const val INSTANT = "is-instant"
        const val STRETCH_X = "is-stretch-x"
        const val STRETCH_Y = "is-stretch-y"
        const val DRAGGING = "is-dragging"
        const val LIT = "is-lit"
        const val DRAG_SLOP = 8.0
    }
}

/**
 * A [NavPillState] for a navigation whose selection is [selection]; [onSelect] gets the index of the item a drag
 * ended over (with [draggable]).
 */
@Composable
fun rememberNavPill(selection: Any?, draggable: Boolean = false, onSelect: (Int) -> Unit = {}): NavPillState {
    val state = remember(draggable) { NavPillState(draggable) }
    val latest by rememberUpdatedState(onSelect)
    state.onSelect = { latest(it) }
    // Runs after the DOM carries the new aria-current.
    LaunchedEffect(state, selection) { state.place() }
    return state
}

/** The container's attributes: the pill is placed inside it, and follows its size. */
fun <T : HTMLElement> AttrsScope<T>.navPillContainer(state: NavPillState) {
    classes("t-nav-pilled")
    ref { container ->
        val detach = state.attach(container)
        onDispose { detach() }
    }
}

/** The sliding selection itself; decorative, since the items carry `aria-current`. */
@Composable
fun NavPill() {
    Span({ classes("t-nav-pill"); attr("aria-hidden", "true") })
}
