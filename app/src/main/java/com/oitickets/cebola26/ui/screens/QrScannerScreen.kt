package com.oitickets.cebola26.ui.screens

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.oitickets.cebola26.domain.QrCodeAnalyzer
import java.util.concurrent.Executors

@Composable
fun QrScannerScreen(
    onCodeScanned: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    // Estado para evitar múltiplas leituras seguidas
    var hasScanned by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // 1. CÂMERA
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

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(executor, QrCodeAnalyzer { code ->
                                if (!hasScanned) {
                                    hasScanned = true
                                    // Sair da thread de background para a UI
                                    ContextCompat.getMainExecutor(ctx).execute {
                                        onCodeScanned(code)
                                    }
                                }
                            })
                        }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("QrScanner", "Erro câmera", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        // 2. OVERLAY (Quadrado Transparente)
        QrOverlay()

        // 3. TEXTO DE AJUDA
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Aponte para o QR Code do ingresso",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 4. BOTÃO FECHAR
        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 24.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                .size(48.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
        }
    }
}

@Composable
fun QrOverlay() {
    // Animação da linha de scanner
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val linePosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "line"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val squareSize = size.width * 0.7f // 70% da largura da tela
        val left = (size.width - squareSize) / 2
        val top = (size.height - squareSize) / 2

        // 1. Escurecer tudo exceto o quadrado
        val path = Path().apply {
            addRect(Rect(0f, 0f, size.width, size.height))
            val squarePath = Path().apply {
                addRect(Rect(left, top, left + squareSize, top + squareSize))
            }
            op(this, squarePath, PathOperation.Difference)
        }

        drawPath(path = path, color = Color.Black.copy(alpha = 0.7f))

        // 2. Borda do Quadrado
        drawRect(
            color = Color.White.copy(alpha = 0.5f),
            topLeft = Offset(left, top),
            size = Size(squareSize, squareSize),
            style = Stroke(width = 4.dp.toPx())
        )

        // 3. Cantos "Tech" (Verdes)
        val cornerLength = 40.dp.toPx()
        val cornerColor = Color(0xFF00E676)
        val cornerStroke = 6.dp.toPx()

        // Top Left
        drawLine(cornerColor, Offset(left, top), Offset(left + cornerLength, top), cornerStroke)
        drawLine(cornerColor, Offset(left, top), Offset(left, top + cornerLength), cornerStroke)
        // Top Right
        drawLine(cornerColor, Offset(left + squareSize, top), Offset(left + squareSize - cornerLength, top), cornerStroke)
        drawLine(cornerColor, Offset(left + squareSize, top), Offset(left + squareSize, top + cornerLength), cornerStroke)
        // Bottom Left
        drawLine(cornerColor, Offset(left, top + squareSize), Offset(left + cornerLength, top + squareSize), cornerStroke)
        drawLine(cornerColor, Offset(left, top + squareSize), Offset(left, top + squareSize - cornerLength), cornerStroke)
        // Bottom Right
        drawLine(cornerColor, Offset(left + squareSize, top + squareSize), Offset(left + squareSize - cornerLength, top + squareSize), cornerStroke)
        drawLine(cornerColor, Offset(left + squareSize, top + squareSize), Offset(left + squareSize, top + squareSize - cornerLength), cornerStroke)

        // 4. Linha de Scanner Animada
        val lineY = top + (squareSize * linePosition)
        drawLine(
            color = Color(0xFFFFCA28).copy(alpha = 0.8f), // Cor Âmbar da cebola
            start = Offset(left, lineY),
            end = Offset(left + squareSize, lineY),
            strokeWidth = 2.dp.toPx()
        )
    }
}