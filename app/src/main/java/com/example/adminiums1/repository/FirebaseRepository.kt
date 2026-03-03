package com.example.adminiums1.repository

import com.example.adminiums1.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    suspend fun login(email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user?.uid ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() = auth.signOut()

    fun getCurrentUid() = auth.currentUser?.uid

    suspend fun getUsuario(uid: String): Usuario? = withContext(Dispatchers.IO) {
        try {
            val doc = db.collection("usuarios").document(uid).get().await()
            doc.toObject(Usuario::class.java)
        } catch (e: Exception) { null }
    }

    suspend fun crearUsuario(usuario: Usuario): Boolean = withContext(Dispatchers.IO) {
        try {
            db.collection("usuarios").document(usuario.uid).set(usuario).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun crearVisitante(visitante: Visitante): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = db.collection("visitantes").document()
            db.collection("visitantes").document(docRef.id)
                .set(visitante.copy(id = docRef.id)).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun validarVisitante(qrCode: String): Visitante? = withContext(Dispatchers.IO) {
        try {
            val query = db.collection("visitantes")
                .whereEqualTo("qrCode", qrCode)
                .whereEqualTo("validado", false)
                .get().await()
            val doc = query.documents.firstOrNull()
            if (doc != null) {
                db.collection("visitantes").document(doc.id)
                    .update("validado", true).await()
                doc.toObject(Visitante::class.java)
            } else null
        } catch (e: Exception) { null }
    }

    suspend fun crearReservacion(reservacion: Reservacion): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = db.collection("reservaciones").document()
            db.collection("reservaciones").document(docRef.id)
                .set(reservacion.copy(id = docRef.id)).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun getReservaciones(): List<Reservacion> = withContext(Dispatchers.IO) {
        try {
            val query = db.collection("reservaciones").get().await()
            query.toObjects(Reservacion::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun registrarPago(pago: Pago): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = db.collection("pagos").document()
            db.collection("pagos").document(docRef.id)
                .set(pago.copy(id = docRef.id)).await()
            val uid = getCurrentUid() ?: return@withContext false
            db.collection("usuarios").document(uid)
                .update("balance", com.google.firebase.firestore.FieldValue.increment(-pago.monto))
                .await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun getTodosUsuarios(): List<Usuario> = withContext(Dispatchers.IO) {
        try {
            val q = db.collection("usuarios").whereEqualTo("rol", "residente").get().await()
            q.toObjects(Usuario::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getTotalRecaudado(): Double = withContext(Dispatchers.IO) {
        try {
            val q = db.collection("pagos").whereEqualTo("estado", "pagado").get().await()
            q.toObjects(Pago::class.java).sumOf { it.monto }
        } catch (e: Exception) { 0.0 }
    }
}