package com.example.adminiums1.ui.admin

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.adminiums1.R
import com.example.adminiums1.databinding.ActivityConfiguracionEdificioBinding
import com.example.adminiums1.model.Condominio
import com.example.adminiums1.repository.FirebaseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConfiguracionEdificioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfiguracionEdificioBinding
    private val repo = FirebaseRepository()
    private var edificioId: String = ""
    private var condominioActual: Condominio? = null

    companion object {
        const val CUOTA_DEFAULT = 3000.0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfiguracionEdificioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = ContextCompat.getColor(this, R.color.colorPrimaryDark)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }

        edificioId = intent.getStringExtra("edificioId") ?: ""

        if (edificioId.isEmpty()) {
            Toast.makeText(this, "Error: No se proporcionó ID de edificio", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Set default hint
        binding.etCuotaBase.hint = CUOTA_DEFAULT.toString()

        cargarDatos()

        binding.btnGuardarConfig.setOnClickListener {
            guardarCambios()
        }
    }

    private fun cargarDatos() {
        CoroutineScope(Dispatchers.IO).launch {
            val cond = repo.getCondominio(edificioId)
            withContext(Dispatchers.Main) {
                if (cond != null) {
                    condominioActual = cond
                    binding.etNombreEdificio.setText(cond.nombre)
                    binding.etDireccion.setText(cond.direccion)
                    binding.etCiudad.setText(cond.ciudad)
                    
                    // FIX 1: Show "3000.0" as hint/text if cuotaBase is 0.0
                    binding.etCuotaBase.setText(if (cond.cuotaBase == 0.0) "3000.0" else cond.cuotaBase.toString())
                    
                    binding.etTotalUnidades.setText(cond.totalUnidades.toString())
                    binding.etDatosBancarios.setText(cond.datosBancarios)
                } else {
                    Toast.makeText(this@ConfiguracionEdificioActivity, "No se pudo cargar la información", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun guardarCambios() {
        val nombre = binding.etNombreEdificio.text.toString().trim()
        val direccion = binding.etDireccion.text.toString().trim()
        val ciudad = binding.etCiudad.text.toString().trim()
        
        // Use CUOTA_DEFAULT as default if input is empty or invalid
        val cuotaBase = binding.etCuotaBase.text.toString().toDoubleOrNull() ?: CUOTA_DEFAULT

        val totalUnidades = binding.etTotalUnidades.text.toString().toIntOrNull() ?: 0
        val datosBancarios = binding.etDatosBancarios.text.toString().trim()

        if (nombre.isEmpty()) {
            binding.etNombreEdificio.error = "Requerido"
            return
        }

        val nuevoCondominio = condominioActual?.copy(
            nombre = nombre,
            direccion = direccion,
            ciudad = ciudad,
            cuotaBase = cuotaBase,
            totalUnidades = totalUnidades,
            datosBancarios = datosBancarios
        ) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val exito = repo.actualizarCondominio(nuevoCondominio)
            withContext(Dispatchers.Main) {
                if (exito) {
                    Toast.makeText(this@ConfiguracionEdificioActivity, "Configuración actualizada correctamente", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@ConfiguracionEdificioActivity, "Error al actualizar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
