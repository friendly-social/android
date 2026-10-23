package friendly.android

import android.icu.text.RelativeDateTimeFormatter
import android.icu.text.RelativeDateTimeFormatter.AbsoluteUnit.NOW
import android.icu.text.RelativeDateTimeFormatter.Direction.LAST
import android.icu.text.RelativeDateTimeFormatter.RelativeUnit.DAYS
import android.icu.text.RelativeDateTimeFormatter.RelativeUnit.HOURS
import android.icu.text.RelativeDateTimeFormatter.RelativeUnit.MINUTES
import kotlin.time.Instant

fun formatInstantRelatively(
    now: Instant,
    target: Instant,
): String {
    val relativeFormatter = RelativeDateTimeFormatter.getInstance()

    val duration = now - target

    relativeFormatter.format(12.0, LAST, MINUTES)

    return when {
        duration.inWholeMinutes < 1 -> relativeFormatter.format(
            RelativeDateTimeFormatter.Direction.PLAIN,
            NOW,
        )

        duration.inWholeHours < 1 -> relativeFormatter.format(
            duration.inWholeMinutes.toDouble(),
            LAST,
            MINUTES,
        )


        duration.inWholeDays < 1 -> relativeFormatter.format(
            duration.inWholeHours.toDouble(),
            LAST,
            HOURS,
        )

        duration.inWholeDays <= 7 -> relativeFormatter.format(
            duration.inWholeDays.toDouble(),
            LAST,
            DAYS,
        )

        else -> formatInstantVerbosely(target)
    }
}
