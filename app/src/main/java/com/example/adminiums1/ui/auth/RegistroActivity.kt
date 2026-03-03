package com.example.adminiums1.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.adminiums1.R
import com.example.adminiums1.model.Usuario
import com.example.adminiums1.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RegistroActivity : AppCompatActivity() {

    private val repo = FirebaseRepository()
    private val auth = FirebaseAuth.getInstance()
    private var rol: String = "residente"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        rol = intent.getStringExtra("rol") ?: "residente"

        val btnBack = findViewById<Button>(R.id.btnBack)
        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etUnidad = findViewById<EditText>(R.id.etUnidad)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirm = findViewById<EditText>(R.id.etConfirmPassword)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)
        val tvIrLogin = findViewById<TextView>(R.id.tvIrLogin)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        btnBack.setOnClickListener { finish() }
        tvIrLogin.setOnClickListener { finish() }

        btnRegistrar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val unidad = etUnidad.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirm = etConfirm.text.toString().trim()

            if (nombre.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != confirm) {
                etConfirm.error = "Las contraseñas no coinciden"
                return@setOnClickListener
            }
            if (password.length < 6) {
                etPassword.error = "Mínimo 6 caracteres"
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnRegistrar.isEnabled = false

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val result = auth.createUserWithEmailAndPassword(email, password).await()
                    val uid = result.user?.uid ?: throw Exception("UID nulo")

                    val usuario = Usuario(
                        uid = uid,
                        nombre = nombre,
                        email = email,
                        rol = rol,
                        unidad = if (rol == "residente") unidad else "",
                        balance = if (rol == "residente") 1250.0 else 0.0,
                        proximoPago = if (rol == "residente") 450.0 else 0.0,
                        fechaVencimiento = if (rol == "residente") "15 Mar 2026" else ""
                    )

                    repo.crearUsuario(usuario)

                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        Toast.makeText(
                            this@RegistroActivity,
                            "¡Registro exitoso! Inicia sesión.",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        btnRegistrar.isEnabled = true
                        val msg = when {
                            e.message?.contains("email address is already") == true ->
                                "Este correo ya está registrado"
                            e.message?.contains("badly formatted") == true ->
                                "Correo inválido"
                            else -> "Error: ${e.message}"
                        }
                        Toast.makeText(this@RegistroActivity, msg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}