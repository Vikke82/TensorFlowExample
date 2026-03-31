package com.example.tensorflowexample

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.ui.graphics.Path as ComposePath

/**
 * DigitRecognitionViewModel - MVVM-arkkitehtuurin ViewModel-kerros.
 *
 * MVVM (Model-View-ViewModel) -arkkitehtuurissa ViewModel:
 * - Säilyttää UI:n tilan (state) konfiguraatiomuutosten (esim. näytön kääntö) yli
 * - Sisältää liiketoimintalogiikan (business logic), kuten kuvan käsittelyn ja ennustamisen
 * - Tarjoaa tilan View-kerrokselle reaktiivisesti StateFlow:n kautta
 * - EI sisällä viittauksia UI-komponentteihin (Activity, Fragment, Composable)
 *
 * AndroidViewModel (vs. tavallinen ViewModel):
 * - Tarjoaa pääsyn Application-kontekstiin, jota tarvitaan TFLite-mallin lataamiseen.
 * - Application-konteksti on turvallinen, koska se elää koko sovelluksen ajan
 *   (toisin kuin Activity-konteksti, joka voi aiheuttaa muistivuodon).
 */
class DigitRecognitionViewModel(application: Application) : AndroidViewModel(application) {

    // Model-kerros: TFLite-malli ladataan kerran ja säilyy ViewModelin elinkaaren ajan.
    private val mnistModel = MnistModel(application)

    /**
     * UI:n tila kapseloituna data-luokkaan.
     *
     * StateFlow on Kotlinin Flow-pohjainen tilan hallintamekanismi:
     * - MutableStateFlow: ViewModel voi muuttaa tilaa (private)
     * - StateFlow: View-kerros voi vain lukea tilaa (public, asStateFlow())
     * - Tämä toteuttaa yhdensuuntaisen tiedonkulun (unidirectional data flow):
     *   Käyttäjän toiminto → ViewModel muuttaa tilaa → UI päivittyy automaattisesti
     */
    private val _uiState = MutableStateFlow(DigitRecognitionUiState())
    val uiState: StateFlow<DigitRecognitionUiState> = _uiState.asStateFlow()

    // Piirtopolku säilytetään ViewModelissa, jotta se kestää konfiguraatiomuutokset.
    val drawPath = ComposePath()

    /**
     * Käsittelee "Tunnista"-napin painalluksen.
     *
     * Tämä on esimerkki ViewModel-kerroksen vastuusta:
     * kuvan esikäsittely ja mallin kutsuminen tapahtuvat täällä,
     * eivät UI-kerroksessa.
     *
     * @param drawingBitmap Käyttäjän piirtämä kuva (280x280 pikseliä)
     */
    fun recognizeDigit(drawingBitmap: ImageBitmap) {
        // 1. Esikäsitellään kuva MNIST-mallin vaatimaan muotoon (28x28, harmaasävy)
        val (inputArray, processedBitmap) = preprocessImage(drawingBitmap)

        // 2. Ajetaan ennustus TFLite-mallilla
        val result = mnistModel.predict(inputArray)

        // 3. Päivitetään UI-tila - Compose havaitsee muutoksen ja piirtää UI:n uudelleen
        _uiState.value = _uiState.value.copy(
            prediction = result,
            processedBitmap = processedBitmap.asImageBitmap()
        )
    }

    /**
     * Tyhjentää piirtoalueen ja nollaa ennustuksen.
     */
    fun clearCanvas() {
        drawPath.reset()
        _uiState.value = DigitRecognitionUiState()
    }

    /**
     * Esikäsittelee piirretyn kuvan TensorFlow Lite -mallin vaatimaan muotoon.
     *
     * MNIST-malli odottaa syötteenä 28x28 pikselin harmaasävykuvan, jossa:
     * - Valkoinen (0.0) = tausta
     * - Musta (1.0) = piirretty viiva
     *
     * Prosessi:
     * 1. Luodaan 28x28 bitmap valkoisella taustalla
     * 2. Skaalataan alkuperäinen piirros 28x28 kokoon suodattimilla
     * 3. Muunnetaan pikselit float-taulukoksi ja käännetään värit
     *    (koska piirros on musta-valkoisella, mutta MNIST käyttää valkoista mustalla)
     */
    private fun preprocessImage(imageBitmap: ImageBitmap): Pair<FloatArray, Bitmap> {
        val originalBitmap = imageBitmap.asAndroidBitmap()

        // Luodaan kohde-bitmap (28x28) valkoisella taustalla
        val resizedBitmap = Bitmap.createBitmap(28, 28, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(resizedBitmap)
        canvas.drawColor(Color.WHITE)

        // Paint-asetuksilla parannetaan kuvan laatua pienessä koossa:
        // - isFilterBitmap: bilineaarinen interpolointi pehmentää pikselöintiä
        // - isAntiAlias: pehmentää viivan reunoja
        // - BlurMaskFilter: lisää pientä sumennusta, mikä parantaa tunnistusta
        val paint = android.graphics.Paint().apply {
            isFilterBitmap = true
            isAntiAlias = true
            strokeWidth = 30f
            maskFilter = android.graphics.BlurMaskFilter(
                5f, android.graphics.BlurMaskFilter.Blur.NORMAL
            )
        }

        // Skaalataan alkuperäinen kuva (280x280) -> 28x28 pikseliin
        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 28, 28, true)
        canvas.drawBitmap(scaledBitmap, 0f, 0f, paint)

        // Muunnetaan bitmap MNIST-syötteeksi: 784 float-arvoa (28 * 28)
        val floatArray = FloatArray(28 * 28)
        for (y in 0 until 28) {
            for (x in 0 until 28) {
                var pixel = resizedBitmap.getPixel(x, y)

                // Läpinäkyvät pikselit tulkitaan valkoisiksi (tausta)
                if (Color.alpha(pixel) == 0) {
                    pixel = Color.WHITE
                }

                // RGB -> harmaasävy (keskiarvo kolmesta kanavasta), normalisointi 0–1
                val grayscale =
                    (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3.0f / 255.0f

                // Käännetään: MNIST-datasetissa numero on valkoinen (1.0) mustalla taustalla (0.0),
                // mutta piirros on musta viiva valkoisella taustalla → 1.0 - arvo
                floatArray[y * 28 + x] = 1.0f - grayscale
            }
        }

        return Pair(floatArray, resizedBitmap)
    }
}

/**
 * UI:n tilamalli (UI State).
 *
 * Data class kuvaa koko näytön tilan yhdellä objektilla.
 * Compose tarkkailee tätä tilaa ja piirtää UI:n uudelleen
 * aina kun tila muuttuu (recomposition).
 *
 * @param prediction Mallin ennustama numero (-1 = ei ennustetta vielä)
 * @param processedBitmap 28x28 esikäsitelty kuva (näytetään käyttäjälle havainnollistuksena)
 */
data class DigitRecognitionUiState(
    val prediction: Int = -1,
    val processedBitmap: ImageBitmap? = null
)
