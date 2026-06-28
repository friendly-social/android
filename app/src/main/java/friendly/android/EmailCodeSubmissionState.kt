package friendly.android

import android.os.Parcelable
import friendly.sdk.Email
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

// TODO: note for future feature-split architecture: this could be perfectly
//  moved out to the feature layer.

/**
 * This class represents one verification code entry attempt.
 */
@Parcelize
@TypeParceler<Email, EmailParceler>
data class EmailCodeSubmissionState(
    val email: Email,
    val successful: Boolean,
) : Parcelable
