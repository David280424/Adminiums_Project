// app/src/main/java/com/example/adminiums1/utils/UiExtensions.kt
package com.example.adminiums1.utils

import android.view.View
import android.widget.EditText
import androidx.core.content.ContextCompat
import com.example.adminiums1.R
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout

// ── Visibilidad ────────────────────────────────────────────────────────────

/** Hace visible la view */
fun View.mostrar() { visibility = View.VISIBLE }

/** Oculta la view (sin ocupar espacio) */
fun View.ocultar() { visibility = View.GONE }

// ── Snackbar con colores ───────────────────────────────────────────────────

/** Snackbar rojo para errores */
fun View.snackbarError(mensaje: String) {
    Snackbar.make(this, mensaje, Snackbar.LENGTH_LONG)
        .setBackgroundTint(ContextCompat.getColor(context, R.color.colorRed))
        .setTextColor(ContextCompat.getColor(context, R.color.white))
        .show()
}

/** Snackbar verde para éxito */
fun View.snackbarExito(mensaje: String) {
    Snackbar.make(this, mensaje, Snackbar.LENGTH_SHORT)
        .setBackgroundTint(ContextCompat.getColor(context, R.color.colorGreen))
        .setTextColor(ContextCompat.getColor(context, R.color.white))
        .show()
}

/** Snackbar con botón de acción */
fun View.snackbarConAccion(mensaje: String, textoAccion: String, accion: () -> Unit) {
    Snackbar.make(this, mensaje, Snackbar.LENGTH_INDEFINITE)
        .setAction(textoAccion) { accion() }
        .show()
}

// ── Validación de TextInputLayout ─────────────────────────────────────────

/**
 * Valida el campo y muestra el error si no se cumple la condición.
 * Devuelve true si es válido.
 * Uso: tilEmail.validar("Campo requerido") { !etEmail.estaVacio() }
 */
fun TextInputLayout.validar(mensajeError: String, condicion: () -> Boolean): Boolean {
    return if (!condicion()) {
        error = mensajeError
        false
    } else {
        error = null
        true
    }
}

// ── Helpers de EditText ────────────────────────────────────────────────────

/** true si el campo está vacío (ignorando espacios) */
fun EditText.estaVacio() = text.toString().trim().isEmpty()

/** Devuelve el texto sin espacios al inicio/fin */
fun EditText.valor() = text.toString().trim()

// ── Formato de moneda ──────────────────────────────────────────────────────

/** Formatea un Double como peso mexicano: "$1,250.00" */
fun Double.formatearPeso(): String = "$${"%.2f".format(this)}"

/** Formatea un Double como porcentaje: "92.5%" */
fun Double.formatearPorcentaje(): String = "${"%.1f".format(this)}%"
