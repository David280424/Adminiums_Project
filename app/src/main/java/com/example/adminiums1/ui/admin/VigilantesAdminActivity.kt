package com.example.adminiums1.ui.admin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adminiums1.databinding.ActivityVigilantesAdminBinding
import com.example.adminiums1.model.RegistroAcceso
import com.example.adminiums1.model.Usuario
import com.example.adminiums1.repository.FirebaseRepository
import com.example.adminiums1.ui.admin.adapter.AccesosAdminAdapter
import com.example.adminiums1.ui.admin.adapter.VigilantesAdapter
import com.example.adminiums1.utils.ErrorHandler
import com.example.adminiums1.utils.mostrar
import com.example.adminiums1.utils.ocultar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

class VigilantesAdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVigilantesAdminBinding
    private val db = FirebaseFirestore.getInstance()
    private val repo = FirebaseRepository()
    private val listeners = mutableListOf<ListenerRegistration>()

    private lateinit var vigilantesAdapter: VigilantesAdapter
    private lateinit var accesosAdapter: AccesosAdminAdapter

    // Vigilante seleccionado actualmente
    private var vigilanteSeleccionadoNombre: String = ""
    private var edificioIdAdmin: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVigilantesAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // REQUIRED FIX — Read edificioId from Intent extra
        edificioIdAdmin = intent.getStringExtra("edificioId") ?: ""

        configurarVigilantes()
        configurarAccesos()
        
        // REQUIRED FIX — Call directly, no coroutine needed
        escucharVigilantes()

        binding.cardAccesosVigilante.ocultar()
    }

    // ── Lista de vigilantes activos ─────────────────────────────────────────

    private fun configurarVigilantes() {
        vigilantesAdapter = VigilantesAdapter { vigilante ->
            // Al tocar un vigilante, muestra sus registros de acceso
            vigilanteSeleccionadoNombre = vigilante.nombre
            binding.tvTituloAccesosVigilante.text = "Accesos registrados por: ${vigilante.nombre}"
            binding.cardAccesosVigilante.mostrar()
            cargarAccesosDeVigilante(vigilante.nombre)
        }
        binding.rvVigilantes.apply {
            layoutManager = LinearLayoutManager(this@VigilantesAdminActivity)
            adapter = vigilantesAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun escucharVigilantes() {
        binding.progressVigilantes.mostrar()

        // Fallback: if edificioIdAdmin is empty (superadmin), fetch all vigilantes
        val query = if (edificioIdAdmin.isNotEmpty()) {
            db.collection("usuarios")
                .whereEqualTo("rol", "vigilante")
                .whereEqualTo("edificioId", edificioIdAdmin)
        } else {
            db.collection("usuarios")
                .whereEqualTo("rol", "vigilante")
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) { 
                ErrorHandler.log(error, "VigilantesAdmin")
                binding.progressVigilantes.ocultar()
                return@addSnapshotListener 
            }
            binding.progressVigilantes.ocultar()
            val lista = snapshot?.documents?.mapNotNull { it.toObject(Usuario::class.java) } ?: emptyList()
            binding.tvTotalVigilantes.text = "${lista.size} vigilantes activos"
            vigilantesAdapter.setDatos(lista)

            if (lista.isEmpty()) binding.tvSinVigilantes.mostrar()
            else binding.tvSinVigilantes.ocultar()
        }
        listeners.add(listener)
    }

    // ── Accesos del vigilante seleccionado ──────────────────────────────────

    private fun configurarAccesos() {
        accesosAdapter = AccesosAdminAdapter()
        binding.rvAccesosVigilante.apply {
            layoutManager = LinearLayoutManager(this@VigilantesAdminActivity)
            adapter = accesosAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun cargarAccesosDeVigilante(nombreVigilante: String) {
        binding.progressAccesos.mostrar()
        val hoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        db.collection("accesos")
            .whereEqualTo("fecha", hoy)
            .whereEqualTo("vigilanteNombre", nombreVigilante)
            .get()
            .addOnSuccessListener { snapshot ->
                binding.progressAccesos.ocultar()
                val todos = snapshot.toObjects(RegistroAcceso::class.java)
                accesosAdapter.setDatos(todos)
                binding.tvTotalAccesosVigilante.text = "${todos.size} accesos hoy"
                if (todos.isEmpty()) binding.tvSinAccesosVigilante.mostrar()
                else binding.tvSinAccesosVigilante.ocultar()
            }
            .addOnFailureListener { e ->
                binding.progressAccesos.ocultar()
                ErrorHandler.log(e, "VigilantesAdmin:accesos")
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        listeners.forEach { it.remove() }
        listeners.clear()
    }
}
