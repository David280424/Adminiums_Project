// app/src/main/java/com/example/adminiums1/utils/ErrorHandler.kt
package com.example.adminiums1.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException

/**
 * Parte 5 — Manejo centralizado de errores Firebase.
 * Uso: ErrorHandler.mostrar(context, exception, "NombreActivity")
 */
object ErrorHandler {

    private const val TAG = "Adminiums"

    /** Devuelve un mensaje amigable en español según el tipo de error */
    fun mensajeAmigable(error: Exception): String = when (error) {

        // ── Sin conexión ────────────────────────────────────────────────────
        is FirebaseNetworkException ->
            "Sin conexión a internet. Verifica tu red e intenta de nuevo."

        // ── Firebase Auth ───────────────────────────────────────────────────
        is FirebaseAuthException -> when (error.errorCode) {
            "ERROR_WRONG_PASSWORD"        -> "Contraseña incorrecta."
            "ERROR_USER_NOT_FOUND"        -> "No existe una cuenta con ese correo."
            "ERROR_EMAIL_ALREADY_IN_USE"  -> "Este correo ya está registrado."
            "ERROR_WEAK_PASSWORD"         -> "La contraseña debe tener al menos 6 caracteres."
            "ERROR_INVALID_EMAIL"         -> "El formato del correo no es válido."
            "ERROR_USER_DISABLED"         -> "Esta cuenta fue deshabilitada. Contacta al administrador."
            "ERROR_TOO_MANY_REQUESTS"     -> "Demasiados intentos fallidos. Espera unos minutos."
            "ERROR_INVALID_CREDENTIAL"    -> "Correo o contraseña incorrectos."
            else -> "Error de autenticación (${error.errorCode})."
        }

        // ── Firestore ───────────────────────────────────────────────────────
        is FirebaseFirestoreException -> when (error.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED  -> "No tienes permiso para realizar esta acción."
            FirebaseFirestoreException.Code.NOT_FOUND          -> "El registro solicitado no existe."
            FirebaseFirestoreException.Code.UNAVAILABLE        -> "Servicio no disponible. Intenta más tarde."
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED  -> "Tiempo de espera agotado. Revisa tu conexión."
            FirebaseFirestoreException.Code.ALREADY_EXISTS     -> "Este registro ya existe."
            else -> "Error de base de datos (${error.code.name})."
        }

        // ── Genérico ────────────────────────────────────────────────────────
        else -> when {
            error.message?.contains("email address is already") == true ->
                "Este correo ya está registrado."
            error.message?.contains("badly formatted") == true ->
                "El formato del correo no es válido."
            error.message?.contains("INVALID_LOGIN_CREDENTIALS") == true ->
                "Correo o contraseña incorrectos."
            else -> error.message ?: "Ocurrió un error inesperado."
        }
    }

    /** Muestra el error como Toast y lo registra en Logcat */
    fun mostrar(context: Context, error: Exception, origen: String = "") {
        val mensaje = mensajeAmigable(error)
        Log.e(TAG, "[$origen] ${error.javaClass.simpleName}: ${error.message}", error)
        Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
    }

    /** Solo registra en Logcat, sin mostrar UI (para errores de listeners) */
    fun log(error: Exception, origen: String = "") {
        Log.e(TAG, "[$origen] ${error.javaClass.simpleName}: ${error.message}", error)
    }
}
