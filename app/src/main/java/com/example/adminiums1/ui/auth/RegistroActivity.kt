package com.example.adminiums1.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.adminiums1.databinding.ActivityRegistroBinding
import com.example.adminiums1.model.Usuario
import com.example.adminiums1.model.Condominio
import com.example.adminiums1.repository.FirebaseRepository
import com.example.adminiums1.ui.residente.ResidenteActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegistroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroBinding
    private val repo = FirebaseRepository()
    
    private var listaCondominios: List<Condominio> = emptyList()
    private var edificioSeleccionadoId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cargarCondominios()

        binding.btnRegistrar.setOnClickListener {
            registrarUsuario()
        }

        binding.tvIrLogin.setOnClickListener {
            finish()
        }
        
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun cargarCondominios() {
        CoroutineScope(Dispatchers.IO).launch {
            val condominios = repo.getCondominios()
            withContext(Dispatchers.Main) {
                listaCondominios = condominios
                // Para evitar nombres duplicados que confundan, mostramos "Nombre - Ciudad"
                val nombres = condominios.map { "${it.nombre} (${it.ciudad})" }
                val adapter = ArrayAdapter(this@RegistroActivity, android.R.layout.simple_dropdown_item_1line, nombres)
                binding.spinnerEdificios.setAdapter(adapter)
                
                binding.spinnerEdificios.setOnItemClickListener { _, _, position, _ ->
                    edificioSeleccionadoId = listaCondominios[position].id
                }
            }
        }
    }

    private fun registrarUsuario() {
        val nombre = binding.etNombre.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val pass = binding.etPassword.text.toString().trim()
        val confirmPass = binding.etConfirmPassword.text.toString().trim()
        val unidad = binding.etUnidad.text.toString().trim()

        if (edificioSeleccionadoId.isEmpty()) {
            Toast.makeText(this, "Selecciona un edificio", Toast.LENGTH_SHORT).show()
            return
        }
        if (nombre.isEmpty() || email.isEmpty() || pass.isEmpty() || unidad.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }
        if (pass != confirmPass) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnRegistrar.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            val authResult = repo.registrarAuth(email, pass)
            
            withContext(Dispatchers.Main) {
                if (authResult.isSuccess) {
                    val uid = authResult.getOrNull()!!
                    val nuevoUsuario = Usuario(
                        uid = uid,
                        nombre = nombre,
                        email = email,
                        rol = "residente", // Por defecto siempre es residente
                        unidad = unidad,
                        edificioId = edificioSeleccionadoId,
                        balance = 0.0
                    )
                    
                    val firestoreResult = repo.crearUsuario(nuevoUsuario)
                    binding.progressBar.visibility = View.GONE
                    
                    if (firestoreResult) {
                        Toast.makeText(this@RegistroActivity, "Registro exitoso", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@RegistroActivity, ResidenteActivity::class.java))
                        finishAffinity()
                    } else {
                        binding.btnRegistrar.isEnabled = true
                        Toast.makeText(this@RegistroActivity, "Error al guardar datos", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    binding.progressBar.visibility = View.GONE
                    binding.btnRegistrar.isEnabled = true
                    Toast.makeText(this@RegistroActivity, "Error: ${authResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
