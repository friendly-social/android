package friendly.android

import android.content.Context
import android.net.Uri
import friendly.sdk.FriendlyClient
import friendly.sdk.FriendlyFilesClient.UploadFileResult
import io.ktor.http.ContentType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

class AvatarUploadUseCase(
    private val client: FriendlyClient,
    private val authStorage: AuthStorage,
    context: Context,
) {
    private val contentResolver = context.contentResolver

    @JvmInline
    value class UploadingPercentage(val float: Float)

    suspend operator fun invoke(
        uri: Uri,
        block: suspend (Flow<UploadingPercentage>) -> Unit,
    ): UploadFileResult {
        val compressedImage = ImageUploadUtils
            .compressedImage(contentResolver, uri)
        val fileSize = compressedImage.size.toLong()
        val fileName = ImageUploadUtils.fileName(contentResolver, uri)

        val uploadResult = CompletableDeferred<UploadFileResult>()

        val uploadingFlow = channelFlow {
            val result = client.files.upload(
                authorization = authStorage.getAuth(),
                filename = fileName,
                size = fileSize,
                contentType = ContentType.Image.Any,
                onUpload = { sent, _ ->
                    send(uploadingPercentage(sent, fileSize))
                },
            ) {
                compressedImage.source().transferTo(this)
            }
            uploadResult.complete(result)
        }
        block(uploadingFlow)
        return uploadResult.await()
    }

    private fun uploadingPercentage(
        sent: Long,
        size: Long,
    ): UploadingPercentage = UploadingPercentage(sent.toFloat() / size * 100)
}
