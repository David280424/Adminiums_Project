package com.example.adminiums1.ui.admin

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adminiums1.databinding.ActivityResidenteDetalleBinding
import com.example.adminiums1.model.Pago
import com.example.adminiums1.model.Reservacion
import com.example.adminiums1.model.Visitante
import com.example.adminiums1.ui.admin.adapter.PagosDetalleAdapter
import com.example.adminiums1.ui.admin.adapter.ReservacionesDetalleAdapter
import com.example.adminiums1.ui.admin.adapter.InvitadosDetalleAdapter
import com.example.adminiums1.utils.ErrorHandler
import com.example.adminiums1.utils.mostrar
import com.example.adminiums1.utils.ocultar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ResidenteDetalleActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_UID    = "extra_uid"
        const val EXTRA_NOMBRE = "extra_nombre"
        const val EXTRA_UNIDAD = "extra_unidad"
        const val EXTRA_EMAIL  = "extra_email"
    }

    private lateinit var binding: ActivityResidenteDetalleBinding
    private val db = FirebaseFirestore.getInstance()

    private lateinit var residenteUid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResidenteDetalleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        residenteUid = intent.getStringExtra(EXTRA_UID) ?: return

        // Header del residente
        binding.tvDetalleNombre.text = intent.getStringExtra(EXTRA_NOMBRE) ?: ""
        binding.tvDetalleUnidad.text = "Unidad: ${intent.getStringExtra(EXTRA_UNIDAD) ?: ""}"
        binding.tvDetalleEmail.text  = intent.getStringExtra(EXTRA_EMAIL) ?: ""

        configurarSecciones()
        cargarPagos()
        cargarReservaciones()
        cargarInvitados()

        binding.btnEliminarResidente.setOnClickListener { confirmarEliminacion() }
    }

    // ── Configurar RecyclerViews ────────────────────────────────────────────

    private fun configurarSecciones() {
        binding.rvPagos.apply {
            layoutManager = LinearLayoutManager(this@ResidenteDetalleActivity)
            adapter = PagosDetalleAdapter()
            isNestedScrollingEnabled = false
        }
        binding.rvReservaciones.apply {
            layoutManager = LinearLayoutManager(this@ResidenteDetalleActivity)
            adapter = ReservacionesDetalleAdapter()
            isNestedScrollingEnabled = false
        }
        binding.rvInvitados.apply {
            layoutManager = LinearLayoutManager(this@ResidenteDetalleActivity)
            adapter = InvitadosDetalleAdapter()
            isNestedScrollingEnabled = false
        }
    }

    // ── Historial de pagos ──────────────────────────────────────────────────

    private fun cargarPagos() {
        binding.progressPagos.mostrar()
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val docs = db.collection("pagos")
                    .whereEqualTo("residenteUid", residenteUid)
                    .orderBy("fecha", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get().await()
                val pagos = docs.toObjects(Pago::class.java)
                binding.progressPagos.ocultar()
                binding.tvSinPagos.apply {
                    if (pagos.isEmpty()) mostrar() else ocultar()
                }
                (binding.rvPagos.adapter as PagosDetalleAdapter).setDatos(pagos)
                binding.tvHeaderPagos.text = "Pagos (${pagos.size})"
            } catch (e: Exception) {
                binding.progressPagos.ocultar()
                ErrorHandler.log(e, "ResidenteDetalle:pagos")
            }
        }
    }

    // ── Reservaciones ───────────────────────────────────────────────────────

    private fun cargarReservaciones() {
        binding.progressReservaciones.mostrar()
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val docs = db.collection("reservaciones")
                    .whereEqualTo("residenteUid", residenteUid)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get().await()
                val reservaciones = docs.toObjects(Reservacion::class.java)
                binding.progressReservaciones.ocultar()
                binding.tvSinReservaciones.apply {
                    if (reservaciones.isEmpty()) mostrar() else ocultar()
                }
                (binding.rvReservaciones.adapter as ReservacionesDetalleAdapter).setDatos(reservaciones)
                binding.tvHeaderReservaciones.text = "Reservaciones (${reservaciones.size})"
            } catch (e: Exception) {
                binding.progressReservaciones.ocultar()
                ErrorHandler.log(e, "ResidenteDetalle:reservaciones")
            }
        }
    }

    // ── Invitados registrados ───────────────────────────────────────────────

    private fun cargarInvitados() {
        binding.progressInvitados.mostrar()
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val docs = db.collection("visitantes")
                    .whereEqualTo("autorizadoPor", intent.getStringExtra(EXTRA_NOMBRE) ?: "")
                    .get().await()
                val invitados = docs.toObjects(Visitante::class.java)
                binding.progressInvitados.ocultar()
                binding.tvSinInvitados.apply {
                    if (invitados.isEmpty()) mostrar() else ocultar()
                }
                (binding.rvInvitados.adapter as InvitadosDetalleAdapter).setDatos(invitados)
                binding.tvHeaderInvitados.text = "Invitados registrados (${invitados.size})"
            } catch (e: Exception) {
                binding.progressInvitados.ocultar()
                ErrorHandler.log(e, "ResidenteDetalle:invitados")
            }
        }
    }

    // ── Eliminar residente ──────────────────────────────────────────────────

    private fun confirmarEliminacion() {
        AlertDialog.Builder(this)
            .setTitle("Eliminar residente")
            .setMessage("¿Seguro que deseas eliminar a ${binding.tvDetalleNombre.text}?\nEsta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ -> eliminarResidente() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarResidente() {
        binding.btnEliminarResidente.isEnabled = false
        CoroutineScope(Dispatchers.Main).launch {
            try {
                db.collection("usuarios").document(residenteUid).delete().await()
                Toast.makeText(this@ResidenteDetalleActivity,
                    "Residente eliminado", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                binding.btnEliminarResidente.isEnabled = true
                ErrorHandler.mostrar(this@ResidenteDetalleActivity, e, "eliminarResidente")
            }
        }
    }
}
