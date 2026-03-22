package com.example.adminiums1.ui.admin

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adminiums1.databinding.ActivityHistorialEntradasBinding
import com.example.adminiums1.model.RegistroAcceso
import com.example.adminiums1.model.Usuario
import com.example.adminiums1.ui.admin.adapter.AccesosAdminAdapter
import com.example.adminiums1.utils.ErrorHandler
import com.example.adminiums1.utils.mostrar
import com.example.adminiums1.utils.ocultar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class HistorialEntradasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistorialEntradasBinding
    private val db = FirebaseFirestore.getInstance()
    private lateinit var accesosAdapter: AccesosAdminAdapter

    // Filtros activos
    private var fechaFiltro     = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    private var residenteFiltro = ""
    private var todosLosAccesos = listOf<RegistroAcceso>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistorialEntradasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        configurarLista()
        configurarFiltros()
        cargarAccesos()
    }

    // ── Lista ───────────────────────────────────────────────────────────────

    private fun configurarLista() {
        accesosAdapter = AccesosAdminAdapter()
        binding.rvHistorialEntradas.apply {
            layoutManager = LinearLayoutManager(this@HistorialEntradasActivity)
            adapter = accesosAdapter
            isNestedScrollingEnabled = false
        }
    }

    // ── Filtros ─────────────────────────────────────────────────────────────

    private fun configurarFiltros() {
        // Mostrar fecha actual por defecto
        binding.btnFiltroFecha.text = "📅 $fechaFiltro"

        // DatePicker al tocar el botón de fecha
        binding.btnFiltroFecha.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this,
                { _, year, month, day ->
                    val cal2 = Calendar.getInstance().apply { set(year, month, day) }
                    fechaFiltro = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal2.time)
                    binding.btnFiltroFecha.text = "📅 $fechaFiltro"
                    cargarAccesos()
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Búsqueda por nombre de residente en tiempo real
        binding.etFiltroResidente.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                residenteFiltro = s.toString().trim()
                aplicarFiltros()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Limpiar filtros
        binding.btnLimpiarFiltros.setOnClickListener {
            fechaFiltro = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            residenteFiltro = ""
            binding.btnFiltroFecha.text = "📅 $fechaFiltro"
            binding.etFiltroResidente.setText("")
            cargarAccesos()
        }
    }

    // ── Cargar accesos de Firestore ─────────────────────────────────────────

    private fun cargarAccesos() {
        binding.progressHistorial.mostrar()
        binding.tvSinEntradas.ocultar()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val docs = db.collection("accesos")
                    .whereEqualTo("fecha", fechaFiltro)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get().await()

                todosLosAccesos = docs.toObjects(RegistroAcceso::class.java)
                binding.progressHistorial.ocultar()
                aplicarFiltros()

            } catch (e: Exception) {
                binding.progressHistorial.ocultar()
                ErrorHandler.log(e, "HistorialEntradas:cargar")
            }
        }
    }

    // ── Filtrar en memoria por nombre de residente ──────────────────────────

    private fun aplicarFiltros() {
        val filtrados = if (residenteFiltro.isBlank()) {
            todosLosAccesos
        } else {
            todosLosAccesos.filter {
                it.residenteNombre.contains(residenteFiltro, ignoreCase = true) ||
                it.unidad.contains(residenteFiltro, ignoreCase = true)
            }
        }

        accesosAdapter.setDatos(filtrados)
        binding.tvTotalEntradas.text = "${filtrados.size} entradas — $fechaFiltro"

        if (filtrados.isEmpty()) binding.tvSinEntradas.mostrar()
        else binding.tvSinEntradas.ocultar()
    }
}
