package friendly.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun FadingEdgesOverlay(color: Color, modifier: Modifier = Modifier) {
    val topFade = Brush.verticalGradient(
        0f to Color.Transparent,
        1f to Color.Red,
    )
    Box(
        modifier = modifier
            .fadingEdges(topFade)
            .background(color = color),
    )
}

private fun Modifier.fadingEdges(brush: Brush): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(brush = brush, blendMode = BlendMode.DstIn)
    }
