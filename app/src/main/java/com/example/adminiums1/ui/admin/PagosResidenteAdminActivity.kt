package com.example.adminiums1.ui.admin

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adminiums1.databinding.ActivityPagosResidenteAdminBinding
import com.example.adminiums1.repository.FirebaseRepository
import com.example.adminiums1.ui.admin.adapter.PagosDetalleAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PagosResidenteAdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPagosResidenteAdminBinding
    private val repo = FirebaseRepository()
    private lateinit var adapter: PagosDetalleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPagosResidenteAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uid = intent.getStringExtra("uid") ?: ""
        val nombre = intent.getStringExtra("nombre") ?: "Residente"

        binding.toolbar.title = "Pagos: $nombre"
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = PagosDetalleAdapter()
        binding.rvPagos.layoutManager = LinearLayoutManager(this)
        binding.rvPagos.adapter = adapter

        cargarPagos(uid)
    }

    private fun cargarPagos(uid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val lista = repo.getHistorialPagosUsuario(uid)
            withContext(Dispatchers.Main) {
                adapter.setDatos(lista)
                binding.layoutEmpty.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
}
