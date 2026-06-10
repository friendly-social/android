package friendly.android

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
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
        /* Extract EXIF metadata to fix orientation issues.
         * Some device cameras save images rotated sideways alongside an EXIF orientation flag
         * rather than encoding the physical pixel array right-side up.
         */
        val rotationDegrees = getRotationDegrees(uri)

        val bitmap = BitmapFactory.decodeStream(inputStream)
            ?: error("Failed to decode image")
        val rotatedBitmap = rotateBitmapIfNeeded(bitmap, rotationDegrees)

        val croppedDimension = minOf(bitmap.width, bitmap.height)
        val croppedBitmap = Bitmap
            .createBitmap(rotatedBitmap, 0, 0, croppedDimension, croppedDimension)
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

    private fun getRotationDegrees(uri: Uri): Int {
        return contentResolver.openInputStream(uri)?.use { stream ->
            val exif = ExifInterface(stream)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
    }

    private fun rotateBitmapIfNeeded(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
