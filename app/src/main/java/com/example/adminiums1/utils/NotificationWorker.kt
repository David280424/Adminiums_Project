package com.example.adminiums1.utils

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class NotificationWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val db = FirebaseFirestore.getInstance()
        return try {
            val pending = db.collection("notificaciones_pendientes")
                .whereEqualTo("procesado", false)
                .get()
                .await()

            if (pending.isEmpty) return Result.success()

            for (doc in pending.documents) {
                val token = doc.getString("token")
                val titulo = doc.getString("titulo")
                val mensaje = doc.getString("mensaje")

                if (token != null && titulo != null && mensaje != null) {
                    // Nota: Desde una app Android no podemos enviar FCM directamente de forma segura.
                    // Este worker simula el procesamiento que haría una Cloud Function.
                    // En un entorno real, este worker podría enviar notificaciones locales 
                    // si el Admin estuviera monitoreando la cola.
                    
                    Log.d("NotificationWorker", "Procesando notificación para: $token")
                    
                    db.collection("notificaciones_pendientes").document(doc.id)
                        .update("procesado", true, "fechaProcesado", System.currentTimeMillis())
                        .await()
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("NotificationWorker", "Error procesando notificaciones", e)
            Result.retry()
        }
    }
}
