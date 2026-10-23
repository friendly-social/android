
package friendly.android

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import kotlin.time.Instant

@Composable
fun RelativeTimestampText(
    instant: Instant,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
) {
    val now = rememberNow()

    Text(
        text = formatInstantRelatively(now, instant),
        color = color,
        style = style,
        modifier = modifier,
    )
}
