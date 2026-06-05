package friendly.android

import android.util.Patterns
import friendly.sdk.SocialLink

fun SocialLink.Companion.validate(string: String): Boolean {
    return Patterns.WEB_URL.matcher(string).matches()
}
