package com.example.tensorflowexample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tensorflowexample.ui.theme.TensorFlowExampleTheme

/**
 * MainActivity - Sovelluksen aloituspiste (View-kerros).
 *
 * MVVM-arkkitehtuurissa Activity toimii vain "liimana":
 * - Se luo teeman ja Scaffold-rakenteen
 * - Varsinainen UI-logiikka on Composable-funktioissa
 * - Tilan hallinta on ViewModelissa
 *
 * enableEdgeToEdge() mahdollistaa piirtämisen status- ja navigointipalkkien alle,
 * mikä on Material Design 3 -suositus modernille Android-sovellukselle.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // setContent on Compose-sovelluksen juuripiste, joka korvaa perinteisen setContentView():n.
        setContent {
            TensorFlowExampleTheme {
                DigitRecognitionScreen()
            }
        }
    }
}

/**
 * DigitRecognitionScreen - Päänäyttö numerontunnistukselle.
 *
 * Tämä on "screen-level" composable, joka:
 * 1. Hankkii ViewModelin Compose-integraatiolla (viewModel())
 * 2. Kerää UI-tilan StateFlow:sta (collectAsState())
 * 3. Välittää tilan ja tapahtumat alas komponenttipuussa
 *
 * viewModel()-funktio:
 * - Luo ViewModelin automaattisesti tai palauttaa olemassaolevan
 * - ViewModel säilyy konfiguraatiomuutosten (esim. näytön kääntö) yli
 * - Lifecycle-tietoinen: tuhotaan kun Activity lopullisesti tuhotaan
 *
 * collectAsState():
 * - Muuntaa Kotlin StateFlow:n Compose State -objektiksi
 * - Compose tarkkailee State-objektia ja piirtää UI:n uudelleen
 *   automaattisesti kun arvo muuttuu (recomposition)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitRecognitionScreen(
    viewModel: DigitRecognitionViewModel = viewModel()
) {
    // collectAsState() muuntaa Flow → Compose State, jotta UI reagoi muutoksiin
    val uiState by viewModel.uiState.collectAsState()

    val density = LocalDensity.current
    val bitmapWidth = with(density) { 280.dp.toPx().toInt() }
    val bitmapHeight = with(density) { 280.dp.toPx().toInt() }

    // Piirtoalueen bitmap ja invalidointi-triggeri ovat paikallista UI-tilaa,
    // koska ne liittyvät vain piirtokomponentin toimintaan.
    val drawingBitmap = remember { mutableStateOf(ImageBitmap(bitmapWidth, bitmapHeight)) }
    var invalidateTrigger by remember { mutableStateOf(false) }

    // Scaffold on Material Design 3:n perusrakenne, joka tarjoaa
    // valmiin layoutin TopAppBarille, FAB:lle, BottomBarille jne.
    Scaffold(
        topBar = {
            // TopAppBar Material Design 3 -tyylillä
            TopAppBar(
                title = { Text("Numerontunnistus") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        // innerPadding sisältää TopAppBarin ja järjestelmäpalkkien varaamat alueet
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Piirrä numero alla olevaan ruutuun",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Card korostaa piirtoalueen visuaalisesti Material Design 3 -tyylillä.
            // elevation luo varjon, joka erottaa alueen taustasta.
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                // DrawingCanvas on eriytetty composable, joka vastaa piirtämisestä
                DrawingCanvas(
                    bitmap = drawingBitmap,
                    path = viewModel.drawPath,
                    invalidateTrigger = invalidateTrigger,
                    onInvalidate = { invalidateTrigger = !invalidateTrigger }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Painikkeet vierekkäin Material Design 3 -tyylillä
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                // Button = MD3 Filled button (pääasiallisin toiminto)
                Button(
                    onClick = { viewModel.recognizeDigit(drawingBitmap.value) }
                ) {
                    Text("Tunnista")
                }

                Spacer(modifier = Modifier.width(12.dp))

                // FilledTonalButton = MD3 toissijainen painike (vähemmän korostava)
                FilledTonalButton(
                    onClick = {
                        viewModel.clearCanvas()
                        drawingBitmap.value = ImageBitmap(bitmapWidth, bitmapHeight)
                        invalidateTrigger = !invalidateTrigger
                    }
                ) {
                    Text("Tyhjennä")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tuloskortti näyttää ennustuksen Material Design 3 Card -komponentilla
            ResultCard(
                prediction = uiState.prediction,
                processedBitmap = uiState.processedBitmap
            )
        }
    }
}

/**
 * ResultCard - Näyttää tunnistustuloksen ja esikäsitellyn kuvan.
 *
 * Tämä on esimerkki "state-hoisting" -periaatteesta:
 * composable EI hae tilaansa itse, vaan saa sen parametreina.
 * Tämä tekee komponentista uudelleenkäytettävän ja testattavan.
 *
 * @param prediction Mallin ennustama numero (-1 = ei tulosta)
 * @param processedBitmap 28x28 esikäsitelty kuva (mitä malli "näki")
 */
@Composable
fun ResultCard(
    prediction: Int,
    processedBitmap: ImageBitmap?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Näytetään ennustettu numero isolla fontilla
            Text(
                text = if (prediction >= 0) "Ennuste: $prediction" else "Piirrä numero ja paina Tunnista",
                style = MaterialTheme.typography.headlineMedium,
                color = if (prediction >= 0)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Näytetään esikäsitelty 28x28 kuva, jotta käyttäjä näkee
            // mitä malli "näki" - tämä on hyödyllistä oppimismielessä
            if (processedBitmap != null) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Mallin näkemä kuva (28x28 pikseliä):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Image(
                    bitmap = processedBitmap,
                    contentDescription = "Esikäsitelty kuva, jonka malli analysoi",
                    modifier = Modifier.size(140.dp),
                    contentScale = ContentScale.FillBounds
                )
            }
        }
    }
}
