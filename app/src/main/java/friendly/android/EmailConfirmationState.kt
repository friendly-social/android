package friendly.android

import android.os.Parcel
import android.os.Parcelable
import friendly.sdk.Email
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

// TODO: note for future feature-split architecture: this could be perfectly
//  moved out to the feature layer.

/**
 * This class represents one verification code entry attempt.
 */
@Parcelize
@TypeParceler<Email, EmailParceler>
data class EmailConfirmationState(val email: Email, val successful: Boolean) :
    Parcelable

private object EmailParceler : Parceler<Email> {
    override fun create(parcel: Parcel) = Email.orThrow(parcel.readString()!!)

    override fun Email.write(parcel: Parcel, flags: Int) {
        parcel.writeString(string)
    }
}
