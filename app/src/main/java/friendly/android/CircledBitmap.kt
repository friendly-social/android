package friendly.android

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import kotlin.math.min

/**
 * Crops bitmap and then masks it with circle
 */
fun Bitmap.getCircledBitmap(): Bitmap {
    val output = Bitmap.createBitmap(
        this.width,
        this.height,
        Bitmap.Config.ARGB_8888,
    )
    val canvas = Canvas(output)
    val paint = Paint()
    val rect = Rect(0, 0, this.width, this.height)
    paint.isAntiAlias = true
    canvas.drawARGB(0, 0, 0, 0)
    val radius = min(this.height, this.width) / 2f
    canvas.drawCircle(this.width / 2f, this.height / 2f, radius, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(this, rect, rect, paint)
    return output
}
