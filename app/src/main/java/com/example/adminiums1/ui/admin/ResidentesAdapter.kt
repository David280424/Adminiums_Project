package com.example.adminiums1.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.adminiums1.databinding.ItemResidenteBinding
import com.example.adminiums1.model.Usuario

/**
 * Adapter de la lista de residentes en AdminActivity.
 * Al hacer click → abre ResidenteDetalleActivity.
 * Filtro en tiempo real por nombre o unidad.
 */
class ResidentesAdapter(
    private val onClick: (Usuario) -> Unit
) : RecyclerView.Adapter<ResidentesAdapter.ViewHolder>() {

    private var listaCompleta: List<Usuario> = emptyList()
    private var listaFiltrada: List<Usuario> = emptyList()

    fun setDatos(datos: List<Usuario>) {
        listaCompleta = datos
        listaFiltrada = datos
        notifyDataSetChanged()
    }

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

            val (texto, color) = when {
                u.balance <= 0 -> "Al día"    to 0xFF48BB78.toInt()
                else           -> "Pendiente" to 0xFFF56565.toInt()
            }
            b.tvResidenteEstado.text = texto
            b.tvResidenteEstado.setBackgroundColor(color)

            // Click abre el detalle del residente
            b.root.setOnClickListener { onClick(u) }
        }
    }
}
