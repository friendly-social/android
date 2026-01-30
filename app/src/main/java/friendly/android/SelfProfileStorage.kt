package friendly.android

import android.content.Context
import androidx.core.content.edit
import friendly.sdk.FileAccessHash
import friendly.sdk.FileDescriptor
import friendly.sdk.FileId
import friendly.sdk.Interest
import friendly.sdk.InterestList
import friendly.sdk.Nickname
import friendly.sdk.SocialLink
import friendly.sdk.UserDescription
import friendly.sdk.UserId

class SelfProfileStorage(context: Context) {
    companion object {
        private const val SELF_PROFILE_CACHE_PREFERENCES =
            "SELF_PROFILE_CACHE_PREFERENCES"
        private const val NICKNAME = "NICKNAME"
        private const val DESCRIPTION = "DESCRIPTION"
        private const val AVATAR_ACCESS_HASH = "AVATAR_ACCESS_HASH"
        private const val AVATAR_ID = "AVATAR_ID"
        private const val USER_ID = "USER_ID"
        private const val INTERESTS = "INTERESTS"
        private const val SOCIAL_LINK = "SOCIAL_LINK"
    }

    data class Cache(
        val nickname: Nickname,
        val description: UserDescription,
        val avatar: FileDescriptor?,
        val interests: InterestList,
        val socialLink: SocialLink?,
        val userId: UserId,
    )

    private val preferences = context.getSharedPreferences(
        SELF_PROFILE_CACHE_PREFERENCES,
        Context.MODE_PRIVATE,
    )

    fun store(
        nickname: Nickname,
        userId: UserId,
        description: UserDescription,
        avatar: FileDescriptor?,
        interests: InterestList,
        socialLink: SocialLink,
    ) {
        preferences.edit {
            putString(NICKNAME, nickname.string)
            putString(DESCRIPTION, description.string)
            avatar?.accessHash?.string?.let { avatarAccessHash ->
                putString(AVATAR_ACCESS_HASH, avatarAccessHash)
            }
            val interestsSet = interests.raw
                .map { interest -> interest.string }
                .toSet()
            putStringSet(INTERESTS, interestsSet)
            avatar?.id?.long?.let { long ->
                putLong(AVATAR_ID, long)
            }
            putLong(USER_ID, userId.long)
            putString(SOCIAL_LINK, socialLink.string)
            commit()
        }
    }

    fun getNickname(): Nickname? {
        val string = preferences.getString(NICKNAME, null)
        return string?.let { string -> Nickname.orThrow(string) }
    }

    fun getSocialLink(): SocialLink? {
        val string = preferences.getString(SOCIAL_LINK, null)
        return string?.let { string -> SocialLink.orThrow(string) }
    }

    fun getUserId(): UserId {
        val long = preferences.getLong(USER_ID, 0L)
        return UserId(long)
    }

    fun getDescription(): UserDescription? {
        val string = preferences.getString(DESCRIPTION, null)
        return string?.let { string -> UserDescription.orThrow(string) }
    }

    fun getInterests(): InterestList? {
        val set = preferences.getStringSet(INTERESTS, null)
        return set?.let { interestsSet ->
            InterestList.orThrow(
                raw = interestsSet.map { string -> Interest.orThrow(string) },
            )
        }
    }

    fun getAvatar(): FileDescriptor? {
        val accessHash = preferences.getString(AVATAR_ACCESS_HASH, null)
        val id = preferences.getLong(AVATAR_ID, 0)
        return accessHash?.let { accessHash ->
            FileDescriptor(
                id = FileId(id),
                accessHash = FileAccessHash.orThrow(accessHash),
            )
        }
    }

    fun clear() {
        preferences.edit {
            this.clear()
            commit()
        }
    }

    fun getCache(): Cache? {
        return Cache(
            nickname = getNickname() ?: return null,
            description = getDescription() ?: return null,
            avatar = getAvatar(),
            interests = getInterests() ?: return null,
            socialLink = getSocialLink(),
            userId = getUserId(),
        )
    }
}
