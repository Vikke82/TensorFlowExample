package com.example.tensorflowexample

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MnistModel(context: Context) {
    private val interpreter: Interpreter

    init {
        val assetManager = context.assets
        val inputStream = assetManager.open("mnist_model.tflite")
        val model = inputStream.readBytes()
        inputStream.close()

        val buffer = ByteBuffer.allocateDirect(model.size).apply {
            order(ByteOrder.nativeOrder())
            put(model)
            rewind()
        }
        interpreter = Interpreter(buffer)
    }

    fun predict(input: FloatArray): Int {
        val output = Array(1) { FloatArray(10) }

        //println("DEBUG: Lähetetään mallille syöte: ${input.slice(0..50)}") // Debug: Tulosta syöte
        interpreter.run(input, output)
        //println("DEBUG: Mallin output -> ${output[0].toList()}") // Debug: Tulosta output

        return output[0].indices.maxByOrNull { output[0][it] } ?: -1
    }
}

