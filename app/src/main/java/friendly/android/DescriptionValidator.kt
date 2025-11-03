package friendly.android

import friendly.sdk.UserDescription

private val validRange = 4..1000

fun UserDescription.Companion.validate(value: String): Boolean {
    val hasLengthValid = value.length in validRange
    return hasLengthValid
}
