package com.example.adminiums1.ui.admin

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adminiums1.R
import com.example.adminiums1.databinding.ActivityAdminBinding
import com.example.adminiums1.model.Usuario
import com.example.adminiums1.repository.FirebaseRepository
import com.example.adminiums1.ui.auth.LoginActivity
import com.example.adminiums1.ui.auth.RegistroActivity
import com.example.adminiums1.ui.limpieza.LimpiezaActivity
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
        
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorPrimaryDark)

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

        binding.tvEdificioActual.setOnClickListener { seleccionarEdificioDialog() }

        binding.btnVerVigilantes.setOnClickListener {
            val intent = Intent(this, VigilantesAdminActivity::class.java)
            intent.putExtra("edificioId", edificioIdActual)
            startActivity(intent)
        }

        binding.btnAmenidades.setOnClickListener {
            val intent = Intent(this, AmenidadesAdminActivity::class.java)
            intent.putExtra("edificioId", edificioIdActual)
            startActivity(intent)
        }

        binding.btnVerHistorial.setOnClickListener {
            val intent = Intent(this, HistorialEntradasActivity::class.java)
            intent.putExtra("edificioId", edificioIdActual)
            startActivity(intent)
        }

        binding.btnAgregarResidente.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }

        binding.btnGestionarIncidencias.setOnClickListener {
            abrirGestionIncidencias()
        }
        binding.statIncidencias.setOnClickListener { abrirGestionIncidencias() }

        binding.btnGestionarLimpieza.setOnClickListener {
            abrirGestionLimpieza()
        }
        binding.statLimpieza.setOnClickListener { abrirGestionLimpieza() }

        binding.btnLogout.setOnClickListener {
            repo.logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
    }

    private fun abrirGestionIncidencias() {
        val intent = Intent(this, ManejoIncidenciasActivity::class.java)
        intent.putExtra("edificioId", edificioIdActual)
        startActivity(intent)
    }

    private fun abrirGestionLimpieza() {
        val intent = Intent(this, LimpiezaActivity::class.java)
        intent.putExtra("edificioId", edificioIdActual)
        startActivity(intent)
    }

    private fun seleccionarEdificioDialog() {
        CoroutineScope(Dispatchers.IO).launch {
            val condominios = repo.getCondominios()
            withContext(Dispatchers.Main) {
                if (condominios.isEmpty()) {
                    Toast.makeText(this@AdminActivity, "No hay condominios registrados", Toast.LENGTH_LONG).show()
                    return@withContext
                }
                val nombres = condominios.map { "${it.nombre} (${it.ciudad})" }.toTypedArray()
                AlertDialog.Builder(this@AdminActivity)
                    .setTitle("🏢 Seleccionar Edificio")
                    .setItems(nombres) { _, which ->
                        val sel = condominios[which]
                        edificioIdActual = sel.id
                        binding.tvEdificioActual.text = sel.nombre
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
                val incidenciasPend = repo.getIncidenciasPorEstado(edificioIdActual, "Pendiente")
                val tareasActivas = repo.getTareasLimpiezaActivas(edificioIdActual)
                
                withContext(Dispatchers.Main) {
                    listaResidentes = residentes
                    adapter.setDatos(residentes)
                    
                    val deuda = residentes.filter { it.balance < 0 }.sumOf { abs(it.balance) }
                    val pagado = residentes.filter { it.balance >= 0 }.sumOf { it.balance }
                    
                    binding.tvTotalResidentes.text = residentes.size.toString()
                    binding.tvAlCorriente.text = residentes.count { it.balance >= 0 }.toString()
                    binding.tvTotalRecaudado.text = "$ ${"%.2f".format(pagado)}"
                    binding.tvTotalDeuda.text = "$ ${"%.2f".format(deuda)}"
                    
                    binding.tvIncidenciasPendientes.text = incidenciasPend.size.toString()
                    binding.tvTareasLimpiezaActivas.text = tareasActivas.size.toString()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AdminActivity, "Error de carga", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun mostrarOpcionesResidente(u: Usuario) {
        val opciones = arrayOf("Ver Estado de Cuenta", "➕ Registrar Pago", "Historial de Pagos", "Eliminar")
        AlertDialog.Builder(this)
            .setTitle(u.nombre)
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(this, ResidenteDetalleActivity::class.java)
                        intent.putExtra("extra_uid", u.uid)
                        startActivity(intent)
                    }
                    1 -> mostrarDialogRecarga(u)
                    2 -> {
                        val intent = Intent(this, PagosResidenteAdminActivity::class.java)
                        intent.putExtra("uid", u.uid)
                        intent.putExtra("nombre", u.nombre)
                        startActivity(intent)
                    }
                    3 -> confirmarEliminar(u)
                }
            }.show()
    }

    private fun confirmarEliminar(u: Usuario) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Residente")
            .setMessage("¿Estás seguro de eliminar a ${u.nombre}?")
            .setPositiveButton("Eliminar") { _, _ ->
                // Lógica de borrado en repo si fuera necesario
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun mostrarDialogRecarga(u: Usuario) {
        val input = android.widget.EditText(this)
        input.hint = "Monto"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        
        AlertDialog.Builder(this)
            .setTitle("Registrar Pago")
            .setView(input)
            .setPositiveButton("Registrar") { _, _ ->
                val monto = input.text.toString().toDoubleOrNull() ?: 0.0
                if (monto > 0) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val exito = repo.crearUsuario(u.copy(balance = u.balance + monto))
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
        else if (binding.chipAlCorriente.isChecked) filtrada = filtrada.filter { it.balance >= 0 }
        adapter.setDatos(filtrada)
    }
}
