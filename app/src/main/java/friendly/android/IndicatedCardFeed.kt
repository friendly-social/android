package friendly.android

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import friendly.cards.LazySwipeableCards
import friendly.cards.SwipeableCardDirection
import friendly.cards.SwipeableCardsAnimations
import friendly.cards.SwipeableCardsProperties
import friendly.cards.items
import friendly.cards.rememberSwipeableCardsState
import kotlin.math.absoluteValue

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
        val cardOffset = swipeCardsState.dragOffsets.values
            .toList()
            .firstOrNull()
            ?: Offset.Zero
        val xOffset = cardOffset.x
        val xPercentage = xOffset.absoluteValue / MAX_CARD_OFFSET
        val size = (128 * xPercentage).dp

        LazySwipeableCards(
            animations = SwipeableCardsAnimations(
                cardsAnimationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow,
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
                    xOffset = xOffset,
                    indicatorSize = size,
                    entry = item,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
