package com.oitickets.cebola26.data.model

data class Participant(
    val id: String,
    val name: String,
    val cpf: String,
    val qrCode: String = "", // Campo Novo: QR Code do ingresso
    val photoUrl: String = "",
    val createdAt: String,
    val status: String = "pending",
    val registeredBy: String = ""
) {
    // Construtor vazio para o Firebase conseguir ler os dados de volta se necessário
    constructor() : this("", "", "", "", "", "", "", "")
}