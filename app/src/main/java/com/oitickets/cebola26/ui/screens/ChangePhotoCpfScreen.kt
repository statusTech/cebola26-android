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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.oitickets.cebola26.domain.CpfVisualTransformation
import com.oitickets.cebola26.ui.viewmodel.RegistrationViewModel

@Composable
fun ChangePhotoCpfScreen(viewModel: RegistrationViewModel) {
    val focusManager = LocalFocusManager.current
    val isBuscarEnabled = viewModel.changePhotoCpf.length == 11 && !viewModel.isSearchingCpf

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
                text = "Trocar Foto",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 24.dp)
            )

            Text(
                text = "Digite o CPF do participante para buscar o cadastro.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )

            val cpfVisualTransformation = remember { CpfVisualTransformation() }
            OutlinedTextField(
                value = viewModel.changePhotoCpf,
                onValueChange = { viewModel.updateChangePhotoCpf(it) },
                label = { Text("CPF*") },
                leadingIcon = { Icon(Icons.Default.Badge, null) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                visualTransformation = cpfVisualTransformation,
                isError = viewModel.changePhotoCpfError != null,
                supportingText = {
                    if (viewModel.changePhotoCpfError != null) {
                        Text(
                            text = viewModel.changePhotoCpfError!!,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                enabled = !viewModel.isSearchingCpf
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.searchParticipantByCpf()
                    },
                    enabled = isBuscarEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (viewModel.isSearchingCpf) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("BUSCANDO...", fontWeight = FontWeight.Bold)
                    } else {
                        Text("BUSCAR", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
