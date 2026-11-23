package friendly.android

import friendly.sdk.FriendToken

@JvmInline
value class FriendInviteLink(val string: String) {
    fun asFriendToken(): FriendToken? {
        val friendToken = string
            .split("invite.getfriend.ly/")
            .lastOrNull { it.length == 16 }
            ?: return null
        return FriendToken.orThrow(friendToken)
    }

    companion object {
        fun validate(string: String): Boolean {
            // TODO: add proper validation
            return string.isNotBlank()
        }
    }
}
