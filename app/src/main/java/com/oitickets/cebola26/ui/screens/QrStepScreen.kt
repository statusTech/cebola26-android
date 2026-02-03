package com.oitickets.cebola26.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oitickets.cebola26.ui.viewmodel.RegistrationViewModel

@Composable
fun QrStepScreen(viewModel: RegistrationViewModel) {
    val rules = viewModel.rules

    // Lógica do Botão Avançar Atualizada:
    // Habilita se NÃO for obrigatório OU se tiver algum texto digitado.
    // A validação rigorosa acontece no onClick.
    val isNextEnabled = !rules.requireQrCode || viewModel.qrCode.isNotBlank()

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        Box(modifier = Modifier.weight(1f)) {
            if (viewModel.isQrManualMode) {
                // MODO MANUAL: Fundo escuro com campo de texto centralizado
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Digite o código do ingresso",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = viewModel.qrCode,
                        onValueChange = {
                            // Permite digitar letras e números livremente
                            viewModel.updateQrCode(it)
                        },
                        placeholder = { Text("QR CODE") },
                        leadingIcon = { Icon(Icons.Default.QrCode, null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        isError = viewModel.qrCodeFieldError != null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            errorContainerColor = Color.White
                        )
                    )

                    if (viewModel.qrCodeFieldError != null) {
                        Text(
                            text = viewModel.qrCodeFieldError!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // MODO CÂMERA:
                QrScannerScreen(
                    onCodeScanned = { code -> viewModel.onQrCodeScanned(code) },
                    // CORREÇÃO: O botão "X" agora fecha a câmera e volta para o manual
                    onCancel = { viewModel.toggleQrManualMode() }
                )
            }
        }

        // BARRA INFERIOR
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                // Link para alternar modo
                TextButton(
                    onClick = { viewModel.toggleQrManualMode() },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = if (viewModel.isQrManualMode) "Abrir Câmera / Scanner" else "Informar QR Code manualmente",
                        style = MaterialTheme.typography.labelLarge,
                        textDecoration = TextDecoration.Underline
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        // Validação Local Rigorosa ao clicar
                        val qrCode = viewModel.qrCode
                        if (qrCode.isNotEmpty()) {
                            val isNumeric = qrCode.all { it.isDigit() }
                            val isValidLength = qrCode.length == 12

                            if (!isNumeric || !isValidLength) {
                                viewModel.qrCodeFieldError = "QR Code Inválido"
                                return@Button // Para aqui e não avança
                            }
                        }

                        viewModel.goToDataStep()
                    },
                    enabled = isNextEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("AVANÇAR (1/3)", fontWeight = FontWeight.Bold)
                }

                if (!rules.requireQrCode) {
                    Text(
                        text = "* QR Code opcional",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 8.dp)
                    )
                }
            }
        }
    }
}