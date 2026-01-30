package com.oitickets.cebola26.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.oitickets.cebola26.domain.FaceQualityAnalyzer
import com.oitickets.cebola26.ui.viewmodel.RegistrationViewModel
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.math.max

@Composable
fun CameraScreen(
    viewModel: RegistrationViewModel,
    onPhotoTaken: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    // 1. SE JÁ TEM FOTO, MOSTRA A TELA DE REVISÃO
    if (viewModel.capturedBitmap != null) {
        PhotoPreviewScreen(viewModel)
        return
    }

    // 2. SE NÃO TEM FOTO, MOSTRA A CÂMERA DE LIVENESS
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    var isProcessing by remember { mutableStateOf(false) }
    var isLivenessPassed by remember { mutableStateOf(false) }

    // Verifica se foto é opcional para habilitar botão de pular
    val canSkip = !viewModel.rules.requirePhoto

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build()

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(executor, FaceQualityAnalyzer(
                                targetAction = viewModel.currentLivenessAction,
                                onQualityUpdate = { isGood, msg ->
                                    if (!isProcessing) {
                                        // Se perder o rosto (moveu o celular), perde o status de validado
                                        if (msg.contains("Nenhum rosto", ignoreCase = true) ||
                                            msg.contains("Centralize", ignoreCase = true)) {
                                            isLivenessPassed = false
                                        }

                                        if (isLivenessPassed) {
                                            viewModel.isFaceGood = true
                                            viewModel.cameraFeedback = "PRONTO! PODE CAPTURAR"
                                        } else {
                                            viewModel.isFaceGood = isGood
                                            viewModel.cameraFeedback = msg
                                        }
                                    }
                                },
                                onActionCompleted = {
                                    // Ação de Liveness detectada! Libera o botão.
                                    if (!isLivenessPassed) {
                                        isLivenessPassed = true
                                        viewModel.isFaceGood = true
                                        viewModel.cameraFeedback = "PRONTO! PODE CAPTURAR"
                                    }
                                }
                            ))
                        }

                    try {
                        cameraProvider.unbindAll()
                        // Câmera Traseira
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("CameraScreen", "Erro ao iniciar câmera", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        CameraOverlay(isFaceGood = viewModel.isFaceGood)

        // Header com Feedback
        if (!isProcessing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 24.dp, end = 24.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusColor by animateColorAsState(
                    if (viewModel.isFaceGood) Color(0xFF2E7D32) else Color(0xFFB00020), label = "color"
                )

                Surface(
                    color = statusColor.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (viewModel.isFaceGood) Icons.Default.Face else Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = viewModel.cameraFeedback.uppercase(),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                }
            }
        }

        // Rodapé
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(160.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isProcessing) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Processando...", color = Color.White, fontSize = 12.sp)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botão Pular (Se não for obrigatória)
                    if (canSkip) {
                        TextButton(onClick = { viewModel.submitRegistration() }) {
                            Text("PULAR", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(60.dp)) // Spacer para centralizar
                    }

                    // Botão Disparo Manual (Centro)
                    val buttonScale by animateFloatAsState(if (viewModel.isFaceGood) 1f else 0.8f, label = "scale")
                    val buttonAlpha by animateFloatAsState(if (viewModel.isFaceGood) 1f else 0.3f, label = "alpha")

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .graphicsLayer {
                                scaleX = buttonScale
                                scaleY = buttonScale
                                alpha = buttonAlpha
                            }
                            .border(4.dp, Color.White, CircleShape)
                            .padding(6.dp)
                            .background(if (viewModel.isFaceGood) Color.White else Color.Transparent, CircleShape)
                            .clickable(enabled = viewModel.isFaceGood) {
                                if (viewModel.isFaceGood) {
                                    isProcessing = true
                                    captureAndCrop(
                                        context = context,
                                        imageCapture = imageCapture,
                                        executor = executor,
                                        onSuccess = { bitmap ->
                                            Handler(Looper.getMainLooper()).post {
                                                isProcessing = false
                                                onPhotoTaken(bitmap)
                                            }
                                        },
                                        onError = {
                                            Handler(Looper.getMainLooper()).post {
                                                isProcessing = false
                                                Toast.makeText(context, "Erro ao capturar", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                            }
                    )

                    Spacer(modifier = Modifier.width(60.dp)) // Espaço direita
                }
            }
        }
    }
}

@Composable
fun PhotoPreviewScreen(viewModel: RegistrationViewModel) {
    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(modifier = Modifier.weight(1f)) {
            Image(
                bitmap = viewModel.capturedBitmap!!.asImageBitmap(),
                contentDescription = "Preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Overlay no topo para instrução
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(0.6f), Color.Transparent, Color.Black.copy(0.6f))
                        )
                    )
            )
        }

        // Barra Inferior de Ação
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { viewModel.retakePhoto() },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TIRAR OUTRA")
                }

                Button(
                    onClick = { viewModel.submitRegistration() },
                    modifier = Modifier.height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SALVAR (3/3)")
                }
            }
        }
    }
}

// --- Componentes Visuais e Funções Auxiliares ---

@Composable
fun CameraOverlay(isFaceGood: Boolean) {
    val borderColor by animateColorAsState(
        if (isFaceGood) Color(0xFF00E676) else Color(0xFFFFFFFF), label = "border"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val ovalWidth = size.width * 0.65f
        val ovalHeight = size.height * 0.45f
        val ovalLeft = (size.width - ovalWidth) / 2
        val ovalTop = (size.height - ovalHeight) / 2 * 0.8f

        val path = Path().apply {
            addRect(ComposeRect(0f, 0f, size.width, size.height))
            val ovalPath = Path().apply {
                addOval(ComposeRect(ovalLeft, ovalTop, ovalLeft + ovalWidth, ovalTop + ovalHeight))
            }
            op(this, ovalPath, PathOperation.Difference)
        }

        drawPath(path = path, color = Color.Black.copy(alpha = 0.7f))

        drawOval(
            color = borderColor.copy(alpha = 0.8f),
            topLeft = Offset(ovalLeft, ovalTop),
            size = Size(ovalWidth, ovalHeight),
            style = Stroke(width = 4.dp.toPx())
        )

        // Cantoneiras "Tech"
        val cornerLength = 40.dp.toPx()
        val stroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)

        drawLine(borderColor, Offset(ovalLeft - 20, ovalTop), Offset(ovalLeft - 20 + cornerLength, ovalTop), strokeWidth = 6f)
        drawLine(borderColor, Offset(ovalLeft - 20, ovalTop), Offset(ovalLeft - 20, ovalTop + cornerLength), strokeWidth = 6f)
        drawLine(borderColor, Offset(ovalLeft + ovalWidth + 20, ovalTop), Offset(ovalLeft + ovalWidth + 20 - cornerLength, ovalTop), strokeWidth = 6f)
        drawLine(borderColor, Offset(ovalLeft + ovalWidth + 20, ovalTop), Offset(ovalLeft + ovalWidth + 20, ovalTop + cornerLength), strokeWidth = 6f)
        drawLine(borderColor, Offset(ovalLeft - 20, ovalTop + ovalHeight), Offset(ovalLeft - 20 + cornerLength, ovalTop + ovalHeight), strokeWidth = 6f)
        drawLine(borderColor, Offset(ovalLeft - 20, ovalTop + ovalHeight), Offset(ovalLeft - 20, ovalTop + ovalHeight - cornerLength), strokeWidth = 6f)
        drawLine(borderColor, Offset(ovalLeft + ovalWidth + 20, ovalTop + ovalHeight), Offset(ovalLeft + ovalWidth + 20 - cornerLength, ovalTop + ovalHeight), strokeWidth = 6f)
        drawLine(borderColor, Offset(ovalLeft + ovalWidth + 20, ovalTop + ovalHeight), Offset(ovalLeft + ovalWidth + 20, ovalTop + ovalHeight - cornerLength), strokeWidth = 6f)
    }
}

fun captureAndCrop(
    context: Context,
    imageCapture: ImageCapture?,
    executor: Executor,
    onSuccess: (Bitmap) -> Unit,
    onError: () -> Unit
) {
    val imgCap = imageCapture ?: run { onError(); return }

    imgCap.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                try {
                    val originalBitmap = imageProxyToBitmap(imageProxy)
                    imageProxy.close()

                    val detector = FaceDetection.getClient(
                        FaceDetectorOptions.Builder()
                            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                            .build()
                    )

                    val image = InputImage.fromBitmap(originalBitmap, 0)

                    detector.process(image)
                        .addOnSuccessListener(executor) { faces ->
                            if (faces.isNotEmpty()) {
                                val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }!!
                                val croppedBitmap = cropBitmapToFace(originalBitmap, face.boundingBox)
                                val optimizedBitmap = resizeBitmap(croppedBitmap, 800)
                                onSuccess(optimizedBitmap)
                            } else {
                                val optimizedBitmap = resizeBitmap(originalBitmap, 800)
                                onSuccess(optimizedBitmap)
                            }
                        }
                        .addOnFailureListener(executor) { onError() }
                } catch (e: Exception) {
                    e.printStackTrace()
                    onError()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                onError()
            }
        }
    )
}

fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    val matrix = Matrix()
    matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

fun cropBitmapToFace(original: Bitmap, faceRect: Rect): Bitmap {
    val margin = 0.25f
    val widthMargin = (faceRect.width() * margin).toInt()
    val heightMargin = (faceRect.height() * margin).toInt()

    val x1 = max(0, faceRect.left - widthMargin)
    val y1 = max(0, faceRect.top - heightMargin)
    val x2 = kotlin.math.min(original.width, faceRect.right + widthMargin)
    val y2 = kotlin.math.min(original.height, faceRect.bottom + heightMargin)

    val newWidth = x2 - x1
    val newHeight = y2 - y1

    if (newWidth <= 0 || newHeight <= 0) return original
    return Bitmap.createBitmap(original, x1, y1, newWidth, newHeight)
}

fun resizeBitmap(source: Bitmap, maxLength: Int): Bitmap {
    try {
        if (source.width <= maxLength && source.height <= maxLength) return source
        val aspectRatio = source.width.toDouble() / source.height.toDouble()
        val targetWidth = if (aspectRatio >= 1) maxLength else (maxLength * aspectRatio).toInt()
        val targetHeight = if (aspectRatio < 1) maxLength else (maxLength / aspectRatio).toInt()
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    } catch (e: Exception) {
        return source
    }
}