package com.example.adminiums1.ui.residente

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.adminiums1.databinding.ActivityGenerarQrBinding
import com.example.adminiums1.repository.FirebaseRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GenerarQRActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGenerarQrBinding
    private val repo = FirebaseRepository()
    private val db = FirebaseFirestore.getInstance()
    private var qrGenerado: String = ""
    private var bmpQR: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGenerarQrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        // Validar sesión al inicio
        val uid = repo.getCurrentUid()
        if (uid == null) {
            Toast.makeText(this, "No se encontró sesión activa", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Cargar datos iniciales del usuario
        cargarDatosUsuario(uid)

        binding.btnGenerarQR.setOnClickListener {
            val nombre = binding.etNombreVisitante.text.toString().trim()
            if (nombre.isEmpty()) {
                binding.etNombreVisitante.error = "Ingresa el nombre del visitante"
                return@setOnClickListener
            }

            generarYGuardarQR(nombre, uid)
        }

        binding.btnCompartir.setOnClickListener {
            if (qrGenerado.isNotEmpty()) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Acceso Adminiums para: $qrGenerado\n\nMuestra este código al llegar."
                    )
                }
                startActivity(Intent.createChooser(shareIntent, "Compartir código"))
            }
        }
    }

    private fun cargarDatosUsuario(uid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val usuario = repo.getUsuario(uid)
                // Usamos Locale("es", "MX") directamente para compatibilidad o Locale.ROOT
                val localeMX = Locale("es", "MX")
                val hoy = SimpleDateFormat("d MMM yyyy", localeMX).format(Date())

                withContext(Dispatchers.Main) {
                    usuario?.let {
                        binding.tvAutorizadoPor.text = "Autorizado por: ${it.nombre}"
                        binding.tvUnidadInfo.text = "Unidad: ${it.unidad}"
                        binding.tvVigencia.text = "Vigencia: Hoy $hoy"
                    }
                }
            } catch (e: Exception) {
                Log.e("QR_DEBUG", "Error cargando usuario: ${e.message}")
            }
        }
    }

    private fun generarYGuardarQR(nombre: String, uid: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnGenerarQR.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val usuario = repo.getUsuario(uid)
                val localeMX = Locale("es", "MX")
                val hoy = SimpleDateFormat("d MMM yyyy", localeMX).format(Date())
                val timestamp = System.currentTimeMillis()

                // Contenido del QR
                val qrContent =
                    "ADMINIUMS|$nombre|${usuario?.nombre ?: "Residente"}|${usuario?.unidad ?: "N/A"}|$hoy|$timestamp"

                // 1. GENERAR QR USANDO BARCODE ENCODER
                val barcodeEncoder = BarcodeEncoder()
                val bitmap = barcodeEncoder.encodeBitmap(qrContent, BarcodeFormat.QR_CODE, 512, 512)

                // 2. GUARDAR EN FIRESTORE
                val docRef = db.collection("visitantes").document()
                val visitante = hashMapOf(
                    "id" to docRef.id,
                    "nombre" to nombre,
                    "autorizadoPor" to (usuario?.nombre ?: "Residente"),
                    "unidad" to (usuario?.unidad ?: "N/A"),
                    "vigencia" to hoy,
                    "qrCode" to qrContent,
                    "timestamp" to timestamp,
                    "validado" to false
                )
                docRef.set(visitante).await()

                // 3. ACTUALIZAR UI
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnGenerarQR.isEnabled = true

                    qrGenerado = nombre
                    bmpQR = bitmap

                    binding.ivQR.setImageBitmap(bitmap)
                    binding.layoutQRResult.visibility = View.VISIBLE

                    binding.tvVisitanteQR.text = "Visitante: $nombre"
                    binding.tvAutorizadoQR.text =
                        "Autorizado por: ${usuario?.nombre ?: "Residente"}"
                    binding.tvUnidadQR.text = "Unidad: ${usuario?.unidad ?: "N/A"}"
                    binding.tvVigenciaQR.text = "Vigencia: Hoy $hoy"

                    Toast.makeText(
                        this@GenerarQRActivity,
                        "QR Generado con éxito",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Log.e("QR_DEBUG", "Error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnGenerarQR.isEnabled = true
                    Toast.makeText(
                        this@GenerarQRActivity,
                        "Error al generar: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}