package friendly.android

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spartapps.swipeablecards.state.rememberSwipeableCardsState
import com.spartapps.swipeablecards.ui.SwipeableCardDirection
import com.spartapps.swipeablecards.ui.SwipeableCardsProperties
import com.spartapps.swipeablecards.ui.animation.SwipeableCardsAnimations
import com.spartapps.swipeablecards.ui.lazy.LazySwipeableCards
import com.spartapps.swipeablecards.ui.lazy.item
import kotlin.math.absoluteValue

private const val MAX_CARD_OFFSET = 500

private val animations = SwipeableCardsAnimations(
    cardsAnimationSpec = spring(
        dampingRatio = 0.6f,
        stiffness = 100f,
    ),
    rotationAnimationSpec = spring(),
)

private val cardsProperties = SwipeableCardsProperties(
    padding = 0.dp,
    swipeThreshold = 100.dp,
    lockBelowCardDragging = true,
    enableHapticFeedbackOnThreshold = true,
    stackedCardsOffset = 0.dp,
    draggingAcceleration = 0.6f,
)

// TODO 1:
//  make own card implementation that should work without
//  workarounds with goBack()
// TODO 2: stop using indicators in favor of ard stack usage
@Composable
fun IndicatedCardFeed(
    currentItem: FeedItem,
    like: () -> Unit,
    dislike: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val swipeCardsState = rememberSwipeableCardsState { 1 }

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

        if (xOffset < 0) {
            DislikeIndicator(size)
        }

        if (xOffset > 0) {
            LikeIndicator(size)
        }

        LazySwipeableCards(
            state = swipeCardsState,
            properties = cardsProperties,
            animations = animations,
            onSwipe = { _, direction ->
                when (direction) {
                    SwipeableCardDirection.Right -> like()

                    SwipeableCardDirection.Left -> dislike()
                }
                swipeCardsState.goBack()
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            item(currentItem) { item, _, _ ->
                when (item) {
                    is FeedItem.Loading -> {
                        ShimmerFeedCard(modifier = Modifier.fillMaxSize())
                    }

                    is FeedItem.Entry -> {
                        FeedCard(
                            entry = item,
                            modifier = Modifier
                                .fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.LikeIndicator(size: Dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .size(size)
            .align(Alignment.CenterStart),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_thumb_up),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(size / 2),
        )
    }
}

@Composable
private fun BoxScope.DislikeIndicator(size: Dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.errorContainer)
            .size(size)
            .align(Alignment.CenterEnd),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_thumb_down),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(size / 2),
        )
    }
}
