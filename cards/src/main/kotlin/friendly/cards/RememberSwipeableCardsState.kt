package friendly.cards

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * Creates and remembers a [SwipeableCardsState] instance across recompositions.
 *
 * @param initialCardIndex The starting index for the card stack. Defaults to 0 (first card).
 * @param itemCount Lambda that returns the total number of items in the card stack.
 *                 This is provided as a lambda to handle dynamic lists.
 *
 * @return A remembered [SwipeableCardsState] instance that maintains the stack's state.
 *
 * Example usage:
 * ```
 * val state = rememberSwipeableCardsState(
 *     initialCardIndex = 0,
 *     itemCount = { profiles.size }
 * )
 * ```
 */
@Composable
fun rememberSwipeableCardsState(
    visibleCardsInStack: Int = SwipeableCardsDefaults.VISIBLE_CARDS_IN_STACK,
    initialCardIndex: Int = 0,
    itemCount: () -> Int,
): SwipeableCardsState {
    val state = rememberSaveable(saver = SwipeableCardsStateSaver(itemCount)) {
        SwipeableCardsState(
            visibleCardsInStack = visibleCardsInStack,
            initialCardIndex = initialCardIndex,
            itemCount = itemCount,
        )
    }
    return state
}

private class SwipeableCardsStateSaver(private val itemCount: () -> Int) :
    Saver<SwipeableCardsState, Int> {
    override fun SaverScope.save(value: SwipeableCardsState): Int =
        value.currentCardIndex

    override fun restore(value: Int) = SwipeableCardsState(
        initialCardIndex = value,
        itemCount = itemCount,
    )
}
