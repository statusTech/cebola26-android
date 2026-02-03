package com.oitickets.cebola26.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oitickets.cebola26.data.model.RegistrationRules
import com.oitickets.cebola26.ui.viewmodel.QrCodeViewModel

@Composable
fun QrStepScreen(
    qrViewModel: QrCodeViewModel,      // Lógica interna da tela
    rules: RegistrationRules,          // Regras de negócio (injetadas)
    onNext: (String) -> Unit,          // Callback de sucesso
    initialQrCode: String = ""         // Valor inicial (para quando voltar da tela de dados)
) {
    // Sincroniza o QR Code se já existir um valor salvo anteriormente no fluxo
    LaunchedEffect(initialQrCode) {
        if (initialQrCode.isNotBlank() && qrViewModel.qrCode.isBlank()) {
            qrViewModel.updateQrCode(initialQrCode)
            qrViewModel.isQrManualMode = true
        }
    }

    // Lógica de UI do QrCodeViewModel
    val isNextEnabled = !rules.requireQrCode || qrViewModel.qrCode.isNotBlank()

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        Box(modifier = Modifier.weight(1f)) {
            if (qrViewModel.isQrManualMode) {
                // MODO MANUAL
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
                        value = qrViewModel.qrCode,
                        onValueChange = { qrViewModel.updateQrCode(it) },
                        placeholder = { Text("000000000000") },
                        leadingIcon = { Icon(Icons.Default.QrCode, null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        isError = qrViewModel.qrCodeFieldError != null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            errorContainerColor = Color.White
                        )
                    )

                    if (qrViewModel.qrCodeFieldError != null) {
                        Text(
                            text = qrViewModel.qrCodeFieldError!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (qrViewModel.isValidating) {
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(color = Color.White)
                        Text("Verificando duplicidade...", color = Color.White, fontSize = 12.sp)
                    }
                }
            } else {
                // MODO CÂMERA INTEGRADO
                QrScannerScreen(
                    onCodeScanned = { code ->
                        qrViewModel.onQrCodeScanned(code)
                    },
                    onCancel = { qrViewModel.toggleQrManualMode() }
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
                    onClick = { qrViewModel.toggleQrManualMode() },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = if (qrViewModel.isQrManualMode) "Abrir Câmera / Scanner" else "Informar QR Code manualmente",
                        style = MaterialTheme.typography.labelLarge,
                        textDecoration = TextDecoration.Underline
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        // VALIDAÇÃO: Usa o QrCodeViewModel
                        qrViewModel.validateQrCode(rules) {
                            // Se sucesso (formato OK e não duplicado), chama o callback
                            onNext(qrViewModel.qrCode)
                        }
                    },
                    enabled = isNextEnabled && !qrViewModel.isValidating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (qrViewModel.isValidating) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("AVANÇAR (1/3)", fontWeight = FontWeight.Bold)
                    }
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