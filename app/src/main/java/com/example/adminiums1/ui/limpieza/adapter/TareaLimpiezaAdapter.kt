package com.example.adminiums1.ui.limpieza.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.adminiums1.R
import com.example.adminiums1.databinding.ItemTareaLimpiezaBinding
import com.example.adminiums1.model.TareaLimpieza

import android.view.View
import com.example.adminiums1.ui.limpieza.adapter.TareaLimpiezaAdapter.TareaViewHolder

class TareaLimpiezaAdapter(
    private val onTareaClick: (TareaLimpieza) -> Unit,
    private val onDeleteClick: (TareaLimpieza) -> Unit
) : RecyclerView.Adapter<TareaLimpiezaAdapter.TareaViewHolder>() {

    private var tareas = listOf<TareaLimpieza>()
    private var isUserAdmin = false

    fun setTareas(nuevasTareas: List<TareaLimpieza>, isAdmin: Boolean = false) {
        tareas = nuevasTareas
        isUserAdmin = isAdmin
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TareaViewHolder {
        val binding = ItemTareaLimpiezaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TareaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TareaViewHolder, position: Int) {
        holder.bind(tareas[position])
    }

    override fun getItemCount(): Int = tareas.size

    inner class TareaViewHolder(private val binding: ItemTareaLimpiezaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(tarea: TareaLimpieza) {
            binding.tvAreaNombre.text = tarea.area
            binding.tvTipoLimpieza.text = tarea.tipoLimpieza
            binding.tvAsignadaA.text = if (tarea.asignadaA.isEmpty()) "Sin asignar" else "Asignada a: ${tarea.asignadaA}"
            binding.tvSolicitadaPor.text = "Solicitado por: ${tarea.solicitadaPor}"
            binding.chipFechaLimite.text = tarea.fechaLimite
            
            // Color de prioridad
            val color = when (tarea.prioridad) {
                "Alta" -> R.color.colorEstadoPendiente
                "Normal" -> R.color.colorEstadoEnProceso
                else -> R.color.colorTextTertiary
            }
            binding.viewPrioridad.setBackgroundColor(ContextCompat.getColor(binding.root.context, color))

            binding.btnDelete.visibility = if (isUserAdmin) View.VISIBLE else View.GONE
            binding.btnDelete.setOnClickListener { onDeleteClick(tarea) }

            binding.root.setOnClickListener { onTareaClick(tarea) }
        }
    }
}
