package com.oitickets.cebola26.data.model

data class RegistrationRules(
    val requireName: Boolean = true,
    val requireCpf: Boolean = true,
    val requirePhoto: Boolean = true,
    val requireQrCode: Boolean = false
)