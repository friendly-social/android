package friendly.android

import androidx.compose.ui.graphics.Color

private val lightPastels = listOf(
    Color(255, 179, 179),
    Color(255, 217, 153),
    Color(255, 242, 179),
    Color(204, 255, 179),
    Color(179, 255, 217),
    Color(179, 230, 255),
    Color(204, 191, 255),
    Color(242, 191, 255),
    Color(255, 191, 230),
    Color(230, 217, 191),
)

private val darkPastels = listOf(
    Color(153, 64, 64),
    Color(153, 102, 51),
    Color(140, 128, 51),
    Color(89, 128, 64),
    Color(64, 128, 102),
    Color(64, 102, 153),
    Color(89, 77, 140),
    Color(128, 77, 140),
    Color(153, 77, 115),
    Color(128, 102, 77),
)

fun Color.Companion.pastelFromString(string: String, useDark: Boolean): Color {
    val hash = string.toByteArray(Charsets.UTF_8).fold(0) { acc, byte ->
        (acc * 31 + byte.toInt()) and 0x7FFFFFFF
    }
    val pastels = if (useDark) darkPastels else lightPastels
    return pastels[hash % pastels.size]
}

private val knuthHashConstant = 2654435761L

fun Color.Companion.pastelFromLong(long: Long, useDark: Boolean): Color {
    val pastels = if (useDark) darkPastels else lightPastels
    val hash = (long * knuthHashConstant) and 0x7FFFFFF
    val index = hash.toInt() % pastels.size
    return pastels[index]
}
