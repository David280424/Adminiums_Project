
// ─────────────────────────────────────────────────────────────────────────────
// ARCHIVO: PagosDetalleAdapter.kt
// RUTA: ui/admin/adapter/PagosDetalleAdapter.kt
// ─────────────────────────────────────────────────────────────────────────────
package com.example.adminiums1.ui.admin.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.adminiums1.databinding.ItemPagoDetalleBinding
import com.example.adminiums1.model.Pago

class PagosDetalleAdapter : RecyclerView.Adapter<PagosDetalleAdapter.VH>() {
    private var lista: List<Pago> = emptyList()
    fun setDatos(d: List<Pago>) { lista = d; notifyDataSetChanged() }
    override fun getItemCount() = lista.size
    override fun onCreateViewHolder(p: ViewGroup, v: Int) =
        VH(ItemPagoDetalleBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun onBindViewHolder(h: VH, i: Int) = h.bind(lista[i])
    inner class VH(private val b: ItemPagoDetalleBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(p: Pago) {
            b.tvPagoMonto.text  = "$ ${"%.2f".format(p.monto)}"
            b.tvPagoFecha.text  = p.fecha
            val (txt, color) = when (p.estado) {
                "pagado"    -> "Pagado"    to 0xFF48BB78.toInt()
                "pendiente" -> "Pendiente" to 0xFFF56565.toInt()
                else        -> p.estado    to 0xFF718096.toInt()
            }
            b.tvPagoEstado.text = txt
            b.tvPagoEstado.setBackgroundColor(color)
        }
    }
}

