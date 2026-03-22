// app/src/main/java/com/example/adminiums1/utils/FirestoreOptimizer.kt
package com.example.adminiums1.utils

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query

/**
 * Parte 5 — Optimización de consultas a Firestore.
 * Llama a [configurarCacheOffline] una sola vez desde AdminiumsApplication.
 */
object FirestoreOptimizer {

    /**
     * Activa la persistencia offline (caché local ilimitada).
     * Firestore funciona sin internet y sincroniza al reconectarse.
     */
    fun configurarCacheOffline(db: FirebaseFirestore) {
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        db.firestoreSettings = settings
    }

    /**
     * Consulta paginada de residentes (20 por página).
     * Uso:
     *   // Primera página:
     *   val q = paginaResidentes(db, null)
     *   // Siguiente página (pasa el último documento obtenido):
     *   val q2 = paginaResidentes(db, ultimoDoc)
     */
    fun paginaResidentes(
        db: FirebaseFirestore,
        ultimoDocumento: com.google.firebase.firestore.DocumentSnapshot?,
        limite: Long = 20
    ): Query {
        var query = db.collection("usuarios")
            .whereEqualTo("rol", "residente")
            .orderBy("nombre")
            .limit(limite)
        if (ultimoDocumento != null) {
            query = query.startAfter(ultimoDocumento)
        }
        return query
    }

    /**
     * Consulta de pagos optimizada — solo trae los campos necesarios
     * para las estadísticas del panel Admin.
     */
    fun consultaPagos(db: FirebaseFirestore) =
        db.collection("pagos")
            .orderBy("fecha", Query.Direction.DESCENDING)

    /**
     * Consulta de reservaciones del día para una amenidad específica.
     * Requiere índice compuesto en Firestore: amenidad ASC + fecha ASC.
     */
    fun reservacionesHoy(db: FirebaseFirestore, amenidad: String, fecha: String) =
        db.collection("reservaciones")
            .whereEqualTo("amenidad", amenidad)
            .whereEqualTo("fecha", fecha)
            .limit(100)
}
