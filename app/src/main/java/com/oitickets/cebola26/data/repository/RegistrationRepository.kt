package com.oitickets.cebola26.data.repository

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.oitickets.cebola26.data.model.Participant
import com.oitickets.cebola26.data.model.RegistrationRules
import com.oitickets.cebola26.data.model.Staff
import com.oitickets.cebola26.data.worker.UploadWorker
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream

class RegistrationRepository(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val gson = Gson()
    private val workManager = WorkManager.getInstance(context) // Instância do WorkManager

    // --- MONITORIZAÇÃO OFFLINE ---

    // Retorna LiveData para observar o status dos uploads em tempo real na tela de Pendências
    fun getPendingUploadsInfo(): LiveData<List<WorkInfo>> {
        return workManager.getWorkInfosByTagLiveData("upload_participant")
    }

    // --- REGRAS DE NEGÓCIO (FEATURE FLAGS) ---
    suspend fun getRegistrationRules(staffId: String?): RegistrationRules {
        return try {
            if (staffId != null) {
                val staffDoc = db.collection("staff_config").document(staffId).get().await()
                if (staffDoc.exists()) return staffDoc.toObject(RegistrationRules::class.java) ?: RegistrationRules()
            }
            val globalDoc = db.collection("app_config").document("registration_rules").get().await()
            if (globalDoc.exists()) globalDoc.toObject(RegistrationRules::class.java) ?: RegistrationRules() else RegistrationRules()
        } catch (e: Exception) {
            // Em caso de erro (sem internet), retorna o padrão para não bloquear o app
            RegistrationRules()
        }
    }

    // --- STAFF ---
    suspend fun findStaffByName(name: String): Staff? {
        return try {
            val snapshot = db.collection("staff").whereEqualTo("name", name).limit(1).get().await()
            if (!snapshot.isEmpty) snapshot.documents[0].toObject(Staff::class.java) else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveStaffLogin(staff: Staff): Result<Unit> {
        return try {
            db.collection("staff").document(staff.id).set(staff).await()
            Result.success(Unit)
        } catch (e: Exception) {
            // Se falhar login (offline), permitimos o acesso se a lógica local validar,
            // ou simplesmente retornamos sucesso para não travar o fluxo em campo.
            Result.success(Unit)
        }
    }

    // --- PARTICIPANTES ---

    suspend fun checkCpfExists(cpf: String): Boolean {
        return try {
            val snapshot = db.collection("participants").whereEqualTo("cpf", cpf).limit(1).get().await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            false // Se offline, permite passar. O backend tratará duplicatas depois.
        }
    }

    /**
     * SALVAR PARTICIPANTE (OFFLINE-FIRST)
     * 1. Salva a foto num ficheiro local temporário.
     * 2. Agenda um Worker para fazer o upload e salvar no Firestore quando houver rede.
     * 3. Retorna sucesso imediato para a UI.
     */
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
                .addTag("upload_participant")
                .build()

            workManager.enqueue(uploadWork)
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}