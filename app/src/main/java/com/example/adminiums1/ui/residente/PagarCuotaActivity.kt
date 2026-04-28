package com.example.adminiums1.ui.residente

import android.animation.ObjectAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adminiums1.R
import com.example.adminiums1.databinding.ActivityPagarCuotaBinding
import com.example.adminiums1.model.Pago
import com.example.adminiums1.repository.FirebaseRepository
import com.example.adminiums1.ui.admin.adapter.PagosDetalleAdapter
import com.example.adminiums1.utils.PdfGenerator
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class PagarCuotaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPagarCuotaBinding
    private val repo = FirebaseRepository()
    private lateinit var adapterHistorial: PagosDetalleAdapter
    
    private var buildingId = ""
    private var buildingName = ""
    private var residentName = ""
    private var unit = ""
    private var amount = 0.0
    private var selectedMethod = ""
    
    private var bankDetails = ""

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

        binding.btnBackResumen.setOnClickListener { finish() }
        binding.btnBackPaso2.setOnClickListener { binding.viewFlipper.displayedChild = 0 }
        
        binding.btnIrAPaso2.setOnClickListener { binding.viewFlipper.displayedChild = 1 }

        // Selección de métodos
        binding.methodOxxo.setOnClickListener { selectMethod("OXXO") }
        binding.methodTransfer.setOnClickListener { selectMethod("Transferencia") }
        binding.methodCard.setOnClickListener { selectMethod("Tarjeta") }
        binding.methodCash.setOnClickListener { selectMethod("Efectivo") }

        binding.btnCopyRef.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Referencia OXXO", binding.tvOxxoRef.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Referencia copiada", Toast.LENGTH_SHORT).show()
        }

        binding.etCardNumber.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val digits = s.toString().replace(" ", "")
                if (digits.isNotEmpty()) {
                    if (digits.startsWith("4")) {
                        binding.etCardNumber.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.bg_avatar_blue, 0) // Simular logo Visa
                    } else if (digits.startsWith("5")) {
                        binding.etCardNumber.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.bg_avatar_red, 0) // Simular logo MC
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.btnConfirmarPago.setOnClickListener {
            confirmarYProcesar()
        }

        binding.btnFinalizar.setOnClickListener { finish() }
    }

    private fun selectMethod(method: String) {
        selectedMethod = method
        resetMethodStyles()
        
        binding.layoutFormOxxo.visibility = View.GONE
        binding.layoutFormTransfer.visibility = View.GONE
        binding.layoutFormCard.visibility = View.GONE
        binding.btnConfirmarPago.visibility = View.VISIBLE

        when(method) {
            "OXXO" -> {
                binding.methodOxxo.strokeWidth = 4
                binding.layoutFormOxxo.visibility = View.VISIBLE
                generarReferenciaOxxo()
            }
            "Transferencia" -> {
                binding.methodTransfer.strokeWidth = 4
                binding.layoutFormTransfer.visibility = View.VISIBLE
                binding.tvTransferDetails.text = bankDetails.ifEmpty { "Consulta los datos con tu administrador" }
            }
            "Tarjeta" -> {
                binding.methodCard.strokeWidth = 4
                binding.layoutFormCard.visibility = View.VISIBLE
            }
            "Efectivo" -> {
                binding.methodCash.strokeWidth = 4
            }
        }
    }

    private fun resetMethodStyles() {
        binding.methodOxxo.strokeWidth = 1
        binding.methodTransfer.strokeWidth = 1
        binding.methodCard.strokeWidth = 1
        binding.methodCash.strokeWidth = 1
    }

    private fun generarReferenciaOxxo() {
        val uid = repo.getCurrentUid() ?: ""
        val ref = "2706" + buildingId.take(4).padEnd(4, '0') + uid.take(4) + amount.toLong().toString().padStart(6, '0')
        binding.tvOxxoRef.text = ref.chunked(4).joinToString(" ")
        
        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(ref, BarcodeFormat.CODE_128, 600, 150)
            binding.ivBarcode.setImageBitmap(bitmap)
        } catch (e: Exception) { }
    }

    private fun cargarDatos() {
        val uid = repo.getCurrentUid() ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val user = repo.getUsuario(uid)
            val build = user?.edificioId?.let { repo.getCondominio(it) }
            val logs = repo.getHistorialPagosUsuario(uid).take(3)
            
            withContext(Dispatchers.Main) {
                user?.let {
                    residentName = it.nombre
                    unit = it.unidad
                    amount = it.proximoPago
                    buildingId = it.edificioId
                    binding.tvResumenNombre.text = it.nombre
                    binding.tvResumenUnidadEdificio.text = "${it.unidad} • ${build?.nombre ?: ""}"
                    binding.tvMontoGrande.text = "$ ${"%.2f".format(it.proximoPago)}"
                    binding.tvBalanceActual.text = "Balance actual: $ ${"%.2f".format(it.balance)}"
                    
                    if (it.balance < 0) {
                        binding.chipVencimiento.text = "Adeudo pendiente"
                        binding.chipVencimiento.setChipBackgroundColorResource(R.color.colorErrorBg)
                    } else {
                        binding.chipVencimiento.text = "Al corriente"
                        binding.chipVencimiento.setChipBackgroundColorResource(R.color.colorSuccessBg)
                    }
                }
                adapterHistorial.setDatos(logs)
            }
        }
    }

    private fun confirmarYProcesar() {
        val methodText = when(selectedMethod) {
            "OXXO" -> "Pago en OXXO"
            "Tarjeta" -> "Tarjeta de Crédito"
            "Transferencia" -> "Transferencia SPEI"
            else -> "Efectivo Directo"
        }

        AlertDialog.Builder(this)
            .setTitle("Confirmar Registro")
            .setMessage("¿Deseas registrar este pago por $ ${"%.2f".format(amount)} mediante $methodText?")
            .setPositiveButton("Confirmar") { _, _ -> ejecutarRegistro() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun ejecutarRegistro() {
        val uid = repo.getCurrentUid() ?: return
        binding.progressBar.visibility = View.VISIBLE
        
        CoroutineScope(Dispatchers.IO).launch {
            val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            val status = if (selectedMethod == "Tarjeta" || selectedMethod == "OXXO") "Aprobado" else "Pendiente verificación"
            
            val reference = when(selectedMethod) {
                "OXXO" -> binding.tvOxxoRef.text.toString().replace(" ", "")
                "Transferencia" -> binding.etTransferRef.text.toString()
                else -> ""
            }

            val p = Pago(
                residenteUid = uid,
                edificioId = buildingId,
                monto = amount,
                metodoPago = selectedMethod,
                referencia = reference,
                fecha = fecha,
                estado = status,
                concepto = "Cuota mensual - " + SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
            )

            val ok = repo.registrarPago(p)
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                if (ok) mostrarPaso3(p)
                else Toast.makeText(this@PagarCuotaActivity, "Error al procesar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarPaso3(p: Pago) {
        binding.viewFlipper.displayedChild = 2
        
        binding.ivCheckAnim.scaleX = 0f
        binding.ivCheckAnim.scaleY = 0f
        ObjectAnimator.ofFloat(binding.ivCheckAnim, "scaleX", 1f).setDuration(500).start()
        ObjectAnimator.ofFloat(binding.ivCheckAnim, "scaleY", 1f).setDuration(500).start()

        binding.tvReciboFolio.text = "Folio: ADM-${System.currentTimeMillis().toString().takeLast(8)}"
        binding.tvReciboFecha.text = p.fecha
        binding.tvReciboResidente.text = residentName
        binding.tvReciboMonto.text = "$ ${"%.2f".format(p.monto)}"
        binding.tvReciboMetodo.text = "Método: ${p.metodoPago}"
        binding.tvReciboEstado.text = p.estado.uppercase()
    }
}
