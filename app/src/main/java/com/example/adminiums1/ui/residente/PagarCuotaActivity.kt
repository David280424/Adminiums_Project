package com.example.adminiums1.ui.residente

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.adminiums1.databinding.ActivityPagarCuotaBinding
import com.example.adminiums1.model.Pago
import com.example.adminiums1.repository.FirebaseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PagarCuotaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPagarCuotaBinding
    private val repo = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPagarCuotaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        val uid = repo.getCurrentUid() ?: return
        CoroutineScope(Dispatchers.Main).launch {
            val usuario = repo.getUsuario(uid)
            usuario?.let {
                binding.tvMontoCuota.text = "$ ${"%.2f".format(it.proximoPago)}"
                binding.tvBalanceActual.text = "Balance actual: $ ${"%.2f".format(it.balance)}"
                binding.tvFechaVence.text = "Vence: ${it.fechaVencimiento}"
            }
        }

        binding.btnPagar.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            binding.btnPagar.isEnabled = false
            val uid2 = repo.getCurrentUid() ?: return@setOnClickListener

            CoroutineScope(Dispatchers.Main).launch {
                val usuario = repo.getUsuario(uid2)
                val fecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                val pago = Pago(
                    residenteUid = uid2,
                    monto = usuario?.proximoPago ?: 0.0,
                    fecha = fecha,
                    estado = "pagado"
                )
                val success = repo.registrarPago(pago)
                binding.progressBar.visibility = View.GONE
                binding.btnPagar.isEnabled = true
                if (success) {
                    Toast.makeText(this@PagarCuotaActivity,
                        "¡Pago registrado exitosamente!", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(this@PagarCuotaActivity, "Error al procesar pago", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
