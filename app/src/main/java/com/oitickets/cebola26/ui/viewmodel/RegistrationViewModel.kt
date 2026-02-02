package com.oitickets.cebola26.ui.viewmodel

import android.content.SharedPreferences
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oitickets.cebola26.data.model.Participant
import com.oitickets.cebola26.data.model.RegistrationRules
import com.oitickets.cebola26.data.model.Staff
import com.oitickets.cebola26.data.repository.RegistrationRepository
import com.oitickets.cebola26.domain.LivenessAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlin.random.Random

class RegistrationViewModel(
    private val repository: RegistrationRepository,
    private val prefs: SharedPreferences
) : ViewModel() {

    // --- Estados de Login ---
    var staffName by mutableStateOf("")
    var staffPassword by mutableStateOf("")
    var loginError by mutableStateOf<String?>(null)

    private var currentStaffId: String? = null
    private var currentStaffName: String? = null

    // --- Regras de Negócio (Feature Flags) ---
    var rules by mutableStateOf(RegistrationRules())
        private set

    // --- Estados do Formulário ---
    var name by mutableStateOf("")
    var cpf by mutableStateOf("")
    var qrCode by mutableStateOf("")
    var cpfFieldError by mutableStateOf<String?>(null)
    var qrCodeFieldError by mutableStateOf<String?>(null)

    // --- Controle de UI do Passo QR Code ---
    var isQrManualMode by mutableStateOf(false)

    // --- Estados da Câmera ---
    var capturedBitmap by mutableStateOf<Bitmap?>(null)
    var isFaceGood by mutableStateOf(false)
    var cameraFeedback by mutableStateOf("Posicione o rosto no centro")

    // --- Estado da Prova de Vida (Liveness) ---
    var currentLivenessAction by mutableStateOf(LivenessAction.NONE)

    // --- UI State (Fluxo de Navegação) ---
    private val _uiState = MutableStateFlow<RegistrationUiState>(RegistrationUiState.Login)
    val uiState = _uiState.asStateFlow()

    init {
        checkSession()
    }

    // --- SESSÃO E LOGIN ---

    private fun checkSession() {
        val savedId = prefs.getString("staff_id", null)
        val savedName = prefs.getString("staff_name", null)

        if (savedId != null && savedName != null) {
            currentStaffId = savedId
            currentStaffName = savedName
            // Se já tem sessão, vai direto para o primeiro passo do fluxo
            _uiState.value = RegistrationUiState.StepQr
            fetchRules()
        }
    }

    private fun fetchRules() {
        viewModelScope.launch(Dispatchers.IO) {
            // Carrega as regras do Firestore
            val loadedRules = repository.getRegistrationRules(currentStaffId)
            rules = loadedRules
        }
    }

    // --- NAVEGAÇÃO / VOLTAR ---
    fun navigateBack() {
        val currentState = _uiState.value
        when (currentState) {
            // Se está nos Dados -> Volta para QR
            is RegistrationUiState.StepData -> _uiState.value = RegistrationUiState.StepQr
            // Se está na Foto -> Volta para Dados
            is RegistrationUiState.StepPhoto -> _uiState.value = RegistrationUiState.StepData
            // Se está na Câmera/Scanner -> Cancela e volta para tela anterior adequada
            is RegistrationUiState.Camera -> cancelCamera()
            is RegistrationUiState.QrScanner -> cancelCamera()
            else -> { /* Não faz nada (ex: Login, StepQr raiz) */ }
        }
    }

    fun performLogin() {
        if (staffName.isBlank()) {
            loginError = "Preencha seu nome."
            return
        }
        if (staffPassword != "Cebol@26") {
            loginError = "Senha incorreta."
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val loginTime = getUtcTimestamp()
                val existingStaff = repository.findStaffByName(staffName)
                val staffId = existingStaff?.id ?: UUID.randomUUID().toString()

                val staff = Staff(
                    id = staffId,
                    name = staffName,
                    lastLogin = loginTime
                )

                val result = repository.saveStaffLogin(staff)

                if (result.isSuccess) {
                    currentStaffId = staffId
                    currentStaffName = staffName

                    prefs.edit()
                        .putString("staff_id", staffId)
                        .putString("staff_name", staffName)
                        .apply()

                    loginError = null
                    staffPassword = ""

                    // Inicia fluxo no passo 1
                    _uiState.value = RegistrationUiState.StepQr
                    fetchRules()
                } else {
                    loginError = "Erro de conexão ao logar."
                }
            } catch (e: Exception) {
                loginError = "Erro: ${e.message}"
            }
        }
    }

    fun logout() {
        prefs.edit().clear().apply()
        currentStaffId = null
        currentStaffName = null
        staffName = ""
        staffPassword = ""
        _uiState.value = RegistrationUiState.Login
    }

    // --- FLUXO PASSO A PASSO ---

    // 1. Passo QR Code
    fun onStartQrScanner() {
        _uiState.value = RegistrationUiState.QrScanner
    }

    fun onQrCodeScanned(code: String) {
        qrCode = code
        qrCodeFieldError = null
        isQrManualMode = true
        _uiState.value = RegistrationUiState.StepQr
    }

    fun updateQrCode(input: String) {
        qrCode = input
        if (qrCodeFieldError != null) qrCodeFieldError = null
    }

    fun toggleQrManualMode() {
        isQrManualMode = !isQrManualMode
    }

    fun goToDataStep() {
        // Validação do Passo 1
        // Se tiver algo digitado, valida o formato (independente se é obrigatório ou não)
        if (qrCode.isNotBlank()) {
            if (!qrCode.all { it.isDigit() } || qrCode.length != 12) {
                qrCodeFieldError = "QR Code Inválido (Deve ter 12 números)"
                return
            }
        } else if (rules.requireQrCode) {
            // Se estiver vazio e for obrigatório
            qrCodeFieldError = "QR Code é obrigatório"
            return
        }
        _uiState.value = RegistrationUiState.StepData
    }

    // 2. Passo Dados (Nome/CPF)
    fun updateCpf(input: String) {
        if (input.length <= 11) {
            cpf = input.filter { it.isDigit() }
            if (cpfFieldError != null) cpfFieldError = null
        }
    }

    fun goToPhotoStep() {
        // Validação do Passo 2
        if (rules.requireName && name.isBlank()) return

        if (rules.requireCpf) {
            if (cpf.length != 11) {
                cpfFieldError = "CPF Incompleto"
                return
            }
        }

        // Prepara câmera para o Passo 3
        onStartCamera()
    }

    // 3. Passo Foto
    fun onStartCamera() {
        // Define o desafio de Liveness (Blink)
        currentLivenessAction = LivenessAction.BLINK

        isFaceGood = false
        cameraFeedback = "Centralize o rosto"
        capturedBitmap = null // Limpa foto anterior

        // Define o estado para a UI mostrar a câmera
        _uiState.value = RegistrationUiState.Camera
    }

    fun cancelCamera() {
        // Se estava na Câmera -> volta para Passo Foto (que decide se mostra preview ou nada)
        if (_uiState.value is RegistrationUiState.Camera) {
            _uiState.value = RegistrationUiState.StepPhoto
        }
        // Se estava no Scanner -> volta para Passo QR
        else if (_uiState.value is RegistrationUiState.QrScanner) {
            _uiState.value = RegistrationUiState.StepQr
        }
        // [AJUSTE] Se cancelou a câmera inicial do Passo 3 -> volta para Passo 2 (Dados)
        else if (_uiState.value is RegistrationUiState.StepPhoto) {
            _uiState.value = RegistrationUiState.StepData
        }
    }

    fun onPhotoCaptured(bitmap: Bitmap) {
        capturedBitmap = bitmap
        // Volta para o Passo 3 (que agora mostrará o preview da foto capturada)
        _uiState.value = RegistrationUiState.StepPhoto
    }

    fun retakePhoto() {
        capturedBitmap = null
        onStartCamera()
    }

    // --- FINALIZAR CADASTRO ---

    fun submitRegistration() {
        // Validação Final (Passo 3)
        if (rules.requirePhoto && capturedBitmap == null) return

        // Validação Extra de QR Code para garantir consistência
        if (qrCode.isNotBlank()) {
            if (!qrCode.all { it.isDigit() } || qrCode.length != 12) {
                // QR Inválido não deveria chegar aqui, mas bloqueia envio
                return
            }
        } else if (rules.requireQrCode) {
            return
        }

        _uiState.value = RegistrationUiState.Uploading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Checa duplicidade de CPF se necessário
                val shouldCheckCpf = rules.requireCpf || cpf.isNotEmpty()

                if (shouldCheckCpf && repository.checkCpfExists(cpf)) {
                    cpfFieldError = "Este CPF já está cadastrado."
                    // Volta para o passo 2 para corrigir o CPF
                    _uiState.value = RegistrationUiState.StepData
                } else {
                    val timestamp = getUtcTimestamp()

                    val participant = Participant(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        cpf = cpf,
                        qrCode = qrCode,
                        createdAt = timestamp,
                        registeredBy = "${currentStaffName} (${currentStaffId})"
                    )

                    val result = repository.saveParticipant(participant, capturedBitmap)

                    if (result.isSuccess) {
                        _uiState.value = RegistrationUiState.Success
                        delay(1500)
                        resetNewFlow() // Reinicia o fluxo
                        fetchRules()
                    } else {
                        _uiState.value = RegistrationUiState.Error("Falha ao salvar dados.")
                        delay(3000)
                        _uiState.value = RegistrationUiState.StepPhoto
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = RegistrationUiState.Error("Erro: ${e.message}")
                delay(3000)
                _uiState.value = RegistrationUiState.StepPhoto
            }
        }
    }

    // Reseta o estado para iniciar um novo cadastro limpo
    private fun resetNewFlow() {
        name = ""
        cpf = ""
        qrCode = ""
        cpfFieldError = null
        qrCodeFieldError = null
        capturedBitmap = null
        isQrManualMode = false
        isFaceGood = false

        // Volta para o primeiro passo do fluxo
        _uiState.value = RegistrationUiState.StepQr
    }

    private fun getUtcTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }
}

class RegistrationViewModelFactory(private val prefs: SharedPreferences) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegistrationViewModel::class.java)) {
            return RegistrationViewModel(RegistrationRepository(), prefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}