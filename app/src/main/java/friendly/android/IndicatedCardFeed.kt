package friendly.android

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import friendly.cards.LazySwipeableCards
import friendly.cards.SwipeableCardDirection
import friendly.cards.SwipeableCardsAnimations
import friendly.cards.SwipeableCardsProperties
import friendly.cards.items
import friendly.cards.rememberSwipeableCardsState

private val cardsProperties = SwipeableCardsProperties(
    stackedCardsOffset = 0.dp,
)

@Composable
fun IndicatedCardFeed(
    currentItems: List<FeedEntry>,
    like: (FeedEntry) -> Unit,
    dislike: (FeedEntry) -> Unit,
    onProfilePictureClick: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    val swipeCardsState = rememberSwipeableCardsState(
        visibleCardsInStack = 2,
        itemCount = { currentItems.size },
    )
    val cardThresholdPx = with(LocalDensity.current) {
        cardsProperties.swipeThreshold.toPx()
    }

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
            items(currentItems) { item, index, offset ->
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 64.dp),
                ) {
                    AnimatedVisibility(
                        enter = slideInHorizontally(initialOffsetX = {
                            -2 * it
                        }),
                        exit = slideOutHorizontally(targetOffsetX = {
                            -2 * it
                        }),
                        visible = offset.x < -cardThresholdPx,
                    ) {
                        QuarterTurn(90f) { DislikeIndicator() }
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (-64).dp),
                ) {
                    AnimatedVisibility(
                        enter = slideInHorizontally(initialOffsetX = {
                            2 * it
                        }),
                        exit = slideOutHorizontally(targetOffsetX = { 2 * it }),
                        visible = offset.x > cardThresholdPx,
                    ) {
                        QuarterTurn(-90f) { LikeIndicator() }
                    }
                }
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
                    onProfilePictureClick = onProfilePictureClick,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun QuarterTurn(degrees: Float, content: @Composable () -> Unit) {
    Layout(
        content = {
            Box(Modifier.rotate(degrees)) {
                content()
            }
        },
    ) { measurables, constraints ->
        val placeable = measurables.single().measure(
            Constraints(
                minWidth = constraints.minHeight,
                maxWidth = constraints.maxHeight,
                minHeight = constraints.minWidth,
                maxHeight = constraints.maxWidth,
            ),
        )
        layout(
            width = placeable.height,
            height = placeable.width,
        ) {
            placeable.place(
                x = (placeable.height - placeable.width) / 2,
                y = (placeable.width - placeable.height) / 2,
            )
        }
    }
}

@Composable
private fun LikeIndicator(modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = modifier.clip(CircleShape),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                vertical = 8.dp,
                horizontal = 24.dp,
            ),
        ) {
            Text(
                text = stringResource(R.string.like),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

@Composable
private fun DislikeIndicator(modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.error,
        modifier = modifier.clip(CircleShape),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                vertical = 8.dp,
                horizontal = 24.dp,
            ),
        ) {
            Text(
                text = stringResource(R.string.dislike),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}
