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
import com.example.adminiums1.utils.ErrorHandler
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

        binding.tvEdificioActual.setOnClickListener { mostrarMenuEdificio() }

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

        binding.btnNotificarDeudores.setOnClickListener {
            notificarTodosDeudores()
        }

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

    private fun mostrarMenuEdificio() {
        val opciones = arrayOf("Cambiar Edificio", "Configuración del Edificio")
        AlertDialog.Builder(this)
            .setTitle(binding.tvEdificioActual.text)
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> seleccionarEdificioDialog()
                    1 -> {
                        val intent = Intent(this, ConfiguracionEdificioActivity::class.java)
                        intent.putExtra("edificioId", edificioIdActual)
                        startActivity(intent)
                    }
                }
            }.show()
    }

    private fun seleccionarEdificioDialog() {
        CoroutineScope(Dispatchers.IO).launch {
            val result = repo.getCondominios()
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    val condominios = result.getOrDefault(emptyList())
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
                } else {
                    Toast.makeText(this@AdminActivity, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun cargarDashboard() {
        if (edificioIdActual.isEmpty()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resultResidentes = repo.getResidentesPorEdificio(edificioIdActual)
                val residentes = resultResidentes.getOrDefault(emptyList())
                
                val incidenciasPend = repo.getIncidenciasPorEstado(edificioIdActual, "Pendiente")
                val tareasActivas = repo.getTareasLimpiezaActivas(edificioIdActual)
                
                withContext(Dispatchers.Main) {
                    if (resultResidentes.isFailure) {
                        ErrorHandler.mostrar(this@AdminActivity, resultResidentes.exceptionOrNull()!!, "cargarDashboard")
                    }

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
        val opciones = arrayOf("Ver Estado de Cuenta", "➕ Registrar Pago", "Historial de Pagos", "🔔 Enviar Recordatorio", "Eliminar")
        AlertDialog.Builder(this)
            .setTitle(u.nombre)
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(this, com.example.adminiums1.ui.residente.EstadoCuentaActivity::class.java)
                        intent.putExtra("uid_residente", u.uid)
                        startActivity(intent)
                    }
                    1 -> mostrarDialogRecarga(u)
                    2 -> {
                        val intent = Intent(this, PagosResidenteAdminActivity::class.java)
                        intent.putExtra("uid", u.uid)
                        intent.putExtra("nombre", u.nombre)
                        startActivity(intent)
                    }
                    3 -> enviarRecordatorio(u)
                    4 -> confirmarEliminar(u)
                }
            }.show()
    }

    private fun enviarRecordatorio(u: Usuario) {
        val balanceStr = "%.2f".format(abs(u.balance))
        val msj = "Hola ${u.nombre}, te recordamos que tienes un adeudo pendiente de $ $balanceStr en tu cuenta de Adminiums. Por favor, realiza tu pago a la brevedad."
        
        AlertDialog.Builder(this)
            .setTitle("Enviar Recordatorio")
            .setMessage("Se enviará una notificación a ${u.nombre} sobre su adeudo de $ $balanceStr.")
            .setPositiveButton("Enviar") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    val exito = repo.solicitarNotificacionDeuda(u, msj)
                    withContext(Dispatchers.Main) {
                        if (exito) {
                            Toast.makeText(this@AdminActivity, "Recordatorio enviado a ${u.nombre}", Toast.LENGTH_SHORT).show()
                        } else {
                            // Fallback if FCM token is missing
                            val mockIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, msj)
                            }
                            startActivity(Intent.createChooser(mockIntent, "Compartir vía..."))
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
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
                        val result = repo.crearUsuario(u.copy(balance = u.balance + monto))
                        withContext(Dispatchers.Main) {
                            if (result.isSuccess) {
                                cargarDashboard()
                                Toast.makeText(this@AdminActivity, "Saldo actualizado", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@AdminActivity, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
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

    private fun notificarTodosDeudores() {
        val deudores = listaResidentes.filter { it.balance < 0 }
        if (deudores.isEmpty()) {
            Toast.makeText(this, "No hay deudores para notificar", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Notificar a Todos")
            .setMessage("¿Deseas enviar un recordatorio automático a los ${deudores.size} residentes con adeudos?")
            .setPositiveButton("Enviar Todo") { _, _ ->
                var enviados = 0
                CoroutineScope(Dispatchers.IO).launch {
                    deudores.forEach { u ->
                        val balanceStr = "%.2f".format(abs(u.balance))
                        val msj = "Hola ${u.nombre}, recordatorio de pago pendiente: $ $balanceStr."
                        if (repo.solicitarNotificacionDeuda(u, msj)) {
                            enviados++
                        }
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@AdminActivity, "Se solicitaron $enviados notificaciones", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
