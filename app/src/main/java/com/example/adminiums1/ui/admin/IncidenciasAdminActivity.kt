package com.example.adminiums1.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adminiums1.databinding.ActivityManejoIncidenciasBinding
import com.example.adminiums1.databinding.DialogIncidenciaDetalleBinding
import com.example.adminiums1.model.Incidencia
import com.example.adminiums1.repository.FirebaseRepository
import com.example.adminiums1.ui.admin.adapter.IncidenciasAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IncidenciasAdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManejoIncidenciasBinding
    private val repo = FirebaseRepository()
    private lateinit var adapter: IncidenciasAdapter
    private var edificioId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManejoIncidenciasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        edificioId = intent.getStringExtra("edificioId") ?: ""
        
        setupUI()
        cargarIncidencias("Todas")
    }

    private fun setupUI() {
        // Corregido: Usar toolbar con el ID correcto del XML
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        adapter = IncidenciasAdapter { incidencia ->
            mostrarDetalleIncidencia(incidencia)
        }
        
        binding.rvIncidencias.layoutManager = LinearLayoutManager(this)
        binding.rvIncidencias.adapter = adapter
        
        binding.chipTodas.setOnClickListener { cargarIncidencias("Todas") }
        binding.chipPendientes.setOnClickListener { cargarIncidencias("Pendiente") }
        binding.chipEnProceso.setOnClickListener { cargarIncidencias("En Proceso") }
        binding.chipResueltas.setOnClickListener { cargarIncidencias("Resuelta") }
    }

    private fun cargarIncidencias(estado: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val lista = if (estado == "Todas") {
                repo.getIncidenciasEdificio(edificioId)
            } else {
                repo.getIncidenciasPorEstado(edificioId, estado)
            }
            
            withContext(Dispatchers.Main) {
                adapter.setDatos(lista)
                // Aseguramos que los componentes existan en activity_manejo_incidencias.xml
                binding.layoutEmpty.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                binding.toolbar.title = "Incidencias (${lista.size})"
            }
        }
    }

    private fun mostrarDetalleIncidencia(incidencia: Incidencia) {
        val dialog = BottomSheetDialog(this)
        val b = DialogIncidenciaDetalleBinding.inflate(layoutInflater)
        dialog.setContentView(b.root)

        b.tvDetalleTitulo.text = incidencia.titulo
        b.tvDetalleDesc.text = incidencia.descripcion
        b.tvDetalleInfo.text = "De: ${incidencia.residenteNombre} • Unidad ${incidencia.unidad}\nCategoría: ${incidencia.categoria} • Ubicación: ${incidencia.ubicacion}\nPrioridad: ${incidencia.prioridad}"
        b.etRespuestaAdmin.setText(incidencia.respuestaAdmin)

        b.btnMarcarPendiente.setOnClickListener {
            actualizarEstado(incidencia.id, "Pendiente", b.etRespuestaAdmin.text.toString(), dialog)
        }
        b.btnMarcarProceso.setOnClickListener {
            actualizarEstado(incidencia.id, "En Proceso", b.etRespuestaAdmin.text.toString(), dialog)
        }
        b.btnMarcarResuelta.setOnClickListener {
            actualizarEstado(incidencia.id, "Resuelta", b.etRespuestaAdmin.text.toString(), dialog)
        }

        dialog.show()
    }

    private fun actualizarEstado(id: String, estado: String, respuesta: String, dialog: BottomSheetDialog) {
        CoroutineScope(Dispatchers.IO).launch {
            val exito = repo.actualizarEstadoIncidencia(id, estado, respuesta)
            withContext(Dispatchers.Main) {
                if (exito) {
                    dialog.dismiss()
                    cargarIncidencias("Todas")
                    Toast.makeText(this@IncidenciasAdminActivity, "Incidencia actualizada", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
