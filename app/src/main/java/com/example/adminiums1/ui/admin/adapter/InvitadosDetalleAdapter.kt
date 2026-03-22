
// ─────────────────────────────────────────────────────────────────────────────
// ARCHIVO: InvitadosDetalleAdapter.kt
// RUTA: ui/admin/adapter/InvitadosDetalleAdapter.kt
// ─────────────────────────────────────────────────────────────────────────────
package com.example.adminiums1.ui.admin.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.adminiums1.databinding.ItemInvitadoDetalleBinding
import com.example.adminiums1.model.Visitante

class InvitadosDetalleAdapter : RecyclerView.Adapter<InvitadosDetalleAdapter.VH>() {
    private var lista: List<Visitante> = emptyList()
    fun setDatos(d: List<Visitante>) { lista = d; notifyDataSetChanged() }
    override fun getItemCount() = lista.size
    override fun onCreateViewHolder(p: ViewGroup, v: Int) =
        VH(ItemInvitadoDetalleBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun onBindViewHolder(h: VH, i: Int) = h.bind(lista[i])
    inner class VH(private val b: ItemInvitadoDetalleBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(v: Visitante) {
            b.tvInvitadoNombre.text  = v.nombre
            b.tvInvitadoVigencia.text = "Vigencia: ${v.vigencia}"
            val (txt, color) = if (v.validado)
                "Validado" to 0xFF48BB78.toInt()
            else
                "Pendiente" to 0xFFF57F17.toInt()
            b.tvInvitadoEstado.text = txt
            b.tvInvitadoEstado.setBackgroundColor(color)
        }
    }
}
