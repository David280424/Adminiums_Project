package com.example.adminiums1.ui.residente

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.adminiums1.databinding.ActivityGenerarQrBinding
import com.example.adminiums1.model.Visitante
import com.example.adminiums1.repository.FirebaseRepository
import com.example.adminiums1.utils.QRUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class GenerarQRActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGenerarQrBinding
    private val repo = FirebaseRepository()
    private var qrGenerado: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGenerarQrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        val uid = repo.getCurrentUid() ?: return
        CoroutineScope(Dispatchers.Main).launch {
            val usuario = repo.getUsuario(uid)
            usuario?.let {
                binding.tvAutorizadoPor.text = "Autorizado por: ${it.nombre}"
                binding.tvUnidadInfo.text = "Unidad: ${it.unidad}"
                val hoy = SimpleDateFormat("d MMM yyyy", Locale("es", "MX")).format(Date())
                binding.tvVigencia.text = "Vigencia: Hoy $hoy"
            }
        }

        binding.btnGenerarQR.setOnClickListener {
            val nombre = binding.etNombreVisitante.text.toString().trim()
            if (nombre.isEmpty()) {
                Toast.makeText(this, "Ingresa el nombre del visitante", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.progressBar.visibility = View.VISIBLE
            binding.btnGenerarQR.isEnabled = false

            val uid2 = repo.getCurrentUid() ?: return@setOnClickListener

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val usuario = repo.getUsuario(uid2)
                    val hoy = SimpleDateFormat("d MMM yyyy", Locale("es", "MX")).format(Date())

                    val qrContent = QRUtils.generateQRContent(
                        nombre,
                        usuario?.nombre ?: "",
                        usuario?.unidad ?: "",
                        hoy
                    )

                    val visitante = Visitante(
                        nombre = nombre,
                        autorizadoPor = usuario?.nombre ?: "",
                        unidad = usuario?.unidad ?: "",
                        vigencia = hoy,
                        qrCode = qrContent,
                        timestamp = System.currentTimeMillis()
                    )

                    val success = repo.crearVisitante(visitante)

                    // Generar bitmap en hilo de fondo
                    val bmp: Bitmap = withContext(Dispatchers.Default) {
                        QRUtils.generateQR(qrContent)
                    }

                    binding.progressBar.visibility = View.GONE
                    binding.btnGenerarQR.isEnabled = true

                    if (success) {
                        qrGenerado = qrContent
                        binding.ivQR.setImageBitmap(bmp)
                        binding.layoutQRResult.visibility = View.VISIBLE
                        binding.tvVisitanteQR.text = "Visitante: $nombre"
                        binding.tvAutorizadoQR.text = "Autorizado por: ${usuario?.nombre}"
                        binding.tvUnidadQR.text = "Unidad: ${usuario?.unidad}"
                        binding.tvVigenciaQR.text = "Vigencia: Hoy $hoy"
                    } else {
                        Toast.makeText(this@GenerarQRActivity,
                            "Error al guardar en base de datos", Toast.LENGTH_SHORT).show()
                    }

                } catch (e: Exception) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnGenerarQR.isEnabled = true
                    Toast.makeText(this@GenerarQRActivity,
                        "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.btnCompartir.setOnClickListener {
            if (qrGenerado.isEmpty()) return@setOnClickListener
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Código de acceso Adminiums: $qrGenerado")
            }
            startActivity(Intent.createChooser(shareIntent, "Compartir QR"))
        }
    }
}