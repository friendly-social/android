package friendly.android

import android.net.Uri
import friendly.sdk.Interest
import friendly.sdk.Nickname
import friendly.sdk.UserAccessHash
import friendly.sdk.UserDescription
import friendly.sdk.UserId

data class FeedEntry(
    val id: UserId,
    val accessHash: UserAccessHash,
    val nickname: Nickname,
    val description: UserDescription,
    val interests: List<Interest>,
    val avatarUri: Uri?,
    val isRequest: Boolean,
    val isExtendedNetwork: Boolean,
    // TODO: commonFriends
    // TODO: isExtendedNetwork
)
