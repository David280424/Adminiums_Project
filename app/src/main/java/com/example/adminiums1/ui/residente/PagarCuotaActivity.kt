package com.example.adminiums1.ui.residente

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.adminiums1.databinding.ActivityPagarCuotaBinding
import com.example.adminiums1.model.Pago
import com.example.adminiums1.repository.FirebaseRepository
import com.example.adminiums1.utils.ErrorHandler
import com.example.adminiums1.utils.mostrar
import com.example.adminiums1.utils.ocultar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PagarCuotaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPagarCuotaBinding
    private val repo = FirebaseRepository()

    private var nombreResidente = ""
    private var unidadResidente = ""
    private var montoPagado     = 0.0
    private var folioRecibo     = ""
    private var fechaRecibo     = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPagarCuotaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.layoutRecibo.ocultar()
        cargarDatos()

        binding.btnPagar.setOnClickListener          { procesarPago() }
        binding.btnCompartirImagen.setOnClickListener { compartirImagen() }
        binding.btnDescargarPDF.setOnClickListener    { generarYCompartirPDF() }
    }

    // ── Cargar datos del residente ──────────────────────────────────────────

    private fun cargarDatos() {
        val uid = repo.getCurrentUid() ?: return
        CoroutineScope(Dispatchers.Main).launch {
            val usuario = repo.getUsuario(uid)
            usuario?.let {
                nombreResidente = it.nombre
                unidadResidente = it.unidad
                montoPagado     = it.proximoPago
                binding.tvNombreResidente.text = it.nombre
                binding.tvUnidadResidente.text = "Unidad: ${it.unidad}"
                binding.tvMontoCuota.text      = "$ ${"%.2f".format(it.proximoPago)}"
                binding.tvBalanceActual.text   = "Balance actual: $ ${"%.2f".format(it.balance)}"
                binding.tvFechaVence.text      = "Vence: ${it.fechaVencimiento}"
            }
        }
    }

    // ── Procesar pago ───────────────────────────────────────────────────────

    private fun procesarPago() {
        binding.progressBar.mostrar()
        binding.btnPagar.isEnabled = false
        val uid = repo.getCurrentUid() ?: return

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val usuario = repo.getUsuario(uid)
                fechaRecibo = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                folioRecibo = "ADM-${System.currentTimeMillis().toString().takeLast(8)}"

                val pago = Pago(
                    residenteUid = uid,
                    monto        = usuario?.proximoPago ?: 0.0,
                    fecha        = fechaRecibo,
                    estado       = "pagado"
                )
                val ok = repo.registrarPago(pago)
                binding.progressBar.ocultar()

                if (ok) mostrarRecibo()
                else {
                    binding.btnPagar.isEnabled = true
                    Toast.makeText(this@PagarCuotaActivity, "Error al procesar pago", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.progressBar.ocultar()
                binding.btnPagar.isEnabled = true
                ErrorHandler.mostrar(this@PagarCuotaActivity, e, "PagarCuotaActivity")
            }
        }
    }

    // ── Mostrar recibo en pantalla ──────────────────────────────────────────

    private fun mostrarRecibo() {
        binding.btnPagar.ocultar()
        binding.layoutRecibo.mostrar()
        binding.tvReciboNombre.text = nombreResidente
        binding.tvReciboUnidad.text = "Unidad: $unidadResidente"
        binding.tvReciboMonto.text  = "$ ${"%.2f".format(montoPagado)}"
        binding.tvReciboFecha.text  = "Fecha: $fechaRecibo"
        binding.tvReciboFolio.text  = "Folio: $folioRecibo"
        binding.tvReciboEstado.text = "✅ PAGO CONFIRMADO"
    }

    // ── Compartir como imagen (WhatsApp) ────────────────────────────────────

    private fun compartirImagen() {
        val card = binding.cardRecibo
        val bmp  = Bitmap.createBitmap(card.width, card.height, Bitmap.Config.ARGB_8888)
        Canvas(bmp).also { card.draw(it) }

        val file = File(cacheDir, "recibo_adminiums.png")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
        startActivity(Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Recibo de pago — Adminiums\nFolio: $folioRecibo")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Compartir recibo"
        ))
    }

    // ── Generar y compartir PDF ─────────────────────────────────────────────

    private fun generarYCompartirPDF() {
        try {
            val document = PdfDocument()
            val page = document.startPage(
                PdfDocument.PageInfo.Builder(595, 842, 1).create()
            )
            val canvas = page.canvas
            val paint  = Paint()

            // Fondo blanco
            paint.color = Color.WHITE
            canvas.drawRect(0f, 0f, 595f, 842f, paint)

            // Header azul
            paint.color = Color.parseColor("#2D3748")
            canvas.drawRect(0f, 0f, 595f, 120f, paint)

            paint.color = Color.WHITE
            paint.textSize = 26f
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("ADMINIUMS", 40f, 60f, paint)

            paint.textSize = 13f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("Comprobante de Pago", 40f, 90f, paint)

            // Estado
            paint.color = Color.parseColor("#48BB78")
            paint.textSize = 18f
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("✓ PAGO CONFIRMADO", 40f, 170f, paint)

            // Línea
            paint.color = Color.parseColor("#E2E8F0")
            paint.strokeWidth = 1f
            canvas.drawLine(40f, 190f, 555f, 190f, paint)

            // Datos
            val etiquetas = listOf("Residente:", "Unidad:", "Monto:", "Fecha:", "Folio:")
            val valores   = listOf(
                nombreResidente,
                unidadResidente,
                "$ ${"%.2f".format(montoPagado)}",
                fechaRecibo,
                folioRecibo
            )

            paint.typeface = Typeface.DEFAULT
            etiquetas.forEachIndexed { i, etiqueta ->
                val y = 240f + (i * 45f)
                paint.color = Color.parseColor("#718096")
                paint.textSize = 13f
                canvas.drawText(etiqueta, 40f, y, paint)

                paint.color = Color.parseColor("#2D3748")
                paint.textSize = 15f
                paint.typeface = Typeface.DEFAULT_BOLD
                canvas.drawText(valores[i], 200f, y, paint)
                paint.typeface = Typeface.DEFAULT
            }

            // Footer
            paint.color = Color.parseColor("#A0AEC0")
            paint.textSize = 11f
            canvas.drawText("Generado por Adminiums — Sistema de Administración de Condominios", 40f, 800f, paint)

            document.finishPage(page)

            // Guardar en Downloads
            val fileName = "recibo_$folioRecibo.pdf"
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            document.writeTo(file.outputStream())
            document.close()

            // Compartir
            val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
            startActivity(Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "Compartir PDF"
            ))

        } catch (e: Exception) {
            ErrorHandler.mostrar(this, e, "generarPDF")
        }
    }
}
