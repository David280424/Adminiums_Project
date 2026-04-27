package com.example.adminiums1.ui.residente

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adminiums1.R
import com.example.adminiums1.databinding.ActivityPagarCuotaBinding
import com.example.adminiums1.model.Pago
import com.example.adminiums1.repository.FirebaseRepository
import com.example.adminiums1.ui.admin.adapter.PagosDetalleAdapter
import com.example.adminiums1.utils.ErrorHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PagarCuotaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPagarCuotaBinding
    private val repo = FirebaseRepository()
    private lateinit var adapterHistorial: PagosDetalleAdapter
    
    private var nombreResidente = ""
    private var unidadResidente = ""
    private var edificioNombre = ""
    private var edificioId = ""
    private var montoAPagar = 0.0
    private var clabeCondominio = ""
    
    private var pagoFinal: Pago? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPagarCuotaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        cargarDatos()
    }

    private fun setupUI() {
        adapterHistorial = PagosDetalleAdapter()
        binding.rvUltimosPagos.layoutManager = LinearLayoutManager(this)
        binding.rvUltimosPagos.adapter = adapterHistorial

        // PASO 1 -> PASO 2
        binding.btnProcederPago.setOnClickListener {
            binding.viewFlipper.displayedChild = 1
        }

        // PASO 2 -> PASO 1
        binding.btnAtrasPaso1.setOnClickListener {
            binding.viewFlipper.displayedChild = 0
        }

        // Manejo de visibilidad de CLABE
        binding.rgMetodoPago.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbTransferencia) {
                binding.layoutTransferenciaInfo.visibility = View.VISIBLE
                binding.tvClabeCondominio.text = if (clabeCondominio.isNotEmpty()) "CLABE: $clabeCondominio" else "Consulta con administración para CLABE"
            } else {
                binding.layoutTransferenciaInfo.visibility = View.GONE
            }
        }

        // CONFIRMAR PAGO
        binding.btnConfirmarPago.setOnClickListener {
            val metodo = when (binding.rgMetodoPago.checkedRadioButtonId) {
                R.id.rbEfectivo -> "Efectivo"
                R.id.rbTransferencia -> "Transferencia"
                R.id.rbTarjeta -> "Tarjeta"
                else -> ""
            }

            if (metodo.isEmpty()) {
                Toast.makeText(this, "Selecciona un método de pago", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val ref = binding.etReferencia.text.toString().trim()
            if (metodo == "Transferencia" && ref.isEmpty()) {
                Toast.makeText(this, "Ingresa el número de referencia", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            mostrarDialogConfirmacion(metodo, ref)
        }

        binding.btnVolverInicio.setOnClickListener { finish() }
        
        binding.btnShareImg.setOnClickListener { compartirComoImagen() }
        binding.btnDownloadPdf.setOnClickListener { generarYCompartirPDF() }
    }

    private fun cargarDatos() {
        val uid = repo.getCurrentUid() ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val user = repo.getUsuario(uid)
            val building = user?.edificioId?.let { repo.getCondominio(it) }
            val historial = repo.getHistorialPagosUsuario(uid).take(3)

            withContext(Dispatchers.Main) {
                user?.let {
                    nombreResidente = it.nombre
                    unidadResidente = it.unidad
                    montoAPagar = it.proximoPago
                    edificioId = it.edificioId
                    
                    binding.tvResumenNombre.text = it.nombre
                    binding.tvResumenUnidad.text = "Unidad: ${it.unidad}"
                    binding.tvMontoGrande.text = "$ ${"%.2f".format(it.proximoPago)}"
                    binding.tvBalanceActual.text = "Balance: $ ${"%.2f".format(it.balance)}"
                    
                    if (it.balance < 0) {
                        binding.chipVencimiento.text = "Adeudo pendiente"
                        binding.chipVencimiento.setChipBackgroundColorResource(R.color.colorErrorBg)
                        binding.chipVencimiento.setTextColor(ContextCompat.getColor(this@PagarCuotaActivity, R.color.colorError))
                    } else {
                        binding.chipVencimiento.text = "Al corriente"
                        binding.chipVencimiento.setChipBackgroundColorResource(R.color.colorSuccessBg)
                        binding.chipVencimiento.setTextColor(ContextCompat.getColor(this@PagarCuotaActivity, R.color.colorSuccess))
                    }
                }
                
                building?.let {
                    edificioNombre = it.nombre
                    binding.tvResumenEdificio.text = it.nombre
                    // clabeCondominio = it.cuentaBancaria // Asumiendo que existe el campo
                }

                adapterHistorial.setDatos(historial)
            }
        }
    }

    private fun mostrarDialogConfirmacion(metodo: String, referencia: String) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Pago")
            .setMessage("Monto: $ ${"%.2f".format(montoAPagar)}\nMétodo: $metodo\nReferencia: ${referencia.ifEmpty { "N/A" }}\n\n¿Deseas registrar este pago?")
            .setPositiveButton("Confirmar") { _, _ -> procesarPagoFinal(metodo, referencia) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun procesarPagoFinal(metodo: String, referencia: String) {
        val uid = repo.getCurrentUid() ?: return
        binding.progressBar.visibility = View.VISIBLE
        binding.btnConfirmarPago.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            val pago = Pago(
                residenteUid = uid,
                edificioId = edificioId,
                monto = montoAPagar,
                metodoPago = metodo,
                referencia = referencia,
                concepto = "Cuota mensual - " + SimpleDateFormat("MMMM", Locale.getDefault()).format(Date()),
                fecha = fecha
            )

            val exito = repo.registrarPago(pago)
            
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                if (exito) {
                    // Obtener el pago guardado para el recibo (con el folio generado por el repo)
                    val ultimos = repo.getHistorialPagosUsuario(uid)
                    pagoFinal = ultimos.firstOrNull()
                    mostrarPaso3()
                } else {
                    binding.btnConfirmarPago.isEnabled = true
                    Toast.makeText(this@PagarCuotaActivity, "Error al registrar pago", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun mostrarPaso3() {
        binding.viewFlipper.displayedChild = 2
        
        // Animación de check
        binding.ivCheckAnim.scaleX = 0f
        binding.ivCheckAnim.scaleY = 0f
        ObjectAnimator.ofFloat(binding.ivCheckAnim, "scaleX", 1f).apply {
            duration = 500
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        ObjectAnimator.ofFloat(binding.ivCheckAnim, "scaleY", 1f).apply {
            duration = 500
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        pagoFinal?.let {
            binding.tvReciboFolio.text = "Folio: ${it.folio}"
            binding.tvReciboFecha.text = "Fecha: ${it.fecha}"
            binding.tvReciboDatoResidente.text = it.residenteNombre
            binding.tvReciboDatoEdificio.text = "${it.edificioNombre} • Unidad ${it.unidad}"
            binding.tvReciboMonto.text = "$ ${"%.2f".format(it.monto)}"
            binding.tvReciboMetodo.text = it.metodoPago
            binding.tvReciboConcepto.text = it.concepto
        }
    }

    private fun compartirComoImagen() {
        val card = binding.cardReciboFinal
        val bitmap = Bitmap.createBitmap(card.width, card.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        card.draw(canvas)

        val file = File(cacheDir, "recibo_pago.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Compartir Recibo"))
    }

    private fun generarYCompartirPDF() {
        val p = pagoFinal ?: return
        try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()

            // Header con gradiente
            paint.shader = android.graphics.LinearGradient(0f, 0f, 595f, 0f, Color.parseColor("#1A365D"), Color.parseColor("#2A4A7F"), android.graphics.Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, 595f, 150f, paint)
            paint.shader = null

            paint.color = Color.WHITE
            paint.textSize = 30f
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("RECIBO DE PAGO", 40f, 70f, paint)
            
            paint.textSize = 15f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("Adminiums - Gestión Residencial", 40f, 110f, paint)

            paint.color = Color.BLACK
            paint.textSize = 14f
            var y = 200f
            
            val lines = listOf(
                "Folio: ${p.folio}",
                "Fecha: ${p.fecha}",
                "------------------------------------------",
                "Residente: ${p.residenteNombre}",
                "Unidad: ${p.unidad}",
                "Edificio: ${p.edificioNombre}",
                "------------------------------------------",
                "Concepto: ${p.concepto}",
                "Método de Pago: ${p.metodoPago}",
                "Referencia: ${p.referencia.ifEmpty { "N/A" }}",
                "------------------------------------------",
                "MONTO TOTAL: $ ${"%.2f".format(p.monto)}"
            )

            for (line in lines) {
                canvas.drawText(line, 40f, y, paint)
                y += 30f
            }

            // Cuadro de QR vacío
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawRect(400f, 200f, 520f, 320f, paint)
            paint.style = Paint.Style.FILL
            paint.textSize = 10f
            canvas.drawText("Folio verificable", 415f, 335f, paint)

            document.finishPage(page)

            val file = File(getExternalFilesDir(null), "Recibo_${p.folio}.pdf")
            document.writeTo(file.outputStream())
            document.close()

            val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Descargar PDF"))

        } catch (e: Exception) {
            Toast.makeText(this, "Error al generar PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
