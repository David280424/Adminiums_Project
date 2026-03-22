package com.example.adminiums1.ui.vigilante.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.adminiums1.databinding.ItemAccesoBinding
import com.example.adminiums1.model.RegistroAcceso

class AccesosAdapter : RecyclerView.Adapter<AccesosAdapter.ViewHolder>() {

    private var lista: List<RegistroAcceso> = emptyList()

    fun setDatos(datos: List<RegistroAcceso>) {
        lista = datos
        notifyDataSetChanged()
    }

    override fun getItemCount() = lista.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemAccesoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(lista[position])

    inner class ViewHolder(private val b: ItemAccesoBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(acceso: RegistroAcceso) {
            b.tvAccesoNombre.text = acceso.residenteNombre
            b.tvAccesoUnidad.text = "Unidad: ${acceso.unidad}"
            b.tvAccesoHora.text   = acceso.hora

            // Ícono y color según método de registro
            val (icono, color) = when (acceso.metodo) {
                "qr"     -> "📱 QR"     to 0xFF2D3748.toInt()
                "manual" -> "✋ Manual" to 0xFF4A5568.toInt()
                else     -> "•"         to 0xFF718096.toInt()
            }
            b.tvAccesoMetodo.text = icono
            b.tvAccesoMetodo.setBackgroundColor(color)
        }
    }
}
