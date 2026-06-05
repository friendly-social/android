package friendly.android

import friendly.sdk.Email

private val regexp = Regex(
    "^(?:[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?|\\[(?:(?:(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])|IPv6:[a-fA-F0-9:.]+)])$",
)

fun Email.Companion.validate(string: String): Boolean = regexp.matches(string)
