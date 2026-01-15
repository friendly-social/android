package friendly.android

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import friendly.sdk.FileDescriptor
import friendly.sdk.FriendlyClient
import friendly.sdk.FriendlyFilesClient.UploadFileResult
import io.ktor.http.ContentType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.io.asSource
import kotlinx.io.buffered
import java.io.InputStream

class AvatarUploadUseCase(
    private val client: FriendlyClient,
    private val context: Context,
) {
    companion object {
        private const val MAX_IMAGE_SIZE = 2048L
        private const val COMPRESSION_QUALITY = 75
        private const val MAX_FILE_SIZE_BYTES = 2L * 1024L
    }

    @JvmInline
    value class UploadingPercentage(val double: Double)

    sealed interface UploadingResult {
        data class Success(val fileDescriptor: FileDescriptor) : UploadingResult

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
        val fileDescriptorUploadResult = CompletableDeferred<UploadFileResult>()
        val compressor = ImageCompressor(
            uri = avatarUri,
            inputStream = inputStream,
            maxImageSize = MAX_IMAGE_SIZE,
            compressionQuality = COMPRESSION_QUALITY,
            maxFileSizeBytes = MAX_FILE_SIZE_BYTES,
            context = context,
        )

        val (compressedInputStream, compressedSize) = compressor.compress()

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
            is UploadFileResult.IOError -> {
                UploadingResult.IOError(result.cause)
            }

            is UploadFileResult.ServerError -> UploadingResult.ServerError

            is UploadFileResult.Success -> {
                UploadingResult.Success(result.descriptor)
            }
        }
    }

    private suspend fun ProducerScope<UploadingPercentage>.uploadAvatar(
        fileName: String,
        size: Long,
        inputStream: InputStream,
    ): UploadFileResult {
        val uploadResult = client.files.upload(
            filename = fileName,
            contentType = ContentType.Image.Any,
            size = size,
            onUpload = { sent, total ->
                val percent = sent.toDouble() / size * 100
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
