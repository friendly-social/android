package friendly.android

import friendly.sdk.FileAccessHash
import friendly.sdk.FileDescriptor
import friendly.sdk.FileId
import friendly.sdk.FilePreuploadAccessHash
import friendly.sdk.FilePreuploadDescriptor
import friendly.sdk.FilePreuploadId

/**
 * A temp solution
 */
data class CommonFileDescriptor(
    val id: FileId,
    val accessHash: FileAccessHash,
) {
    fun regularDescriptor(): FileDescriptor = FileDescriptor(id, accessHash)

    fun preuploadDescriptor(): FilePreuploadDescriptor =
        FilePreuploadDescriptor(
            id = FilePreuploadId(id.long),
            accessHash = FilePreuploadAccessHash.orThrow(accessHash.string),
        )
}

fun FileDescriptor.common(): CommonFileDescriptor = CommonFileDescriptor(
    id = this.id,
    accessHash = this.accessHash,
)

fun FilePreuploadDescriptor.common(): CommonFileDescriptor =
    CommonFileDescriptor(
        id = FileId(long = this.id.long),
        accessHash = FileAccessHash.orThrow(this.accessHash.string),
    )
