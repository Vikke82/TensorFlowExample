package com.example.tensorflowexample

//import androidx.compose.ui.graphics.Color

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.tensorflowexample.ui.theme.TensorFlowExampleTheme
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import androidx.compose.ui.graphics.Path as ComposePath

class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TensorFlowExampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    PredictionScreen(
                        context = this
                    )
                }
            }
        }
    }
}



@Composable
fun PredictionScreen(context: Context) {
    val model = remember { MnistModel(context) } // Käytetään MnistModel

    val density = LocalDensity.current

    val bitmapWidth = with(density) { 280.dp.toPx().toInt() }
    val bitmapHeight = with(density) { 280.dp.toPx().toInt() }

    val loResBitmapWidth = with(density) { 28.dp.toPx().toInt() }
    val loResBitmapHeight = with(density) { 28.dp.toPx().toInt() }

    var loResBitmap by remember { mutableStateOf(ImageBitmap(loResBitmapWidth, loResBitmapHeight)) }

    var bitmap = remember { mutableStateOf(ImageBitmap(bitmapWidth, bitmapHeight)) }

    var path by remember { mutableStateOf(ComposePath()) }

    var prediction by remember { mutableStateOf(-1) }

    // Päivitetään composable, kun `path` muuttuu
    var invalidateTrigger by remember { mutableStateOf(false) }



    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Piirrä numero", style = MaterialTheme.typography.headlineMedium)

        DrawView(
            bitmap = bitmap,
            path = path,
            invalidateTrigger = invalidateTrigger,
            onInvalidate = { invalidateTrigger = !invalidateTrigger }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // "Valmis"-nappi tekee ennustuksen
        Button(onClick = {
            val (input, loResBm) = processBitmap(bitmap.value)//Muunnetaan oikeaan muotoon
            loResBitmap = loResBm.asImageBitmap()
            saveBitmapToFile(context, bitmap = bitmap.value.asAndroidBitmap())
            prediction = model.predict(input) // Ennustetaan MnistModelilla
        }) {
            Text("Valmis")
        }

        Spacer(modifier = Modifier.height(16.dp))
        // Näytä matalaresoluutioinen kuva UI:ssa

        Image(
            bitmap = loResBitmap,
            contentDescription = "Matalan resoluution bitmap",
            modifier = Modifier
                .size(280.dp)
                .padding(8.dp),
            contentScale = ContentScale.FillBounds

        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Ennustettu numero: $prediction", style = MaterialTheme.typography.headlineMedium)

        Button(onClick = {
            bitmap.value = ImageBitmap(bitmapWidth, bitmapHeight)
            path.reset()

            prediction = -1
        }) {
            Text("Tyhjennä")
        }

    }
}



fun processBitmap(imageBitmap: ImageBitmap): Pair<FloatArray, Bitmap> {
    val originalBitmap = imageBitmap.asAndroidBitmap()

    // Luodaan uusi 28x28-pikselin bitmap valkoisella taustalla
    val resizedBitmap = Bitmap.createBitmap(28, 28, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(resizedBitmap)
    canvas.drawColor(Color.WHITE) // Pakotetaan valkoinen tausta

    // Käytetään pehmeämpää skaalausta
    val paint = android.graphics.Paint().apply {
        isFilterBitmap = true // Interpolointi käytössä, pehmentää viivoja
        isAntiAlias = true // Viivojen pehmentäminen pois
        strokeWidth = 30f
        maskFilter = android.graphics.BlurMaskFilter(5f, android.graphics.BlurMaskFilter.Blur.NORMAL) // Parantaa viivan erottuvuutta

    }

    val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 28, 28, true) // Skaalaus pehmeämmäksi
    canvas.drawBitmap(scaledBitmap, 0f, 0f, paint) // Piirretään bitmap pehmeämmällä asetuksella

    // Muunnetaan bitmap MNIST-formaattiin
    val floatArray = FloatArray(28 * 28)
    for (y in 0 until 28) {
        for (x in 0 until 28) {
            var pixel = resizedBitmap.getPixel(x, y)

            // Jos pikseli on läpinäkyvä, tehdään siitä valkoinen
            if (Color.alpha(pixel) == 0) {
                pixel = Color.WHITE
            }

            // Muunnetaan harmaasävyksi ja skaalataan MNIST-formaattiin
            val grayscale = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3.0f / 255.0f
            floatArray[y * 28 + x] = 1.0f - grayscale // MNIST käyttää valkoista numeroa mustalla taustalla
        }
    }

    return Pair(floatArray, resizedBitmap)
}

//tätä voi käyttää debuggaukseen
fun saveBitmapToFile(context: Context, bitmap: Bitmap, filename: String = "mnist_image.png"): File? {
    val directory = context.getExternalFilesDir(null) // Tallennetaan sovelluksen omaan kansioon
    val file = File(directory, filename)

    return try {
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream) // Tallennetaan PNG-muotoon
        outputStream.flush()
        outputStream.close()
        file // Palautetaan tiedoston polku
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}






