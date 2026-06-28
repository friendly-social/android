package friendly.android

import android.os.Parcel
import friendly.sdk.Authorization
import friendly.sdk.Token
import friendly.sdk.UserAccessHash
import friendly.sdk.UserId
import kotlinx.parcelize.Parceler

object AuthorizationParceler : Parceler<Authorization?> {
    override fun Authorization?.write(
        parcel: Parcel,
        flags: Int,
    ) {
        when (this) {
            null -> parcel.writeBoolean(false)

            else -> {
                parcel.writeBoolean(true)
                parcel.writeLong(this.id.long)
                parcel.writeString(this.accessHash.string)
                parcel.writeString(this.token.string)
            }
        }
    }

    override fun create(parcel: Parcel): Authorization? {
        val isSuccess = parcel.readBoolean()
        if (!isSuccess) return null

        val id = UserId(parcel.readLong())
        val accessHash = UserAccessHash.orThrow(
            parcel.readString() ?: error("accessHash can not be null"),
        )
        val token = Token.orThrow(
            parcel.readString() ?: error("token can not be null"),
        )
        return Authorization(id, accessHash, token)
    }
}
