package com.oitickets.cebola26.domain

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy

fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    // Corrige rotação (Câmera traseira geralmente vem rotacionada 90 graus)
    val matrix = Matrix()
    matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}