package friendly.android

import friendly.sdk.CommunityPostText

fun CommunityPostText.Companion.validate(string: String): Boolean =
    string.length < CommunityPostText.MaxLength && string.isNotBlank()
