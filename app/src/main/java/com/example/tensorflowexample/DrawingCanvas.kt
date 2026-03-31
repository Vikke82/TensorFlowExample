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

/**
 * DrawingCanvas - Compose-piirtokomponentti (View-kerros).
 *
 * Tämä composable tarjoaa kosketuksella piirrettävän alueen,
 * johon käyttäjä voi kirjoittaa numeron sormella tai styluksella.
 *
 * MVVM:n kannalta tämä on puhdas UI-komponentti:
 * - Ottaa vastaan tilan parametreina (bitmap, path)
 * - Ilmoittaa muutoksista callback-funktioilla (onDrawingChanged)
 * - Ei sisällä liiketoimintalogiikkaa
 *
 * @param bitmap Mutable-tila, johon piirretty kuva tallennetaan. MutableState mahdollistaa
 *               bitmapin päivityksen suoraan composablen sisältä.
 * @param path Piirtopolku, joka sisältää käyttäjän piirtämät viivat.
 * @param invalidateTrigger Boolean-arvo, jonka muutos pakottaa Canvasin piirtymään uudelleen.
 *                          Compose piirtää Canvasin uudelleen vain kun jokin sen lukema tila muuttuu.
 * @param onInvalidate Callback, joka kääntää invalidateTrigger-arvon ja laukaisee uudelleenpiirron.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DrawingCanvas(
    bitmap: MutableState<ImageBitmap>,
    path: ComposePath,
    invalidateTrigger: Boolean,
    onInvalidate: () -> Unit
) {
    val density = LocalDensity.current

    // Viivan paksuus muunnetaan dp → px, jotta se näyttää samalta eri näyttötiheyksillä.
    // LaunchedEffect suoritetaan kerran kun density on saatavilla.
    var strokeWidthPx by remember { mutableStateOf(10.dp.value) }
    LaunchedEffect(density) {
        strokeWidthPx = with(density) { 10.dp.toPx() }
    }

    // Compose Paint -objekti piirtämiseen.
    // remember {} varmistaa, ettei objektia luoda uudelleen joka recompositionissa.
    val paint = remember {
        ComposePaint().apply {
            color = Color.Black
            style = PaintingStyle.Stroke  // Vain viiva, ei täyttöä
            strokeWidth = strokeWidthPx
        }
    }

    // Canvas on Compose-komponentti, joka mahdollistaa vapaamuotoisen piirtämisen.
    // Se on analoginen Androidin perinteiselle Canvas/View-piirtämiselle.
    Canvas(
        modifier = Modifier
            .size(280.dp, 280.dp)
            .background(Color.White)
            // pointerInteropFilter mahdollistaa matalan tason kosketustapahtumien käsittelyn.
            // Tämä on tarpeen piirtämisessä, koska tarvitsemme tarkat x,y-koordinaatit.
            .pointerInteropFilter { event ->
                when (event.action) {
                    // ACTION_DOWN: Sormi koskettaa näyttöä → aloitetaan uusi viiva
                    MotionEvent.ACTION_DOWN -> {
                        path.moveTo(event.x, event.y)
                        onInvalidate()
                    }
                    // ACTION_MOVE: Sormi liikkuu → piirretään viiva edellisestä pisteestä nykyiseen
                    MotionEvent.ACTION_MOVE -> {
                        path.lineTo(event.x, event.y)
                        onInvalidate()
                    }
                }
                true // true = tapahtuma käsitelty, ei välitetä eteenpäin
            }
    ) {
        // drawIntoCanvas antaa pääsyn matalan tason Canvas-rajapintaan,
        // jolla voidaan piirtää polkuja ja bitmappeja.
        drawIntoCanvas { canvas ->
            canvas.drawPath(path, paint)
            canvas.drawImage(bitmap.value, Offset.Zero, paint)
            // Päivitetään bitmap vastaamaan nykyistä piirustusta
            bitmap.value = renderPathToBitmap(bitmap.value, path)
        }
    }
}

/**
 * Piirtää polun (Path) bitmappiin, jotta piirustus säilyy.
 *
 * Tämä funktio muuntaa Compose Path → Android Path ja piirtää sen
 * Android Canvas -rajapinnalla bitmappiin. Bitmap toimii "muistina",
 * johon aiemmat piirrokset tallennetaan.
 *
 * @param bitmap Nykyinen bitmap, johon piirretään
 * @param path Käyttäjän piirtämä polku
 * @return Uusi ImageBitmap, joka sisältää polun
 */
fun renderPathToBitmap(bitmap: ImageBitmap, path: ComposePath): ImageBitmap {
    // Kopioidaan bitmap muokattavaksi (Compose ImageBitmap on oletuksena immutable)
    val androidBitmap = bitmap.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, true)
    val canvas = android.graphics.Canvas(androidBitmap)

    // Piirretään valkoinen tausta
    canvas.drawColor(Color.White.toArgb())

    // Määritellään piirtotyyli Android Paint -objektilla
    val paint = android.graphics.Paint().apply {
        color = Color.Black.toArgb()
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 10f
    }

    // Muunnetaan Compose Path → Android Path ja piirretään se canvasille
    canvas.drawPath(path.asAndroidPath(), paint)

    return androidBitmap.asImageBitmap()
}
