package friendly.android

import android.os.Parcelable
import friendly.sdk.Authorization
import friendly.sdk.Email
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

@Parcelize
@TypeParceler<Email, EmailParceler>
@TypeParceler<Authorization?, AuthorizationParceler>
data class EmailCodeLoginState(
    val email: Email,
    val authorization: Authorization?,
    val success: Boolean,
) : Parcelable
