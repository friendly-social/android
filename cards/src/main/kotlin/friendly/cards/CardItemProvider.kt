package friendly.cards

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset

@Composable
internal fun <T> rememberItemProvider(
    state: SwipeableCardsState,
    properties: SwipeableCardsProperties,
    animations: SwipeableCardsAnimations,
    factors: SwipeableCardsFactors,
    onSwipe: (T, SwipeableCardDirection) -> Unit,
    customLazyListScope: LazySwipeableCardsScope<T>.() -> Unit
): CardItemProvider<T> {
    val customLazyListScopeState = rememberUpdatedState(customLazyListScope)

    return remember {
        CardItemProvider(
            itemsState = derivedStateOf {
                val layoutScope =
                    LazySwipeableCardsScopeImpl<T>().apply(customLazyListScopeState.value)
                layoutScope.items
            },
            state = state,
            properties = properties,
            animations = animations,
            factors = factors,
            onSwipe = onSwipe,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
class CardItemProvider<T>(
    private val itemsState: State<List<LazyCardItemContent<T>>>,
    private val state: SwipeableCardsState,
    private val properties: SwipeableCardsProperties,
    private val animations: SwipeableCardsAnimations,
    private val factors: SwipeableCardsFactors,
    private val onSwipe: (T, SwipeableCardDirection) -> Unit,
) : LazyLayoutItemProvider {

    override val itemCount
        get() = itemsState.value.size

    @Composable
    override fun Item(index: Int, key: Any) {
        val item = itemsState.value.getOrNull(index)
        val scale = factors.scaleFactor(index, state, properties)

        val offset = remember {
            derivedStateOf() {
                state.dragOffsets.getOrDefault(
                    key = index,
                    defaultValue = Offset.Zero,
                )
            }
        }

        SwipeableCard(
            onSwipe = { direction ->
                item?.let { cardItem -> onSwipe(cardItem.item, direction) }
                val targetX = when (direction) {
                    SwipeableCardDirection.Left -> -state.size.width.toFloat() * 1.5f
                    SwipeableCardDirection.Right -> state.size.width.toFloat() * 1.5f
                }

                state.swipingVisibleCards.add(state.currentCardIndex)
                state.dragOffsets[state.currentCardIndex] = Offset(targetX, 0f)

                state.moveNext()
            },
            offset = offset,
            properties = properties,
            animations = animations,
            factors = factors,
            draggable = if (properties.lockBelowCardDragging) {
                index == state.currentCardIndex
            } else {
                true
            },
            scale = scale,
            onDragOffsetChange = { offset ->
                state.onDragOffsetChange(
                    index = index,
                    offset = offset,
                )
            },
        ) { offset ->
            item?.itemContent?.invoke(item.item, index, offset)
        }
    }

    fun getItem(index: Int): T? {
        return itemsState.value.getOrNull(index)?.item
    }
}

internal fun Offset.accelerateX(acceleration: Float): Offset {
    return Offset(x = x * acceleration, y = y)
}

internal fun Offset.consume(other: Offset, reverseX: Boolean = false): Offset {
    val newX = if (reverseX) {
        x - other.x
    } else {
        x + other.x
    }
    return Offset(newX, y + other.y)
}
