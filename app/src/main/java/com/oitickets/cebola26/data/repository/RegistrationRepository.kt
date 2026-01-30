package com.oitickets.cebola26.data.repository

import android.graphics.Bitmap
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.oitickets.cebola26.data.model.Participant
import com.oitickets.cebola26.data.model.RegistrationRules
import com.oitickets.cebola26.data.model.Staff
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

class RegistrationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // --- REGRAS DE NEGÓCIO (FEATURE FLAGS) ---

    suspend fun getRegistrationRules(staffId: String?): RegistrationRules {
        return try {
            if (staffId != null) {
                val staffDoc = db.collection("staff_config").document(staffId).get().await()
                if (staffDoc.exists()) {
                    return staffDoc.toObject(RegistrationRules::class.java) ?: RegistrationRules()
                }
            }

            val globalDoc = db.collection("app_config").document("registration_rules").get().await()
            if (globalDoc.exists()) {
                globalDoc.toObject(RegistrationRules::class.java) ?: RegistrationRules()
            } else {
                RegistrationRules()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            RegistrationRules()
        }
    }

    // --- STAFF ---

    // NOVO: Busca staff pelo nome para evitar duplicidade
    suspend fun findStaffByName(name: String): Staff? {
        return try {
            val snapshot = db.collection("staff")
                .whereEqualTo("name", name)
                .limit(1)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                snapshot.documents[0].toObject(Staff::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveStaffLogin(staff: Staff): Result<Unit> {
        return try {
            // Usa .set com merge para atualizar apenas o lastLogin se o doc já existir,
            // ou criar tudo se for novo.
            db.collection("staff")
                .document(staff.id)
                .set(staff)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // --- PARTICIPANTES ---

    suspend fun checkCpfExists(cpf: String): Boolean {
        return try {
            val snapshot = db.collection("participants")
                .whereEqualTo("cpf", cpf)
                .limit(1)
                .get()
                .await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun saveParticipant(participant: Participant, photoBitmap: Bitmap?): Result<Unit> {
        return try {
            var downloadUrl = ""

            if (photoBitmap != null) {
                val storageRef = storage.reference.child("faces/${participant.id}.jpg")
                val baos = ByteArrayOutputStream()
                photoBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                val data = baos.toByteArray()

                storageRef.putBytes(data).await()
                downloadUrl = storageRef.downloadUrl.await().toString()
            }

            val finalParticipant = participant.copy(photoUrl = downloadUrl)

            db.collection("participants")
                .document(participant.id)
                .set(finalParticipant)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}