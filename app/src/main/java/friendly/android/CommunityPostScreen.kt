package friendly.android

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.m3.Markdown
import friendly.sdk.CommunityPostDescriptor
import friendly.sdk.CommunityPostDetails
import friendly.sdk.FileDescriptor
import friendly.sdk.FriendlyClient
import friendly.sdk.FriendlyCommunityClient.DetailsResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class CommunityPostScreenViewModel(
    private val client: FriendlyClient,
    private val authStorage: AuthStorage,
) : ViewModel() {
    private val _state = MutableStateFlow<DetailsResult.Success?>(null)

    val state: StateFlow<DetailsResult.Success?> = _state.asStateFlow()

    fun fetch(descriptor: CommunityPostDescriptor) {
        viewModelScope.launch {
            val authorization = authStorage.getAuth()
            val postDetails = client.community.details(
                authorization = authorization,
                descriptor = descriptor,
            )
            _state.update { postDetails.orThrow() }
        }
    }

    fun fileUri(fileDescriptor: FileDescriptor): Uri =
        client.files.getEndpoint(fileDescriptor).string.toUri()
}

// TODO: this is a completely WIP screen that has to changed very much

@Composable
fun CommunityPostScreen(
    vm: CommunityPostScreenViewModel,
    descriptor: CommunityPostDescriptor,
    contentPadding: PaddingValues,
    onPostClick: (CommunityPostDescriptor) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // TODO: this bro looks strange
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.fetch(descriptor) }

    when (val state = state) {
        null -> {
            CircularProgressIndicator()
        }

        else -> {
            when (val post = state.post) {
                is CommunityPostDetails.Deleted -> TODO()

                is CommunityPostDetails.Plain -> {
                    PlainPost(
                        post = post,
                        onBack = onBack,
                        modifier = modifier
                            .fillMaxSize()
                            .padding(contentPadding),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlainPost(
    post: CommunityPostDetails.Plain,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val topAppBarBehavior = TopAppBarDefaults
        .enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                onBack = onBack,
                post = post,
                modifier = Modifier,
                scrollBehavior = topAppBarBehavior,
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .nestedScroll(topAppBarBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState()),
        ) {
            val client = FriendlyClient.production().files
            val uri = post.owner.avatar?.let { client.getEndpoint(it) }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier,
            ) {
                UserAvatar(
                    nickname = post.owner.nickname,
                    userId = post.owner.id,
                    uri = uri?.string?.toUri(),
                    style = UserAvatarStyle.Medium,
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = post.owner.nickname.string,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(Modifier.weight(1f))

                FilledTonalButton(
                    onClick = {},
                ) {
                    Text("Show profile")
                }
            }

            Spacer(Modifier.height(8.dp))

            Markdown(
                content = post.text.string,
                imageTransformer = Coil3ImageTransformerImpl,
                modifier = Modifier
                    .fillMaxSize(),
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = formatDateTime(post.instant),
                style = MaterialTheme.typography.labelSmall,
            )

            Spacer(Modifier.height(8.dp))

            HorizontalDivider()

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.replies),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
}

@Composable
private fun TopAppBar(
    onBack: () -> Unit,
    post: CommunityPostDetails.Plain,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        navigationIcon = {
            IconButton(
                onClick = onBack,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = null,
                )
            }
        },
        title = { Text(stringResource(R.string.post)) },
        scrollBehavior = scrollBehavior,
        modifier = modifier,
    )
}

// TODO: make normal global date-time formatting utils
private fun formatDateTime(instant: Instant): String {
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val format = LocalDateTime.Format {
        year()
        chars("-")
        monthNumber()
        chars("-")
        day()

        chars(" ")

        hour()
        chars(":")
        minute()
    }
    val formattedDateTime = localDateTime.format(format)
    return formattedDateTime
}
