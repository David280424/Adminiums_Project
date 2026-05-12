package com.example.adminiums1.ui.admin.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.adminiums1.R
import com.example.adminiums1.databinding.ItemPagoHistorialBinding
import com.example.adminiums1.model.Pago

class PagosDetalleAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var lista: List<Any> = emptyList()

    private val VIEW_TYPE_HEADER = 0
    private val VIEW_TYPE_PAGO = 1

    fun setDatos(d: List<Any>) {
        lista = d
        notifyDataSetChanged()
    }

    override fun getItemCount() = lista.size

    override fun getItemViewType(position: Int): Int {
        return if (lista[position] is String) VIEW_TYPE_HEADER else VIEW_TYPE_PAGO
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItemPagoHistorialBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return if (viewType == VIEW_TYPE_HEADER) HeaderVH(binding) else PagoVH(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = lista[position]
        if (holder is PagoVH && item is Pago) {
            holder.bind(item)
        } else if (holder is HeaderVH && item is String) {
            holder.bind(item)
        }
    }

    inner class HeaderVH(private val binding: ItemPagoHistorialBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(titulo: String) {
            binding.layoutIconMethod.visibility = View.GONE
            binding.tvPagoFolio.text = titulo
            binding.tvPagoFolio.setTextColor(ContextCompat.getColor(binding.root.context, R.color.colorPrimary))
            binding.tvPagoConcepto.visibility = View.GONE
            binding.tvPagoMonto.visibility = View.GONE
            binding.tvPagoFecha.visibility = View.GONE
            binding.tvPagoEstadoBadge.visibility = View.GONE
            binding.root.setCardBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.colorSurfaceSecondary))
            binding.root.strokeWidth = 0
            
            val params = binding.root.layoutParams as ViewGroup.MarginLayoutParams
            params.setMargins(params.leftMargin, 24, params.rightMargin, 8)
            binding.root.layoutParams = params
        }
    }

    inner class PagoVH(private val binding: ItemPagoHistorialBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pago: Pago) {
            binding.layoutIconMethod.visibility = View.VISIBLE
            binding.tvPagoConcepto.visibility = View.VISIBLE
            binding.tvPagoMonto.visibility = View.VISIBLE
            binding.tvPagoFecha.visibility = View.VISIBLE
            binding.tvPagoEstadoBadge.visibility = View.VISIBLE
            binding.root.setCardBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.white))
            binding.root.strokeWidth = 1
            
            val params = binding.root.layoutParams as ViewGroup.MarginLayoutParams
            params.setMargins(params.leftMargin, 6, params.rightMargin, 6)
            binding.root.layoutParams = params

            // FIX 5: Bind fields using item_pago_historial.xml
            binding.tvPagoFolio.text = if (pago.folio.isEmpty()) "Sin folio" else pago.folio
            binding.tvPagoConcepto.text = if (pago.concepto.isEmpty()) "Cuota mensual" else pago.concepto
            binding.tvPagoMonto.text = "$ ${"%.2f".format(pago.monto)}"
            binding.tvPagoFecha.text = pago.fecha
            
            val (texto, colorRes, bgRes) = when (pago.estado) {
                "Aprobado" -> Triple("PAGADO", R.color.colorSuccess, R.drawable.bg_badge_green)
                "Pendiente verificación" -> Triple("PENDIENTE", R.color.colorWarning, 0)
                "Rechazado" -> Triple("RECHAZADO", R.color.colorError, 0)
                else -> Triple(pago.estado.uppercase(), R.color.colorTextSecondary, 0)
            }
            
            binding.tvPagoEstadoBadge.text = texto
            binding.tvPagoEstadoBadge.setTextColor(ContextCompat.getColor(binding.root.context, colorRes))
            if (bgRes != 0) {
                binding.tvPagoEstadoBadge.setBackgroundResource(bgRes)
            } else {
                binding.tvPagoEstadoBadge.background = null
            }

            binding.tvMethodIcon.text = when (pago.metodoPago) {
                "OXXO" -> "🏪"
                "Transferencia" -> "🏦"
                "Tarjeta" -> "💳"
                else -> "💵"
            }

            if (pago.referencia.isNotEmpty()) {
                binding.tvPagoDetalleAdicional.visibility = View.VISIBLE
                binding.tvPagoDetalleAdicional.text = "Ref: ${pago.referencia}"
            } else {
                binding.tvPagoDetalleAdicional.visibility = View.GONE
            }
        }
    }
}
