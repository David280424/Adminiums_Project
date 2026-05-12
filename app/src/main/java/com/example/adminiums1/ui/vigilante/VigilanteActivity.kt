package com.example.adminiums1.ui.vigilante

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adminiums1.databinding.ActivityVigilanteBinding
import com.example.adminiums1.model.RegistroAcceso
import com.example.adminiums1.model.Usuario
import com.example.adminiums1.repository.FirebaseRepository
import com.example.adminiums1.ui.auth.LoginActivity
import com.example.adminiums1.ui.vigilante.adapter.AccesosAdapter
import com.example.adminiums1.utils.ErrorHandler
import com.example.adminiums1.utils.mostrar
import com.example.adminiums1.utils.ocultar
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class VigilanteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVigilanteBinding
    private val repo = FirebaseRepository()
    private lateinit var accesosAdapter: AccesosAdapter

    // Residente encontrado en búsqueda manual (pendiente de confirmar)
    private var residenteEncontrado: Usuario? = null
    private var nombreVigilanteActual: String = ""
    private var edificioIdVigilante: String = ""
    private var contadorAccesosHoy: Int = 0

    private val scanLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        if (result.contents != null) procesarQRResidente(result.contents)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVigilanteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        configurarTabs()
        configurarRegistroManual()
        configurarEscaneoQR()
        configurarHistorial()

        binding.btnConfirmarEntrada.isEnabled = false

        binding.btnLogout.setOnClickListener {
            repo.logout()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        cargarNombreVigilante()
    }

    // ── Nombre del vigilante ────────────────────────────────────────────────

    private fun cargarNombreVigilante() {
        val uid = repo.getCurrentUid() ?: return
        CoroutineScope(Dispatchers.Main).launch {
            val result = repo.getUsuario(uid)
            val usuario = result.getOrNull()
            nombreVigilanteActual = usuario?.nombre ?: "Vigilante"
            edificioIdVigilante   = usuario?.edificioId ?: ""
            binding.tvNombreVigilante.text = nombreVigilanteActual

            binding.btnConfirmarEntrada.isEnabled = true

            // BUG 1 Fix: Initialize counter and load data
            cargarHistorialHoy()
            contadorAccesosHoy = 0

            if (result.isFailure) {
                ErrorHandler.mostrar(this@VigilanteActivity, result.exceptionOrNull()!!, "cargarNombreVigilante")
            }
        }
    }

    // ── Tabs: Manual / QR / Historial ──────────────────────────────────────

    private fun configurarTabs() {
        mostrarTab("manual")

        binding.btnTabManual.setOnClickListener   { mostrarTab("manual") }
        binding.btnTabQR.setOnClickListener       { mostrarTab("qr") }
        binding.btnTabHistorial.setOnClickListener {
            mostrarTab("historial")
            cargarHistorialHoy()
        }
    }

    private fun mostrarTab(tab: String) {
        binding.layoutManual.ocultar()
        binding.layoutQR.ocultar()
        binding.layoutHistorial.ocultar()

        when (tab) {
            "manual"    -> binding.layoutManual.mostrar()
            "qr"        -> binding.layoutQR.mostrar()
            "historial" -> binding.layoutHistorial.mostrar()
        }
    }

    // ── Tab 1: Registro manual ──────────────────────────────────────────────

    private fun configurarRegistroManual() {
        binding.cardConfirmarManual.ocultar()

        binding.btnBuscarManual.setOnClickListener {
            val query = binding.etBuscarManual.text.toString().trim()
            if (query.isEmpty()) {
                Toast.makeText(this, "Ingresa un nombre o número de unidad", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            buscarResidenteManual(query)
        }

        binding.btnConfirmarEntrada.setOnClickListener {
            residenteEncontrado?.let { confirmarEntradaManual(it) }
        }

        // Registrar visitante externo manual
        binding.btnRegistrarVisitante.setOnClickListener {
            val nombreVisitante = binding.etNombreVisitante.text.toString().trim()
            if (nombreVisitante.isEmpty()) {
                Toast.makeText(this, "Ingresa el nombre del visitante", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val ahora = Date()
            val acceso = RegistroAcceso(
                residenteUid    = "",
                residenteNombre = nombreVisitante,
                unidad          = "Visita",
                edificioId      = edificioIdVigilante,
                metodo          = "visitante_manual",
                tipoPersona     = "visitante",
                hora            = SimpleDateFormat("HH:mm", Locale.getDefault()).format(ahora),
                fecha           = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(ahora),
                vigilanteNombre = nombreVigilanteActual,
                timestamp       = ahora.time
            )
            CoroutineScope(Dispatchers.Main).launch {
                val result = repo.registrarAcceso(acceso)
                result.onSuccess {
                    binding.etNombreVisitante.setText("")
                    // BUG 1 Fix: Update UI directly
                    contadorAccesosHoy++
                    binding.tvTotalEntradas.text = "$contadorAccesosHoy entradas registradas"
                    binding.tvSinAccesos.ocultar()

                    Toast.makeText(this@VigilanteActivity, "✅ Visitante registrado: $nombreVisitante", Toast.LENGTH_SHORT).show()
                    cargarHistorialHoy()
                }.onFailure { e ->
                    ErrorHandler.mostrar(this@VigilanteActivity, e, "registrarVisitanteManual")
                }
            }
        }
    }

    private fun buscarResidenteManual(query: String) {
        binding.progressManual.mostrar()
        binding.cardConfirmarManual.ocultar()

        CoroutineScope(Dispatchers.Main).launch {
            val result = repo.getResidentesPorEdificio(edificioIdVigilante)
            binding.progressManual.ocultar()

            result.onSuccess { residentes ->
                val encontrado = residentes.find {
                    it.nombre.contains(query, ignoreCase = true) ||
                    it.unidad.contains(query, ignoreCase = true)
                }

                if (encontrado != null) {
                    residenteEncontrado = encontrado
                    binding.tvResidenteEncontradoNombre.text = encontrado.nombre
                    binding.tvResidenteEncontradoUnidad.text = "Unidad: ${encontrado.unidad}"
                    binding.cardConfirmarManual.mostrar()
                } else {
                    residenteEncontrado = null
                    binding.cardConfirmarManual.ocultar()
                    Toast.makeText(this@VigilanteActivity, "No se encontró ningún residente con esos datos", Toast.LENGTH_SHORT).show()
                }
            }.onFailure { e ->
                ErrorHandler.mostrar(this@VigilanteActivity, e, "buscarResidenteManual")
            }
        }
    }

    private fun confirmarEntradaManual(residente: Usuario) {
        binding.progressManual.mostrar()
        binding.btnConfirmarEntrada.isEnabled = false

        CoroutineScope(Dispatchers.Main).launch {
            val ahora = Date()
            val acceso = RegistroAcceso(
                residenteUid    = residente.uid,
                residenteNombre = residente.nombre,
                unidad          = residente.unidad,
                edificioId      = edificioIdVigilante,
                metodo          = "manual",
                hora            = SimpleDateFormat("HH:mm", Locale.getDefault()).format(ahora),
                fecha           = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(ahora),
                vigilanteNombre = nombreVigilanteActual,
                timestamp       = ahora.time
            )
            val result = repo.registrarAcceso(acceso)
            binding.progressManual.ocultar()
            binding.btnConfirmarEntrada.isEnabled = true

            result.onSuccess {
                binding.cardConfirmarManual.ocultar()
                binding.etBuscarManual.setText("")
                residenteEncontrado = null

                // BUG 1 Fix: Update UI directly
                contadorAccesosHoy++
                binding.tvTotalEntradas.text = "$contadorAccesosHoy entradas registradas"
                binding.tvSinAccesos.ocultar()

                Toast.makeText(this@VigilanteActivity, "✅ Entrada registrada: ${residente.nombre} — ${acceso.hora}", Toast.LENGTH_LONG).show()
                cargarHistorialHoy()
            }.onFailure { e ->
                ErrorHandler.mostrar(this@VigilanteActivity, e, "confirmarEntradaManual")
            }
        }
    }

    // ── Tab 2: Escaneo QR del residente ────────────────────────────────────

    private fun configurarEscaneoQR() {
        binding.tvResultadoQR.ocultar()

        binding.btnEscanearQR.setOnClickListener {
            val options = ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                setPrompt("Escanea el QR del residente")
                setBeepEnabled(true)
                setOrientationLocked(false)
            }
            scanLauncher.launch(options)
        }
    }

    private fun procesarQRResidente(contenido: String) {
        binding.progressQR.mostrar()
        binding.tvResultadoQR.ocultar()

        CoroutineScope(Dispatchers.Main).launch {
            val partes = contenido.split("|")

            if (partes.size >= 4 && partes[0] == "RESIDENTE") {
                val uid    = partes[1]
                val nombre = partes[2]
                val unidad = partes[3]

                val ahora = Date()
                val acceso = RegistroAcceso(
                    residenteUid    = uid,
                    residenteNombre = nombre,
                    unidad          = unidad,
                    edificioId      = edificioIdVigilante,
                    metodo          = "qr",
                    tipoPersona     = "residente",
                    hora            = SimpleDateFormat("HH:mm", Locale.getDefault()).format(ahora),
                    fecha           = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(ahora),
                    vigilanteNombre = nombreVigilanteActual,
                    timestamp       = ahora.time
                )
                val result = repo.registrarAcceso(acceso)
                binding.progressQR.ocultar()

                result.onSuccess {
                    // BUG 1 Fix: Update UI directly
                    contadorAccesosHoy++
                    binding.tvTotalEntradas.text = "$contadorAccesosHoy entradas registradas"
                    binding.tvSinAccesos.ocultar()

                    mostrarResultadoQR(true, "✅ ACCESO REGISTRADO\nResidente: $nombre\nUnidad: $unidad\nHora: ${acceso.hora}")
                    cargarHistorialHoy()
                }.onFailure { e ->
                    ErrorHandler.mostrar(this@VigilanteActivity, e, "procesarQRResidente")
                }

            } else {
                val result = repo.validarVisitante(contenido)
                binding.progressQR.ocultar()

                result.onSuccess { visitante ->
                    if (visitante != null) {
                        mostrarResultadoQR(true, "✅ VISITANTE PERMITIDO\nNombre: ${visitante.nombre}\nAutorizado por: ${visitante.autorizadoPor}\nUnidad: ${visitante.unidad}")

                        val ahora = Date()
                        val accesoVisitante = RegistroAcceso(
                            residenteUid    = "",
                            residenteNombre = "${visitante.nombre} (visitante de ${visitante.autorizadoPor})",
                            unidad          = visitante.unidad,
                            edificioId      = edificioIdVigilante,
                            metodo          = "visitante_qr",
                            tipoPersona     = "visitante",
                            hora            = SimpleDateFormat("HH:mm", Locale.getDefault()).format(ahora),
                            fecha           = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(ahora),
                            vigilanteNombre = nombreVigilanteActual,
                            timestamp       = ahora.time
                        )
                        repo.registrarAcceso(accesoVisitante)
                        
                        // Visitor QR also counts as entry
                        contadorAccesosHoy++
                        binding.tvTotalEntradas.text = "$contadorAccesosHoy entradas registradas"
                        binding.tvSinAccesos.ocultar()
                        
                        cargarHistorialHoy()
                    } else {
                        mostrarResultadoQR(false, "❌ ACCESO DENEGADO\nCódigo QR inválido o ya utilizado")
                    }
                }.onFailure { e ->
                    ErrorHandler.mostrar(this@VigilanteActivity, e, "validarVisitante")
                }
            }
        }
    }

    private fun mostrarResultadoQR(exito: Boolean, mensaje: String) {
        binding.tvResultadoQR.mostrar()
        binding.tvResultadoQR.text = mensaje
        binding.tvResultadoQR.setBackgroundColor(if (exito) 0xFF4CAF50.toInt() else 0xFFF44336.toInt())
    }

    // ── Tab 3: Historial del día ────────────────────────────────────────────

    private fun configurarHistorial() {
        accesosAdapter = AccesosAdapter()
        binding.rvHistorial.apply {
            layoutManager = LinearLayoutManager(this@VigilanteActivity)
            adapter = accesosAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun cargarHistorialHoy() {
        val hoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        binding.tvFechaHistorial.text = "Entradas del $hoy"
        binding.progressHistorial.mostrar()

        CoroutineScope(Dispatchers.Main).launch {
            val result = repo.getAccesosPorFecha(hoy, edificioIdVigilante)
            binding.progressHistorial.ocultar()

            result.onSuccess { accesos ->
                // BUG 1 Fix: Sync counter from loaded data
                contadorAccesosHoy = accesos.size
                binding.tvTotalEntradas.text = "$contadorAccesosHoy entradas registradas"
                accesosAdapter.setDatos(accesos)

                if (contadorAccesosHoy == 0) binding.tvSinAccesos.mostrar()
                else binding.tvSinAccesos.ocultar()
            }.onFailure { e ->
                ErrorHandler.mostrar(this@VigilanteActivity, e, "cargarHistorialHoy")
            }
        }
    }
}
