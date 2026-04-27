package com.example.adminiums1.ui.admin.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.adminiums1.databinding.ItemReservacionDetalleBinding
import com.example.adminiums1.model.Reservacion

class ReservacionesDetalleAdapter(
    private val onItemClick: ((Reservacion) -> Unit)? = null
) : RecyclerView.Adapter<ReservacionesDetalleAdapter.VH>() {
    
    private var lista: List<Reservacion> = emptyList()
    
    fun setDatos(d: List<Reservacion>) { 
        lista = d
        notifyDataSetChanged() 
    }
    
    override fun getItemCount() = lista.size
    
    override fun onCreateViewHolder(p: ViewGroup, v: Int) =
        VH(ItemReservacionDetalleBinding.inflate(LayoutInflater.from(p.context), p, false))
    
    override fun onBindViewHolder(h: VH, i: Int) = h.bind(lista[i])
    
    inner class VH(private val b: ItemReservacionDetalleBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(r: Reservacion) {
            b.tvResAmenidad.text = r.amenidad
            b.tvResFecha.text = "${r.fecha} ${r.horario}"
            
            b.root.setOnClickListener { onItemClick?.invoke(r) }
        }
    }
}
