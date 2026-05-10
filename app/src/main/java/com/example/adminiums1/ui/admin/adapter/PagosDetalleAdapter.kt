package com.example.adminiums1.ui.admin.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.adminiums1.R
import com.example.adminiums1.databinding.ItemPagoDetalleBinding
import com.example.adminiums1.model.Pago

class PagosDetalleAdapter : RecyclerView.Adapter<PagosDetalleAdapter.VH>() {
    private var lista: List<Pago> = emptyList()

    fun setDatos(d: List<Pago>) {
        lista = d
        notifyDataSetChanged()
    }

    override fun getItemCount() = lista.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPagoDetalleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(lista[position])
    }

    inner class VH(private val binding: ItemPagoDetalleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pago: Pago) {
            binding.tvPagoMonto.text = "$ ${"%.2f".format(pago.monto)}"
            binding.tvPagoFecha.text = pago.fecha
            
            val (texto, colorRes) = when (pago.estado) {
                "Aprobado" -> "PAGADO" to R.color.colorSuccess
                "Pendiente verificación" -> "PENDIENTE" to R.color.colorWarning
                "Rechazado" -> "RECHAZADO" to R.color.colorError
                else -> pago.estado.uppercase() to R.color.colorTextSecondary
            }
            
            binding.tvPagoEstado.text = texto
            binding.tvPagoEstado.setBackgroundColor(ContextCompat.getColor(binding.root.context, colorRes))
            
            // Si el concepto existe, mostrarlo o usarlo
            binding.root.setOnClickListener {
                // Posible navegación a detalle o mostrar diálogo con referencia
            }
        }
    }
}
