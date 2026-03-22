package com.example.adminiums1.ui.residente

import android.graphics.Bitmap
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.adminiums1.databinding.ActivityMiQrBinding
import com.example.adminiums1.repository.FirebaseRepository
import com.example.adminiums1.utils.QRUtils
import com.example.adminiums1.utils.mostrar
import com.example.adminiums1.utils.ocultar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MiQRActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMiQrBinding
    private val repo = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMiQrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        generarQRResidente()
    }

    private fun generarQRResidente() {
        val uid = repo.getCurrentUid() ?: return
        binding.progressBar.mostrar()
        binding.ivQRResidente.ocultar()

        CoroutineScope(Dispatchers.Main).launch {
            val usuario = repo.getUsuario(uid)
            usuario?.let {
                // QR fijo con datos del residente — el vigilante lo escanea para registrar entrada
                val contenido = "RESIDENTE|${it.uid}|${it.nombre}|${it.unidad}"

                val bmp: Bitmap = withContext(Dispatchers.Default) {
                    QRUtils.generateQR(contenido, 600)
                }

                binding.progressBar.ocultar()
                binding.ivQRResidente.mostrar()
                binding.ivQRResidente.setImageBitmap(bmp)
                binding.tvNombreQR.text   = it.nombre
                binding.tvUnidadQR.text   = "Unidad: ${it.unidad}"
            }
        }
    }
}
