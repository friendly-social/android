package friendly.android

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import friendly.cards.LazySwipeableCards
import friendly.cards.SwipeableCardDirection
import friendly.cards.SwipeableCardsAnimations
import friendly.cards.SwipeableCardsProperties
import friendly.cards.items
import friendly.cards.rememberSwipeableCardsState

private const val MAX_CARD_OFFSET = 500

private val cardsProperties = SwipeableCardsProperties(
    stackedCardsOffset = 0.dp,
)

@Composable
fun IndicatedCardFeed(
    currentItems: List<FeedEntry>,
    like: (FeedEntry) -> Unit,
    dislike: (FeedEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val swipeCardsState = rememberSwipeableCardsState(
        itemCount = { currentItems.size },
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        LazySwipeableCards(
            animations = SwipeableCardsAnimations(
                cardsAnimationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            ),
            state = swipeCardsState,
            properties = cardsProperties,
            onSwipe = { item, direction ->
                when (direction) {
                    SwipeableCardDirection.Right -> like(item)

                    SwipeableCardDirection.Left -> dislike(item)
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            items(currentItems) { item, _, _ ->
                FeedCard(
                    entry = item,
                    like = { entry ->
                        swipeCardsState.swipe(SwipeableCardDirection.Right)
                        like(entry)
                    },
                    dislike = { entry ->
                        swipeCardsState.swipe(SwipeableCardDirection.Left)
                        dislike(entry)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
