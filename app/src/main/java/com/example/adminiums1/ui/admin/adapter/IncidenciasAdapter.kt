package com.example.adminiums1.ui.admin.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.adminiums1.R
import com.example.adminiums1.model.Incidencia

class IncidenciasAdapter(private val onClick: (Incidencia) -> Unit) : RecyclerView.Adapter<IncidenciasAdapter.ViewHolder>() {

    private var items: List<Incidencia> = emptyList()

    fun setDatos(nuevas: List<Incidencia>) {
        items = nuevas
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_incidencia, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitulo = view.findViewById<TextView>(R.id.tvIncidenciaTitulo)
        private val tvResidente = view.findViewById<TextView>(R.id.tvIncidenciaResidente)
        private val tvEstado = view.findViewById<TextView>(R.id.tvIncidenciaEstado)
        private val tvFecha = view.findViewById<TextView>(R.id.tvIncidenciaFecha)
        private val tvUbicacion = view.findViewById<TextView>(R.id.tvIncidenciaUbicacion)
        private val viewPrioridad = view.findViewById<View>(R.id.viewPrioridad)

        fun bind(i: Incidencia) {
            tvTitulo.text = i.titulo
            tvResidente.text = "${i.residenteNombre} • Unidad ${i.unidad}"
            tvFecha.text = i.fecha
            tvEstado.text = i.estado
            tvUbicacion.text = i.ubicacion
            
            val colorEstado = when(i.estado) {
                "Pendiente" -> 0xFFF44336.toInt()
                "En Proceso" -> 0xFFFF9800.toInt()
                "Resuelta" -> 0xFF4CAF50.toInt()
                else -> 0xFF757575.toInt()
            }
            tvEstado.setTextColor(colorEstado)

            val colorPrioridad = when(i.prioridad) {
                "Baja" -> 0xFFBDBDBD.toInt()
                "Normal" -> 0xFF2196F3.toInt()
                "Alta" -> 0xFFFF9800.toInt()
                "Urgente" -> 0xFFF44336.toInt()
                else -> 0xFFBDBDBD.toInt()
            }
            viewPrioridad.setBackgroundColor(colorPrioridad)
        }
    }
}
