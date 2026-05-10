package com.example.adminiums1.ui.limpieza

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.adminiums1.databinding.ActivitySolicitarLimpiezaBinding
import com.example.adminiums1.model.TareaLimpieza
import com.example.adminiums1.model.Usuario
import com.example.adminiums1.repository.FirebaseRepository
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class SolicitarLimpiezaActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySolicitarLimpiezaBinding
    private val repo = FirebaseRepository()
    private var usuarioActual: Usuario? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySolicitarLimpiezaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinners()
        cargarUsuario()

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnEnviar.setOnClickListener { enviarSolicitud() }
    }

    private fun setupSpinners() {
        val areas = listOf("Lobby", "Pasillo", "Estacionamiento", "Jardín", "Elevador", "Cuarto de basura", "Gimnasio", "Alberca", "Otro")
        val tipos = listOf("Barrido", "Trapeado", "Desinfección", "Recolección basura", "Limpieza de vidrios", "General")
        val prioridades = listOf("Baja", "Normal", "Alta")

        binding.spinnerArea.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, areas)
        binding.spinnerTipo.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, tipos)
        binding.spinnerPrioridad.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, prioridades)
    }

    private fun cargarUsuario() {
        val uid = repo.getCurrentUid() ?: return
        CoroutineScope(Dispatchers.IO).launch {
            usuarioActual = repo.getUsuario(uid)
        }
    }

    private fun enviarSolicitud() {
        val area = binding.spinnerArea.selectedItem.toString()
        val tipo = binding.spinnerTipo.selectedItem.toString()
        val prioridad = binding.spinnerPrioridad.selectedItem.toString()
        val desc = binding.etDescripcion.text.toString()
        val edificioId = usuarioActual?.edificioId ?: ""

        if (edificioId.isEmpty()) {
            Snackbar.make(binding.root, "Error: No se encontró información del edificio", Snackbar.LENGTH_LONG).show()
            return
        }

        binding.btnEnviar.isEnabled = false
        
        CoroutineScope(Dispatchers.IO).launch {
            val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            
            val nuevaTarea = TareaLimpieza(
                edificioId = edificioId,
                area = area,
                tipoLimpieza = tipo,
                descripcion = desc,
                prioridad = prioridad,
                solicitadaPor = usuarioActual?.nombre ?: "Residente",
                fechaAsignada = fecha,
                completada = false
            )

            val exito = repo.crearTareaLimpieza(nuevaTarea)
            withContext(Dispatchers.Main) {
                if (exito) {
                    Toast.makeText(this@SolicitarLimpiezaActivity, "Solicitud enviada correctamente", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    binding.btnEnviar.isEnabled = true
                    Snackbar.make(binding.root, "Error al enviar solicitud", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
}
