package friendly.android

sealed interface NotificationDestinationExtra {
    data object Network : NotificationDestinationExtra
    data object Activity : NotificationDestinationExtra
    data object Feed : NotificationDestinationExtra

    fun raw(): String = when (this) {
        is Activity -> "activity"
        is Feed -> "feed"
        is Network -> "network"
    }

    companion object {
        const val EXTRAS_KEY = "destination"

        fun ofRaw(string: String): NotificationDestinationExtra =
            when (string) {
                "activity" -> Activity
                "network" -> Network
                "feed" -> Feed
                else -> error("Unknown NotificationDestinationExtra: $string")
            }
    }
}
