package friendly.android

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.scale
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

private const val NewAvatarDimension = 500
private const val MaxAvatarSize = 16_384L
private const val StartQuality = 80
private const val QualityStep = 10
private const val MinQuality = 40

class AvatarAdjuster(
    private val uri: Uri,
    private val inputStream: InputStream,
    context: Context,
) {
    private val contentResolver = context.contentResolver

    /**
     * Resizes, crops with aspect ration 1x1 and compresses avatar
     */
    fun adjust(): Pair<InputStream, Long> = adjustAvatar(uri, inputStream)

    private fun adjustAvatar(
        uri: Uri,
        inputStream: InputStream,
    ): Pair<InputStream, Long> {
        val bitmap = BitmapFactory.decodeStream(inputStream)
            ?: error("Failed to decode image")
        val croppedDimension = minOf(bitmap.width, bitmap.height)
        val croppedBitmap = Bitmap
            .createBitmap(bitmap, 0, 0, croppedDimension, croppedDimension)
        val scaledBitmap = croppedBitmap
            .scale(NewAvatarDimension, NewAvatarDimension)

        val outputStream = ByteArrayOutputStream()
        var quality = StartQuality
        do {
            outputStream.reset()
            scaledBitmap.compress(
                Bitmap.CompressFormat.JPEG,
                quality,
                outputStream,
            )
            quality -= QualityStep
        } while (outputStream.size() > MaxAvatarSize && quality > MinQuality)

        val byteArray = outputStream.toByteArray()

        return Pair(
            first = ByteArrayInputStream(outputStream.toByteArray()),
            second = byteArray.size.toLong(),
        )
    }
}
