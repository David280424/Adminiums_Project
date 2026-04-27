package com.example.adminiums1.ui.residente

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.adminiums1.databinding.ActivityReportarIncidenciaBinding
import com.example.adminiums1.model.Incidencia
import com.example.adminiums1.repository.FirebaseRepository
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ReportarIncidenciaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReportarIncidenciaBinding
    private val repo = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportarIncidenciaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinners()
        
        binding.btnBack.setOnClickListener { finish() }
        binding.btnEnviarReporte.setOnClickListener { enviarReporte() }
    }

    private fun setupSpinners() {
        val categorias = listOf("Plomería", "Electricidad", "Ruido", "Seguridad", "Limpieza", "Otro")
        val ubicaciones = listOf("Unidad propia", "Pasillo", "Estacionamiento", "Jardín", "Elevador", "Cuarto de basura", "Otro")
        val prioridades = listOf("Baja", "Normal", "Alta", "Urgente")

        binding.spinnerCategoria.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categorias))
        binding.spinnerUbicacion.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, ubicaciones))
        binding.spinnerPrioridad.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, prioridades))
    }

    private fun enviarReporte() {
        val cat = binding.spinnerCategoria.text.toString()
        val ubi = binding.spinnerUbicacion.text.toString()
        val prio = binding.spinnerPrioridad.text.toString()
        val titulo = binding.etTitulo.text.toString().trim()
        val desc = binding.etDescripcion.text.toString().trim()

        var valid = true
        if (cat.isEmpty()) { binding.tilCategoria.error = "Selecciona una categoría"; valid = false } else binding.tilCategoria.error = null
        if (ubi.isEmpty()) { binding.tilUbicacion.error = "Selecciona una ubicación"; valid = false } else binding.tilUbicacion.error = null
        if (prio.isEmpty()) { binding.tilPrioridad.error = "Selecciona prioridad"; valid = false } else binding.tilPrioridad.error = null
        
        if (titulo.isEmpty()) { binding.tilTitulo.error = "El título es obligatorio"; valid = false } 
        else if (titulo.length > 80) { binding.tilTitulo.error = "Máximo 80 caracteres"; valid = false }
        else binding.tilTitulo.error = null

        if (desc.length < 20) { binding.tilDescripcion.error = "Describe al menos 20 caracteres"; valid = false }
        else if (desc.length > 500) { binding.tilDescripcion.error = "Máximo 500 caracteres"; valid = false }
        else binding.tilDescripcion.error = null

        if (!valid) return

        val uid = repo.getCurrentUid() ?: return
        
        binding.progressBar.visibility = View.VISIBLE
        binding.btnEnviarReporte.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            val user = repo.getUsuario(uid)
            val building = user?.edificioId?.let { repo.getCondominio(it) }
            val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            
            val incidencia = Incidencia(
                residenteUid = uid,
                residenteNombre = user?.nombre ?: "",
                unidad = user?.unidad ?: "",
                edificioId = user?.edificioId ?: "",
                edificioNombre = building?.nombre ?: "",
                categoria = cat,
                ubicacion = ubi,
                prioridad = prio,
                titulo = titulo,
                descripcion = desc,
                fecha = fecha
            )

            val exito = repo.reportarIncidencia(incidencia)
            
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                if (exito) {
                    Snackbar.make(binding.root, "Reporte enviado. El administrador lo revisará pronto", Snackbar.LENGTH_SHORT)
                        .setBackgroundTint(resources.getColor(android.R.color.holo_green_dark))
                        .show()
                    Handler(Looper.getMainLooper()).postDelayed({ finish() }, 1500)
                } else {
                    binding.btnEnviarReporte.isEnabled = true
                    Snackbar.make(binding.root, "Error al enviar reporte", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
}
