package com.oitickets.cebola26.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SuccessScreen() {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFE8F5E9)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(100.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Cadastro Realizado!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
        Spacer(modifier = Modifier.height(8.dp))
        Text("Preparando próximo cadastro...", fontSize = 14.sp, color = Color(0xFF2E7D32))
    }
}