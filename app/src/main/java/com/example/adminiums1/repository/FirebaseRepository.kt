package com.example.adminiums1.repository

import com.example.adminiums1.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

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

    suspend fun registrarAuth(email: String, pass: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
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

    suspend fun getCondominio(id: String): Condominio? = withContext(Dispatchers.IO) {
        try {
            db.collection("condominios").document(id).get().await().toObject(Condominio::class.java)
        } catch (e: Exception) { null }
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

    suspend fun setUserOnlineStatus(uid: String, isOnline: Boolean) = withContext(Dispatchers.IO) {
        try {
            db.collection("usuarios").document(uid).update("isOnline", isOnline).await()
        } catch (e: Exception) { }
    }

    // ── Finanzas (Pagos y Recargas) ─────────────────────────────────────────

    suspend fun registrarPago(pago: Pago): Boolean = withContext(Dispatchers.IO) {
        try {
            val user = getUsuario(pago.residenteUid)
            val building = getCondominio(pago.edificioId)
            
            val docRef = db.collection("pagos").document()
            val folioGen = "ADM-" + System.currentTimeMillis().toString().takeLast(8)
            
            val finalPago = pago.copy(
                id = docRef.id, 
                residenteNombre = user?.nombre ?: "",
                unidad = user?.unidad ?: "",
                edificioNombre = building?.nombre ?: "",
                folio = folioGen
            )
            
            db.collection("pagos").document(docRef.id).set(finalPago).await()
            
            // Si el pago es aprobado, actualizar balance
            if (finalPago.estado == "Aprobado") {
                db.collection("usuarios").document(pago.residenteUid)
                    .update("balance", com.google.firebase.firestore.FieldValue.increment(-pago.monto))
                    .await()
            }
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

    suspend fun getPagosPorEdificio(edificioId: String): List<Pago> = withContext(Dispatchers.IO) {
        try {
            db.collection("pagos")
                .whereEqualTo("edificioId", edificioId)
                .orderBy("fecha", Query.Direction.DESCENDING)
                .get().await().toObjects(Pago::class.java)
        } catch (e: Exception) { emptyList() }
    }

    // ── Incidencias ─────────────────────────────────────────────────────────

    suspend fun reportarIncidencia(incidencia: Incidencia): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = db.collection("incidencias").document()
            db.collection("incidencias").document(docRef.id).set(incidencia.copy(id = docRef.id)).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun getIncidenciasEdificio(edificioId: String): List<Incidencia> = withContext(Dispatchers.IO) {
        try {
            db.collection("incidencias")
                .whereEqualTo("edificioId", edificioId)
                .orderBy("fecha", Query.Direction.DESCENDING)
                .get().await().toObjects(Incidencia::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getIncidenciasPorEstado(edificioId: String, estado: String): List<Incidencia> = withContext(Dispatchers.IO) {
        try {
            db.collection("incidencias")
                .whereEqualTo("edificioId", edificioId)
                .whereEqualTo("estado", estado)
                .orderBy("fecha", Query.Direction.DESCENDING)
                .get().await().toObjects(Incidencia::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getMisIncidencias(uid: String): List<Incidencia> = withContext(Dispatchers.IO) {
        try {
            db.collection("incidencias")
                .whereEqualTo("residenteUid", uid)
                .orderBy("fecha", Query.Direction.DESCENDING)
                .get().await().toObjects(Incidencia::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun actualizarEstadoIncidencia(id: String, estado: String, respuesta: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val updateData = mutableMapOf<String, Any>(
                "estado" to estado,
                "respuestaAdmin" to respuesta
            )
            if (estado == "Resuelta") {
                updateData["fechaResolucion"] = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            }
            db.collection("incidencias").document(id).update(updateData).await()
            true
        } catch (e: Exception) { false }
    }

    // ── Limpieza ────────────────────────────────────────────────────────────

    suspend fun crearTareaLimpieza(tarea: TareaLimpieza): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = db.collection("tareas_limpieza").document()
            db.collection("tareas_limpieza").document(docRef.id).set(tarea.copy(id = docRef.id)).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun getTareasLimpieza(edificioId: String): List<TareaLimpieza> = withContext(Dispatchers.IO) {
        try {
            db.collection("tareas_limpieza")
                .whereEqualTo("edificioId", edificioId)
                .get().await().toObjects(TareaLimpieza::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getTareasLimpiezaActivas(edificioId: String): List<TareaLimpieza> = withContext(Dispatchers.IO) {
        try {
            db.collection("tareas_limpieza")
                .whereEqualTo("edificioId", edificioId)
                .whereEqualTo("completada", false)
                .get().await().toObjects(TareaLimpieza::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun marcarTareaCompletada(id: String, notas: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            db.collection("tareas_limpieza").document(id)
                .update("completada", true, "fechaCompletada", fecha, "notas", notas).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun eliminarTareaLimpieza(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            db.collection("tareas_limpieza").document(id).delete().await()
            true
        } catch (e: Exception) { false }
    }

    // ── Operaciones (Compatibilidad con funciones antiguas de Vigilante y Reservas) ──

    suspend fun crearReservacion(reservacion: Reservacion): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = db.collection("reservaciones").document()
            db.collection("reservaciones").document(docRef.id).set(reservacion.copy(id = docRef.id)).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun getReservacionesPorEdificio(edificioId: String): List<Reservacion> = withContext(Dispatchers.IO) {
        try {
            db.collection("reservaciones")
                .whereEqualTo("edificioId", edificioId)
                .get().await().toObjects(Reservacion::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun eliminarReservacion(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            db.collection("reservaciones").document(id).delete().await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun registrarAcceso(acceso: RegistroAcceso): Boolean = withContext(Dispatchers.IO) {
        try {
            db.collection("accesos").add(acceso).await()
            true
        } catch (e: Exception) { false }
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

    suspend fun getAccesosPorFecha(fecha: String): List<RegistroAcceso> = withContext(Dispatchers.IO) {
        try {
            db.collection("accesos")
                .whereEqualTo("fecha", fecha)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get().await().toObjects(RegistroAcceso::class.java)
        } catch (e: Exception) { emptyList() }
    }
}
