package com.example.adminiums1.ui.residente

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.adminiums1.R
import com.example.adminiums1.databinding.ActivityGenerarQrBinding
import com.example.adminiums1.model.Usuario
import com.example.adminiums1.model.Visitante
import com.example.adminiums1.repository.FirebaseRepository
import com.example.adminiums1.utils.ErrorHandler
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GenerarQRActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGenerarQrBinding
    private val repo = FirebaseRepository()
    private var qrGenerado: String = ""
    private var bmpQR: Bitmap? = null
    private var usuarioActual: Usuario? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGenerarQrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        // Validar sesión al inicio
        val uid = repo.getCurrentUid()
        if (uid == null) {
            Toast.makeText(this, R.string.sesion_no_encontrada, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Cargar datos iniciales del usuario
        cargarDatosUsuario(uid)

        binding.btnGenerarQR.setOnClickListener {
            val nombre = binding.etNombreVisitante.text.toString().trim()
            if (nombre.isEmpty()) {
                binding.etNombreVisitante.error = getString(R.string.error_nombre_visitante)
                return@setOnClickListener
            }

            generarYGuardarQR(nombre)
        }

        binding.btnCompartir.setOnClickListener {
            if (qrGenerado.isNotEmpty()) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_TEXT,
                        getString(R.string.compartir_mensaje, qrGenerado)
                    )
                }
                startActivity(Intent.createChooser(shareIntent, getString(R.string.compartir_titulo)))
            }
        }
    }

    private fun cargarDatosUsuario(uid: String) {
        lifecycleScope.launch {
            repo.getUsuario(uid).onSuccess { usuario ->
                usuarioActual = usuario
                val localeMX = Locale.forLanguageTag("es-MX")
                val hoy = SimpleDateFormat("d MMM yyyy", localeMX).format(Date())

                usuario?.let {
                    binding.tvAutorizadoPor.text = getString(R.string.autorizado_por, it.nombre.ifEmpty { getString(R.string.default_residente) })
                    binding.tvUnidadInfo.text = getString(R.string.unidad_info, it.unidad.ifEmpty { getString(R.string.default_na) })
                    binding.tvVigencia.text = getString(R.string.vigencia_hoy, hoy)
                }
            }.onFailure { e ->
                ErrorHandler.mostrar(this@GenerarQRActivity, e, "cargarDatosUsuario")
            }
        }
    }

    private fun generarYGuardarQR(nombre: String) {
        val usuario = usuarioActual ?: run {
            Toast.makeText(this, R.string.sesion_no_encontrada, Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnGenerarQR.isEnabled = false

        lifecycleScope.launch {
            try {
                val localeMX = Locale.forLanguageTag("es-MX")
                val hoy = SimpleDateFormat("d MMM yyyy", localeMX).format(Date())
                val timestamp = System.currentTimeMillis()

                val resDefault = getString(R.string.default_residente)
                val naDefault = getString(R.string.default_na)

                val nombreAutorizador = usuario.nombre.ifEmpty { resDefault }
                val unidadAutorizador = usuario.unidad.ifEmpty { naDefault }

                // Contenido del QR
                val qrContent = "ADMINIUMS|$nombre|$nombreAutorizador|$unidadAutorizador|$hoy|$timestamp"

                // 1. GENERAR QR (en Dispatchers.Default para no bloquear el hilo principal)
                val bitmap = withContext(Dispatchers.Default) {
                    val barcodeEncoder = BarcodeEncoder()
                    barcodeEncoder.encodeBitmap(qrContent, BarcodeFormat.QR_CODE, 512, 512)
                }

                // 2. GUARDAR EN REPOSITORIO
                val visitante = Visitante(
                    nombre = nombre,
                    autorizadoPor = nombreAutorizador,
                    unidad = unidadAutorizador,
                    vigencia = hoy,
                    qrCode = qrContent,
                    timestamp = timestamp,
                    validado = false
                )

                repo.crearVisitante(visitante).onSuccess {
                    binding.progressBar.visibility = View.GONE
                    binding.btnGenerarQR.isEnabled = true

                    qrGenerado = nombre
                    bmpQR = bitmap

                    binding.ivQR.setImageBitmap(bitmap)
                    binding.layoutQRResult.visibility = View.VISIBLE

                    binding.tvVisitanteQR.text = getString(R.string.visitante_qr, nombre)
                    binding.tvAutorizadoQR.text = getString(R.string.autorizado_por, nombreAutorizador)
                    binding.tvUnidadQR.text = getString(R.string.unidad_info, unidadAutorizador)
                    binding.tvVigenciaQR.text = getString(R.string.vigencia_hoy, hoy)

                    Toast.makeText(this@GenerarQRActivity, R.string.qr_generado_exito, Toast.LENGTH_SHORT).show()
                }.onFailure { e ->
                    binding.progressBar.visibility = View.GONE
                    binding.btnGenerarQR.isEnabled = true
                    ErrorHandler.mostrar(this@GenerarQRActivity, e, "guardarVisitante")
                }

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnGenerarQR.isEnabled = true
                ErrorHandler.mostrar(this@GenerarQRActivity, e, "generarBitmapQR")
            }
        }
    }
}
