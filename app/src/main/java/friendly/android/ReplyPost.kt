package friendly.android

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import friendly.sdk.CommunityPostDescriptor
import friendly.sdk.CommunityPostDetails
import friendly.sdk.CommunityPostReply

@Composable
fun ReplyPost(
    reply: CommunityPostReply,
    onClick: (CommunityPostDescriptor) -> Unit,
    onImageClick: (String) -> Unit,
    vm: CommunityPostScreenViewModel,
    modifier: Modifier = Modifier,
) {
    when (reply) {
        is CommunityPostReply.Single -> {
            SingleReply(
                onClick = onClick,
                reply = reply,
                vm = vm,
                onImageClick = onImageClick,
                modifier = modifier,
            )
        }

        is CommunityPostReply.Thread -> {
            ReplyThread(
                reply = reply,
                onClick = onClick,
                vm = vm,
                onImageClick = onImageClick,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun DeletedSingleReply(
    reply: CommunityPostDetails.Deleted,
    vm: CommunityPostScreenViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        RelativeTimestampText(
            instant = reply.instant,
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.reply_deleted_by_author_text),
            fontStyle = FontStyle.Italic,
            modifier = Modifier,
        )
        Spacer(Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth(),
        ) {
            CommunityPostScreenReplyPreviews(
                vm = vm,
                replyPreviews = reply.replyPreviews,
                modifier = Modifier,
            )
        }
    }
}

@Composable
private fun SingleReply(
    onClick: (CommunityPostDescriptor) -> Unit,
    reply: CommunityPostReply.Single,
    vm: CommunityPostScreenViewModel,
    onImageClick: (String) -> Unit,
    modifier: Modifier,
) {
    OutlinedCard(
        onClick = { onClick(reply.post.descriptor) },
        modifier = modifier,
    ) {
        when (val replyPost = reply.post) {
            is Deleted -> {
                DeletedSingleReply(
                    reply = replyPost,
                    vm = vm,
                    modifier = Modifier.padding(
                        vertical = 8.dp,
                        horizontal = 8.dp,
                    ),
                )
            }

            is Plain -> {
                Column(
                    modifier = Modifier
                        .padding(16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        UserAvatar(
                            nickname = replyPost.owner.nickname,
                            userId = replyPost.owner.id,
                            uri = replyPost.owner.avatar?.let(vm::fileUri),
                            style = UserAvatarStyle.Small,
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = replyPost.owner.nickname.string,
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Spacer(Modifier.height(4.dp))
                            RelativeTimestampText(
                                instant = replyPost.instant,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    BoxWithConstraints(
                        modifier = modifier
                            .fillMaxWidth(),
                    ) {
                        val imageWidth = maxWidth

                        CommunityPostMarkdown(
                            markdown = replyPost.text.string,
                            imageWidth = imageWidth,
                            onImageClick = onImageClick,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        CommunityPostScreenReplyPreviews(
                            vm = vm,
                            replyPreviews = replyPost.replyPreviews,
                            modifier = Modifier,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplyThread(
    reply: CommunityPostReply.Thread,
    onClick: (CommunityPostDescriptor) -> Unit,
    vm: CommunityPostScreenViewModel,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        reply.thread.forEachIndexed { index, replyPost ->
            when (replyPost) {
                is Deleted -> {
                    DeletedSingleReply(
                        reply = replyPost,
                        vm = vm,
                        modifier = Modifier.padding(
                            vertical = 8.dp,
                            horizontal = 8.dp,
                        ),
                    )
                }

                is Plain -> {
                    Column(
                        modifier = Modifier
                            .clickable(
                                onClick = { onClick(replyPost.descriptor) },
                            )
                            .padding(16.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            UserAvatar(
                                nickname = replyPost.owner.nickname,
                                userId = replyPost.owner.id,
                                uri = replyPost.owner.avatar?.let(vm::fileUri),
                                style = UserAvatarStyle.Small,
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = replyPost.owner.nickname.string,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                Spacer(Modifier.height(4.dp))
                                RelativeTimestampText(
                                    instant = replyPost.instant,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        BoxWithConstraints(
                            modifier = modifier
                                .fillMaxWidth(),
                        ) {
                            val imageWidth = maxWidth

                            CommunityPostMarkdown(
                                markdown = replyPost.text.string,
                                imageWidth = imageWidth,
                                onImageClick = onImageClick,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (index == reply.thread.size - 1) {
                                CommunityPostScreenReplyPreviews(
                                    vm = vm,
                                    replyPreviews = replyPost.replyPreviews,
                                    modifier = Modifier,
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                        }
                    }
                }
            }
            HorizontalDivider()
        }
    }
}
