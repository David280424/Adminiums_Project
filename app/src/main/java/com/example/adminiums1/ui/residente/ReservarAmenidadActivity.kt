package com.example.adminiums1.ui.residente

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.adminiums1.R
import com.example.adminiums1.databinding.ActivityReservarAmenidadBinding
import com.example.adminiums1.model.Reservacion
import com.example.adminiums1.repository.FirebaseRepository
import com.example.adminiums1.utils.ErrorHandler
import com.example.adminiums1.utils.mostrar
import com.example.adminiums1.utils.ocultar
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class ReservarAmenidadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReservarAmenidadBinding
    private val repo = FirebaseRepository()
    private val db   = FirebaseFirestore.getInstance()

    private var amenidadSeleccionada = ""
    private var fechaSeleccionada    = ""
    private var listenerHorarios: ListenerRegistration? = null

    private val horarios = listOf(
        "08:00 - 10:00",
        "10:00 - 12:00",
        "12:00 - 14:00",
        "14:00 - 16:00",
        "16:00 - 18:00"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReservarAmenidadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        configurarAmenidades()
        configurarCalendario()
    }

    // ── Amenidades ──────────────────────────────────────────────────────────

    private fun configurarAmenidades() {
        val botones = listOf(
            binding.btnPiscina  to "Piscina",
            binding.btnGimnasio to "Gimnasio",
            binding.btnSalon    to "Salón de Fiestas",
            binding.btnBBQ      to "Área BBQ"
        )
        botones.forEach { (btn, nombre) ->
            btn.setOnClickListener {
                amenidadSeleccionada = nombre
                botones.forEach { (b, _) ->
                    b.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.darker_gray)
                }
                btn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.colorPrimary)
                binding.tvAmenidadSeleccionada.text = "Seleccionado: $nombre"
                if (fechaSeleccionada.isNotEmpty()) escucharHorarios()
            }
        }
    }

    // ── Calendario ──────────────────────────────────────────────────────────

    private fun configurarCalendario() {
        binding.calendarView.minDate = System.currentTimeMillis() - 1000
        binding.calendarView.setOnDateChangeListener { _, year, month, day ->
            val cal = Calendar.getInstance().apply { set(year, month, day) }
            fechaSeleccionada = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
            binding.tvFechaSeleccionada.text = "Fecha: $fechaSeleccionada"

            if (amenidadSeleccionada.isNotEmpty()) escucharHorarios()
            else Toast.makeText(this, "Primero selecciona una amenidad", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Horarios en tiempo real ─────────────────────────────────────────────

    private fun escucharHorarios() {
        listenerHorarios?.remove()
        binding.cardHorarios.mostrar()
        val uid = repo.getCurrentUid() ?: return

        listenerHorarios = db.collection("reservaciones")
            .whereEqualTo("amenidad", amenidadSeleccionada)
            .whereEqualTo("fecha", fechaSeleccionada)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { ErrorHandler.log(error, "ReservarActivity"); return@addSnapshotListener }

                // horario -> uid que lo reservó
                val reservados = mutableMapOf<String, String>()
                snapshot?.documents?.forEach { doc ->
                    reservados[doc.getString("horario") ?: ""] = doc.getString("residenteUid") ?: ""
                }

                binding.layoutHorarios.removeAllViews()

                horarios.forEach { horario ->
                    val btn = MaterialButton(this)
                    btn.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.setMargins(0, 0, 0, 12) }

                    when {
                        // Reservado por mí → gris, long press para cancelar
                        reservados[horario] == uid -> {
                            btn.text = "$horario  •  Reservado por ti (mantén para cancelar)"
                            btn.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.darker_gray)
                            btn.setOnLongClickListener {
                                cancelarReservacion(horario)
                                true
                            }
                        }
                        // Reservado por otro → rojo, bloqueado
                        reservados.containsKey(horario) -> {
                            btn.text = "$horario  •  No disponible"
                            btn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.colorRed)
                            btn.isEnabled = false
                        }
                        // Disponible → azul/primary
                        else -> {
                            btn.text = "$horario  •  Disponible"
                            btn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.colorPrimary)
                            btn.setOnClickListener { confirmarReservacion(horario) }
                        }
                    }
                    btn.setTextColor(ContextCompat.getColor(this, R.color.white))
                    binding.layoutHorarios.addView(btn)
                }
            }
    }

    // ── Confirmar ───────────────────────────────────────────────────────────

    private fun confirmarReservacion(horario: String) {
        val uid = repo.getCurrentUid() ?: return
        binding.progressBar.mostrar()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val usuario = repo.getUsuario(uid)
                val reservacion = Reservacion(
                    amenidad        = amenidadSeleccionada,
                    residenteUid    = uid,
                    residenteNombre = usuario?.nombre ?: "",
                    unidad          = usuario?.unidad ?: "",
                    fecha           = fechaSeleccionada,
                    horario         = horario,
                    timestamp       = System.currentTimeMillis()
                )
                repo.crearReservacion(reservacion)
                binding.progressBar.ocultar()
                Toast.makeText(this@ReservarAmenidadActivity,
                    "✅ Reservado: $amenidadSeleccionada — $horario", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                binding.progressBar.ocultar()
                ErrorHandler.mostrar(this@ReservarAmenidadActivity, e, "confirmarReservacion")
            }
        }
    }

    // ── Cancelar (long press en horario gris) ───────────────────────────────

    private fun cancelarReservacion(horario: String) {
        val uid = repo.getCurrentUid() ?: return
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val query = db.collection("reservaciones")
                    .whereEqualTo("amenidad", amenidadSeleccionada)
                    .whereEqualTo("fecha", fechaSeleccionada)
                    .whereEqualTo("horario", horario)
                    .whereEqualTo("residenteUid", uid)
                    .get().await()
                query.documents.firstOrNull()?.reference?.delete()?.await()
                Toast.makeText(this@ReservarAmenidadActivity,
                    "Reservación cancelada", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                ErrorHandler.mostrar(this@ReservarAmenidadActivity, e, "cancelarReservacion")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerHorarios?.remove()
    }
}
