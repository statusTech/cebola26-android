package com.oitickets.cebola26.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.oitickets.cebola26.domain.CpfVisualTransformation
import com.oitickets.cebola26.ui.viewmodel.RegistrationViewModel

@Composable
fun DataStepScreen(viewModel: RegistrationViewModel) {
    val focusManager = LocalFocusManager.current
    val rules = viewModel.rules

    // Validação para habilitar botão
    val isNameValid = !rules.requireName || viewModel.name.isNotBlank()
    val isCpfValid = !rules.requireCpf || (viewModel.cpf.length == 11) // Validação matemática pode ser no clique
    val isNextEnabled = isNameValid && isCpfValid

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Dados do Participante",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 24.dp)
            )

            // Nome
            OutlinedTextField(
                value = viewModel.name,
                onValueChange = { input ->
                    val capitalized = input.split(" ").joinToString(" ") { word ->
                        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    }
                    viewModel.name = capitalized
                },
                label = { Text("Nome Completo" + if (rules.requireName) "*" else " (Opcional)") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // CPF
            val cpfVisualTransformation = remember { CpfVisualTransformation() }
            OutlinedTextField(
                value = viewModel.cpf,
                onValueChange = { viewModel.updateCpf(it) },
                label = { Text("CPF" + if (rules.requireCpf) "*" else " (Opcional)") },
                leadingIcon = { Icon(Icons.Default.Badge, null) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                visualTransformation = cpfVisualTransformation,
                isError = viewModel.cpfFieldError != null,
                supportingText = {
                    if (viewModel.cpfFieldError != null) {
                        Text(text = viewModel.cpfFieldError!!, color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // BARRA INFERIOR
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = { viewModel.goToPhotoStep() },
                    enabled = isNextEnabled,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("AVANÇAR (2/3)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}