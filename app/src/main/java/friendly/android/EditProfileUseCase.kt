package friendly.android

import friendly.sdk.Field
import friendly.sdk.FileDescriptor
import friendly.sdk.FriendlyUsersClient
import friendly.sdk.InterestList
import friendly.sdk.Nickname
import friendly.sdk.SocialLink
import friendly.sdk.UserDescription

class EditProfileUseCase(
    private val authStorage: AuthStorage,
    private val selfProfileStorage: SelfProfileStorage,
    private val client: FriendlyUsersClient,
) {
    suspend operator fun invoke(
        newNickname: Field<Nickname>?,
        newDescription: Field<UserDescription>?,
        newInterests: Field<InterestList>?,
        newAvatar: Field<FileDescriptor?>?,
        newSocialLink: Field<SocialLink?>?,
    ): FriendlyUsersClient.EditResult {
        val auth = authStorage.getAuth()

        val result = client.edit(
            authorization = auth,
            nickname = newNickname,
            description = newDescription,
            interests = newInterests,
            avatar = newAvatar,
            socialLink = newSocialLink,
        )

        if (result is Success) {
            newNickname?.let { nickname ->
                selfProfileStorage.storeNickname(nickname.value)
            }
            newDescription?.let { description ->
                selfProfileStorage.storeDescription(description.value)
            }
            newInterests?.let { interests ->
                selfProfileStorage.storeInterests(interests.value)
            }
            newSocialLink?.let { socialLink ->
                selfProfileStorage.storeSocialLink(socialLink.value)
            }
            newAvatar?.let { avatar ->
                selfProfileStorage.storeAvatar(avatar.value)
            }
        }

        return result
    }
}
