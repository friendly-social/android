package friendly.android

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// TODO: introduce double-click gestures

@Composable
fun PictureViewerDialog(
    uri: Uri,
    onDismiss: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val scale = remember { Animatable(1f) }
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

    val scope = rememberCoroutineScope()

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .background(
                color = Color.Black.copy(alpha = 0.4f),
            ),
    ) {
        TopBar(
            onDismiss = onDismiss,
            modifier = Modifier
                .statusBarsPadding()
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
        )

        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier
                .graphicsLayer(
                    scaleX = scale.value,
                    scaleY = scale.value,
                    translationX = offset.value.x,
                    translationY = offset.value.y,
                )
                .aspectRatio(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitFirstDown(requireUnconsumed = true)

                            do {
                                val event = awaitPointerEvent()

                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()

                                scope.launch {
                                    snapOnDrag(scale, zoom, offset, pan)
                                }
                            } while (event.changes.any { it.pressed })

                            scope.launch {
                                snapBackOnReleaseIn(
                                    size = size,
                                    scale = scale,
                                    offset = offset,
                                    scope = this,
                                )
                            }
                        }
                    }
                },
        )
    }
}

@Composable
fun TopBar(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
    ) {
        IconButton(
            onClick = { onDismiss() },
            modifier = Modifier.padding(4.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = null,
            )
        }
    }
}

private fun snapBackOnReleaseIn(
    size: IntSize,
    scale: Animatable<Float, AnimationVector1D>,
    offset: Animatable<Offset, AnimationVector2D>,
    scope: CoroutineScope,
) {
    val snapSpec = spring<Float>()

    scope.launch {
        if (scale.value < 1f) {
            scale.animateTo(1f, snapSpec)
        }
    }
    scope.launch {
        if (scale.value <= 1f) {
            offset.animateTo(Offset.Zero, spring())
        } else {
            snapBackIfOutsideOfBounds(size, offset)
        }
    }
}

private suspend fun snapBackIfOutsideOfBounds(
    size: IntSize,
    offset: Animatable<Offset, AnimationVector2D>,
) {
    val halfWidth = (size.width / 2).toFloat()
    val halfHeight = (size.height / 2).toFloat()

    val x = when {
        offset.value.x > halfWidth -> halfWidth

        offset.value.x < -halfWidth -> -halfWidth

        else -> null
    }
    val y = when {
        offset.value.y > halfHeight -> halfHeight

        offset.value.y < -halfHeight -> -halfHeight

        else -> null
    }

    if (x != null || y != null) {
        val newX = x ?: offset.value.x
        val newY = y ?: offset.value.y
        offset.animateTo(Offset(newX, newY))
    }
}

private suspend fun snapOnDrag(
    scale: Animatable<Float, AnimationVector1D>,
    zoom: Float,
    offset: Animatable<Offset, AnimationVector2D>,
    pan: Offset,
) {
    val xAxisDragFactor = 0.3f

    scale.snapTo(scale.value * zoom)

    val zoomedOut = scale.value <= 1f

    val offsetValue = if (zoomedOut) {
        offset.value.copy(
            x = offset.value.x + pan.x * xAxisDragFactor,
            y = offset.value.y + pan.y,
        )
    } else {
        offset.value + pan * scale.value
    }

    offset.snapTo(offsetValue)
}
