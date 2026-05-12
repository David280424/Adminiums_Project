package com.example.adminiums1.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.adminiums1.databinding.ActivityRegistroBinding
import com.example.adminiums1.model.Usuario
import com.example.adminiums1.model.Condominio
import com.example.adminiums1.repository.FirebaseRepository
import com.example.adminiums1.ui.residente.ResidenteActivity
import com.example.adminiums1.ui.admin.AdminActivity
import com.example.adminiums1.ui.vigilante.VigilanteActivity
import com.example.adminiums1.ui.limpieza.LimpiezaActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegistroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroBinding
    private val repo = FirebaseRepository()
    
    private var listaCondominios: List<Condominio> = emptyList()
    private var edificioSeleccionadoId: String = ""
    private var rolSeleccionado: String = "residente"
    private var isAdminCreating: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Verificamos si quien abre la pantalla es un Admin
        checkIfAdmin()

        cargarCondominios()
        setupRolesSpinner()

        binding.btnRegistrar.setOnClickListener {
            registrarUsuario()
        }

        binding.tvIrLogin.setOnClickListener {
            finish()
        }
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun checkIfAdmin() {
        val uid = repo.getCurrentUid()
        if (uid != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val result = repo.getUsuario(uid)
                withContext(Dispatchers.Main) {
                    val user = result.getOrNull()
                    if (user?.rol == "admin") {
                        isAdminCreating = true
                        binding.layoutSeleccionRol.visibility = View.VISIBLE
                        binding.tvIrLogin.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun setupRolesSpinner() {
        val roles = listOf("residente", "vigilante", "limpieza")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, roles)
        binding.spinnerRoles.setAdapter(adapter)
        binding.spinnerRoles.setOnItemClickListener { _, _, position, _ ->
            rolSeleccionado = roles[position]
            if (rolSeleccionado == "residente") {
                binding.inputUnidad.hint = "Número de Unidad / Depto"
            } else {
                binding.inputUnidad.hint = "Área / Observaciones (Opcional)"
            }
        }
    }

    private fun cargarCondominios() {
        CoroutineScope(Dispatchers.IO).launch {
            val result = repo.getCondominios()
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    val condominios = result.getOrDefault(emptyList())
                    listaCondominios = condominios
                    val nombres = condominios.map { "${it.nombre} (${it.ciudad})" }
                    val adapter = ArrayAdapter(this@RegistroActivity, android.R.layout.simple_dropdown_item_1line, nombres)
                    binding.spinnerEdificios.setAdapter(adapter)

                    binding.spinnerEdificios.setOnItemClickListener { _, _, position, _ ->
                        edificioSeleccionadoId = listaCondominios[position].id
                    }
                }
            }
        }
    }

    private fun registrarUsuario() {
        val nombre = binding.etNombre.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val pass = binding.etPassword.text.toString().trim()
        val confirmPass = binding.etConfirmPassword.text.toString().trim()
        val unidad = binding.etUnidad.text.toString().trim()

        if (edificioSeleccionadoId.isEmpty()) {
            Toast.makeText(this, "Selecciona un edificio", Toast.LENGTH_SHORT).show()
            return
        }
        if (nombre.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Completa los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }
        if (pass != confirmPass) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnRegistrar.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            val authResult = repo.registrarAuth(email, pass)
            
            if (authResult.isSuccess) {
                val uid = authResult.getOrNull()!!
                
                // FIX 1: Fetch condominio first on IO thread
                val condominio = repo.getCondominio(edificioSeleccionadoId)
                val cuotaInicial = condominio?.cuotaBase?.takeIf { it > 0 } ?: 3000.0

                val nuevoUsuario = Usuario(
                    uid = uid,
                    nombre = nombre,
                    email = email,
                    rol = rolSeleccionado,
                    unidad = unidad,
                    edificioId = edificioSeleccionadoId,
                    balance = 0.0,
                    proximoPago = cuotaInicial
                )
                
                val firestoreResult = repo.crearUsuario(nuevoUsuario)
                
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    if (firestoreResult.isSuccess) {
                        Toast.makeText(this@RegistroActivity, "Usuario creado exitosamente", Toast.LENGTH_SHORT).show()
                        if (isAdminCreating) {
                            finish()
                        } else {
                            navegarSegunRol(nuevoUsuario)
                        }
                    } else {
                        binding.btnRegistrar.isEnabled = true
                        Toast.makeText(this@RegistroActivity, "Error al guardar en base de datos", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnRegistrar.isEnabled = true
                    Toast.makeText(this@RegistroActivity, "Error: ${authResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun navegarSegunRol(usuario: Usuario) {
        val intent = when (usuario.rol) {
            "residente" -> Intent(this, ResidenteActivity::class.java)
            "vigilante" -> Intent(this, VigilanteActivity::class.java)
            "limpieza" -> Intent(this, LimpiezaActivity::class.java)
            "admin" -> Intent(this, AdminActivity::class.java)
            else -> Intent(this, ResidenteActivity::class.java)
        }
        startActivity(intent)
        finishAffinity()
    }
}
