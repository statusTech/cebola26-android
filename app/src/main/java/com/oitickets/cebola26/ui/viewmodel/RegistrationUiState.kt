package com.oitickets.cebola26.ui.viewmodel

sealed class RegistrationUiState {
    // Tela de Login
    object Login : RegistrationUiState()

    // --- Passos do Fluxo Principal ---
    object StepQr : RegistrationUiState()
    object StepData : RegistrationUiState()
    object StepPhoto : RegistrationUiState()

    object Camera : RegistrationUiState()
    object QrScanner : RegistrationUiState()

    object Uploading : RegistrationUiState()
    object Success : RegistrationUiState()

    object PendingUploads : RegistrationUiState()
    data class Error(val message: String) : RegistrationUiState()

    // --- Fluxo de Troca de Foto ---
    object ChangePhotoCpfInput : RegistrationUiState()
    object ChangePhotoCamera : RegistrationUiState()
    object ChangePhotoPreview : RegistrationUiState()
}