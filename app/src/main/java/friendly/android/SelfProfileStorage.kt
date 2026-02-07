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

private const val SelfProfileCachePreferences =
    "SELF_PROFILE_CACHE_PREFERENCES"
private const val NicknamePreference = "NICKNAME"
private const val DescriptionPreference = "DESCRIPTION"
private const val AvatarAccessHashPreference = "AVATAR_ACCESS_HASH"
private const val AvatarIdPreference = "AVATAR_ID"
private const val UserIdPreference = "USER_ID"
private const val InterestsPreference = "INTERESTS"
private const val SocialLinkPreference = "SOCIAL_LINK"

class SelfProfileStorage(context: Context) {
    data class Cache(
        val nickname: Nickname,
        val description: UserDescription,
        val avatar: FileDescriptor?,
        val interests: InterestList,
        val socialLink: SocialLink?,
        val userId: UserId,
    )

    private val preferences = context.getSharedPreferences(
        SelfProfileCachePreferences,
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
            putString(NicknamePreference, nickname.string)
            putString(DescriptionPreference, description.string)
            avatar?.accessHash?.string?.let { avatarAccessHash ->
                putString(AvatarAccessHashPreference, avatarAccessHash)
            }
            val interestsSet = interests.raw
                .map { interest -> interest.string }
                .toSet()
            putStringSet(InterestsPreference, interestsSet)
            avatar?.id?.long?.let { long ->
                putLong(AvatarIdPreference, long)
            }
            putLong(UserIdPreference, userId.long)
            putString(SocialLinkPreference, socialLink.string)
            commit()
        }
    }

    fun getNickname(): Nickname? {
        val string = preferences.getString(NicknamePreference, null)
        return string?.let { string -> Nickname.orThrow(string) }
    }

    fun getSocialLink(): SocialLink? {
        val string = preferences.getString(SocialLinkPreference, null)
        return string?.let { string -> SocialLink.orThrow(string) }
    }

    fun getUserId(): UserId {
        val long = preferences.getLong(UserIdPreference, 0L)
        return UserId(long)
    }

    fun getDescription(): UserDescription? {
        val string = preferences.getString(DescriptionPreference, null)
        return string?.let { string -> UserDescription.orThrow(string) }
    }

    fun getInterests(): InterestList? {
        val set = preferences.getStringSet(InterestsPreference, null)
        return set?.let { interestsSet ->
            InterestList.orThrow(
                raw = interestsSet.map { string -> Interest.orThrow(string) },
            )
        }
    }

    fun getAvatar(): FileDescriptor? {
        val accessHash = preferences.getString(AvatarAccessHashPreference, null)
        val id = preferences.getLong(AvatarIdPreference, 0)
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
