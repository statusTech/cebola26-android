package com.oitickets.cebola26.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oitickets.cebola26.domain.CpfVisualTransformation
import com.oitickets.cebola26.ui.viewmodel.RegistrationViewModel

@Composable
fun FormScreen(viewModel: RegistrationViewModel) {
    val focusManager = LocalFocusManager.current
    // Acessa as regras carregadas (feature flags) vindas do ViewModel
    val rules = viewModel.rules

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Cabeçalho ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Credenciamento",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Festa da Cebola 2026",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Medium
            )
        }

        // --- Área da Foto (Estilo Compacto) ---
        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            // Círculo da Foto
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(
                        width = 4.dp,
                        color = if (viewModel.capturedBitmap != null) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    // Torna a própria imagem clicável também
                    .clickable {
                        focusManager.clearFocus()
                        viewModel.onStartCamera()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (viewModel.capturedBitmap != null) {
                    Image(
                        bitmap = viewModel.capturedBitmap!!.asImageBitmap(),
                        contentDescription = "Foto capturada",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.LightGray.copy(alpha = 0.5f),
                        modifier = Modifier.size(80.dp)
                    )
                }
            }

            // Botão Flutuante Pequeno (Badge) no canto
            SmallFloatingActionButton(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.onStartCamera()
                },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.offset(x = (-8).dp, y = (-8).dp),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = "Capturar Foto",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Texto de ajuda (só mostra erro se for obrigatório e estiver vazio)
        if (viewModel.capturedBitmap == null) {
            if (rules.requirePhoto) {
                Text(
                    text = "Foto obrigatória",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp).offset(y = (-16).dp)
                )
            } else {
                Text(
                    text = "Foto opcional",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp).offset(y = (-16).dp)
                )
            }
        }

        // --- Formulário em Card ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // --- 1. Seção QR Code (Destacada) ---
                if (rules.requireQrCode) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = viewModel.qrCode,
                            // Usa a função updateQrCode para limpar erro ao digitar
                            onValueChange = { viewModel.updateQrCode(it) },
                            label = { Text("Código QR (Ingresso)*") },
                            leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                            // Configurado para numérico conforme solicitado
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            // Exibição do erro de validação (12 dígitos, numérico)
                            isError = viewModel.qrCodeFieldError != null,
                            supportingText = {
                                if (viewModel.qrCodeFieldError != null) {
                                    Text(
                                        text = viewModel.qrCodeFieldError!!,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                focusedLabelColor = MaterialTheme.colorScheme.secondary,
                                errorBorderColor = MaterialTheme.colorScheme.error,
                                errorLabelColor = MaterialTheme.colorScheme.error
                            )
                        )

                        // Botão de Scanner de QR Code GRANDE em evidência
                        FilledTonalButton(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.onStartQrScanner()
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ABRIR LEITOR DE QR CODE", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // --- 2. Nome Completo ---
                OutlinedTextField(
                    value = viewModel.name,
                    onValueChange = { input ->
                        // Capitaliza cada palavra
                        val capitalized = input.split(" ").joinToString(" ") { word ->
                            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                        }
                        viewModel.name = capitalized
                    },
                    label = {
                        Text("Nome Completo" + if (rules.requireName) "*" else " (Opcional)")
                    },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                // --- 3. CPF com Máscara ---
                val cpfVisualTransformation = remember { CpfVisualTransformation() }
                OutlinedTextField(
                    value = viewModel.cpf,
                    onValueChange = {
                        viewModel.updateCpf(it)
                    },
                    label = {
                        Text("CPF" + if (rules.requireCpf) "*" else " (Opcional)")
                    },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        // Se tiver QR code, avança, senão conclui
                        imeAction = if (rules.requireQrCode) ImeAction.Next else ImeAction.Done
                    ),
                    visualTransformation = cpfVisualTransformation,
                    isError = viewModel.cpfFieldError != null,
                    supportingText = {
                        if (viewModel.cpfFieldError != null) {
                            Text(
                                text = viewModel.cpfFieldError!!,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        errorBorderColor = MaterialTheme.colorScheme.error,
                        errorLabelColor = MaterialTheme.colorScheme.error
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Validação Dinâmica para habilitar o botão
        val isFormValid =
            (!rules.requireName || viewModel.name.isNotBlank()) &&
                    (!rules.requireCpf || viewModel.cpf.length == 11) &&
                    (!rules.requirePhoto || viewModel.capturedBitmap != null) &&
                    (!rules.requireQrCode || viewModel.qrCode.isNotBlank())

        // --- Botão de Salvar ---
        Button(
            onClick = {
                focusManager.clearFocus()

                // Validação de formato de CPF (apenas se for obrigatório ou se tiver preenchido)
                val shouldValidateCpf = rules.requireCpf || viewModel.cpf.isNotEmpty()

                if (shouldValidateCpf && !isCpfValid(viewModel.cpf)) {
                    viewModel.cpfFieldError = "CPF Inválido"
                } else {
                    // A validação do QR Code (números e 12 dígitos) será feita dentro de submitRegistration
                    viewModel.submitRegistration()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(56.dp)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp), clip = false),
            shape = RoundedCornerShape(12.dp),
            enabled = isFormValid,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFE0E0E0),
                disabledContentColor = Color(0xFF9E9E9E)
            )
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("SALVAR CADASTRO", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- BOTÃO SAIR (LOGOUT) ---
        TextButton(
            onClick = { viewModel.logout() },
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            Icon(
                Icons.Default.ExitToApp,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Sair / Trocar Usuário",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelLarge,
                textDecoration = TextDecoration.Underline
            )
        }
    }
}

// Função utilitária para validar CPF
private fun isCpfValid(cpf: String): Boolean {
    val cleanCpf = cpf.filter { it.isDigit() }
    if (cleanCpf.length != 11) return false
    if (cleanCpf.all { it == cleanCpf[0] }) return false
    try {
        var sum = 0
        for (i in 0..8) sum += (cleanCpf[i] - '0') * (10 - i)
        var remainder = 11 - (sum % 11)
        val digit1 = if (remainder >= 10) 0 else remainder
        if (digit1 != (cleanCpf[9] - '0')) return false
        sum = 0
        for (i in 0..9) sum += (cleanCpf[i] - '0') * (11 - i)
        remainder = 11 - (sum % 11)
        val digit2 = if (remainder >= 10) 0 else remainder
        return digit2 == (cleanCpf[10] - '0')
    } catch (e: Exception) { return false }
}