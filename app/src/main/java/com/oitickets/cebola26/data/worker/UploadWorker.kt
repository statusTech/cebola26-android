package com.oitickets.cebola26.data.worker

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.oitickets.cebola26.data.model.Participant
import kotlinx.coroutines.tasks.await
import java.io.File

class UploadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val gson = Gson()

    override suspend fun doWork(): Result {
        val participantJson = inputData.getString("participant_json") ?: return Result.failure()
        val localImagePath = inputData.getString("local_image_path")

        val participant = gson.fromJson(participantJson, Participant::class.java)

        return try {
            var downloadUrl = ""

            // 1. Se tiver caminho de imagem local, faz o upload
            if (!localImagePath.isNullOrBlank()) {
                val file = File(localImagePath)
                if (file.exists()) {
                    val storageRef = storage.reference.child("faces/${participant.id}.jpg")

                    // Upload do arquivo local
                    storageRef.putFile(Uri.fromFile(file)).await()
                    downloadUrl = storageRef.downloadUrl.await().toString()

                    // Deleta o arquivo local para economizar espaço após subir
                    file.delete()
                }
            }

            // 2. Atualiza o objeto com a URL da nuvem (se houver) ou mantém vazia
            val finalParticipant = participant.copy(photoUrl = downloadUrl)

            // 3. Salva no Firestore
            db.collection("participants")
                .document(participant.id)
                .set(finalParticipant)
                .await()

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // Se der erro (ex: internet caiu no meio), o WorkManager tenta de novo depois (Retry)
            Result.retry()
        }
    }
}