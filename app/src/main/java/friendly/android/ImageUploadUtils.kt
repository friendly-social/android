package friendly.android

import android.content.ContentResolver
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import kotlinx.io.asSource
import kotlinx.io.buffered
import java.io.ByteArrayOutputStream
import java.io.InputStream

private const val NewAvatarDimension = 500
private const val MaxAvatarSize = 16_384L
private const val StartQuality = 80
private const val QualityStep = 10
private const val MinQuality = 40

object ImageUploadUtils {
    class CompressedImage(private val bytes: ByteArray) {
        val size get() = bytes.size

        fun source() = bytes.inputStream().asSource().buffered()
    }

    /**
     * Returns [InputStream] of an adjusted image: properly scaled and
     * compressed image
     */
    fun compressedImage(
        contentResolver: ContentResolver,
        uri: Uri,
    ): CompressedImage {
        val inputStream = contentResolver.openInputStream(uri)

        /* Extract EXIF metadata to fix orientation issues.
         * Some device cameras save images rotated sideways alongside an EXIF orientation flag
         * rather than encoding the physical pixel array right-side up.
         */
        val rotationDegrees = getRotationDegrees(uri, contentResolver)

        val bitmap = BitmapFactory.decodeStream(inputStream)
            ?: error("Failed to decode image")
        val rotatedBitmap = rotateBitmapIfNeeded(bitmap, rotationDegrees)

        val croppedDimension = minOf(bitmap.width, bitmap.height)
        val croppedBitmap = Bitmap.createBitmap(
            rotatedBitmap,
            0,
            0,
            croppedDimension,
            croppedDimension,
        )
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

        return CompressedImage(outputStream.toByteArray())
    }

    fun fileName(contentResolver: ContentResolver, uri: Uri): String {
        fun useCursor(cursor: Cursor): String? {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)

            return if (cursor.moveToFirst() && nameIndex != -1) {
                cursor.getString(nameIndex)
            } else {
                null
            }
        }

        return contentResolver
            .query(uri, null, null, null, null)
            ?.use(::useCursor)
            ?: "avatar"
    }
}

private fun getRotationDegrees(
    uri: Uri,
    contentResolver: ContentResolver,
): Int = contentResolver.openInputStream(uri)?.use { stream ->
    val exif = ExifInterface(stream)
    when (
        exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
    ) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
} ?: 0

private fun rotateBitmapIfNeeded(bitmap: Bitmap, degrees: Int): Bitmap {
    if (degrees == 0) return bitmap
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(
        bitmap,
        0,
        0,
        bitmap.width,
        bitmap.height,
        matrix,
        true,
    )
}
