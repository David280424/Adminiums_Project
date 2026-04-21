package com.example.adminiums1.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.adminiums1.databinding.ActivityRegistroBinding
import com.example.adminiums1.model.Usuario
import com.example.adminiums1.model.Condominio
import com.example.adminiums1.repository.FirebaseRepository
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
    }

    private fun cargarCondominios() {
        CoroutineScope(Dispatchers.IO).launch {
            val condominios = repo.getCondominios()
            withContext(Dispatchers.Main) {
                listaCondominios = condominios
                val nombres = condominios.map { it.nombre }
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
        val unidad = binding.etUnidad.text.toString().trim()

        if (edificioSeleccionadoId.isEmpty()) {
            Toast.makeText(this, "Selecciona un edificio", Toast.LENGTH_SHORT).show()
            return
        }
        if (nombre.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        // Aquí iría la lógica de Auth y guardado en Firestore usando edificioSeleccionadoId
        Toast.makeText(this, "Registrando en $edificioSeleccionadoId...", Toast.LENGTH_SHORT).show()
    }
}
