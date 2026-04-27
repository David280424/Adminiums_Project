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
import com.example.adminiums1.ui.limpieza.LimpiezaActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val repo = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Botón para volver al selector de roles
        binding.btnCambiarRol.setOnClickListener {
            val intent = Intent(this, RolSelectorActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.tvIrRegistro.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
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

            CoroutineScope(Dispatchers.IO).launch {
                val result = repo.login(email, password)
                
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        val uid = result.getOrNull() ?: ""
                        checkUserRoleAndNavigate(uid)
                    } else {
                        binding.progressBar.visibility = View.GONE
                        binding.btnLogin.isEnabled = true
                        Toast.makeText(this@LoginActivity, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun checkUserRoleAndNavigate(uid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val usuario = repo.getUsuario(uid)
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                if (usuario != null) {
                    val intent = when (usuario.rol) {
                        "admin" -> Intent(this@LoginActivity, AdminActivity::class.java)
                        "residente" -> Intent(this@LoginActivity, ResidenteActivity::class.java)
                        "vigilante" -> Intent(this@LoginActivity, VigilanteActivity::class.java)
                        "limpieza" -> Intent(this@LoginActivity, LimpiezaActivity::class.java)
                        else -> {
                            Toast.makeText(this@LoginActivity, "Rol no reconocido", Toast.LENGTH_SHORT).show()
                            return@withContext
                        }
                    }
                    startActivity(intent)
                    finishAffinity()
                } else {
                    Toast.makeText(this@LoginActivity, "Error al obtener perfil", Toast.LENGTH_SHORT).show()
                    binding.btnLogin.isEnabled = true
                }
            }
        }
    }
}
