package com.example.adminiums1.ui.residente

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.adminiums1.R
import com.example.adminiums1.databinding.ActivityResidenteBinding
import com.example.adminiums1.model.Usuario
import com.example.adminiums1.repository.FirebaseRepository
import com.example.adminiums1.ui.auth.LoginActivity
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
        
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorResidenteDark)

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
        
        binding.qaCardPagar.setOnClickListener {
            startActivity(Intent(this, PagarCuotaActivity::class.java))
        }

        binding.btnMiQR.setOnClickListener {
            startActivity(Intent(this, MiQRActivity::class.java))
        }
        
        binding.btnReportarIncidencia.setOnClickListener {
            startActivity(Intent(this, ReportarIncidenciaActivity::class.java))
        }
        
        binding.btnSolicitarLimpieza.setOnClickListener {
            solicitarLimpieza()
        }

        binding.btnLogout.setOnClickListener {
            repo.logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
    }

    private fun solicitarLimpieza() {
        AlertDialog.Builder(this)
            .setTitle("Requerir Limpieza")
            .setMessage("¿Deseas solicitar limpieza para tu unidad o un área común?")
            .setPositiveButton("Confirmar") { _, _ ->
                Toast.makeText(this, "Solicitud enviada", Toast.LENGTH_SHORT).show()
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
                binding.tvUnidad.text = "Unidad ${it.unidad}"
                binding.tvBalance.text = "$ ${"%.2f".format(it.balance)}"
                
                binding.tvBalanceAcumulado.text = "$ ${"%.2f".format(it.balance)}"
                binding.tvCuotaMensual.text = "$ ${"%.2f".format(it.proximoPago)}"
                binding.tvProximoPago.text = "$ ${"%.2f".format(it.proximoPago)} pendiente"
                
                if (it.balance < 0) {
                    binding.tvFechaVencimiento.visibility = View.VISIBLE
                    binding.tvFechaVencimiento.text = "Vence: ${it.fechaVencimiento.ifEmpty { "Fin de mes" }}"
                    binding.tvBalance.setTextColor(ContextCompat.getColor(this@ResidenteActivity, R.color.colorRed))
                } else {
                    binding.tvFechaVencimiento.visibility = View.GONE
                    binding.tvBalance.setTextColor(ContextCompat.getColor(this@ResidenteActivity, R.color.colorGreen))
                }
            }
        }
    }
}
