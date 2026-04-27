package com.example.adminiums1.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adminiums1.databinding.ActivityAmenidadesAdminBinding
import com.example.adminiums1.model.Reservacion
import com.example.adminiums1.repository.FirebaseRepository
import com.example.adminiums1.ui.admin.adapter.ReservacionesDetalleAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AmenidadesAdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAmenidadesAdminBinding
    private val repo = FirebaseRepository()
    private lateinit var adapter: ReservacionesDetalleAdapter
    private var edificioId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAmenidadesAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        edificioId = intent.getStringExtra("edificioId") ?: ""
        
        setupUI()
        cargarReservaciones("Todas")
    }

    private fun setupUI() {
        // En el XML el id es 'toolbar'
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        adapter = ReservacionesDetalleAdapter { reservacion ->
            confirmarCancelacion(reservacion)
        }
        
        binding.rvReservaciones.layoutManager = LinearLayoutManager(this)
        binding.rvReservaciones.adapter = adapter
        
        binding.chipHoy.setOnClickListener { cargarReservaciones("Hoy") }
        binding.chipSemana.setOnClickListener { cargarReservaciones("Semana") }
        binding.chipTodas.setOnClickListener { cargarReservaciones("Todas") }
    }

    private fun cargarReservaciones(filtro: String) {
        CoroutineScope(Dispatchers.IO).launch {
            var lista = repo.getReservacionesPorEdificio(edificioId)
            
            val hoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            
            lista = when(filtro) {
                "Hoy" -> lista.filter { it.fecha == hoy }
                else -> lista
            }

            withContext(Dispatchers.Main) {
                adapter.setDatos(lista)
                binding.layoutEmpty.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun confirmarCancelacion(res: Reservacion) {
        AlertDialog.Builder(this)
            .setTitle("Cancelar Reservación")
            .setMessage("¿Deseas cancelar la reservación de ${res.amenidad} para ${res.residenteNombre}?")
            .setPositiveButton("Sí, cancelar") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    val ok = repo.eliminarReservacion(res.id)
                    withContext(Dispatchers.Main) {
                        if (ok) {
                            cargarReservaciones("Todas")
                            Toast.makeText(this@AmenidadesAdminActivity, "Reservación cancelada", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}
