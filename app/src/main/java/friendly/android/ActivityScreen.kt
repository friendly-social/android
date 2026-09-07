package friendly.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import friendly.android.UserAvatarStyle.UserAvatarShape
import friendly.sdk.Nickname
import friendly.sdk.UserId

@Composable
fun ActivityScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    // todo
                    IconButton(onClick = {}) {
                        UserAvatar(
                            // todo this thing
                            nickname = Nickname.orThrow("pai y"),
                            userId = UserId(21),
                            uri = null,
                            style = UserAvatarStyle(
                                size = 24.dp,
                                noAvatarSize = 14.dp,
                                shape = UserAvatarShape.Certain(CircleShape),
                            ),
                            modifier = Modifier,
                        )
                    }
                },
            )
        },
        modifier = modifier
            .padding(contentPadding)
            .fillMaxSize(),
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_construction_filled),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Activity is under construction",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text =
                "We are planning to release Activity tab by the end of 2026 Q3. Here you will see all replies you will receive in Community.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
