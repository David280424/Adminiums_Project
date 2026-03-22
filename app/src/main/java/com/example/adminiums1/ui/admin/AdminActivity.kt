// app/src/main/java/com/example/adminiums1/ui/admin/AdminActivity.kt
package com.example.adminiums1.ui.admin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adminiums1.databinding.ActivityAdminBinding
import com.example.adminiums1.model.Usuario
import com.example.adminiums1.repository.FirebaseRepository
import com.example.adminiums1.ui.admin.adapter.ResidentesAdapter
import com.example.adminiums1.ui.auth.RolSelectorActivity
import com.example.adminiums1.utils.ErrorHandler
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

/**
 * Parte 4 — Panel Admin completo con:
 *  - Estadísticas en tiempo real (listeners Firestore)
 *  - Barras de ocupación de amenidades
 *  - RecyclerView de residentes con búsqueda por nombre/unidad
 * Parte 5 — Limpia todos los listeners en onDestroy()
 */
class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private val repo = FirebaseRepository()
    private val db   = FirebaseFirestore.getInstance()

    // Lista de listeners activos — se limpian en onDestroy()
    private val listeners = mutableListOf<ListenerRegistration>()

    private lateinit var residentesAdapter: ResidentesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cargarNombreAdmin()
        configurarRecyclerView()
        configurarBusqueda()
        escucharEstadisticasPagos()       // Parte 4: listener tiempo real
        escucharOcupacionAmenidades()     // Parte 4: listener tiempo real
        escucharResidentes()              // Parte 4: listener tiempo real

        binding.btnLogout.setOnClickListener {
            repo.logout()
            startActivity(Intent(this, RolSelectorActivity::class.java))
            finishAffinity()
        }
    }

    // ── Nombre del admin ────────────────────────────────────────────────────

    private fun cargarNombreAdmin() {
        val uid = repo.getCurrentUid() ?: return
        val listener = db.collection("usuarios").document(uid)
            .addSnapshotListener { snap, error ->
                if (error != null) { ErrorHandler.log(error, "AdminActivity:nombreAdmin"); return@addSnapshotListener }
                binding.tvNombreAdmin.text = snap?.getString("nombre") ?: "Administrador"
            }
        listeners.add(listener)
    }

    // ── RecyclerView de residentes ──────────────────────────────────────────

    private fun configurarRecyclerView() {
        residentesAdapter = ResidentesAdapter()
        binding.rvResidentes.apply {
            layoutManager = LinearLayoutManager(this@AdminActivity)
            adapter = residentesAdapter
            // Desactiva scroll anidado para que el ScrollView externo funcione bien
            isNestedScrollingEnabled = false
        }
    }

    /** TextWatcher que filtra en tiempo real por nombre O por unidad */
    private fun configurarBusqueda() {
        binding.etBuscarResidente.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                residentesAdapter.filtrar(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    // ── Listener: estadísticas de pagos ────────────────────────────────────

    /**
     * Escucha la colección "pagos" en tiempo real.
     * Calcula: total recaudado, cuotas pendientes y % pagos al día.
     */
    private fun escucharEstadisticasPagos() {
        val listener = db.collection("pagos")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { ErrorHandler.log(error, "AdminActivity:pagos"); return@addSnapshotListener }

                var totalRecaudado = 0.0
                var pagados = 0
                var pendientes = 0
                val total = snapshot?.size() ?: 0

                snapshot?.documents?.forEach { doc ->
                    when (doc.getString("estado")) {
                        "pagado"    -> { totalRecaudado += doc.getDouble("monto") ?: 0.0; pagados++ }
                        "pendiente" -> pendientes++
                    }
                }

                val porcentaje = if (total > 0) (pagados * 100 / total) else 0

                binding.tvTotalRecaudado.text    = "$ ${"%.2f".format(totalRecaudado)}"
                binding.tvCuotasPendientes.text  = "$pendientes"
                binding.tvPagosAlDia.text        = "$porcentaje%"
                binding.tvResidentesPagados.text = "$pagados de $total residentes"
            }
        listeners.add(listener)
    }

    // ── Listener: ocupación de amenidades ──────────────────────────────────

    private val capacidades = mapOf(
        "Piscina"         to 8,
        "Gimnasio"        to 10,
        "Salón de Fiestas" to 1,
        "Área BBQ"        to 10
    )

    /**
     * Escucha "reservaciones" filtrando por fecha de hoy.
     * Actualiza las ProgressBar y etiquetas de cada amenidad.
     */
    private fun escucharOcupacionAmenidades() {
        val hoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        val listener = db.collection("reservaciones")
            .whereEqualTo("fecha", hoy)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { ErrorHandler.log(error, "AdminActivity:amenidades"); return@addSnapshotListener }

                // Contar reservaciones por amenidad
                val conteo = mutableMapOf("Piscina" to 0, "Gimnasio" to 0, "Salón de Fiestas" to 0, "Área BBQ" to 0)
                snapshot?.documents?.forEach { doc ->
                    val amenidad = doc.getString("amenidad") ?: ""
                    if (conteo.containsKey(amenidad)) conteo[amenidad] = conteo[amenidad]!! + 1
                }

                val cap = capacidades
                val p = conteo["Piscina"]!!;          val capP = cap["Piscina"]!!
                val g = conteo["Gimnasio"]!!;         val capG = cap["Gimnasio"]!!
                val s = conteo["Salón de Fiestas"]!!; val capS = cap["Salón de Fiestas"]!!
                val b = conteo["Área BBQ"]!!;         val capB = cap["Área BBQ"]!!

                binding.tvPiscinaReservas.text  = "$p / $capP reservadas"
                binding.progressPiscina.progress  = (p * 100 / capP).coerceAtMost(100)

                binding.tvGimnasioReservas.text = "$g / $capG reservadas"
                binding.progressGimnasio.progress = (g * 100 / capG).coerceAtMost(100)

                binding.tvSalonReservas.text    = "$s / $capS reservadas"
                binding.progressSalon.progress    = (s * 100 / capS).coerceAtMost(100)

                binding.tvBBQReservas.text      = "$b / $capB reservadas"
                binding.progressBBQ.progress      = (b * 100 / capB).coerceAtMost(100)
            }
        listeners.add(listener)
    }

    // ── Listener: lista de residentes ───────────────────────────────────────

    /**
     * Escucha la colección "usuarios" filtrando por rol = "residente".
     * Actualiza el RecyclerView con el total y los datos de cada residente.
     */
    private fun escucharResidentes() {
        val listener = db.collection("usuarios")
            .whereEqualTo("rol", "residente")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { ErrorHandler.log(error, "AdminActivity:residentes"); return@addSnapshotListener }

                val lista = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Usuario::class.java)
                } ?: emptyList()

                binding.tvTotalResidentes.text = "${lista.size}"
                residentesAdapter.setDatos(lista)
            }
        listeners.add(listener)
    }

    // ── Limpieza de listeners (Parte 5) ────────────────────────────────────

    override fun onDestroy() {
        super.onDestroy()
        // Evita memory leaks y callbacks innecesarios al cerrar la pantalla
        listeners.forEach { it.remove() }
        listeners.clear()
    }
}
