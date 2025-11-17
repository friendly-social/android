package friendly.android

import android.content.Context
import androidx.core.content.edit
import friendly.sdk.FileAccessHash
import friendly.sdk.FileDescriptor
import friendly.sdk.FileId
import friendly.sdk.Interest
import friendly.sdk.Nickname
import friendly.sdk.UserDescription

class SelfProfileStorage(context: Context) {
    companion object {
        private const val SELF_PROFILE_CACHE_PREFERENCES =
            "SELF_PROFILE_CACHE_PREFERENCES"
        private const val NICKNAME = "NICKNAME"
        private const val DESCRIPTION = "DESCRIPTION"
        private const val AVATAR_ACCESS_HASH = "AVATAR_ACCESS_HASH"
        private const val AVATAR_ID = "AVATAR_ID"
        private const val INTERESTS = "INTERESTS"
    }

    data class Cache(
        val nickname: Nickname,
        val description: UserDescription,
        val avatar: FileDescriptor,
        val interest: List<Interest>,
    )

    private val preferences = context.getSharedPreferences(
        SELF_PROFILE_CACHE_PREFERENCES,
        Context.MODE_PRIVATE,
    )

    fun store(
        nickname: Nickname,
        description: UserDescription,
        avatar: FileDescriptor,
        interests: List<Interest>,
    ) {
        preferences.edit {
            putString(NICKNAME, nickname.string)
            putString(DESCRIPTION, description.string)
            putString(AVATAR_ACCESS_HASH, avatar.accessHash.string)
            val interestsSet = interests
                .map { interest -> interest.string }
                .toSet()
            putStringSet(INTERESTS, interestsSet)
            putLong(AVATAR_ID, avatar.id.long)
            commit()
        }
    }

    fun getNickname(): Nickname? {
        val string = preferences.getString(NICKNAME, null)
        return string?.let { string -> Nickname.orThrow(string) }
    }

    fun getDescription(): UserDescription? {
        val string = preferences.getString(DESCRIPTION, null)
        return string?.let { string -> UserDescription.orThrow(string) }
    }

    fun getInterests(): List<Interest>? {
        val set = preferences.getStringSet(INTERESTS, null)
        return set?.map { string -> Interest.orThrow(string) }
    }

    fun getAvatar(): FileDescriptor? {
        val accessHash = preferences.getString(AVATAR_ACCESS_HASH, null)
        val id = preferences.getLong(AVATAR_ID, 0)
        return accessHash?.let { string ->
            FileDescriptor(
                id = FileId(id),
                accessHash = FileAccessHash.orThrow(accessHash),
            )
        }
    }

    fun getCache(): Cache? {
        return Cache(
            nickname = getNickname() ?: return null,
            description = getDescription() ?: return null,
            avatar = getAvatar() ?: return null,
            interest = getInterests() ?: return null,
        )
    }
}
