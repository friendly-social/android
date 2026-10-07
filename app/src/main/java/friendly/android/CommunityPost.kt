package friendly.android

import android.net.Uri
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.model.ImageData
import com.mikepenz.markdown.model.ImageTransformer
import com.mikepenz.markdown.model.markdownAnimations
import com.mikepenz.markdown.model.markdownAnnotator
import com.mikepenz.markdown.model.markdownAnnotatorConfig
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes
import friendly.sdk.CommunityPostDescriptor
import friendly.sdk.CommunityPostDetails
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@Composable
fun CommunityPostPreview(
    details: CommunityPostDetails,
    avatarUri: Uri?,
    onClick: (CommunityPostDescriptor) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (details) {
        is CommunityPostDetails.Deleted -> {
            Text("Deleted, TODO")
        }

        is CommunityPostDetails.Plain -> {
            PlainPostViewer(
                details = details,
                onClick = onClick,
                avatarUri = avatarUri,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun PlainPostViewer(
    details: CommunityPostDetails.Plain,
    avatarUri: Uri?,
    onClick: (CommunityPostDescriptor) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDarkTheme = isSystemInDarkTheme()

    val highlightsBuilder = remember(isDarkTheme) {
        Highlights.Builder().theme(SyntaxThemes.atom(darkMode = isDarkTheme))
    }

    Card(
        onClick = { onClick(details.descriptor) },
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(
                vertical = 12.dp,
                horizontal = 12.dp,
            ),
        ) {
            UserAvatar(
                userId = details.owner.id,
                nickname = details.owner.nickname,
                // todo idk move to the separate model
                uri = avatarUri,
                style = UserAvatarStyle.Small,
                modifier = Modifier,
            )
            Spacer(Modifier.width(4.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row {
                    Text(
                        text = details.owner.nickname.string,
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = formatDateTime(details.instant),
                        fontSize = 12.sp,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Light,
                    )
                    Spacer(Modifier.width(6.dp))
                    if (details.edited) {
                        Text(
                            text = "[ed1t3d]",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }


                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Markdown(
                        content = details.text.string,
                        imageTransformer = object : ImageTransformer {
                            @Composable
                            override fun transform(link: String): ImageData? {
                                return ImageData(
                                    painter = painterResource(
                                        R.drawable.ic_construction_filled,
                                    ),
                                )
                            }
                        },
                        annotator = markdownAnnotator(
                            config = markdownAnnotatorConfig(inlineImageAsBlock = false),
                        ),
                        components = markdownComponents(
                            codeBlock = {
                                MarkdownHighlightedCodeBlock(
                                    content = it.content,
                                    node = it.node,
                                    highlightsBuilder = highlightsBuilder,
                                    showHeader = true,
                                )
                            },
                            codeFence = {
                                MarkdownHighlightedCodeFence(
                                    content = it.content,
                                    node = it.node,
                                    highlightsBuilder = highlightsBuilder,
                                    showHeader = true,
                                )
                            },
                        ),
                        animations = markdownAnimations(
                            animateTextSize = { this },
                        ),
                        modifier = Modifier
                            .heightIn(max = 300.dp)
                            .fillMaxWidth(),
                    )
                }
            }
        }
    }
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
