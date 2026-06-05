package friendly.android

import android.net.Uri
import android.util.Patterns
import androidx.core.net.toUri
import friendly.sdk.SocialLink

/**
 * Automatically uses https protocol if no one was specified.
 *
 * @return A valid [Uri] if [SocialLink] is a valid URI or null in it is
 * invalid.
 */
fun SocialLink.toNormalizedUri(): Uri? {
    val string = this.string

    if (!Patterns.WEB_URL.matcher(string).matches()) return null

    val startsWithHttp = string.startsWith("http://", ignoreCase = true)
    val startsWithHttps = string.startsWith("https://", ignoreCase = true)
    if (startsWithHttp || startsWithHttps) return string.toUri()

    return "https://$string".toUri()
}
