package com.example.adminiums1.ui.residente

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.adminiums1.databinding.ActivityReportarIncidenciaBinding
import com.example.adminiums1.model.Incidencia
import com.example.adminiums1.repository.FirebaseRepository
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adminiums1.ui.residente.adapter.FotosAdjuntasAdapter
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.text.SimpleDateFormat
import java.util.*

class ReportarIncidenciaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReportarIncidenciaBinding
    private val repo = FirebaseRepository()

    private lateinit var fotosAdapter: FotosAdjuntasAdapter
    private var tempImageUri: Uri? = null

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempImageUri?.let { uri ->
                fotosAdapter.addFoto(uri)
                actualizarVisibilidadFotos()
            }
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            prepararCamara()
        } else {
            Snackbar.make(binding.root, "Se requiere permiso de cámara para tomar fotos", Snackbar.LENGTH_SHORT).show()
        }
    }

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { uri ->
            fotosAdapter.addFoto(uri)
        }
        actualizarVisibilidadFotos()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportarIncidenciaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFotosRecyclerView()
        setupSpinners()
        
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnTomarFoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                prepararCamara()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
        binding.btnSeleccionarFoto.setOnClickListener { selectImageLauncher.launch("image/*") }
        binding.btnEnviarReporte.setOnClickListener { enviarReporte() }
    }

    private fun setupFotosRecyclerView() {
        fotosAdapter = FotosAdjuntasAdapter { position ->
            fotosAdapter.removeFoto(position)
            actualizarVisibilidadFotos()
        }
        binding.rvFotos.apply {
            layoutManager = LinearLayoutManager(this@ReportarIncidenciaActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = fotosAdapter
        }
    }

    private fun actualizarVisibilidadFotos() {
        val tieneFotos = fotosAdapter.itemCount > 0
        binding.rvFotos.visibility = if (tieneFotos) View.VISIBLE else View.GONE
        binding.layoutPlaceholderFoto.visibility = if (tieneFotos) View.GONE else View.VISIBLE
    }

    private fun prepararCamara() {
        try {
            val tempFile = File.createTempFile("temp_image_${System.currentTimeMillis()}", ".jpg", cacheDir).apply {
                createNewFile()
                deleteOnExit()
            }
            tempImageUri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", tempFile)
            tempImageUri?.let { takePictureLauncher.launch(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            Snackbar.make(binding.root, "Error al preparar la cámara: ${e.message}", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun getBytesFromUri(uri: Uri): ByteArray? {
        return contentResolver.openInputStream(uri)?.use { inputStream ->
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val baos = ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 70, baos)
            baos.toByteArray()
        }
    }

    private fun setupSpinners() {
        val categorias = listOf("Plomería", "Electricidad", "Ruido", "Seguridad", "Limpieza", "Otro")
        val ubicaciones = listOf("Unidad propia", "Pasillo", "Estacionamiento", "Jardín", "Elevador", "Cuarto de basura", "Otro")
        val prioridades = listOf("Baja", "Normal", "Alta", "Urgente")

        binding.spinnerCategoria.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categorias))
        binding.spinnerUbicacion.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, ubicaciones))
        binding.spinnerPrioridad.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, prioridades))
    }

    private fun enviarReporte() {
        val cat = binding.spinnerCategoria.text.toString()
        val ubi = binding.spinnerUbicacion.text.toString()
        val prio = binding.spinnerPrioridad.text.toString()
        val titulo = binding.etTitulo.text.toString().trim()
        val desc = binding.etDescripcion.text.toString().trim()

        var valid = true
        if (cat.isEmpty()) { binding.tilCategoria.error = "Selecciona una categoría"; valid = false } else binding.tilCategoria.error = null
        if (ubi.isEmpty()) { binding.tilUbicacion.error = "Selecciona una ubicación"; valid = false } else binding.tilUbicacion.error = null
        if (prio.isEmpty()) { binding.tilPrioridad.error = "Selecciona prioridad"; valid = false } else binding.tilPrioridad.error = null
        
        if (titulo.isEmpty()) { binding.tilTitulo.error = "El título es obligatorio"; valid = false } 
        else if (titulo.length > 80) { binding.tilTitulo.error = "Máximo 80 caracteres"; valid = false }
        else binding.tilTitulo.error = null

        if (desc.length < 20) { binding.tilDescripcion.error = "Describe al menos 20 caracteres"; valid = false }
        else if (desc.length > 500) { binding.tilDescripcion.error = "Máximo 500 caracteres"; valid = false }
        else binding.tilDescripcion.error = null

        if (!valid) return

        val uid = repo.getCurrentUid() ?: return
        
        binding.progressBar.visibility = View.VISIBLE
        binding.btnEnviarReporte.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            val user = repo.getUsuario(uid)
            val building = user?.edificioId?.let { repo.getCondominio(it) }
            val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            
            val tempId = UUID.randomUUID().toString()
            val uris = fotosAdapter.getItems()
            val uploadedUrls = mutableListOf<String>()

            // Subir múltiples imágenes en paralelo
            val uploadTasks = uris.map { uri ->
                async {
                    val bytes = getBytesFromUri(uri)
                    if (bytes != null) {
                        repo.subirImagenIncidencia(tempId, bytes)
                    } else null
                }
            }
            
            uploadedUrls.addAll(uploadTasks.awaitAll().filterNotNull())
            
            val incidencia = Incidencia(
                id = tempId,
                residenteUid = uid,
                residenteNombre = user?.nombre ?: "",
                unidad = user?.unidad ?: "",
                edificioId = user?.edificioId ?: "",
                edificioNombre = building?.nombre ?: "",
                categoria = cat,
                ubicacion = ubi,
                prioridad = prio,
                titulo = titulo,
                descripcion = desc,
                fecha = fecha,
                fotos = uploadedUrls
            )

            val exito = repo.reportarIncidencia(incidencia)
            
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                if (exito) {
                    Snackbar.make(binding.root, "Reporte enviado correctamente", Snackbar.LENGTH_SHORT)
                        .setBackgroundTint(androidx.core.content.ContextCompat.getColor(this@ReportarIncidenciaActivity, android.R.color.holo_green_dark))
                        .show()
                    Handler(Looper.getMainLooper()).postDelayed({ finish() }, 1500)
                } else {
                    binding.btnEnviarReporte.isEnabled = true
                    Snackbar.make(binding.root, "Error al enviar reporte", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
}
