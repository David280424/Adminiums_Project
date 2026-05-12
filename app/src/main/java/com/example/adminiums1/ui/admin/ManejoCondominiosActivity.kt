package com.example.adminiums1.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adminiums1.R
import com.example.adminiums1.databinding.ActivityManejoCondominiosBinding
import com.example.adminiums1.databinding.DialogAddCondominioBinding
import com.example.adminiums1.model.Condominio
import com.example.adminiums1.repository.FirebaseRepository
import com.example.adminiums1.ui.admin.adapter.CondominiosAdapter
import kotlinx.coroutines.*
import java.util.UUID

class ManejoCondominiosActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManejoCondominiosBinding
    private val repo = FirebaseRepository()
    private lateinit var adapter: CondominiosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManejoCondominiosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        cargarCondominios()
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        adapter = CondominiosAdapter { condominio ->
            confirmarEliminar(condominio)
        }
        binding.rvCondominios.layoutManager = LinearLayoutManager(this)
        binding.rvCondominios.adapter = adapter

        binding.btnAgregarCondominio.setOnClickListener { mostrarDialogAgregar() }
    }

    private fun confirmarEliminar(condominio: Condominio) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Condominio")
            .setMessage("¿Estás seguro de eliminar '${condominio.nombre}'? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarCondominio(condominio.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarCondominio(id: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val exito = repo.eliminarCondominio(id)
            withContext(Dispatchers.Main) {
                if (exito) {
                    Toast.makeText(this@ManejoCondominiosActivity, "Condominio eliminado", Toast.LENGTH_SHORT).show()
                    cargarCondominios()
                } else {
                    Toast.makeText(this@ManejoCondominiosActivity, "Error al eliminar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun cargarCondominios() {
        CoroutineScope(Dispatchers.IO).launch {
            val result = repo.getCondominios()
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    val lista = result.getOrDefault(emptyList())
                    adapter.setDatos(lista)
                } else {
                    Toast.makeText(this@ManejoCondominiosActivity, "Error al cargar edificios", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun mostrarDialogAgregar() {
        val dialogBinding = DialogAddCondominioBinding.inflate(LayoutInflater.from(this))

        AlertDialog.Builder(this)
            .setTitle("Nuevo Condominio")
            .setView(dialogBinding.root)
            .setPositiveButton("Guardar") { _, _ ->
                val nom = dialogBinding.etNombreCondominio.text.toString().trim()
                val ciu = dialogBinding.etCiudadCondominio.text.toString().trim()
                if (nom.isNotEmpty()) {
                    guardarCondominio(nom, ciu)
                } else {
                    Toast.makeText(this, "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun guardarCondominio(nombre: String, ciudad: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val id = UUID.randomUUID().toString()
            val nuevo = Condominio(id = id, nombre = nombre, ciudad = ciudad)
            val exito = repo.actualizarCondominio(nuevo)
            withContext(Dispatchers.Main) {
                if (exito) {
                    Toast.makeText(this@ManejoCondominiosActivity, "Edificio '$nombre' guardado", Toast.LENGTH_SHORT).show()
                    cargarCondominios()
                } else {
                    Toast.makeText(this@ManejoCondominiosActivity, "Error al guardar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
