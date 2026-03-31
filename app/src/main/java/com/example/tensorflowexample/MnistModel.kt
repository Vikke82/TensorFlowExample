package com.example.tensorflowexample

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * MnistModel - TensorFlow Lite -mallin lataus ja päättely (inference).
 *
 * MNIST on klassinen koneoppimisen harjoitusdatasetti, joka sisältää
 * käsinkirjoitettuja numeroita 0–9. Malli ottaa syötteenä 28x28 pikselin
 * harmaasävykuvan (784 float-arvoa) ja palauttaa todennäköisyydet
 * kullekin numerolle 0–9.
 *
 * Tämä luokka vastaa MVVM-arkkitehtuurin Model-kerroksesta (data layer).
 * Se kapseloi TensorFlow Lite -tulkin (Interpreter) ja tarjoaa
 * yksinkertaisen rajapinnan ennusteiden tekemiseen.
 */
class MnistModel(context: Context) {

    // TensorFlow Lite Interpreter suorittaa neuroverkon päättelyn laitteella.
    // Se ladataan kerran alustuksessa ja käytetään uudelleen jokaiselle ennusteelle.
    private val interpreter: Interpreter

    init {
        // 1. Ladataan .tflite-mallitiedosto sovelluksen assets-kansiosta.
        //    Assets-kansio paketoidaan APK:n mukaan, joten malli kulkee sovelluksen mukana.
        val assetManager = context.assets
        val inputStream = assetManager.open("mnist_model.tflite")
        val model = inputStream.readBytes()
        inputStream.close()

        // 2. Muunnetaan mallin tavut ByteBufferiksi, jota TFLite vaatii.
        //    allocateDirect() varaa muistia suoraan (ei JVM-kekoon), mikä on
        //    tehokkaampi natiivikoodille. nativeOrder() varmistaa oikean tavujärjestyksen.
        val buffer = ByteBuffer.allocateDirect(model.size).apply {
            order(ByteOrder.nativeOrder())
            put(model)
            rewind() // Palautetaan lukupositio alkuun
        }

        // 3. Luodaan Interpreter-olio, joka lataa mallin rakenteen ja painot muistiin.
        interpreter = Interpreter(buffer)
    }

    /**
     * Ennustaa piirretyn numeron.
     *
     * @param input FloatArray, jossa on 784 arvoa (28x28 pikseliä).
     *              Jokainen arvo on välillä 0.0–1.0, missä 1.0 = musta ja 0.0 = valkoinen.
     * @return Ennustettu numero 0–9 (suurimman todennäköisyyden indeksi).
     *
     * Toiminta:
     * 1. Syöte (784 float-arvoa) annetaan mallille
     * 2. Malli laskee todennäköisyydet kymmenelle luokalle (0–9)
     * 3. Palautetaan luokka, jolla on suurin todennäköisyys
     */
    fun predict(input: FloatArray): Int {
        // Tulosmatriisi: 1 rivi (yksi kuva), 10 saraketta (numerot 0–9).
        // TFLite käyttää batch-kokoa 1, siksi ulompi Array(1).
        val output = Array(1) { FloatArray(10) }

        // Suoritetaan päättely: syöte sisään, tulos output-taulukkoon.
        // Tämä ajaa neuroverkon eteenpäin (forward pass) laitteella.
        interpreter.run(input, output)

        // Etsitään suurimman todennäköisyyden indeksi (= ennustettu numero).
        // maxByOrNull palauttaa indeksin, jonka kohdalla output-arvo on suurin.
        return output[0].indices.maxByOrNull { output[0][it] } ?: -1
    }
}
