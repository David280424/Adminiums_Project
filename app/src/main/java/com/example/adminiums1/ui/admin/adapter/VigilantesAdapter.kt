
// ─────────────────────────────────────────────────────────────────────────────
// ARCHIVO: VigilantesAdapter.kt
// RUTA: ui/admin/adapter/VigilantesAdapter.kt
// ─────────────────────────────────────────────────────────────────────────────
package com.example.adminiums1.ui.admin.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.adminiums1.databinding.ItemVigilanteBinding
import com.example.adminiums1.model.Usuario

class VigilantesAdapter(
    private val onClick: (Usuario) -> Unit
) : RecyclerView.Adapter<VigilantesAdapter.VH>() {
    private var lista: List<Usuario> = emptyList()
    fun setDatos(d: List<Usuario>) { lista = d; notifyDataSetChanged() }
    override fun getItemCount() = lista.size
    override fun onCreateViewHolder(p: ViewGroup, v: Int) =
        VH(ItemVigilanteBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun onBindViewHolder(h: VH, i: Int) = h.bind(lista[i])
    inner class VH(private val b: ItemVigilanteBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(u: Usuario) {
            b.tvVigilanteNombre.text = u.nombre
            b.tvVigilanteEmail.text  = u.email
            b.root.setOnClickListener { onClick(u) }
        }
    }
}