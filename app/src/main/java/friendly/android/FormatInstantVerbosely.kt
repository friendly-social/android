package friendly.android

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

fun formatInstantVerbosely(instant: Instant): String {
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val format = LocalDateTime.Format {
        year()
        chars("-")
        monthNumber()
        chars("-")
        day()

        chars(" ")

        hour()
        chars(":")
        minute()
    }
    val formattedDateTime = localDateTime.format(format)
    return formattedDateTime
}
