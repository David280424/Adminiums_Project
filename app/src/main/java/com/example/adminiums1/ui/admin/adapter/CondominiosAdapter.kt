package com.example.adminiums1.ui.admin.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.adminiums1.databinding.ItemCondominioBinding
import com.example.adminiums1.model.Condominio

class CondominiosAdapter(private val onDelete: (Condominio) -> Unit) : RecyclerView.Adapter<CondominiosAdapter.VH>() {
    private var lista: List<Condominio> = emptyList()

    fun setDatos(nuevaLista: List<Condominio>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCondominioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(lista[position])
    }

    override fun getItemCount(): Int = lista.size

    inner class VH(private val binding: ItemCondominioBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Condominio) {
            binding.tvNombreCondominio.text = item.nombre
            binding.tvCiudadCondominio.text = item.ciudad
            binding.btnEliminarCondominio.setOnClickListener { onDelete(item) }
        }
    }
}
