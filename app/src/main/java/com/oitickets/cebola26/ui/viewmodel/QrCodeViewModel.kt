package com.oitickets.cebola26.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oitickets.cebola26.data.model.RegistrationRules
import com.oitickets.cebola26.data.repository.QrCodeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QrCodeViewModel(
    private val repository: QrCodeRepository
) : ViewModel() {

    // --- Estado da Tela de QR Code ---
    var qrCode by mutableStateOf("")
    var qrCodeFieldError by mutableStateOf<String?>(null)
    var isQrManualMode by mutableStateOf(false)
    var isValidating by mutableStateOf(false) // Loading state para feedback visual durante a validação

    // --- Ações de UI ---

    fun onStartQrScanner() {
        isQrManualMode = false
        qrCodeFieldError = null
    }

    fun onQrCodeScanned(code: String) {
        qrCode = code
        isQrManualMode = true // Ao ler, vai para o modo manual para conferência visual
        qrCodeFieldError = null
    }

    fun updateQrCode(input: String) {
        qrCode = input
        if (qrCodeFieldError != null) qrCodeFieldError = null
    }

    fun toggleQrManualMode() {
        isQrManualMode = !isQrManualMode
    }

    // --- Validação ---

    /**
     * Valida o QR Code conforme as regras passadas.
     * @param rules As regras de negócio atuais (vindas do RegistrationViewModel)
     * @param onValidationSuccess Callback executado apenas se estiver válido
     */
    fun validateQrCode(rules: RegistrationRules, onValidationSuccess: () -> Unit) {
        // 1. Validação Local (Obrigatoriedade e Formato)
        if (qrCode.isBlank()) {
            if (rules.requireQrCode) {
                qrCodeFieldError = "QR Code é obrigatório"
                return
            } else {
                // Vazio e opcional -> Passa direto
                onValidationSuccess()
                return
            }
        }

        // Se tem texto, valida formato (Apenas números e 12 dígitos)
        val isNumeric = qrCode.all { it.isDigit() }
        val isValidLength = qrCode.length == 12

        if (!isNumeric || !isValidLength) {
            qrCodeFieldError = "QR Code Inválido"
            return
        }

        // 2. Validação de Duplicidade (Remota)
        // Se a regra PERMITE duplicados, não perdemos tempo checando o banco
        if (rules.allowDuplicateQrCode) {
            onValidationSuccess()
            return
        }

        // Se duplicidade for proibida, verifica no banco se já existe
        isValidating = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val exists = repository.checkQrCodeExists(qrCode)
                isValidating = false

                if (exists) {
                    qrCodeFieldError = "Este QR Code já foi utilizado!"
                } else {
                    onValidationSuccess()
                }
            } catch (e: Exception) {
                isValidating = false
                e.printStackTrace()
                onValidationSuccess()
            }
        }
    }

    // Reseta estado para novo cadastro
    fun reset() {
        qrCode = ""
        qrCodeFieldError = null
        isQrManualMode = false
        isValidating = false
    }
}

// Factory exclusiva para este ViewModel
class QrCodeViewModelFactory() : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QrCodeViewModel::class.java)) {
            return QrCodeViewModel(QrCodeRepository()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}