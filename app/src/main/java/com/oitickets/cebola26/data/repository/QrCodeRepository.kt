package com.oitickets.cebola26.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class QrCodeRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun checkQrCodeExists(qrCode: String): Boolean {
        return try {
            val snapshot = db.collection("participants")
                .whereEqualTo("qrCode", qrCode)
                .limit(1)
                .get()
                .await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            e.printStackTrace()
            false // Em caso de erro (offline), não bloqueia
        }
    }
}