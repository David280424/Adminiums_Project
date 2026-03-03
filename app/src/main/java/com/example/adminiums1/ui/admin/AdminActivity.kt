package com.example.adminiums1.ui.admin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.adminiums1.databinding.ActivityAdminBinding
import com.example.adminiums1.repository.FirebaseRepository
import com.example.adminiums1.ui.auth.RolSelectorActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private val repo = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cargarDatos()

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
            val admin = repo.getUsuario(uid)
            binding.tvNombreAdmin.text = "${admin?.nombre ?: "Administrador"}"

            val residentes = repo.getTodosUsuarios()
            val totalRecaudado = repo.getTotalRecaudado()
            val pendientes = residentes.count { it.balance > 0 }
            val pagados = residentes.count { it.balance <= 0 }
            val total = residentes.size
            val porcentajePago = if (total > 0) (pagados.toFloat() / total * 100).toInt() else 0

            binding.tvTotalRecaudado.text = "$ ${"%.2f".format(totalRecaudado)}"
            binding.tvCuotasPendientes.text = "$pendientes"
            binding.tvPagosAlDia.text = "$porcentajePago%"
            binding.tvTotalResidentes.text = "$total"
            binding.tvResidentesPagados.text = "$pagados de $total residentes"

            // Reservaciones de amenidades
            val reservaciones = repo.getReservaciones()
            val piscinaCount = reservaciones.count { it.amenidad == "Piscina" }
            val gimnasioCount = reservaciones.count { it.amenidad == "Gimnasio" }
            val salonCount = reservaciones.count { it.amenidad == "Salón de Fiestas" }
            val bbqCount = reservaciones.count { it.amenidad == "Área BBQ" }

            binding.tvPiscinaReservas.text = "$piscinaCount / 8 reservadas"
            binding.progressPiscina.progress = (piscinaCount * 100 / 8).coerceAtMost(100)
            binding.tvGimnasioReservas.text = "$gimnasioCount / 10 reservadas"
            binding.progressGimnasio.progress = (gimnasioCount * 100 / 10).coerceAtMost(100)
            binding.tvSalonReservas.text = "$salonCount / 1 reservadas"
            binding.progressSalon.progress = (salonCount * 100 / 1).coerceAtMost(100)
            binding.tvBBQReservas.text = "$bbqCount / 10 reservadas"
            binding.progressBBQ.progress = (bbqCount * 100 / 10).coerceAtMost(100)
        }
    }
}
