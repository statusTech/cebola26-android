package com.oitickets.cebola26

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.oitickets.cebola26.ui.components.AppTopBar
import com.oitickets.cebola26.ui.screens.*
import com.oitickets.cebola26.ui.theme.Cebola26Theme
import com.oitickets.cebola26.ui.viewmodel.QrCodeViewModel
import com.oitickets.cebola26.ui.viewmodel.QrCodeViewModelFactory
import com.oitickets.cebola26.ui.viewmodel.RegistrationUiState
import com.oitickets.cebola26.ui.viewmodel.RegistrationViewModel
import com.oitickets.cebola26.ui.viewmodel.RegistrationViewModelFactory

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "O aplicativo precisa da câmera para funcionar.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissionLauncher.launch(Manifest.permission.CAMERA)

        val prefs = getSharedPreferences("cebola_prefs", Context.MODE_PRIVATE)
        // Passa o applicationContext para a Factory (necessário para o WorkManager no Repository)
        val viewModel: RegistrationViewModel by viewModels {
            RegistrationViewModelFactory(prefs, applicationContext)
        }

        val qrViewModel: QrCodeViewModel by viewModels { QrCodeViewModelFactory() }

        setContent {
            Cebola26Theme {
                val uiState by viewModel.uiState.collectAsState()

                Scaffold(
                    topBar = {
                        // Lógica de visibilidade da Barra Superior
                        // Esconde no Login, Sucesso, Câmera e Scanner para foco total
                        val showTopBar = uiState !is RegistrationUiState.Login &&
                                uiState !is RegistrationUiState.Success &&
                                uiState !is RegistrationUiState.Camera &&
                                uiState !is RegistrationUiState.QrScanner

                        // Mostra botão de voltar nas etapas intermediárias ou na tela de uploads
                        val showBackButton = uiState is RegistrationUiState.StepData ||
                                uiState is RegistrationUiState.StepPhoto ||
                                uiState is RegistrationUiState.PendingUploads

                        if (showTopBar) {
                            AppTopBar(
                                onBackClick = if (showBackButton) { { viewModel.navigateBack() } } else null,
                                pendingUploadsCount = viewModel.pendingUploadsCount,
                                onUploadsClick = { viewModel.openPendingUploads() }
                            )
                        }
                    }
                ) { paddingValues ->

                    Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                        when (val state = uiState) {
                            is RegistrationUiState.Login -> LoginScreen(viewModel)

                            // Fluxo de Cadastro
                            is RegistrationUiState.StepData -> DataStepScreen(viewModel)
                            is RegistrationUiState.StepPhoto -> CameraScreen(
                                viewModel = viewModel,
                                onPhotoTaken = { viewModel.onPhotoCaptured(it) },
                                onCancel = { viewModel.cancelCamera() }
                            )

                            // Estados de Câmera/Scanner
                            is RegistrationUiState.Camera -> CameraScreen(
                                viewModel = viewModel,
                                onPhotoTaken = { viewModel.onPhotoCaptured(it) },
                                onCancel = { viewModel.cancelCamera() }
                            )

                            is RegistrationUiState.QrScanner -> {
                                QrScannerScreen(
                                    onCodeScanned = { code ->
                                        qrViewModel.onQrCodeScanned(code)
                                        viewModel.navigateBack()
                                    },
                                    onCancel = {
                                        qrViewModel.toggleQrManualMode()
                                        viewModel.navigateBack()
                                    }
                                )
                            }

                            is RegistrationUiState.StepQr -> {
                                QrStepScreen(
                                    qrViewModel = qrViewModel,
                                    rules = viewModel.rules,
                                    initialQrCode = viewModel.qrCode,
                                    onNext = { validCode ->

                                        viewModel.onQrStepCompleted(validCode)
                                    }
                                )
                            }

                            // Nova Tela de Uploads Offline
                            is RegistrationUiState.PendingUploads -> PendingUploadsScreen(viewModel)

                            // Feedback
                            is RegistrationUiState.Uploading -> LoadingScreen()
                            is RegistrationUiState.Success -> SuccessScreen()
                            is RegistrationUiState.Error -> ErrorScreen(message = state.message)

                        }
                    }
                }
            }
        }
    }
}