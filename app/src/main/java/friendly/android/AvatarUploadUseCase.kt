package friendly.android

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import friendly.sdk.FileAccessHash
import friendly.sdk.FileDescriptor
import friendly.sdk.FileId
import friendly.sdk.FilePreuploadAccessHash
import friendly.sdk.FilePreuploadDescriptor
import friendly.sdk.FilePreuploadId
import friendly.sdk.FriendlyClient
import friendly.sdk.FriendlyFilesClient.PreuploadFileResult
import io.ktor.http.ContentType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.io.asSource
import kotlinx.io.buffered
import java.io.InputStream

// todo: remove once sdk will be updated
fun FilePreuploadDescriptor.regular() = FileDescriptor(
    id = FileId(this.id.long),
    accessHash = FileAccessHash.orThrow(this.accessHash.string),
)

fun FileDescriptor.preupload() = FilePreuploadDescriptor(
    id = FilePreuploadId(this.id.long),
    accessHash = FilePreuploadAccessHash.orThrow(this.accessHash.string),
)

class AvatarUploadUseCase(
    private val client: FriendlyClient,
    private val context: Context,
) {
    @JvmInline
    value class UploadingPercentage(val float: Float)

    sealed interface UploadingResult {
        data class Success(val fileDescriptor: FilePreuploadDescriptor) :
            UploadingResult

        data object CompressionFailure : UploadingResult

        data class IOError(val cause: Exception) : UploadingResult

        data object ServerError : UploadingResult
    }

    private val contentResolver = context.contentResolver

    suspend operator fun invoke(
        avatarUri: Uri,
        block: suspend (Flow<UploadingPercentage>) -> Unit,
    ): UploadingResult {
        val inputStream = contentResolver
            .openInputStream(avatarUri)
            ?: error("Couldn't read $avatarUri from the storage")

        val fileName = getFileName(avatarUri)
        val fileDescriptorUploadResult =
            CompletableDeferred<PreuploadFileResult>()
        val compressor = AvatarAdjuster(
            uri = avatarUri,
            inputStream = inputStream,
            context = context,
        )

        val (compressedInputStream, compressedSize) = compressor.adjust()

        compressedInputStream.use { inputStream ->
            val flow = channelFlow {
                val uploadResult = uploadAvatar(
                    fileName = fileName,
                    size = compressedSize,
                    inputStream = inputStream,
                )
                fileDescriptorUploadResult.complete(uploadResult)
            }
            block(flow)
        }

        return when (val result = fileDescriptorUploadResult.await()) {
            is IOError -> {
                UploadingResult.IOError(result.cause)
            }

            is ServerError -> {
                UploadingResult.ServerError
            }

            is Success -> {
                UploadingResult.Success(result.descriptor)
            }
        }
    }

    private suspend fun ProducerScope<UploadingPercentage>.uploadAvatar(
        fileName: String,
        size: Long,
        inputStream: InputStream,
    ): PreuploadFileResult {
        val uploadResult = client.files.preupload(
            filename = fileName,
            contentType = ContentType.Image.Any,
            size = size,
            onUpload = { sent, total ->
                val percent = sent.toFloat() / size * 100
                send(UploadingPercentage(percent))
                Log.d("avatar", "$sent of $total (${"%.0f".format(percent)}%)")
            },
        ) {
            inputStream
                .asSource()
                .buffered()
                .transferTo(this)
        }
        return uploadResult
    }

    private fun getFileName(avatarUri: Uri): String {
        fun useCursor(cursor: Cursor): String? {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)

            return if (cursor.moveToFirst() && nameIndex != -1) {
                cursor.getString(nameIndex)
            } else {
                null
            }
        }

        return contentResolver
            .query(avatarUri, null, null, null, null)
            ?.use(::useCursor)
            ?: "avatar"
    }
}
