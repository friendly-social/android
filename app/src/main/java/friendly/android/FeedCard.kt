package friendly.android

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import friendly.sdk.Interest

@Composable
fun FeedCard(entry: FeedItem.Entry, modifier: Modifier = Modifier) {
    OutlinedCard(
        modifier = modifier.fillMaxSize(),
    ) {
        SubcomposeAsyncImage( // todo make wrapper for that and use in other places
            model = entry.avatarUri,
            loading = {
                Box(modifier = Modifier.shimmer())
            },
            error = {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                        .size(64.dp),
                )
            },
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        )
//
//        AsyncImage(
//            model = entry.avatarUri,
//            contentDescription = null,
//            contentScale = ContentScale.FillBounds,
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(256.dp),
//        )

        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = entry.nickname.string,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))

            FeedCardInterests(
                interests = entry.interests,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = entry.description.string,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun FeedCardInterests(
    interests: List<Interest>,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.CenterHorizontally,
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        for (interest in interests) {
            Text(
                text = interest.string,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        color = Color.pastelFromString(
                            string = interest.string,
                            useDark = isSystemInDarkTheme(),
                        ),
                    )
                    .padding(4.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
    }
}
