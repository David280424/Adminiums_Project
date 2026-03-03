package com.example.adminiums1.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.adminiums1.databinding.ActivityRolSelectorBinding
import com.example.adminiums1.repository.FirebaseRepository
import com.example.adminiums1.ui.admin.AdminActivity
import com.example.adminiums1.ui.residente.ResidenteActivity
import com.example.adminiums1.ui.vigilante.VigilanteActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RolSelectorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRolSelectorBinding
    private val repo = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRolSelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Cierra las sesiones antiguas
        repo.logout()
        binding.cardResidente.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java).apply {
                putExtra("rol", "residente")
            })
        }
        binding.cardVigilante.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java).apply {
                putExtra("rol", "vigilante")
            })
        }
        binding.cardAdmin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java).apply {
                putExtra("rol", "admin")
            })
        }
    }

    private fun navegarPorRol(rol: String) {
        val intent = when (rol) {
            "residente" -> Intent(this, ResidenteActivity::class.java)
            "vigilante" -> Intent(this, VigilanteActivity::class.java)
            "admin" -> Intent(this, AdminActivity::class.java)
            else -> return
        }
        startActivity(intent)
        finish()
    }
}
