package com.example.adminiums1.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.adminiums1.databinding.ActivityLoginBinding
import com.example.adminiums1.repository.FirebaseRepository
import com.example.adminiums1.ui.admin.AdminActivity
import com.example.adminiums1.ui.residente.ResidenteActivity
import com.example.adminiums1.ui.vigilante.VigilanteActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val repo = FirebaseRepository()
    private var rol: String = "residente"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        rol = intent.getStringExtra("rol") ?: "residente"
        val rolNombre = when (rol) {
            "residente" -> "Residente"
            "vigilante" -> "Vigilante"
            "admin" -> "Administrador"
            else -> "Residente"
        }
        binding.tvRolTitle.text = rolNombre
        binding.tvSubtitle.text = "Inicio de Sesión - $rolNombre"

        binding.btnCambiarRol.setOnClickListener { finish() }

        // Ocultar botón de registro para admin (acceso privilegiado)
        if (rol == "admin") {
            binding.tvIrRegistro.visibility = View.GONE
        } else {
            binding.tvIrRegistro.setOnClickListener {
                startActivity(Intent(this, RegistroActivity::class.java).apply {
                    putExtra("rol", rol)
                })
            }
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.progressBar.visibility = View.VISIBLE
            binding.btnLogin.isEnabled = false

            CoroutineScope(Dispatchers.Main).launch {
                val result = repo.login(email, password)
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true

                if (result.isSuccess) {
                    val uid = result.getOrNull() ?: ""
                    val usuario = repo.getUsuario(uid)
                    if (usuario == null) {
                        Toast.makeText(this@LoginActivity, "Usuario no encontrado en BD", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    if (usuario.rol != rol) {
                        Toast.makeText(this@LoginActivity, "Rol incorrecto para este acceso", Toast.LENGTH_SHORT).show()
                        repo.logout()
                        return@launch
                    }
                    val intent = when (rol) {
                        "residente" -> Intent(this@LoginActivity, ResidenteActivity::class.java)
                        "vigilante" -> Intent(this@LoginActivity, VigilanteActivity::class.java)
                        "admin" -> Intent(this@LoginActivity, AdminActivity::class.java)
                        else -> Intent(this@LoginActivity, ResidenteActivity::class.java)
                    }
                    startActivity(intent)
                    finishAffinity()
                } else {
                    Toast.makeText(this@LoginActivity, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
