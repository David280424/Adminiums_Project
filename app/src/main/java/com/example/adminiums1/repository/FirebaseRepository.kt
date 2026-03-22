package com.example.adminiums1.repository

import com.example.adminiums1.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    // ── Auth ────────────────────────────────────────────────────────────────

    suspend fun login(email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user?.uid ?: "")
        } catch (e: Exception) { Result.failure(e) }
    }

    fun logout() = auth.signOut()

    fun getCurrentUid() = auth.currentUser?.uid

    // ── Usuarios ────────────────────────────────────────────────────────────

    suspend fun getUsuario(uid: String): Usuario? = withContext(Dispatchers.IO) {
        try {
            db.collection("usuarios").document(uid).get().await().toObject(Usuario::class.java)
        } catch (e: Exception) { null }
    }

    suspend fun crearUsuario(usuario: Usuario): Boolean = withContext(Dispatchers.IO) {
        try {
            db.collection("usuarios").document(usuario.uid).set(usuario).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun getTodosUsuarios(): List<Usuario> = withContext(Dispatchers.IO) {
        try {
            db.collection("usuarios").whereEqualTo("rol", "residente")
                .get().await().toObjects(Usuario::class.java)
        } catch (e: Exception) { emptyList() }
    }

    // ── Visitantes ──────────────────────────────────────────────────────────

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

    // ── Reservaciones ───────────────────────────────────────────────────────

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
            db.collection("reservaciones").get().await().toObjects(Reservacion::class.java)
        } catch (e: Exception) { emptyList() }
    }

    // ── Pagos ───────────────────────────────────────────────────────────────

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

    suspend fun getTotalRecaudado(): Double = withContext(Dispatchers.IO) {
        try {
            db.collection("pagos").whereEqualTo("estado", "pagado")
                .get().await().toObjects(Pago::class.java).sumOf { it.monto }
        } catch (e: Exception) { 0.0 }
    }

    // ── Registros de acceso (NUEVO) ─────────────────────────────────────────

    /**
     * Guarda un registro de entrada en Firestore.
     * Colección: "accesos"
     * Usado tanto por el flujo manual como por el escaneo de QR.
     */
    suspend fun registrarAcceso(acceso: RegistroAcceso): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = db.collection("accesos").document()
            db.collection("accesos").document(docRef.id)
                .set(acceso.copy(id = docRef.id)).await()
            true
        } catch (e: Exception) { false }
    }

    /**
     * Devuelve todos los registros de acceso de la fecha indicada.
     * Formato de fecha esperado: "dd/MM/yyyy"
     */
    suspend fun getAccesosPorFecha(fecha: String): List<RegistroAcceso> = withContext(Dispatchers.IO) {
        try {
            db.collection("accesos")
                .whereEqualTo("fecha", fecha)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get().await()
                .toObjects(RegistroAcceso::class.java)
        } catch (e: Exception) { emptyList() }
    }
}
