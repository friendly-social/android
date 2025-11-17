package friendly.android

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import kotlinx.io.IOException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

class ImageCompressor(
    private val uri: Uri,
    private val inputStream: InputStream,
    private val maxImageSize: Long,
    private val compressionQuality: Int,
    private val maxFileSizeBytes: Long,
    private val context: Context,
) {
    private val contentResolver = context.contentResolver

    fun compress(): Pair<InputStream, Long> = compressImage(uri, inputStream)

    private fun compressImage(
        uri: Uri,
        inputStream: InputStream,
    ): Pair<InputStream, Long> {
        val bitmap = BitmapFactory.decodeStream(inputStream)
            ?: error("Failed to decode image")

        val rotatedBitmap = handleImageRotation(uri, bitmap)

        val widthIsLarger = rotatedBitmap.width > maxImageSize
        val heightIsLarger = rotatedBitmap.height > maxImageSize
        val isScaled = widthIsLarger || heightIsLarger

        val scaledBitmap = if (isScaled) {
            scaleBitmap(rotatedBitmap)
        } else {
            rotatedBitmap
        }

        val outputStream = ByteArrayOutputStream()
        var quality = compressionQuality

        do {
            outputStream.reset()
            scaledBitmap.compress(
                Bitmap.CompressFormat.JPEG,
                quality,
                outputStream,
            )
            quality -= 5
        } while (outputStream.size() > maxFileSizeBytes && quality > 50)

        val byteArray = outputStream.toByteArray()

        if (scaledBitmap != rotatedBitmap) scaledBitmap.recycle()
        if (rotatedBitmap != bitmap) rotatedBitmap.recycle()
        bitmap.recycle()

        Log.d(
            "avatar",
            "Compressed image: ${byteArray.size} bytes (quality: $quality)",
        )

        return Pair(ByteArrayInputStream(byteArray), byteArray.size.toLong())
    }

    private fun handleImageRotation(uri: Uri, bitmap: Bitmap): Bitmap {
        val inputStream = contentResolver.openInputStream(uri) ?: return bitmap

        return try {
            val exif = ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )

            val rotation = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            if (rotation != 0f) {
                val matrix = Matrix().apply { postRotate(rotation) }
                Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.width,
                    bitmap.height,
                    matrix,
                    true,
                )
            } else {
                bitmap
            }
        } catch (exception: IOException) {
            Log.w("avatar", "Failed to read EXIF data", exception)
            bitmap
        } finally {
            inputStream.close()
        }
    }

    private fun scaleBitmap(bitmap: Bitmap): Bitmap {
        val ratio = minOf(
            maxImageSize.toFloat() / bitmap.width,
            maxImageSize.toFloat() / bitmap.height,
        )

        val newWidth = (bitmap.width * ratio).toInt()
        val newHeight = (bitmap.height * ratio).toInt()

        return bitmap.scale(newWidth, newHeight)
    }
}
