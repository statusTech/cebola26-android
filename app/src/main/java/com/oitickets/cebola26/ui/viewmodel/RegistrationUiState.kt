package com.oitickets.cebola26.ui.viewmodel

sealed class RegistrationUiState {
    // Tela de Login
    object Login : RegistrationUiState()

    // --- Passos do Fluxo Principal ---
    object StepQr : RegistrationUiState()
    object StepData : RegistrationUiState()
    object StepPhoto : RegistrationUiState()

    // --- Estados Auxiliares / Legado ---
    // Necessários pois o ViewModel ainda referencia eles para ações específicas
    object Camera : RegistrationUiState()     // Usado ao clicar em "Tirar Outra" ou reiniciar câmera
    object QrScanner : RegistrationUiState()  // Usado para o scanner tela cheia
    object Form : RegistrationUiState()       // Fallback de segurança

    // --- Feedback ---
    object Uploading : RegistrationUiState()
    object Success : RegistrationUiState()
    data class Error(val message: String) : RegistrationUiState()
}