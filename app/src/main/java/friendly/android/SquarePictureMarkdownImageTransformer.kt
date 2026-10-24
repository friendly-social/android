package friendly.android

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.error
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.model.ImageData
import com.mikepenz.markdown.model.ImageTransformer
import com.mikepenz.markdown.model.ImageWidth
import com.mikepenz.markdown.model.PlaceholderConfig
import kotlin.math.roundToInt

class SquarePictureMarkdownImageTransformer(private val imageWidth: Dp) :
    ImageTransformer by Coil3ImageTransformerImpl {
    // Plans on aspect ratio here:
    // 1. to make pictures fit this square with blurred background
    // 2. make pictures possible for taking several possible aspect ratios,
    // and force them take specified ratios when they stop looking convenient
    val ratio = 1f

    @Composable
    override fun transform(link: String): ImageData {
        val context = LocalPlatformContext.current
        val density = LocalDensity.current

        val widthPx = with(density) {
            imageWidth.roundToPx().coerceAtLeast(1)
        }

        val heightPx = (widthPx / ratio).roundToInt().coerceAtLeast(1)

        val request = remember(link, context, widthPx, heightPx) {
            ImageRequest.Builder(context)
                .data(link)
                .size(widthPx, heightPx)
                .error(R.drawable.ic_error)
                .build()
        }

        val painter = rememberAsyncImagePainter(model = request)

        val imageData = ImageData(
            alignment = Center,
            painter = painter,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .width(imageWidth)
                .aspectRatio(ratio)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
        )
        return imageData
    }

    @Composable
    override fun intrinsicSize(painter: Painter): Size = Size.Unspecified

    override fun placeholderConfig(
        link: String,
        density: Density,
        containerSize: Size,
        imageWidth: ImageWidth,
        imageSize: Size,
        imageSizeChanged: ((String, Size) -> Unit)?,
    ): PlaceholderConfig = PlaceholderConfig(
        size = Size(
            width = this.imageWidth.value,
            height = this.imageWidth.value / ratio,
        ),
    )
}
