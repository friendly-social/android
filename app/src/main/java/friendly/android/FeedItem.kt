package friendly.android

import android.net.Uri
import friendly.sdk.Interest
import friendly.sdk.Nickname
import friendly.sdk.UserAccessHash
import friendly.sdk.UserDescription
import friendly.sdk.UserId

sealed interface FeedItem {
    data object Loading : FeedItem

    data class Entry(
        val id: UserId,
        val accessHash: UserAccessHash,
        val nickname: Nickname,
        val description: UserDescription,
        val interests: List<Interest>,
        val avatarUri: Uri?,
        // TODO: commonFriends
        // TODO: isExtendedNetwork
    ) : FeedItem
}
