package friendly.android

import android.content.Context
import android.net.Uri
import friendly.sdk.FriendlyClient
import friendly.sdk.FriendlyFilesClient.PreuploadFileResult
import io.ktor.http.ContentType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

class AvatarPreuploadUseCase(
    private val client: FriendlyClient,
    context: Context,
) {
    private val contentResolver = context.contentResolver

    data class UploadingPercentage(val percentage: Float)

    suspend operator fun invoke(
        avatarUri: Uri,
        block: suspend (Flow<UploadingPercentage>) -> Unit,
    ): PreuploadFileResult {
        val compressedImage = ImageUploadUtils
            .compressedImage(contentResolver, avatarUri)
        val fileSize = compressedImage.size.toLong()
        val fileName = ImageUploadUtils.fileName(contentResolver, avatarUri)

        val preuploadResult = CompletableDeferred<PreuploadFileResult>()

        val uploadingFlow = channelFlow {
            val result = client.files.preupload(
                filename = fileName,
                size = fileSize,
                contentType = ContentType.Image.Any,
                onUpload = { sent, _ ->
                    send(uploadingPercentage(sent, fileSize))
                },
            ) {
                compressedImage.source().transferTo(this)
            }
            preuploadResult.complete(result)
        }
        block(uploadingFlow)
        return preuploadResult.await()
    }

    private fun uploadingPercentage(
        sent: Long,
        size: Long,
    ): UploadingPercentage = UploadingPercentage(sent.toFloat() / size * 100)
}
