package com.example.adminiums1.ui.limpieza

import androidx.appcompat.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adminiums1.databinding.ActivityLimpiezaBinding
import com.example.adminiums1.databinding.DialogTareaLimpiezaBinding
import com.example.adminiums1.model.TareaLimpieza
import com.example.adminiums1.model.Usuario
import com.example.adminiums1.repository.FirebaseRepository
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.firestore.ListenerRegistration
import com.example.adminiums1.ui.limpieza.adapter.TareaLimpiezaAdapter

class LimpiezaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLimpiezaBinding
    private val repo = FirebaseRepository()
    private lateinit var adapter: TareaLimpiezaAdapter
    private var todasLasTareas = listOf<TareaLimpieza>()
    private var edificioId: String = ""
    private var usuarioActual: Usuario? = null
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLimpiezaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Prioridad al edificioId del intent (para admins)
        edificioId = intent.getStringExtra("edificioId") ?: ""

        setupRecyclerView()
        setupTabs()
        cargarDatos()

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.fabNuevaTarea.setOnClickListener { mostrarDialogoSolicitud() }
    }

    private fun setupRecyclerView() {
        adapter = TareaLimpiezaAdapter(
            onTareaClick = { tarea ->
                if (usuarioActual?.rol != "residente" && !tarea.completada) {
                    mostrarDialogoCompletar(tarea)
                }
            },
            onDeleteClick = { tarea ->
                if (usuarioActual?.rol == "admin") {
                    confirmarEliminarTarea(tarea)
                }
            }
        )
        binding.rvTareas.layoutManager = LinearLayoutManager(this)
        binding.rvTareas.adapter = adapter
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                filtrarTareas(tab?.position ?: 0)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun cargarDatos() {
        val uid = repo.getCurrentUid() ?: return
        binding.progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            usuarioActual = repo.getUsuario(uid)
            
            if (usuarioActual?.rol == "residente") {
                withContext(Dispatchers.Main) {
                    startActivity(Intent(this@LimpiezaActivity, SolicitarLimpiezaActivity::class.java))
                    finish()
                }
                return@launch
            }

            // Si no vino por intent, usamos el del usuario
            if (edificioId.isEmpty()) {
                edificioId = usuarioActual?.edificioId ?: ""
            }

            withContext(Dispatchers.Main) {
                if (edificioId.isNotEmpty()) {
                    iniciarListener()
                } else {
                    binding.progressBar.visibility = View.GONE
                    Snackbar.make(binding.root, "No se encontró el edificio", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun iniciarListener() {
        listenerRegistration?.remove()
        listenerRegistration = repo.escucharTareasLimpieza(edificioId) { tareas ->
            todasLasTareas = tareas
            binding.progressBar.visibility = View.GONE
            filtrarTareas(binding.tabLayout.selectedTabPosition)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
    }

    private fun filtrarTareas(position: Int) {
        val completadas = position == 1
        val filtradas = todasLasTareas.filter { it.completada == completadas }
        adapter.setTareas(filtradas, usuarioActual?.rol == "admin")
        binding.layoutEmpty.visibility = if (filtradas.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun mostrarDialogoSolicitud() {
        val dialogBinding = DialogTareaLimpiezaBinding.inflate(layoutInflater)
        
        val areas = listOf("Lobby", "Pasillo", "Estacionamiento", "Jardín", "Elevador", "Cuarto de basura", "Gimnasio", "Alberca", "Otro")
        val tipos = listOf("Barrido", "Trapeado", "Desinfección", "Recolección basura", "Limpieza de vidrios", "General")
        val prioridades = listOf("Baja", "Normal", "Alta")

        dialogBinding.spinnerArea.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, areas)
        dialogBinding.spinnerTipo.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, tipos)
        dialogBinding.spinnerPrioridad.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, prioridades)

        AlertDialog.Builder(this)
            .setTitle("Solicitar Limpieza")
            .setView(dialogBinding.root)
            .setPositiveButton("Enviar") { _, _ ->
                enviarSolicitud(dialogBinding)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun enviarSolicitud(dialogBinding: DialogTareaLimpiezaBinding) {
        val area = dialogBinding.spinnerArea.selectedItem.toString()
        val tipo = dialogBinding.spinnerTipo.selectedItem.toString()
        val prioridad = dialogBinding.spinnerPrioridad.selectedItem.toString()
        val desc = dialogBinding.etDescripcion.text.toString()

        if (edificioId.isEmpty()) return
        
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
                    Snackbar.make(binding.root, "Solicitud enviada", Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(binding.root, "Error al enviar solicitud", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun mostrarDialogoCompletar(tarea: TareaLimpieza) {
        AlertDialog.Builder(this)
            .setTitle("Finalizar Tarea")
            .setMessage("¿Deseas marcar esta tarea como completada?")
            .setPositiveButton("Sí") { _, _ ->
                completarTarea(tarea)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun completarTarea(tarea: TareaLimpieza) {
        CoroutineScope(Dispatchers.IO).launch {
            val exito = repo.marcarTareaCompletada(tarea.id, "Completada por ${usuarioActual?.nombre}")
            withContext(Dispatchers.Main) {
                if (exito) {
                    Snackbar.make(binding.root, "Tarea finalizada", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun confirmarEliminarTarea(tarea: TareaLimpieza) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Tarea")
            .setMessage("¿Estás seguro de que deseas eliminar esta tarea?")
            .setPositiveButton("Eliminar") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    val exito = repo.eliminarTareaLimpieza(tarea.id)
                    withContext(Dispatchers.Main) {
                        if (exito) {
                            Snackbar.make(binding.root, "Tarea eliminada", Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}

