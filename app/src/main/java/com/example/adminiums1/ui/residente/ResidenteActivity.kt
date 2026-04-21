package com.example.adminiums1.ui.residente

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.adminiums1.databinding.ActivityResidenteBinding
import com.example.adminiums1.model.Usuario
import com.example.adminiums1.repository.FirebaseRepository
import com.example.adminiums1.ui.auth.RolSelectorActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResidenteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResidenteBinding
    private val repo = FirebaseRepository()
    private var usuarioActual: Usuario? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResidenteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        cargarDatos()
    }

    private fun setupUI() {
        binding.btnGenerarQR.setOnClickListener {
            startActivity(Intent(this, GenerarQRActivity::class.java))
        }
        binding.btnReservar.setOnClickListener {
            startActivity(Intent(this, ReservarAmenidadActivity::class.java))
        }
        binding.btnPagar.setOnClickListener {
            startActivity(Intent(this, PagarCuotaActivity::class.java))
        }
        
        // --- FUNCIONALIDAD AÑADIR FONDOS (USUARIO) ---
        binding.btnRecargarFondos.setOnClickListener {
            mostrarDialogRecarga()
        }

        binding.btnLogout.setOnClickListener {
            repo.logout()
            startActivity(Intent(this, RolSelectorActivity::class.java))
            finishAffinity()
        }
    }

    private fun mostrarDialogRecarga() {
        val usuario = usuarioActual ?: return
        val input = android.widget.EditText(this)
        input.hint = "Monto a recargar (ej: 500)"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        
        AlertDialog.Builder(this)
            .setTitle("💰 Recargar Saldo")
            .setMessage("Ingresa el monto que deseas añadir a tu cuenta de Adminiums")
            .setView(input)
            .setPositiveButton("Recargar") { _, _ ->
                val monto = input.text.toString().toDoubleOrNull() ?: 0.0
                if (monto > 0) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val exito = repo.recargarBalance(usuario.uid, monto, usuario.edificioId)
                        withContext(Dispatchers.Main) {
                            if (exito) {
                                delay(500)
                                cargarDatos()
                                Toast.makeText(this@ResidenteActivity, "¡Saldo actualizado con éxito!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        cargarDatos()
    }

    private fun cargarDatos() {
        val uid = repo.getCurrentUid() ?: return
        CoroutineScope(Dispatchers.Main).launch {
            val usuario = repo.getUsuario(uid)
            usuarioActual = usuario
            usuario?.let {
                binding.tvBienvenido.text = "Hola, ${it.nombre}"
                binding.tvUnidad.text = "Unidad ${it.unidad} • ${it.edificioId ?: "General"}"
                binding.tvBalance.text = "$ ${"%.2f".format(it.balance)}"
            }
        }
    }
}
