package com.example.tensorflowexample


import android.graphics.Bitmap
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Paint as ComposePaint
import androidx.compose.ui.graphics.Path as ComposePath


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DrawView(
    bitmap: MutableState<ImageBitmap>,
    path: ComposePath, // Path pysyy muistissa
    invalidateTrigger: Boolean, // Triggeri vastaanotetaan
    onInvalidate: () -> Unit // Päivityksen laukaisu
) {

    val density = LocalDensity.current

    var strokeWid by remember { mutableStateOf(10.dp.value) }

    // Lasketaan strokeWidth Composable-kontekstissa
    LaunchedEffect(density) {
        strokeWid = with(density) { 10.dp.toPx() }
    }

    val paint = remember {
        ComposePaint().apply {
            color = androidx.compose.ui.graphics.Color.Black
            style = PaintingStyle.Stroke
            strokeWidth = strokeWid
        }
    }


    var isDrawing by remember { mutableStateOf(false) }
    val width = 280.dp
    val height = 280.dp


    Canvas(
        modifier = Modifier
            .size(width, height)
            .background(androidx.compose.ui.graphics.Color.White)
            .pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        //path.value.reset()
                        path.moveTo(event.x, event.y)
                        onInvalidate() // Laukaistaan päivitys
                        isDrawing = true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        path.lineTo(event.x, event.y)
                        onInvalidate() // Laukaistaan päivitys


                    }

                }
                true
            }
    ) {
        drawIntoCanvas { canvas ->
            canvas.drawPath(path, paint)
            canvas.drawImage(bitmap.value, Offset.Zero, paint)
            bitmap.value = updateBitmap(bitmap.value, path)
        }

    }


}

/**
 * Päivittää bitmapin, joka sisältää piirretyn polun.
 */
fun updateBitmap(bitmap: ImageBitmap, path: ComposePath): ImageBitmap {

    val androidBitmap = bitmap.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, true)
    val canvas = android.graphics.Canvas(androidBitmap)

    // Piirretään valkoinen tausta
    canvas.drawColor(Color.White.toArgb())

    // Piirretään aiempi kuva
    val paint = android.graphics.Paint().apply {
        color = Color.Black.toArgb()
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 10f
    }

    // Piirretään uusi polku (käyttäjän piirtojälki)
    canvas.drawPath(path.asAndroidPath(), paint)

    // Päivitetään bitmap, jotta se näkyy UI:ssa
    val newBitmap = androidBitmap.asImageBitmap()


    return newBitmap
}








