package friendly.android

import androidx.compose.ui.graphics.Color

fun Color.darken(darkenBy: Float = 0.8f): Color = copy(
    red = red * darkenBy,
    green = green * darkenBy,
    blue = blue * darkenBy,
    alpha = alpha,
)
