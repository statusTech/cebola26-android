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

    // --- Regras ---
    var rules by mutableStateOf(RegistrationRules())
        private set

    // --- Formulário ---
    var name by mutableStateOf("")
    var cpf by mutableStateOf("")
    var qrCode by mutableStateOf("")
    var cpfFieldError by mutableStateOf<String?>(null)
    var qrCodeFieldError by mutableStateOf<String?>(null)

    // --- Controle de UI QR ---
    var isQrManualMode by mutableStateOf(false)

    // --- Câmera ---
    var capturedBitmap by mutableStateOf<Bitmap?>(null)
    var isFaceGood by mutableStateOf(false)
    var cameraFeedback by mutableStateOf("Posicione o rosto")

    // --- Liveness ---
    var currentLivenessAction by mutableStateOf(LivenessAction.NONE)

    // --- UI State ---
    private val _uiState = MutableStateFlow<RegistrationUiState>(RegistrationUiState.Login)
    val uiState = _uiState.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        val savedId = prefs.getString("staff_id", null)
        val savedName = prefs.getString("staff_name", null)
        if (savedId != null && savedName != null) {
            currentStaffId = savedId
            currentStaffName = savedName
            _uiState.value = RegistrationUiState.StepQr
            fetchRules()
        }
    }

    private fun fetchRules() {
        viewModelScope.launch(Dispatchers.IO) {
            rules = repository.getRegistrationRules(currentStaffId)
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
        if (staffName.isBlank()) { loginError = "Preencha seu nome."; return }
        if (staffPassword != "Cebol@26") { loginError = "Senha incorreta."; return }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val loginTime = getUtcTimestamp()
                val existingStaff = repository.findStaffByName(staffName)
                val staffId = existingStaff?.id ?: UUID.randomUUID().toString()

                val staff = Staff(id = staffId, name = staffName, lastLogin = loginTime)
                val result = repository.saveStaffLogin(staff)

                if (result.isSuccess) {
                    currentStaffId = staffId
                    currentStaffName = staffName
                    prefs.edit().putString("staff_id", staffId).putString("staff_name", staffName).apply()
                    loginError = null
                    staffPassword = ""
                    _uiState.value = RegistrationUiState.StepQr
                    fetchRules()
                } else {
                    loginError = "Erro de conexão."
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
        if (rules.requireQrCode) {
            if (qrCode.isBlank()) { qrCodeFieldError = "QR Code é obrigatório"; return }
            if (!qrCode.all { it.isDigit() } || qrCode.length != 12) { qrCodeFieldError = "QR Code Inválido"; return }
        }
        _uiState.value = RegistrationUiState.StepData
    }

    fun updateCpf(input: String) {
        if (input.length <= 11) {
            cpf = input.filter { it.isDigit() }
            if (cpfFieldError != null) cpfFieldError = null
        }
    }

    fun goToPhotoStep() {
        if (rules.requireName && name.isBlank()) return
        if (rules.requireCpf) {
            if (cpf.length != 11) { cpfFieldError = "CPF Incompleto"; return }
        }
        currentLivenessAction = LivenessAction.BLINK
        isFaceGood = false
        cameraFeedback = "Centralize o rosto"
        _uiState.value = RegistrationUiState.StepPhoto
    }

    fun onStartCamera() {
        currentLivenessAction = LivenessAction.BLINK
        isFaceGood = false
        cameraFeedback = "Centralize o rosto"
        capturedBitmap = null
        _uiState.value = RegistrationUiState.Camera
    }

    fun cancelCamera() {
        if (_uiState.value is RegistrationUiState.Camera) {
            _uiState.value = RegistrationUiState.StepPhoto
        } else if (_uiState.value is RegistrationUiState.QrScanner) {
            _uiState.value = RegistrationUiState.StepQr
        }
    }

    fun onPhotoCaptured(bitmap: Bitmap) {
        capturedBitmap = bitmap
        _uiState.value = RegistrationUiState.StepPhoto
    }

    fun retakePhoto() {
        capturedBitmap = null
        onStartCamera()
    }

    fun submitRegistration() {
        if (rules.requirePhoto && capturedBitmap == null) return

        _uiState.value = RegistrationUiState.Uploading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val shouldCheckCpf = rules.requireCpf || cpf.isNotEmpty()
                if (shouldCheckCpf && repository.checkCpfExists(cpf)) {
                    cpfFieldError = "Este CPF já está cadastrado."
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
                        delay(3000)
                        resetNewFlow()
                        fetchRules()
                    } else {
                        _uiState.value = RegistrationUiState.Error("Falha ao salvar dados.")
                        delay(3000)
                        _uiState.value = RegistrationUiState.StepPhoto
                    }
                }
            } catch (e: Exception) {
                _uiState.value = RegistrationUiState.Error("Erro: ${e.message}")
                delay(3000)
                _uiState.value = RegistrationUiState.StepPhoto
            }
        }
    }

    private fun resetNewFlow() {
        name = ""
        cpf = ""
        qrCode = ""
        cpfFieldError = null
        qrCodeFieldError = null
        capturedBitmap = null
        isQrManualMode = false
        isFaceGood = false
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