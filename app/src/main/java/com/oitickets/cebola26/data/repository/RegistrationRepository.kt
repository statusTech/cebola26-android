package com.oitickets.cebola26.data.repository

import android.content.Context
import android.graphics.Bitmap
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.oitickets.cebola26.data.model.Participant
import com.oitickets.cebola26.data.model.RegistrationRules
import com.oitickets.cebola26.data.model.Staff
import com.oitickets.cebola26.data.worker.UploadWorker
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class RegistrationRepository(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val gson = Gson()


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
            var localImagePath = ""

            // 1. Salva a imagem em arquivo local (Cache do app)
            if (photoBitmap != null) {
                val fileName = "temp_${participant.id}.jpg"
                val file = File(context.filesDir, fileName)
                FileOutputStream(file).use { out ->
                    photoBitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
                }
                localImagePath = file.absolutePath
            }

            val participantJson = gson.toJson(participant)

            val inputData = workDataOf(
                "participant_json" to participantJson,
                "local_image_path" to localImagePath
            )

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val uploadWork = OneTimeWorkRequestBuilder<UploadWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueue(uploadWork)

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}