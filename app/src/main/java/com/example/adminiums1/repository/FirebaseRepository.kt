package com.example.adminiums1.repository

import com.example.adminiums1.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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
    fun getCurrentUid(): String? = auth.currentUser?.uid

    suspend fun getCondominios(): List<Condominio> = withContext(Dispatchers.IO) {
        try {
            db.collection("condominios").get().await()
                .toObjects(Condominio::class.java)
                .sortedBy { it.nombre }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun getCondominio(id: String): Condominio? = withContext(Dispatchers.IO) {
        try {
            db.collection("condominios").document(id).get().await().toObject(Condominio::class.java)
        } catch (_: Exception) { null }
    }

    // ── Usuarios & Residentes ───────────────────────────────────────────────

    suspend fun getUsuario(uid: String): Usuario? = withContext(Dispatchers.IO) {
        try {
            db.collection("usuarios").document(uid).get().await().toObject(Usuario::class.java)
        } catch (_: Exception) { null }
    }

    suspend fun crearUsuario(usuario: Usuario): Boolean = withContext(Dispatchers.IO) {
        try {
            db.collection("usuarios").document(usuario.uid).set(usuario).await()
            true
        } catch (_: Exception) { false }
    }

    suspend fun eliminarUsuario(uid: String): Boolean = withContext(Dispatchers.IO) {
        try {
            db.collection("usuarios").document(uid).delete().await()
            true
        } catch (_: Exception) { false }
    }

    suspend fun getTodosUsuarios(): List<Usuario> = withContext(Dispatchers.IO) {
        try {
            db.collection("usuarios").get().await()
                .toObjects(Usuario::class.java)
                .sortedBy { it.nombre }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun getResidentesPorEdificio(edificioId: String): List<Usuario> = withContext(Dispatchers.IO) {
        try {
            db.collection("usuarios")
                .whereEqualTo("edificioId", edificioId)
                .whereEqualTo("rol", "residente")
                .get().await()
                .toObjects(Usuario::class.java)
                .sortedBy { it.nombre }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun setUserOnlineStatus(uid: String, isOnline: Boolean): Unit = withContext(Dispatchers.IO) {
        try {
            db.collection("usuarios").document(uid).update("isOnline", isOnline).await()
        } catch (_: Exception) { }
    }

    /** Listener en tiempo real para la lista de residentes de un edificio */
    fun escucharResidentesPorEdificio(
        edificioId: String,
        onUpdate: (List<Usuario>) -> Unit
    ): ListenerRegistration {
        return db.collection("usuarios")
            .whereEqualTo("edificioId", edificioId)
            .whereEqualTo("rol", "residente")
            .addSnapshotListener { snapshot, _ ->
                val lista = snapshot?.toObjects(Usuario::class.java)
                    ?.sortedBy { it.nombre }
                    ?: emptyList()
                onUpdate(lista)
            }
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
                folio = folioGen,
                timestamp = System.currentTimeMillis()
            )

            db.collection("pagos").document(docRef.id).set(finalPago).await()

            if (finalPago.estado == "Aprobado") {
                db.collection("usuarios").document(pago.residenteUid)
                    .update("balance", com.google.firebase.firestore.FieldValue.increment(-pago.monto))
                    .await()
            }
            true
        } catch (_: Exception) { false }
    }

    suspend fun getHistorialPagosUsuario(uid: String): List<Pago> = withContext(Dispatchers.IO) {
        try {
            db.collection("pagos")
                .whereEqualTo("residenteUid", uid)
                .get().await()
                .toObjects(Pago::class.java)
                .sortedByDescending { it.timestamp }
        } catch (_: Exception) { emptyList() }
    }

    /** Listener en tiempo real para historial de pagos de un residente */
    fun escucharHistorialPagos(
        uid: String,
        onUpdate: (List<Pago>) -> Unit
    ): ListenerRegistration {
        return db.collection("pagos")
            .whereEqualTo("residenteUid", uid)
            .addSnapshotListener { snapshot, _ ->
                val lista = snapshot?.toObjects(Pago::class.java)
                    ?.sortedByDescending { it.timestamp }
                    ?: emptyList()
                onUpdate(lista)
            }
    }

    suspend fun getPagosPorEdificio(edificioId: String): List<Pago> = withContext(Dispatchers.IO) {
        try {
            db.collection("pagos")
                .whereEqualTo("edificioId", edificioId)
                .get().await()
                .toObjects(Pago::class.java)
                .sortedByDescending { it.timestamp }
        } catch (_: Exception) { emptyList() }
    }

    // ── Incidencias ─────────────────────────────────────────────────────────

    suspend fun reportarIncidencia(incidencia: Incidencia): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = db.collection("incidencias").document()
            db.collection("incidencias").document(docRef.id).set(
                incidencia.copy(
                    id = docRef.id,
                    timestamp = System.currentTimeMillis()
                )
            ).await()
            true
        } catch (_: Exception) { false }
    }

    suspend fun getIncidenciasEdificio(edificioId: String): List<Incidencia> = withContext(Dispatchers.IO) {
        try {
            db.collection("incidencias")
                .whereEqualTo("edificioId", edificioId)
                .get().await()
                .toObjects(Incidencia::class.java)
                .sortedByDescending { it.timestamp }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun getIncidenciasPorEstado(edificioId: String, estado: String): List<Incidencia> = withContext(Dispatchers.IO) {
        try {
            db.collection("incidencias")
                .whereEqualTo("edificioId", edificioId)
                .whereEqualTo("estado", estado)
                .get().await()
                .toObjects(Incidencia::class.java)
                .sortedByDescending { it.timestamp }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun getMisIncidencias(uid: String): List<Incidencia> = withContext(Dispatchers.IO) {
        try {
            db.collection("incidencias")
                .whereEqualTo("residenteUid", uid)
                .get().await()
                .toObjects(Incidencia::class.java)
                .sortedByDescending { it.timestamp }
        } catch (_: Exception) { emptyList() }
    }

    /** Listener en tiempo real para incidencias del admin por edificio */
    fun escucharIncidenciasEdificio(
        edificioId: String,
        onUpdate: (List<Incidencia>) -> Unit
    ): ListenerRegistration {
        return db.collection("incidencias")
            .whereEqualTo("edificioId", edificioId)
            .addSnapshotListener { snapshot, _ ->
                val lista = snapshot?.toObjects(Incidencia::class.java)
                    ?.sortedByDescending { it.timestamp }
                    ?: emptyList()
                onUpdate(lista)
            }
    }

    /** Listener en tiempo real para mis incidencias (residente) */
    fun escucharMisIncidencias(
        uid: String,
        onUpdate: (List<Incidencia>) -> Unit
    ): ListenerRegistration {
        return db.collection("incidencias")
            .whereEqualTo("residenteUid", uid)
            .addSnapshotListener { snapshot, _ ->
                val lista = snapshot?.toObjects(Incidencia::class.java)
                    ?.sortedByDescending { it.timestamp }
                    ?: emptyList()
                onUpdate(lista)
            }
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
        } catch (_: Exception) { false }
    }

    // ── Limpieza ────────────────────────────────────────────────────────────

    suspend fun crearTareaLimpieza(tarea: TareaLimpieza): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = db.collection("tareas_limpieza").document()
            db.collection("tareas_limpieza").document(docRef.id).set(
                tarea.copy(
                    id = docRef.id,
                    timestamp = System.currentTimeMillis()
                )
            ).await()
            true
        } catch (_: Exception) { false }
    }

    suspend fun getTareasLimpieza(edificioId: String): List<TareaLimpieza> = withContext(Dispatchers.IO) {
        try {
            db.collection("tareas_limpieza")
                .whereEqualTo("edificioId", edificioId)
                .get().await()
                .toObjects(TareaLimpieza::class.java)
                .sortedByDescending { it.timestamp }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun getTareasLimpiezaActivas(edificioId: String): List<TareaLimpieza> = withContext(Dispatchers.IO) {
        try {
            db.collection("tareas_limpieza")
                .whereEqualTo("edificioId", edificioId)
                .whereEqualTo("completada", false)
                .get().await()
                .toObjects(TareaLimpieza::class.java)
                .sortedByDescending { it.timestamp }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun marcarTareaCompletada(id: String, notas: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            db.collection("tareas_limpieza").document(id)
                .update("completada", true, "fechaCompletada", fecha, "notas", notas).await()
            true
        } catch (_: Exception) { false }
    }

    suspend fun eliminarTareaLimpieza(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            db.collection("tareas_limpieza").document(id).delete().await()
            true
        } catch (_: Exception) { false }
    }

    // ── Reservaciones ───────────────────────────────────────────────────────

    suspend fun crearReservacion(reservacion: Reservacion): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = db.collection("reservaciones").document()
            db.collection("reservaciones").document(docRef.id).set(
                reservacion.copy(
                    id = docRef.id,
                    timestamp = System.currentTimeMillis()
                )
            ).await()
            true
        } catch (_: Exception) { false }
    }

    suspend fun getReservacionesPorEdificio(edificioId: String): List<Reservacion> = withContext(Dispatchers.IO) {
        try {
            db.collection("reservaciones")
                .whereEqualTo("edificioId", edificioId)
                .get().await()
                .toObjects(Reservacion::class.java)
                .sortedByDescending { it.timestamp }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun eliminarReservacion(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            db.collection("reservaciones").document(id).delete().await()
            true
        } catch (_: Exception) { false }
    }

    // ── Accesos ─────────────────────────────────────────────────────────────

    suspend fun registrarAcceso(acceso: RegistroAcceso): Boolean = withContext(Dispatchers.IO) {
        try {
            db.collection("accesos").add(acceso).await()
            true
        } catch (_: Exception) { false }
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
        } catch (_: Exception) { null }
    }

    suspend fun getAccesosPorFecha(fecha: String): List<RegistroAcceso> = withContext(Dispatchers.IO) {
        try {
            db.collection("accesos")
                .whereEqualTo("fecha", fecha)
                .get().await()
                .toObjects(RegistroAcceso::class.java)
                .sortedByDescending { it.timestamp }
        } catch (_: Exception) { emptyList() }
    }

    /** Listener en tiempo real para historial de entradas filtrado por fecha */
    fun escucharAccesosPorFecha(
        fecha: String,
        onUpdate: (List<RegistroAcceso>) -> Unit
    ): ListenerRegistration {
        return db.collection("accesos")
            .whereEqualTo("fecha", fecha)
            .addSnapshotListener { snapshot, _ ->
                val lista = snapshot?.toObjects(RegistroAcceso::class.java)
                    ?.sortedByDescending { it.timestamp }
                    ?: emptyList()
                onUpdate(lista)
            }
    }

    // ── CRUD Unidades / Departamentos ───────────────────────────────────────

    suspend fun crearUnidad(unidad: Unidad): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = db.collection("unidades").document()
            db.collection("unidades").document(docRef.id).set(unidad.copy(id = docRef.id)).await()
            true
        } catch (_: Exception) { false }
    }

    suspend fun eliminarUnidad(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            db.collection("unidades").document(id).delete().await()
            true
        } catch (_: Exception) { false }
    }

    suspend fun actualizarUnidad(unidad: Unidad): Boolean = withContext(Dispatchers.IO) {
        try {
            db.collection("unidades").document(unidad.id).set(unidad).await()
            true
        } catch (_: Exception) { false }
    }

    /** Listener en tiempo real para unidades de un edificio */
    fun escucharUnidadesPorEdificio(
        edificioId: String,
        onUpdate: (List<Unidad>) -> Unit
    ): ListenerRegistration {
        return db.collection("unidades")
            .whereEqualTo("edificioId", edificioId)
            .addSnapshotListener { snapshot, _ ->
                val lista = snapshot?.toObjects(Unidad::class.java)
                    ?.sortedBy { it.numero }
                    ?: emptyList()
                onUpdate(lista)
            }
    }
}
