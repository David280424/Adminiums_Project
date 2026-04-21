package com.example.adminiums1.ui.vigilante

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adminiums1.databinding.ActivityVigilanteBinding
import com.example.adminiums1.model.RegistroAcceso
import com.example.adminiums1.model.Usuario
import com.example.adminiums1.model.Visitante
import com.example.adminiums1.repository.FirebaseRepository
import com.example.adminiums1.ui.auth.RolSelectorActivity
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

    private val scanLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        if (result.contents != null) procesarQRResidente(result.contents)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVigilanteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        cargarNombreVigilante()
        configurarTabs()
        configurarRegistroManual()
        configurarEscaneoQR()
        configurarHistorial()
        cargarHistorialHoy()

        binding.btnLogout.setOnClickListener {
            repo.logout()
            startActivity(Intent(this, RolSelectorActivity::class.java))
            finishAffinity()
        }
    }

    // ── Nombre del vigilante ────────────────────────────────────────────────

    private fun cargarNombreVigilante() {
        val uid = repo.getCurrentUid() ?: return
        CoroutineScope(Dispatchers.Main).launch {
            val usuario = repo.getUsuario(uid)
            binding.tvNombreVigilante.text = usuario?.nombre ?: "Vigilante"
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
    }

    private fun buscarResidenteManual(query: String) {
        binding.progressManual.mostrar()
        binding.cardConfirmarManual.ocultar()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val residentes: List<Usuario> = repo.getTodosUsuarios()
                binding.progressManual.ocultar()

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
                    Toast.makeText(this@VigilanteActivity,
                        "No se encontró ningún residente con esos datos", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.progressManual.ocultar()
                ErrorHandler.mostrar(this@VigilanteActivity, e, "buscarResidenteManual")
            }
        }
    }

    private fun confirmarEntradaManual(residente: Usuario) {
        binding.progressManual.mostrar()
        binding.btnConfirmarEntrada.isEnabled = false

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val ahora = Date()
                val acceso = RegistroAcceso(
                    residenteUid    = residente.uid,
                    residenteNombre = residente.nombre,
                    unidad          = residente.unidad,
                    metodo          = "manual",
                    hora            = SimpleDateFormat("HH:mm", Locale.getDefault()).format(ahora),
                    fecha           = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(ahora),
                    timestamp       = ahora.time
                )
                repo.registrarAcceso(acceso)
                binding.progressManual.ocultar()
                binding.btnConfirmarEntrada.isEnabled = true
                binding.cardConfirmarManual.ocultar()
                binding.etBuscarManual.setText("")
                residenteEncontrado = null

                Toast.makeText(this@VigilanteActivity,
                    "✅ Entrada registrada: ${residente.nombre} — ${acceso.hora}",
                    Toast.LENGTH_LONG).show()

                // Refrescar historial en segundo plano
                cargarHistorialHoy()

            } catch (e: Exception) {
                binding.progressManual.ocultar()
                binding.btnConfirmarEntrada.isEnabled = true
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

    /**
     * Procesa el QR del residente (formato: "RESIDENTE|uid|nombre|unidad").
     * Registra la entrada automáticamente sin pasos adicionales.
     */
    private fun procesarQRResidente(contenido: String) {
        binding.progressQR.mostrar()
        binding.tvResultadoQR.ocultar()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Formato del QR del residente: "RESIDENTE|uid|nombre|unidad"
                val partes = contenido.split("|")

                if (partes.size >= 4 && partes[0] == "RESIDENTE") {
                    // QR de residente → registrar acceso directo
                    val uid    = partes[1]
                    val nombre = partes[2]
                    val unidad = partes[3]

                    val ahora = Date()
                    val acceso = RegistroAcceso(
                        residenteUid    = uid,
                        residenteNombre = nombre,
                        unidad          = unidad,
                        metodo          = "qr",
                        hora            = SimpleDateFormat("HH:mm", Locale.getDefault()).format(ahora),
                        fecha           = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(ahora),
                        timestamp       = ahora.time
                    )
                    repo.registrarAcceso(acceso)

                    binding.progressQR.ocultar()
                    mostrarResultadoQR(
                        exito   = true,
                        mensaje = "✅ ACCESO REGISTRADO\nResidente: $nombre\nUnidad: $unidad\nHora: ${acceso.hora}"
                    )
                    cargarHistorialHoy()

                } else {
                    // QR de visitante → flujo anterior de validación
                    val visitante: Visitante? = repo.validarVisitante(contenido)
                    binding.progressQR.ocultar()

                    if (visitante != null) {
                        mostrarResultadoQR(
                            exito   = true,
                            mensaje = "✅ VISITANTE PERMITIDO\nNombre: ${visitante.nombre}\nAutorizado por: ${visitante.autorizadoPor}\nUnidad: ${visitante.unidad}"
                        )
                    } else {
                        mostrarResultadoQR(
                            exito   = false,
                            mensaje = "❌ ACCESO DENEGADO\nCódigo QR inválido o ya utilizado"
                        )
                    }
                }

            } catch (e: Exception) {
                binding.progressQR.ocultar()
                ErrorHandler.mostrar(this@VigilanteActivity, e, "procesarQRResidente")
            }
        }
    }

    private fun mostrarResultadoQR(exito: Boolean, mensaje: String) {
        binding.tvResultadoQR.mostrar()
        binding.tvResultadoQR.text = mensaje
        binding.tvResultadoQR.setBackgroundColor(
            if (exito) 0xFF4CAF50.toInt() else 0xFFF44336.toInt()
        )
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
            try {
                val accesos = repo.getAccesosPorFecha(hoy)
                binding.progressHistorial.ocultar()
                binding.tvTotalEntradas.text = "${accesos.size} entradas registradas"
                accesosAdapter.setDatos(accesos)

                if (accesos.isEmpty()) binding.tvSinAccesos.mostrar()
                else binding.tvSinAccesos.ocultar()

            } catch (e: Exception) {
                binding.progressHistorial.ocultar()
                ErrorHandler.mostrar(this@VigilanteActivity, e, "cargarHistorialHoy")
            }
        }
    }
}
