package com.example.adminiums1

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.adminiums1.databinding.ActivityMainBinding
import com.example.adminiums1.repository.FirebaseRepository
import com.example.adminiums1.ui.admin.AdminActivity
import com.example.adminiums1.ui.auth.LoginActivity
import com.example.adminiums1.ui.limpieza.LimpiezaActivity
import com.example.adminiums1.ui.residente.ResidenteActivity
import com.example.adminiums1.ui.vigilante.VigilanteActivity
import com.google.firebase.messaging.FirebaseMessaging
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val repo = FirebaseRepository()
    private var roleLoaded = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            getAndSaveFCMToken()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        askNotificationPermission()
        setupNotificationSync()

        val uid = repo.getCurrentUid()
        if (uid == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            binding.progressBar.visibility = View.VISIBLE
            checkUserRoleAndNavigate(uid)
            getAndSaveFCMToken()
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun getAndSaveFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@addOnCompleteListener
            }
            val token = task.result
            val uid = repo.getCurrentUid()
            if (uid != null && token != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    repo.updateFcmToken(uid, token)
                }
            }
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
            val result = repo.getUsuario(uid)
            withContext(Dispatchers.Main) {
                roleLoaded = true
                binding.progressBar.visibility = View.GONE
                if (result.isSuccess) {
                    val usuario = result.getOrNull()
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
                } else {
                    // Si hay error (ej: falta de permisos), cerramos sesión para evitar bucles
                    repo.logout()
                    startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                    finish()
                }
            }
        }
    }

    private fun setupNotificationSync() {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()

        val syncRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.adminiums1.utils.NotificationWorker>(
            15, java.util.concurrent.TimeUnit.MINUTES
        ).setConstraints(constraints).build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "NotificationSync",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
