package com.example.adminiums1.ui.residente

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adminiums1.R
import com.example.adminiums1.databinding.ActivityEstadoCuentaBinding
import com.example.adminiums1.model.Pago
import com.example.adminiums1.model.Usuario
import com.example.adminiums1.repository.FirebaseRepository
import com.example.adminiums1.ui.admin.adapter.PagosDetalleAdapter
import com.example.adminiums1.utils.PaymentUtils
import com.example.adminiums1.utils.PdfGenerator
import com.example.adminiums1.utils.formatearPeso
import com.example.adminiums1.utils.mostrar
import com.example.adminiums1.utils.ocultar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class EstadoCuentaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEstadoCuentaBinding
    private val repo = FirebaseRepository()
    private lateinit var adapter: PagosDetalleAdapter
    private var usuarioCargado: Usuario? = null
    private var listaPagos: List<Pago> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEstadoCuentaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        
        val uidExterno = intent.getStringExtra("uid_residente")
        cargarDatos(uidExterno)
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        adapter = PagosDetalleAdapter()
        binding.rvHistorialPagos.layoutManager = LinearLayoutManager(this)
        binding.rvHistorialPagos.adapter = adapter

        binding.btnDownloadPdf.setOnClickListener {
            usuarioCargado?.let { user ->
                PdfGenerator.generarEstadoCuentaPDF(this, user, listaPagos)
            }
        }
    }

    private fun cargarDatos(uidExterno: String?) {
        val uid = uidExterno ?: repo.getCurrentUid() ?: return
        binding.progressBar.mostrar()

        CoroutineScope(Dispatchers.IO).launch {
            val userResult = repo.getUsuario(uid)
            val user = userResult.getOrNull()
            val pagosResult = repo.getHistorialPagosUsuario(uid)
            // FIX 4: Fetch building name
            val buildResult = user?.let { repo.getCondominio(it.edificioId) }
            
            withContext(Dispatchers.Main) {
                binding.progressBar.ocultar()
                val pagos = pagosResult.getOrDefault(emptyList())
                usuarioCargado = user
                listaPagos = pagos

                user?.let { u ->
                    val nombreEdificio = buildResult?.nombre ?: u.edificioId
                    binding.tvResidenteNombre.text = u.nombre
                    binding.tvResidenteUnidadEdificio.text = "${u.unidad} - $nombreEdificio"
                    
                    binding.tvBalanceMonto.text = u.balance.formatearPeso()
                    binding.tvBalanceMonto.setTextColor(if (u.balance >= 0) ContextCompat.getColor(this@EstadoCuentaActivity, R.color.colorSuccess) else ContextCompat.getColor(this@EstadoCuentaActivity, R.color.colorError))

                    binding.tvProximaCuotaMonto.text = u.proximoPago.formatearPeso()
                    
                    val mesActual = SimpleDateFormat("MMMM yyyy", Locale("es", "MX"))
                        .format(Date()).replaceFirstChar { it.uppercase() }
                    val estado = PaymentUtils.obtenerEstadoVencimiento(u.fechaVencimiento)
                    binding.chipFechaVencimiento.text = "$mesActual • $estado"
                    
                    if (PaymentUtils.calcularDiasRestantes(u.fechaVencimiento) < 0) {
                        binding.chipFechaVencimiento.setChipBackgroundColorResource(R.color.colorErrorBg)
                        binding.chipFechaVencimiento.setTextColor(ContextCompat.getColor(this@EstadoCuentaActivity, R.color.colorError))
                    } else {
                        binding.chipFechaVencimiento.setChipBackgroundColorResource(R.color.colorSuccessBg)
                        binding.chipFechaVencimiento.setTextColor(ContextCompat.getColor(this@EstadoCuentaActivity, R.color.colorSuccess))
                    }

                    val pagosAnio = pagos.filter { p -> p.fecha.contains(Calendar.getInstance().get(Calendar.YEAR).toString()) && p.estado == "Aprobado" }
                    binding.tvTotalAnualMonto.text = pagosAnio.sumOf { p -> p.monto }.formatearPeso()
                }

                binding.tvFechaGeneracion.text = "Generado el: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}"
                
                // Grouping history by month
                if (pagos.isNotEmpty()) {
                    val grouped = mutableListOf<Any>()
                    val pagosOrdenados = pagos.sortedByDescending { it.timestamp }
                    val porMes = pagosOrdenados.groupBy { it.fecha.substring(3, 10) } // "MM/yyyy"
                    
                    porMes.forEach { (mesAnio, pagosMes) ->
                        try {
                            val parts = mesAnio.split("/")
                            val cal = Calendar.getInstance().apply {
                                set(Calendar.DAY_OF_MONTH, 1)
                                set(Calendar.MONTH, parts[0].toInt() - 1)
                                set(Calendar.YEAR, parts[1].toInt())
                            }
                            val nombreMes = SimpleDateFormat("MMMM yyyy", Locale("es", "MX"))
                                .format(cal.time).replaceFirstChar { it.uppercase() }
                            grouped.add(nombreMes)
                        } catch (e: Exception) {
                            grouped.add(mesAnio)
                        }
                        grouped.addAll(pagosMes)
                    }
                    adapter.setDatos(grouped)
                } else {
                    adapter.setDatos(emptyList())
                }
            }
        }
    }
}
