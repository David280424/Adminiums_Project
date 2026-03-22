// app/src/main/java/com/example/adminiums1/AdminiumsApplication.kt
package com.example.adminiums1

import android.app.Application
import com.example.adminiums1.utils.FirestoreOptimizer
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Parte 5 — Clase Application: inicializa Firestore con caché offline al arrancar la app.
 *
 * IMPORTANTE: Registrar en AndroidManifest.xml dentro de <application>:
 *   android:name=".AdminiumsApplication"
 */
class AdminiumsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        FirestoreOptimizer.configurarCacheOffline(FirebaseFirestore.getInstance())
    }
}
