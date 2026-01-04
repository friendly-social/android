package friendly.android

import androidx.compose.ui.graphics.Color

private const val DJB_SHIFT_OFFSET = 5
private const val BITS_PER_CHANNEL = 8
private const val BYTE_MASK = 0x16

private val darkPastelColors = listOf(
    Color(19, 37, 54, 255),
    Color(68, 83, 36),
    Color(43, 73, 63),
    Color(90, 13, 5, 255),
    Color(58, 88, 84),
    Color(58, 42, 8),
    Color(100, 66, 108),
    Color(23, 17, 80),
    Color(127, 36, 126),
    Color(120, 30, 32),
    Color(59, 56, 100),
    Color(149, 52, 53),
    Color(50, 47, 94, 255),
    Color(14, 11, 27),
    Color(121, 32, 59),
    Color(93, 42, 18),
    Color(124, 59, 70),
    Color(55, 62, 22, 255),
    Color(54, 116, 78),
    Color(100, 36, 71),
    Color(44, 61, 19),
    Color(34, 32, 171),
    Color(63, 7, 78, 255),
)

private val lightPastelColors = listOf(
    Color(71, 104, 188),
    Color(197, 214, 160),
    Color(168, 203, 192),
    Color(190, 119, 81, 255),
    Color(189, 212, 209),
    Color(232, 187, 89),
    Color(220, 204, 224),
    Color(220, 107, 107, 255),
    Color(235, 184, 234),
    Color(104, 234, 173, 255),
    Color(193, 191, 220),
    Color(241, 215, 215),
    Color(226, 162, 224),
    Color(121, 99, 193),
    Color(234, 174, 192),
    Color(232, 165, 135),
    Color(232, 206, 211),
    Color(106, 125, 177),
    Color(197, 228, 209),
    Color(223, 168, 198),
    Color(173, 193, 10, 255),
    Color(212, 211, 247),
    Color(89, 165, 180),
)

fun Color.Companion.pastelFromString(string: String, useDark: Boolean): Color {
    val utf16String = string.toByteArray(Charsets.UTF_16)
    val hash = utf16String.fold(0) { hash, char ->
        val shifted = hash shl DJB_SHIFT_OFFSET
        char + shifted - hash
    }

    val num = (hash shr (0 * BITS_PER_CHANNEL)) and BYTE_MASK

    return if (useDark) darkPastelColors[num] else lightPastelColors[num]
}
