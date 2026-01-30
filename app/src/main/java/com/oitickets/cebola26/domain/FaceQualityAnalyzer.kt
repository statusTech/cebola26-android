package com.oitickets.cebola26.domain

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.math.abs

enum class LivenessAction {
    NONE,       // Apenas posicionar
    SMILE,      // Sorrir
    BLINK       // Piscar ambos os olhos
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

    // Controle para não disparar o callback de sucesso múltiplas vezes seguidas
    private var lastActionSuccessTime = 0L

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            detector.process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isEmpty()) {
                        // CRÍTICO: Avisa que perdeu o rosto para resetar a UI
                        onQualityUpdate(false, "Nenhum rosto detectado")
                    } else {
                        val face = faces.first()

                        // 1. Verifica posicionamento contínuo
                        if (!isFacePositionGood(face)) {
                            onQualityUpdate(false, "Centralize o rosto e olhe para frente")
                            return@addOnSuccessListener
                        }

                        // 2. Verifica Prova de Vida
                        val actionResult = checkLivenessAction(face)

                        if (actionResult) {
                            // Debounce de 2 segundos para evitar disparos loucos
                            val now = System.currentTimeMillis()
                            if (now - lastActionSuccessTime > 2000) {
                                lastActionSuccessTime = now
                                onActionCompleted()
                            }
                            // Mantém feedback positivo
                            onQualityUpdate(true, "Rosto Verificado!")
                        } else {
                            // Feedback de instrução
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

    private fun isFacePositionGood(face: Face): Boolean {
        val rotY = face.headEulerAngleY
        val rotZ = face.headEulerAngleZ
        return abs(rotY) < 15 && abs(rotZ) < 15
    }

    private fun checkLivenessAction(face: Face): Boolean {
        return when (targetAction) {
            LivenessAction.NONE -> true
            LivenessAction.SMILE -> (face.smilingProbability ?: 0f) > 0.6f
            LivenessAction.BLINK -> {
                val leftOpen = face.leftEyeOpenProbability ?: 1f
                val rightOpen = face.rightEyeOpenProbability ?: 1f
                // Piscar: Olhos fechados (< 0.1)
                leftOpen < 0.1f && rightOpen < 0.1f
            }
        }
    }
}