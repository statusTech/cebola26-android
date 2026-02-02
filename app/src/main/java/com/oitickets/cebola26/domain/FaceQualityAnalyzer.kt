package com.oitickets.cebola26.domain

import android.graphics.Rect
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.math.abs

enum class LivenessAction {
    NONE,
    SMILE,
    BLINK
}

class FaceQualityAnalyzer(
    private val targetAction: LivenessAction,
    private val onQualityUpdate: (Boolean, String) -> Unit,
    private val onActionCompleted: () -> Unit
) : ImageAnalysis.Analyzer {

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setMinFaceSize(0.15f)
        .enableTracking()
        .build()

    private val detector = FaceDetection.getClient(options)
    private var lastActionSuccessTime = 0L

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            // Dimensões da imagem para cálculo de centro
            val imgWidth = image.width.toFloat()
            val imgHeight = image.height.toFloat()

            detector.process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isEmpty()) {
                        onQualityUpdate(false, "Nenhum rosto detectado")
                    } else {
                        val face = faces.first()

                        // 1. Verifica Enquadramento (Oval)
                        val centeringResult = checkCentering(face.boundingBox, imgWidth, imgHeight)
                        if (!centeringResult.first) {
                            onQualityUpdate(false, centeringResult.second)
                            return@addOnSuccessListener
                        }

                        // 2. Verifica Rotação (Cabeça reta)
                        if (!isFaceStraight(face)) {
                            onQualityUpdate(false, "Mantenha a cabeça reta")
                            return@addOnSuccessListener
                        }

                        // 3. Verifica Prova de Vida (Piscar)
                        val actionResult = checkLivenessAction(face)

                        if (actionResult) {
                            val now = System.currentTimeMillis()
                            if (now - lastActionSuccessTime > 2000) {
                                lastActionSuccessTime = now
                                onActionCompleted()
                            }
                            onQualityUpdate(true, "Rosto Verificado!")
                        } else {
                            val instruction = when(targetAction) {
                                LivenessAction.SMILE -> "Por favor, SORRIA!"
                                LivenessAction.BLINK -> "PISQUE os olhos devagar"
                                else -> "Mantenha o rosto parado"
                            }
                            onQualityUpdate(false, instruction)
                        }
                    }
                }
                .addOnFailureListener {
                    onQualityUpdate(false, "Erro de leitura")
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    // Verifica se o rosto está centralizado e com bom tamanho
    private fun checkCentering(box: Rect, imgWidth: Float, imgHeight: Float): Pair<Boolean, String> {
        val centerX = box.centerX().toFloat()
        val centerY = box.centerY().toFloat()

        // Desvio permitido do centro (15% da largura/altura)
        val toleranceX = imgWidth * 0.15f
        val toleranceY = imgHeight * 0.20f // Tolerância vertical um pouco maior

        val isCenteredX = abs(centerX - (imgWidth / 2)) < toleranceX
        val isCenteredY = abs(centerY - (imgHeight / 2)) < toleranceY

        if (!isCenteredX || !isCenteredY) {
            return Pair(false, "Centralize o rosto no oval")
        }

        // Verifica tamanho (se está muito longe)
        // O rosto deve ocupar pelo menos 40% da largura da imagem
        val faceWidthRatio = box.width().toFloat() / imgWidth
        if (faceWidthRatio < 0.40f) {
            return Pair(false, "Aproxime o rosto")
        }

        return Pair(true, "")
    }

    private fun isFaceStraight(face: Face): Boolean {
        val rotY = face.headEulerAngleY // Esquerda/Direita
        val rotZ = face.headEulerAngleZ // Inclinação
        // Tolerância de 12 graus
        return abs(rotY) < 12 && abs(rotZ) < 12
    }

    private fun checkLivenessAction(face: Face): Boolean {
        return when (targetAction) {
            LivenessAction.NONE -> true
            LivenessAction.SMILE -> (face.smilingProbability ?: 0f) > 0.6f
            LivenessAction.BLINK -> {
                val leftOpen = face.leftEyeOpenProbability ?: 1f
                val rightOpen = face.rightEyeOpenProbability ?: 1f
                // Considera piscada se os olhos estiverem quase fechados
                leftOpen < 0.15f && rightOpen < 0.15f
            }
        }
    }
}