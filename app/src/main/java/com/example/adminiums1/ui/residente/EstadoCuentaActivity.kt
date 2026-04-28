package com.example.adminiums1.ui.residente

import android.content.Intent
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
import com.example.adminiums1.utils.PdfGenerator
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
        binding.btnBack.setOnClickListener { finish() }
        
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
        binding.progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            val user = repo.getUsuario(uid)
            val pagos = repo.getHistorialPagosUsuario(uid)
            
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                usuarioCargado = user
                listaPagos = pagos
                
                user?.let {
                    binding.tvResidenteNombre.text = it.nombre
                    binding.tvResidenteUnidadEdificio.text = "${it.unidad} - ${it.edificioId}"
                    binding.tvBalanceMonto.text = "$ ${"%.2f".format(it.balance)}"
                    binding.tvBalanceMonto.setTextColor(if (it.balance >= 0) ContextCompat.getColor(this@EstadoCuentaActivity, R.color.colorSuccess) else ContextCompat.getColor(this@EstadoCuentaActivity, R.color.colorError))
                    
                    binding.tvProximaCuotaMonto.text = "$ ${"%.2f".format(it.proximoPago)}"
                    binding.chipFechaVencimiento.text = it.fechaVencimiento.ifEmpty { "Sin fecha" }
                    
                    val pagosAnio = pagos.filter { p -> p.fecha.contains(Calendar.getInstance().get(Calendar.YEAR).toString()) && p.estado == "Aprobado" }
                    binding.tvTotalAnualMonto.text = "$ ${"%.2f".format(pagosAnio.sumOf { p -> p.monto })}"
                }

                binding.tvFechaGeneracion.text = "Generado el: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}"
                adapter.setDatos(pagos)
            }
        }
    }
}
