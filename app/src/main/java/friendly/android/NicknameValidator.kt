package friendly.android

import friendly.sdk.Nickname

private val validRange = 4..30
private val regex = Regex("^[A-Za-z0-9]+$")

fun Nickname.Companion.validate(value: String): Boolean {
    val hasLengthValid = value.length in validRange
    val hasAlphanumeric = regex.matches(value)

    return hasLengthValid && hasAlphanumeric
}
