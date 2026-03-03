package com.example.adminiums1.ui.residente

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.adminiums1.databinding.ActivityReservarAmenidadBinding
import com.example.adminiums1.model.Reservacion
import com.example.adminiums1.repository.FirebaseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ReservarAmenidadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReservarAmenidadBinding
    private val repo = FirebaseRepository()
    private var amenidadSeleccionada: String = ""
    private var horarioSeleccionado: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReservarAmenidadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        val botones = listOf(binding.btnPiscina, binding.btnGimnasio, binding.btnSalon, binding.btnBBQ)
        val nombres = listOf("Piscina", "Gimnasio", "Salón de Fiestas", "Área BBQ")

        botones.forEachIndexed { i, btn ->
            btn.setOnClickListener {
                amenidadSeleccionada = nombres[i]
                botones.forEach { b -> b.isSelected = false }
                btn.isSelected = true
                binding.tvAmenidadSeleccionada.text = "Seleccionado: ${nombres[i]}"
            }
        }

        val horarios = listOf(
            binding.btnH1 to "08:00 - 10:00",
            binding.btnH2 to "10:00 - 12:00",
            binding.btnH3 to "12:00 - 14:00",
            binding.btnH4 to "14:00 - 16:00"
        )

        horarios.forEach { (btn, horario) ->
            btn.setOnClickListener {
                horarioSeleccionado = horario
                horarios.forEach { (b, _) -> b.isSelected = false }
                btn.isSelected = true
            }
        }

        binding.btnConfirmar.setOnClickListener {
            if (amenidadSeleccionada.isEmpty()) {
                Toast.makeText(this, "Selecciona una amenidad", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (horarioSeleccionado.isEmpty()) {
                Toast.makeText(this, "Selecciona un horario", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val uid = repo.getCurrentUid() ?: return@setOnClickListener
            binding.progressBar.visibility = View.VISIBLE

            CoroutineScope(Dispatchers.Main).launch {
                val usuario = repo.getUsuario(uid)
                val fecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                val reservacion = Reservacion(
                    amenidad = amenidadSeleccionada,
                    residenteUid = uid,
                    residenteNombre = usuario?.nombre ?: "",
                    unidad = usuario?.unidad ?: "",
                    fecha = fecha,
                    horario = horarioSeleccionado,
                    timestamp = System.currentTimeMillis()
                )
                val success = repo.crearReservacion(reservacion)
                binding.progressBar.visibility = View.GONE
                if (success) {
                    Toast.makeText(this@ReservarAmenidadActivity,
                        "¡Reservación confirmada! $amenidadSeleccionada - $horarioSeleccionado",
                        Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(this@ReservarAmenidadActivity, "Error al reservar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
