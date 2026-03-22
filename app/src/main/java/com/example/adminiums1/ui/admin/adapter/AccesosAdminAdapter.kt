// ─────────────────────────────────────────────────────────────────────────────
// ARCHIVO: AccesosAdminAdapter.kt
// RUTA: ui/admin/adapter/AccesosAdminAdapter.kt
// ─────────────────────────────────────────────────────────────────────────────
package com.example.adminiums1.ui.admin.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.adminiums1.databinding.ItemAccesoBinding
import com.example.adminiums1.model.RegistroAcceso

class AccesosAdminAdapter : RecyclerView.Adapter<AccesosAdminAdapter.VH>() {
    private var lista: List<RegistroAcceso> = emptyList()
    fun setDatos(d: List<RegistroAcceso>) { lista = d; notifyDataSetChanged() }
    override fun getItemCount() = lista.size
    override fun onCreateViewHolder(p: ViewGroup, v: Int) =
        VH(ItemAccesoBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun onBindViewHolder(h: VH, i: Int) = h.bind(lista[i])
    inner class VH(private val b: ItemAccesoBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(a: RegistroAcceso) {
            b.tvAccesoNombre.text = a.residenteNombre
            b.tvAccesoUnidad.text = "Unidad: ${a.unidad}"
            b.tvAccesoHora.text   = a.hora
            val (icono, color) = when (a.metodo) {
                "qr"     -> "📱 QR"     to 0xFF2D3748.toInt()
                "manual" -> "✋ Manual" to 0xFF4A5568.toInt()
                else     -> "•"         to 0xFF718096.toInt()
            }
            b.tvAccesoMetodo.text = icono
            b.tvAccesoMetodo.setBackgroundColor(color)
        }
    }
}
