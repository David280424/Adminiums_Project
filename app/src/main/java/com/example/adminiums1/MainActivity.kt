package com.example.adminiums1

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.adminiums1.databinding.ActivityMainBinding
import com.example.adminiums1.repository.FirebaseRepository
import com.example.adminiums1.ui.admin.AdminActivity
import com.example.adminiums1.ui.auth.LoginActivity
import com.example.adminiums1.ui.residente.ResidenteActivity
import com.example.adminiums1.ui.vigilante.VigilanteActivity
import com.example.adminiums1.ui.limpieza.LimpiezaActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val repo = FirebaseRepository()
    private var roleLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uid = repo.getCurrentUid()
        if (uid == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            binding.progressBar.visibility = View.VISIBLE
            checkUserRoleAndNavigate(uid)
        }
    }

    override fun onResume() {
        super.onResume()
        repo.getCurrentUid()?.let { uid ->
            CoroutineScope(Dispatchers.IO).launch {
                repo.setUserOnlineStatus(uid, true)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        repo.getCurrentUid()?.let { uid ->
            CoroutineScope(Dispatchers.IO).launch {
                repo.setUserOnlineStatus(uid, false)
            }
        }
    }

    private fun checkUserRoleAndNavigate(uid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val usuario = repo.getUsuario(uid)
            withContext(Dispatchers.Main) {
                roleLoaded = true
                binding.progressBar.visibility = View.GONE
                if (usuario != null) {
                    val intent = when (usuario.rol) {
                        "admin" -> Intent(this@MainActivity, AdminActivity::class.java)
                        "residente" -> Intent(this@MainActivity, ResidenteActivity::class.java)
                        "vigilante" -> Intent(this@MainActivity, VigilanteActivity::class.java)
                        "limpieza" -> Intent(this@MainActivity, LimpiezaActivity::class.java)
                        else -> Intent(this@MainActivity, LoginActivity::class.java)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    repo.logout()
                    startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                    finish()
                }
            }
        }
    }
}
