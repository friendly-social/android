package friendly.android

import friendly.sdk.SocialLink

fun SocialLink.Companion.validate(string: String): Boolean {
    return string.isNotBlank() // todo: regexp for url checking
}
