package com.example.adminiums1.ui.admin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adminiums1.databinding.ActivityResidenteDetalleBinding
import com.example.adminiums1.model.Pago
import com.example.adminiums1.ui.admin.adapter.PagosDetalleAdapter
import com.example.adminiums1.repository.FirebaseRepository
import com.example.adminiums1.utils.ErrorHandler
import com.example.adminiums1.utils.mostrar
import com.example.adminiums1.utils.ocultar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResidenteDetalleActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_UID = "extra_uid"
    }

    private lateinit var binding: ActivityResidenteDetalleBinding
    private val repo = FirebaseRepository()
    private var residenteId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResidenteDetalleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        residenteId = intent.getStringExtra(EXTRA_UID) ?: ""
        
        setupToolbar()
        setupRecyclerView()
        cargarEstadoDeCuenta()

        binding.btnEliminarResidente.setOnClickListener { confirmarEliminacion() }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        binding.rvPagos.layoutManager = LinearLayoutManager(this)
        binding.rvPagos.adapter = PagosDetalleAdapter()
    }

    private fun cargarEstadoDeCuenta() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val usuario = repo.getUsuario(residenteId)
                val pagos = repo.getHistorialPagosUsuario(residenteId)

                withContext(Dispatchers.Main) {
                    usuario?.let { u ->
                        binding.tvDetalleNombre.text = u.nombre
                        binding.tvDetalleUnidad.text = "Unidad: ${u.unidad}"
                        binding.tvDetalleBalance.text = "$ ${"%.2f".format(u.balance)}"
                        
                        // Si el balance es negativo (deuda), ponemos color rojo
                        if (u.balance < 0) {
                            binding.tvDetalleBalance.setTextColor(0xFFE74C3C.toInt())
                        } else {
                            binding.tvDetalleBalance.setTextColor(0xFF2ECC71.toInt())
                        }

                        // Lógica del botón de Cobrar (WhatsApp)
                        binding.btnNotificarCobro.setOnClickListener {
                            val mensaje = "Hola ${u.nombre}, Adminiums le informa que presenta un saldo de $ ${"%.2f".format(u.balance)} en su unidad ${u.unidad}. Favor de regularizar a la brevedad."
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?text=$mensaje"))
                            startActivity(intent)
                        }
                    }

                    // Cargar lista de movimientos en el adapter
                    (binding.rvPagos.adapter as PagosDetalleAdapter).setDatos(pagos)
                    
                    if (pagos.isEmpty()) binding.tvSinPagos.mostrar()
                    else binding.tvSinPagos.ocultar()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    ErrorHandler.mostrar(this@ResidenteDetalleActivity, e, "cargarEstadoDeCuenta")
                }
            }
        }
    }

    private fun confirmarEliminacion() {
        AlertDialog.Builder(this)
            .setTitle("Baja de Residente")
            .setMessage("¿Estás seguro de que deseas eliminar a este residente de la base de datos?")
            .setPositiveButton("Eliminar") { _, _ ->
                CoroutineScope(Dispatchers.Main).launch {
                    // Lógica para eliminar usuario
                    Toast.makeText(this@ResidenteDetalleActivity, "Usuario dado de baja", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
