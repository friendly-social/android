package friendly.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonDefaults.filledIconButtonColors
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun CommunityPostReplyField(
    fieldState: TextFieldState,
    onSendClick: () -> Unit,
    onMaximizeClick: () -> Unit,
    textValid: Boolean,
    isSending: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(4.dp)
                .height(IntrinsicSize.Min),
        ) {
            ReplyBasicTextField(
                fieldState = fieldState,
                modifier = Modifier.weight(1f),
            )
            ReplyActions(
                onSendClick = onSendClick,
                onMaximizeClick = onMaximizeClick,
                fieldState = fieldState,
                textValid = textValid,
                isSending = isSending,
                modifier = Modifier.fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun ReplyActions(
    onMaximizeClick: () -> Unit,
    onSendClick: () -> Unit,
    fieldState: TextFieldState,
    textValid: Boolean,
    isSending: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Bottom,
        modifier = modifier,
    ) {
        if (fieldState.text.isNotBlank()) {
            FilledIconButton(
                enabled = textValid && !isSending,
                onClick = onSendClick,
            ) {
                if (isSending) {
                    LoadingIndicator(
                        color = filledIconButtonColors().disabledContentColor,
                        modifier = Modifier.size(
                            IconButtonDefaults.mediumIconSize,
                        ),
                    )
                } else {
                    Icon(
                        painter = painterResource(
                            R.drawable.ic_send,
                        ),
                        contentDescription = null,
                    )
                }
            }
        } else {
            IconButton(
                onClick = onMaximizeClick,
            ) {
                Icon(
                    painter = painterResource(
                        R.drawable.ic_open_in_full,
                    ),
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun ReplyBasicTextField(
    fieldState: TextFieldState,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        state = fieldState,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onBackground,
        ),
        lineLimits = TextFieldLineLimits
            .MultiLine(1, 10),
        cursorBrush = SolidColor(
            MaterialTheme.colorScheme.onBackground,
        ),
        decorator = { innerField ->
            if (fieldState.text.isEmpty()) {
                Text(
                    text = stringResource(
                        R.string.minimized_reply_field_placeholder,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            innerField()
        },
        modifier = modifier.padding(8.dp),
    )
}
