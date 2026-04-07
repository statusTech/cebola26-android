package com.oitickets.cebola26.data.worker

import android.content.Context
import android.net.Uri
import android.util.Log
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
        val oldParticipantId = inputData.getString("old_participant_id")

        val participant = gson.fromJson(participantJson, Participant::class.java)

        return try {
            // 0. Se for troca de foto, exclui registro antigo primeiro
            if (!oldParticipantId.isNullOrBlank()) {
                try {
                    storage.reference.child("faces/${oldParticipantId}.jpg").delete().await()
                } catch (e: Exception) {
                    Log.w("UploadWorker", "Foto antiga não encontrada no Storage (pode já ter sido removida): ${e.message}")
                }
                db.collection("participants").document(oldParticipantId).delete().await()
            }

            var downloadUrl = ""
            var file: File? = null

            // 1. Se tiver caminho de imagem local, tenta fazer o upload
            if (!localImagePath.isNullOrBlank()) {
                file = File(localImagePath)

                if (file.exists()) {
                    val storageRef = storage.reference.child("faces/${participant.id}.jpg")

                    // Upload do arquivo local
                    storageRef.putFile(Uri.fromFile(file)).await()

                    // Pega a URL pública
                    downloadUrl = storageRef.downloadUrl.await().toString()
                } else {
                    try {
                        val storageRef = storage.reference.child("faces/${participant.id}.jpg")
                        downloadUrl = storageRef.downloadUrl.await().toString()
                        Log.d("UploadWorker", "Arquivo local ausente, mas recuperado do Storage: $downloadUrl")
                    } catch (e: Exception) {
                        Log.w("UploadWorker", "Arquivo local perdido e não encontrado no Storage.")
                    }
                }
            }


            val finalParticipant = participant.copy(photoUrl = downloadUrl)

            db.collection("participants")
                .document(participant.id)
                .set(finalParticipant)
                .await()

            if (file != null && file.exists()) {
                file.delete()
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}