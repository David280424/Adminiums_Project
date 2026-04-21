package com.example.adminiums1.repository

import com.example.adminiums1.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // ── Autenticación & Edificios ───────────────────────────────────────────

    suspend fun login(email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user?.uid ?: "")
        } catch (e: Exception) { Result.failure(e) }
    }

    fun logout() = auth.signOut()
    fun getCurrentUid() = auth.currentUser?.uid

    suspend fun getCondominios(): List<Condominio> = withContext(Dispatchers.IO) {
        try {
            db.collection("condominios").get().await().toObjects(Condominio::class.java)
        } catch (e: Exception) { emptyList() }
    }

    // ── Usuarios & Residentes ───────────────────────────────────────────────

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
            db.collection("usuarios").get().await().toObjects(Usuario::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getResidentesPorEdificio(edificioId: String): List<Usuario> = withContext(Dispatchers.IO) {
        try {
            db.collection("usuarios")
                .whereEqualTo("edificioId", edificioId)
                .whereEqualTo("rol", "residente")
                .get().await().toObjects(Usuario::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getVigilantesPorEdificio(edificioId: String): List<Usuario> = withContext(Dispatchers.IO) {
        try {
            db.collection("usuarios")
                .whereEqualTo("edificioId", edificioId)
                .whereEqualTo("rol", "vigilante")
                .get().await().toObjects(Usuario::class.java)
        } catch (e: Exception) { emptyList() }
    }

    // ── Finanzas (Pagos y Recargas) ─────────────────────────────────────────

    suspend fun registrarPago(pago: Pago): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = db.collection("pagos").document()
            val finalPago = pago.copy(id = docRef.id, estado = "Aprobado")
            db.collection("pagos").document(docRef.id).set(finalPago).await()
            
            db.collection("usuarios").document(pago.residenteUid)
                .update("balance", com.google.firebase.firestore.FieldValue.increment(-pago.monto))
                .await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun recargarBalance(uid: String, monto: Double, edificioId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Se registra como un movimiento de entrada
            val recarga = Pago(
                residenteUid = uid,
                edificioId = edificioId,
                monto = monto,
                fecha = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
                estado = "Recarga"
            )
            db.collection("pagos").add(recarga).await()
            
            db.collection("usuarios").document(uid)
                .update("balance", com.google.firebase.firestore.FieldValue.increment(monto))
                .await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun getHistorialPagosUsuario(uid: String): List<Pago> = withContext(Dispatchers.IO) {
        try {
            db.collection("pagos")
                .whereEqualTo("residenteUid", uid)
                .orderBy("fecha", Query.Direction.DESCENDING)
                .get().await().toObjects(Pago::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getTotalRecaudadoEdificio(edificioId: String): Double = withContext(Dispatchers.IO) {
        try {
            db.collection("pagos")
                .whereEqualTo("edificioId", edificioId)
                .whereEqualTo("estado", "Aprobado")
                .get().await().toObjects(Pago::class.java).sumOf { it.monto }
        } catch (e: Exception) { 0.0 }
    }

    // ── Operaciones (Accesos y Reservas) ───────────────────────────────────

    suspend fun getReservaciones(): List<Reservacion> = withContext(Dispatchers.IO) {
        try {
            db.collection("reservaciones").get().await().toObjects(Reservacion::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getReservacionesEdificio(edificioId: String): List<Reservacion> = withContext(Dispatchers.IO) {
        try {
            db.collection("reservaciones")
                .whereEqualTo("edificioId", edificioId)
                .get().await().toObjects(Reservacion::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun crearReservacion(reservacion: Reservacion): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = db.collection("reservaciones").document()
            val finalRes = reservacion.copy(id = docRef.id)
            db.collection("reservaciones").document(docRef.id).set(finalRes).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun registrarAcceso(acceso: RegistroAcceso): Boolean = withContext(Dispatchers.IO) {
        try {
            db.collection("accesos").add(acceso).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun getAccesosEdificio(edificioId: String, fecha: String): List<RegistroAcceso> = withContext(Dispatchers.IO) {
        try {
            db.collection("accesos")
                .whereEqualTo("edificioId", edificioId)
                .whereEqualTo("fecha", fecha)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get().await().toObjects(RegistroAcceso::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getAccesosPorFecha(fecha: String): List<RegistroAcceso> = withContext(Dispatchers.IO) {
        try {
            db.collection("accesos")
                .whereEqualTo("fecha", fecha)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get().await().toObjects(RegistroAcceso::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun validarVisitante(qrCode: String): Visitante? = withContext(Dispatchers.IO) {
        try {
            val snapshot = db.collection("visitantes")
                .whereEqualTo("qrCode", qrCode)
                .whereEqualTo("validado", false)
                .get().await()
            
            val doc = snapshot.documents.firstOrNull()
            if (doc != null) {
                val visitante = doc.toObject(Visitante::class.java)
                doc.reference.update("validado", true).await()
                visitante
            } else null
        } catch (e: Exception) { null }
    }
}
