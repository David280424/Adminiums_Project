package com.example.adminiums1.ui.residente

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.adminiums1.databinding.ActivityResidenteBinding
import com.example.adminiums1.repository.FirebaseRepository
import com.example.adminiums1.ui.auth.RolSelectorActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ResidenteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResidenteBinding
    private val repo = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResidenteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cargarDatos()

        binding.btnGenerarQR.setOnClickListener {
            startActivity(Intent(this, GenerarQRActivity::class.java))
        }
        binding.btnReservar.setOnClickListener {
            startActivity(Intent(this, ReservarAmenidadActivity::class.java))
        }
        binding.btnPagar.setOnClickListener {
            startActivity(Intent(this, PagarCuotaActivity::class.java))
        }
        binding.btnLogout.setOnClickListener {
            repo.logout()
            startActivity(Intent(this, RolSelectorActivity::class.java))
            finishAffinity()
        }
    }

    override fun onResume() {
        super.onResume()
        cargarDatos()
    }

    private fun cargarDatos() {
        val uid = repo.getCurrentUid() ?: return
        CoroutineScope(Dispatchers.Main).launch {
            val usuario = repo.getUsuario(uid)
            usuario?.let {
                binding.tvBienvenido.text = "Bienvenido, ${it.nombre}"
                binding.tvUnidad.text = it.unidad
                binding.tvProximoPago.text = "$ ${"%.2f".format(it.proximoPago)}"
                binding.tvFechaVencimiento.text = it.fechaVencimiento
                binding.tvBalance.text = "$ ${"%.2f".format(it.balance)}"
            }
        }
    }
}
