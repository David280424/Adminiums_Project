package com.example.adminiums1.ui.vigilante

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.adminiums1.databinding.ActivityVigilanteBinding
import com.example.adminiums1.repository.FirebaseRepository
import com.example.adminiums1.ui.auth.RolSelectorActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VigilanteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVigilanteBinding
    private val repo = FirebaseRepository()
    private var modoActual = "visitantes"

    private val scanLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        if (result.contents != null) {
            procesarQR(result.contents)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVigilanteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uid = repo.getCurrentUid() ?: return
        CoroutineScope(Dispatchers.Main).launch {
            val usuario = repo.getUsuario(uid)
            binding.tvNombreVigilante.text = usuario?.nombre ?: "Vigilante"
        }

        binding.btnVisitantes.setOnClickListener {
            modoActual = "visitantes"
            binding.layoutVisitantes.visibility = View.VISIBLE
            binding.layoutResidentes.visibility = View.GONE
        }

        binding.btnResidentes.setOnClickListener {
            modoActual = "residentes"
            binding.layoutVisitantes.visibility = View.GONE
            binding.layoutResidentes.visibility = View.VISIBLE
        }

        binding.btnEscanearQR.setOnClickListener {
            val options = ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                setPrompt("Escanea el código QR del visitante")
                setBeepEnabled(true)
                setOrientationLocked(false)
            }
            scanLauncher.launch(options)
        }

        binding.btnBuscarResidente.setOnClickListener {
            val busqueda = binding.etBuscarResidente.text.toString().trim()
            if (busqueda.isEmpty()) {
                Toast.makeText(this, "Ingresa un nombre o unidad", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            buscarResidente(busqueda)
        }

        binding.btnLogout.setOnClickListener {
            repo.logout()
            startActivity(Intent(this, RolSelectorActivity::class.java))
            finishAffinity()
        }
    }

    private fun procesarQR(contenido: String) {
        binding.progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.Main).launch {
            val visitante = repo.validarVisitante(contenido)
            binding.progressBar.visibility = View.GONE
            if (visitante != null) {
                binding.tvResultadoQR.text = "✅ ACCESO PERMITIDO\n" +
                    "Visitante: ${visitante.nombre}\n" +
                    "Autorizado por: ${visitante.autorizadoPor}\n" +
                    "Unidad: ${visitante.unidad}"
                binding.tvResultadoQR.visibility = View.VISIBLE
                binding.tvResultadoQR.setBackgroundColor(0xFF4CAF50.toInt())
            } else {
                binding.tvResultadoQR.text = "❌ ACCESO DENEGADO\nCódigo QR inválido o ya utilizado"
                binding.tvResultadoQR.visibility = View.VISIBLE
                binding.tvResultadoQR.setBackgroundColor(0xFFF44336.toInt())
            }
        }
    }

    private fun buscarResidente(busqueda: String) {
        binding.progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.Main).launch {
            val residentes = repo.getTodosUsuarios()
            binding.progressBar.visibility = View.GONE
            val encontrado = residentes.find {
                it.nombre.contains(busqueda, ignoreCase = true) ||
                it.unidad.contains(busqueda, ignoreCase = true)
            }
            if (encontrado != null) {
                binding.tvResultadoResidente.text = "✅ Residente encontrado\n" +
                    "Nombre: ${encontrado.nombre}\n" +
                    "Unidad: ${encontrado.unidad}\n" +
                    "Email: ${encontrado.email}"
                binding.tvResultadoResidente.visibility = View.VISIBLE
            } else {
                binding.tvResultadoResidente.text = "❌ No se encontró ningún residente con esos datos"
                binding.tvResultadoResidente.visibility = View.VISIBLE
            }
        }
    }
}
