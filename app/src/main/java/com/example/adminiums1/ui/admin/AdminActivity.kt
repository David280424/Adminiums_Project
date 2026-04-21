package com.example.adminiums1.ui.admin

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adminiums1.databinding.ActivityAdminBinding
import com.example.adminiums1.model.Reservacion
import com.example.adminiums1.model.Usuario
import com.example.adminiums1.repository.FirebaseRepository
import com.example.adminiums1.ui.auth.RolSelectorActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private val repo = FirebaseRepository()
    private lateinit var adapter: ResidentesAdapter
    
    private var edificioIdActual: String = "" 
    private var listaResidentes: List<Usuario> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        seleccionarEdificioDialog()
    }

    private fun setupUI() {
        adapter = ResidentesAdapter { residente ->
            mostrarOpcionesResidente(residente)
        }
        binding.rvResidentes.layoutManager = LinearLayoutManager(this)
        binding.rvResidentes.adapter = adapter

        binding.chipGroupFiltros.setOnCheckedChangeListener { _, _ ->
            filtrar(binding.etBuscarResidente.text.toString())
        }

        binding.etBuscarResidente.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filtrar(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.layoutEdificio.setOnClickListener { seleccionarEdificioDialog() }

        binding.btnLogout.setOnClickListener {
            repo.logout()
            startActivity(Intent(this, RolSelectorActivity::class.java))
            finishAffinity()
        }
    }

    private fun seleccionarEdificioDialog() {
        CoroutineScope(Dispatchers.IO).launch {
            val condominios = repo.getCondominios()
            withContext(Dispatchers.Main) {
                if (condominios.isEmpty()) {
                    Toast.makeText(this@AdminActivity, "No hay condominios registrados", Toast.LENGTH_LONG).show()
                    return@withContext
                }
                val nombres = condominios.map { it.nombre }.toTypedArray()
                AlertDialog.Builder(this@AdminActivity)
                    .setTitle("🏢 Seleccionar Edificio")
                    .setItems(nombres) { _, which ->
                        val sel = condominios[which]
                        edificioIdActual = sel.id
                        binding.tvEdificioActual.text = "🏢 ${sel.nombre}"
                        cargarDashboard()
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    private fun cargarDashboard() {
        if (edificioIdActual.isEmpty()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val residentes = repo.getResidentesPorEdificio(edificioIdActual)
                val recaudado = repo.getTotalRecaudadoEdificio(edificioIdActual)
                val reservaciones = repo.getReservaciones()

                withContext(Dispatchers.Main) {
                    listaResidentes = residentes
                    adapter.setDatos(residentes)
                    binding.tvTotalRecaudado.text = "$ ${"%.2f".format(recaudado)}"
                    val deuda = residentes.filter { it.balance < 0 }.sumOf { abs(it.balance) }
                    binding.tvTotalDeuda.text = "$ ${"%.2f".format(deuda)}"
                    
                    // Actualización Real de Amenidades
                    val resEdificio = reservaciones // En un sistema real filtraríamos por edificioId
                    val pPiscina = Math.min(resEdificio.filter { it.amenidad == "Piscina" }.size * 20, 100)
                    val pGim = Math.min(resEdificio.filter { it.amenidad == "Gimnasio" }.size * 20, 100)
                    
                    binding.progressPiscina.progress = pPiscina
                    binding.tvPiscinaReservas.text = "Piscina: $pPiscina% ocupado"
                    binding.progressGimnasio.progress = pGim
                    binding.tvGimnasioReservas.text = "Gimnasio: $pGim% ocupado"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AdminActivity, "Error de carga", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun mostrarOpcionesResidente(u: Usuario) {
        val opciones = arrayOf("Ver Reporte / Estado de Cuenta", "➕ Añadir Fondos / Recargar", "Eliminar Residente")
        AlertDialog.Builder(this)
            .setTitle(u.nombre)
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(this, ResidenteDetalleActivity::class.java)
                        intent.putExtra(ResidenteDetalleActivity.EXTRA_UID, u.uid)
                        startActivity(intent)
                    }
                    1 -> mostrarDialogRecarga(u)
                    2 -> Toast.makeText(this, "Función eliminar deshabilitada", Toast.LENGTH_SHORT).show()
                }
            }.show()
    }

    private fun mostrarDialogRecarga(u: Usuario) {
        val input = android.widget.EditText(this)
        input.hint = "Monto a añadir (ej: 500)"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        
        AlertDialog.Builder(this)
            .setTitle("💰 Añadir Fondos a ${u.nombre}")
            .setView(input)
            .setPositiveButton("Añadir") { _, _ ->
                val monto = input.text.toString().toDoubleOrNull() ?: 0.0
                if (monto > 0) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val exito = repo.recargarBalance(u.uid, monto, u.edificioId)
                        withContext(Dispatchers.Main) {
                            if (exito) {
                                cargarDashboard()
                                Toast.makeText(this@AdminActivity, "Saldo actualizado", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun filtrar(query: String) {
        var filtrada = listaResidentes.filter { 
            it.nombre.contains(query, ignoreCase = true) || it.unidad.contains(query, ignoreCase = true)
        }
        if (binding.chipDeudores.isChecked) filtrada = filtrada.filter { it.balance < 0 }
        else if (binding.chipAlDia.isChecked) filtrada = filtrada.filter { it.balance >= 0 }
        adapter.setDatos(filtrada)
    }
}
