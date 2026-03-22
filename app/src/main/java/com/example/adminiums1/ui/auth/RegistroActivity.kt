// app/src/main/java/com/example/adminiums1/ui/auth/RegistroActivity.kt
package com.example.adminiums1.ui.auth

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.adminiums1.databinding.ActivityRegistroBinding
import com.example.adminiums1.model.Usuario
import com.example.adminiums1.repository.FirebaseRepository
import com.example.adminiums1.utils.ErrorHandler
import com.example.adminiums1.utils.estaVacio
import com.example.adminiums1.utils.mostrar
import com.example.adminiums1.utils.ocultar
import com.example.adminiums1.utils.valor
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Parte 1 — Registro de Residente y Vigilante.
 * Parte 5 — Usa ViewBinding (reemplaza findViewById) y ErrorHandler centralizado.
 */
class RegistroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroBinding
    private val repo = FirebaseRepository()
    private val auth = FirebaseAuth.getInstance()
    private var rol: String = "residente"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        rol = intent.getStringExtra("rol") ?: "residente"

        binding.btnBack.setOnClickListener { finish() }
        binding.tvIrLogin.setOnClickListener { finish() }

        binding.btnRegistrar.setOnClickListener { intentarRegistro() }
    }

    private fun intentarRegistro() {
        val nombre   = binding.etNombre.valor()
        val unidad   = binding.etUnidad.valor()
        val email    = binding.etEmail.valor()
        val password = binding.etPassword.valor()
        val confirm  = binding.etConfirmPassword.valor()

        // Validaciones
        if (nombre.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }
        if (password != confirm) {
            binding.etConfirmPassword.error = "Las contraseñas no coinciden"
            return
        }
        if (password.length < 6) {
            binding.etPassword.error = "Mínimo 6 caracteres"
            return
        }

        binding.progressBar.mostrar()
        binding.btnRegistrar.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: throw Exception("UID nulo")

                val usuario = Usuario(
                    uid              = uid,
                    nombre           = nombre,
                    email            = email,
                    rol              = rol,
                    unidad           = if (rol == "residente") unidad else "",
                    balance          = if (rol == "residente") 1250.0 else 0.0,
                    proximoPago      = if (rol == "residente") 450.0  else 0.0,
                    fechaVencimiento = if (rol == "residente") "15 Abr 2026" else ""
                )
                repo.crearUsuario(usuario)

                withContext(Dispatchers.Main) {
                    binding.progressBar.ocultar()
                    Toast.makeText(
                        this@RegistroActivity,
                        "¡Registro exitoso! Inicia sesión.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.ocultar()
                    binding.btnRegistrar.isEnabled = true
                    ErrorHandler.mostrar(this@RegistroActivity, e, "RegistroActivity")
                }
            }
        }
    }
}
