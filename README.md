# TensorFlowExample

This example project is showing how to use trained Tensorflow Lite model in Android application. The application have drawable canvas where user can draw single numbers. Then ML model is trying identify what number the drawing is presenting.

<img width="141" alt="image" src="https://github.com/user-attachments/assets/c82a397a-a3c9-4ee6-bfb8-cb3a669c29ce" />

This is Python code for creating the model:

import tensorflow as tf
import os
import matplotlib.pyplot as plt

# Ladataan MNIST-data
mnist = tf.keras.datasets.mnist
(x_train, y_train), (x_test, y_test) = mnist.load_data()

# Piirretään 10 ensimmäistä kuvaa datasta
fig, axes = plt.subplots(2, 5, figsize=(10, 5))

for i, ax in enumerate(axes.flat):
    ax.imshow(x_train[i], cmap='gray')
    ax.set_title(f"Label: {y_train[i]}")
    ax.axis('off')

plt.show()

# Normalisoidaan data
x_train, x_test = x_train / 255.0, x_test / 255.0

# Luodaan yksinkertainen malli
model = tf.keras.Sequential([
    tf.keras.layers.Flatten(input_shape=(28, 28)),
    tf.keras.layers.Dense(128, activation='relu'),
    tf.keras.layers.Dense(10, activation='softmax')
])

# Koulutetaan malli
model.compile(optimizer='adam', loss='sparse_categorical_crossentropy', metrics=['accuracy'])
model.fit(x_train, y_train, epochs=5)

# Tallennetaan TensorFlow-malli

model.export("model")

#Muunnetaan malli TensorFlow Lite -muotoon

# Ladataan koulutettu malli
converter = tf.lite.TFLiteConverter.from_saved_model("model")

# Optimoidaan malli
converter.optimizations = [tf.lite.Optimize.DEFAULT]

# Muunnetaan TFLite-muotoon
tflite_model = converter.convert()

# Tallennetaan .tflite-tiedostona
with open("mnist_model.tflite", "wb") as f:
    f.write(tflite_model)

