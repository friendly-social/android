package friendly.android

import android.os.Parcel
import friendly.sdk.Email
import kotlinx.parcelize.Parceler

object EmailParceler : Parceler<Email> {
    override fun create(parcel: Parcel) = Email.orThrow(parcel.readString()!!)

    override fun Email.write(parcel: Parcel, flags: Int) {
        parcel.writeString(string)
    }
}
