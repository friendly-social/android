package friendly.android

import friendly.sdk.Interest
import friendly.sdk.Nickname
import friendly.sdk.UserDescription

sealed interface FeedItem {
    data object Loading : FeedItem

    data class Entry(
        val nickname: Nickname,
        val description: UserDescription,
        val interests: List<Interest>,
    ) : FeedItem
}
