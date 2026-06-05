package friendly.android

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import kotlin.math.roundToInt

private const val SegmentsAmount = 8

@Composable
fun EightDigitVerificationCodeField(
    value: String,
    onValueChange: (String) -> Unit,
    codeVerificationFailed: Boolean,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val transparentSelectionColors = TextSelectionColors(
        handleColor = Color.Transparent,
        backgroundColor = Color.Transparent,
    )

    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(codeVerificationFailed) {
        if (codeVerificationFailed) {
            shakeOffset.animateTo(
                targetValue = 20f,
                animationSpec = keyframes {
                    durationMillis = 220
                    15f at 40
                    -25f at 100
                    10f at 150
                    -10f at 180
                    5f at 200
                    0f at 220
                },
            )
        }
    }

    Box(
        contentAlignment = Center,
        modifier = modifier.offset {
            IntOffset(x = shakeOffset.value.roundToInt(), y = 0)
        },
    ) {
        CompositionLocalProvider(
            LocalTextSelectionColors provides transparentSelectionColors,
        ) {
            BasicTextField(
                value = value,
                onValueChange = { new ->
                    val valid =
                        new.isDigitsOnly() && new.length <= SegmentsAmount
                    if (valid) onValueChange(new)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = NumberPassword,
                ),
                cursorBrush = SolidColor(Color.Unspecified),
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0f),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        ) {
            for (segmentIndex in 0..<SegmentsAmount / 2) {
                val char: String = value
                    .getOrNull(segmentIndex)
                    ?.toString()
                    ?: ""
                val isFocused = segmentIndex == value.length
                Segment(value = char, isFocused = isFocused, isError = isError)
            }

            HorizontalDivider(
                Modifier
                    .width(24.dp)
                    .padding(4.dp),
            )

            for (segmentIndex in SegmentsAmount / 2..<SegmentsAmount) {
                val char: String = value
                    .getOrNull(segmentIndex)
                    ?.toString()
                    ?: ""
                val isFocused = segmentIndex == value.length
                Segment(value = char, isFocused = isFocused, isError = isError)
            }
        }
    }
}

@Composable
private fun Segment(
    value: String,
    isFocused: Boolean,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Center,
        modifier = modifier
            .padding(horizontal = 4.dp)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) {
                    MaterialTheme.colorScheme.primary
                } else if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
                shape = RoundedCornerShape(12.dp),
            )
            .size(height = 48.dp, width = 32.dp),
    ) {
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(24.dp),
        )
    }
}
