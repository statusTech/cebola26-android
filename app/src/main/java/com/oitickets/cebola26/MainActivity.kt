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
import com.oitickets.cebola26.ui.viewmodel.RegistrationUiState
import com.oitickets.cebola26.ui.viewmodel.RegistrationViewModel
import com.oitickets.cebola26.ui.viewmodel.RegistrationViewModelFactory

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "O aplicativo precisa da câmera.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        val prefs = getSharedPreferences("cebola_prefs", Context.MODE_PRIVATE)
        val viewModel: RegistrationViewModel by viewModels {
            RegistrationViewModelFactory(prefs, applicationContext)
        }

        setContent {
            Cebola26Theme {
                val uiState by viewModel.uiState.collectAsState()

                Scaffold(
                    topBar = {
                        // Lógica de visibilidade da Barra Superior
                        val showTopBar = uiState !is RegistrationUiState.Login &&
                                uiState !is RegistrationUiState.Success &&
                                uiState !is RegistrationUiState.Camera &&
                                uiState !is RegistrationUiState.QrScanner

                        // Lógica do botão Voltar (Aparece no StepData e StepPhoto)
                        val showBackButton = uiState is RegistrationUiState.StepData ||
                                uiState is RegistrationUiState.StepPhoto

                        if (showTopBar) {
                            AppTopBar(
                                onBackClick = if (showBackButton) { { viewModel.navigateBack() } } else null
                            )
                        }
                    }
                ) { paddingValues ->

                    Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                        when (val state = uiState) {
                            is RegistrationUiState.Login -> LoginScreen(viewModel)

                            is RegistrationUiState.StepQr -> QrStepScreen(viewModel)

                            is RegistrationUiState.StepData -> DataStepScreen(viewModel)

                            is RegistrationUiState.StepPhoto -> CameraScreen(
                                viewModel = viewModel,
                                onPhotoTaken = { viewModel.onPhotoCaptured(it) },
                                onCancel = { viewModel.cancelCamera() }
                            )

                            // Estados Auxiliares
                            is RegistrationUiState.Camera -> CameraScreen(
                                viewModel = viewModel,
                                onPhotoTaken = { viewModel.onPhotoCaptured(it) },
                                onCancel = { viewModel.cancelCamera() }
                            )

                            is RegistrationUiState.QrScanner -> QrScannerScreen(
                                onCodeScanned = { viewModel.onQrCodeScanned(it) },
                                onCancel = { viewModel.cancelCamera() }
                            )

                            is RegistrationUiState.Uploading -> LoadingScreen()
                            is RegistrationUiState.Success -> SuccessScreen()
                            is RegistrationUiState.Error -> ErrorScreen(message = state.message)

                            else -> {}
                        }
                    }
                }
            }
        }
    }
}