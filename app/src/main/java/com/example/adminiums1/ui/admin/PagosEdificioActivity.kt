package com.example.adminiums1.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adminiums1.databinding.ActivityPagosEdificioBinding
import com.example.adminiums1.repository.FirebaseRepository
import com.example.adminiums1.ui.admin.adapter.PagosDetalleAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PagosEdificioActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPagosEdificioBinding
    private val repo = FirebaseRepository()
    private lateinit var adapter: PagosDetalleAdapter
    private var edificioId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPagosEdificioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        edificioId = intent.getStringExtra("edificioId") ?: ""
        
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        adapter = PagosDetalleAdapter()
        binding.rvPagos.layoutManager = LinearLayoutManager(this)
        binding.rvPagos.adapter = adapter
        
        cargarPagos()
    }

    private fun cargarPagos() {
        CoroutineScope(Dispatchers.IO).launch {
            val result = repo.getPagosPorEdificio(edificioId)
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    val lista = result.getOrDefault(emptyList())
                    adapter.setDatos(lista)
                    binding.layoutEmpty.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                    
                    val totalMes = lista.filter { it.estado == "Aprobado" }.sumOf { it.monto }
                    binding.tvTotalRecaudadoMes.text = "Total recaudado: $ ${"%.2f".format(totalMes)}"
                } else {
                    Toast.makeText(this@PagosEdificioActivity, "Error al cargar pagos", Toast.LENGTH_SHORT).show()
                    binding.layoutEmpty.visibility = View.VISIBLE
                }
            }
        }
    }
}
