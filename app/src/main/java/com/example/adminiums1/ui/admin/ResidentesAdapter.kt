// app/src/main/java/com/example/adminiums1/ui/admin/adapter/ResidentesAdapter.kt
package com.example.adminiums1.ui.admin.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.adminiums1.databinding.ItemResidenteBinding
import com.example.adminiums1.model.Usuario

/**
 * Parte 4 — Adapter para la lista de residentes en el panel Admin.
 * Soporta filtrado en tiempo real por nombre o unidad.
 */
class ResidentesAdapter : RecyclerView.Adapter<ResidentesAdapter.ViewHolder>() {

    private var listaCompleta:  List<Usuario> = emptyList()
    private var listaFiltrada:  List<Usuario> = emptyList()

    /** Carga o refresca todos los residentes */
    fun setDatos(datos: List<Usuario>) {
        listaCompleta = datos
        listaFiltrada = datos
        notifyDataSetChanged()
    }

    /**
     * Filtra por nombre O por unidad (sin distinguir mayúsculas).
     * Llamar con cadena vacía para mostrar todos.
     */
    fun filtrar(query: String) {
        listaFiltrada = if (query.isBlank()) {
            listaCompleta
        } else {
            val q = query.trim().lowercase()
            listaCompleta.filter {
                it.nombre.lowercase().contains(q) || it.unidad.lowercase().contains(q)
            }
        }
        notifyDataSetChanged()
    }

    override fun getItemCount() = listaFiltrada.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemResidenteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(listaFiltrada[position])

    inner class ViewHolder(private val b: ItemResidenteBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(u: Usuario) {
            b.tvResidenteNombre.text  = u.nombre
            b.tvResidenteUnidad.text  = "Unidad: ${u.unidad}"
            b.tvResidenteBalance.text = "Adeudo: $${"%.2f".format(u.balance)}"

            // Color del chip de estado según el balance
            val (textoEstado, colorFondo) = when {
                u.balance <= 0 -> "Al día"     to 0xFF48BB78.toInt() // verde
                else           -> "Pendiente"  to 0xFFF56565.toInt() // rojo
            }
            b.tvResidenteEstado.text = textoEstado
            b.tvResidenteEstado.setBackgroundColor(colorFondo)
        }
    }
}
