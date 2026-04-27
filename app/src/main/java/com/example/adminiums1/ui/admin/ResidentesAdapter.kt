package com.example.adminiums1.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.adminiums1.R
import com.example.adminiums1.databinding.ItemResidenteBinding
import com.example.adminiums1.model.Usuario

class ResidentesAdapter(
    private val onClick: (Usuario) -> Unit
) : RecyclerView.Adapter<ResidentesAdapter.ViewHolder>() {

    private var listaFiltrada: List<Usuario> = emptyList()

    fun setDatos(datos: List<Usuario>) {
        listaFiltrada = datos
        notifyDataSetChanged()
    }

    override fun getItemCount() = listaFiltrada.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemResidenteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(listaFiltrada[position])

    inner class ViewHolder(private val binding: ItemResidenteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(usuario: Usuario) {
            binding.tvResidenteNombre.text = usuario.nombre
            binding.tvResidenteUnidad.text = "Unidad ${usuario.unidad}"

            val iniciales = usuario.nombre
                .split(" ")
                .take(2)
                .mapNotNull { it.firstOrNull()?.toString() }
                .joinToString("")
                .uppercase()

            binding.tvResidenteAvatar.text = iniciales

            if (usuario.balance < 0) {
                binding.tvResidenteAvatar.setBackgroundResource(R.drawable.bg_avatar_red)
                binding.tvResidenteAvatar.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.colorAvatarRedText))
                binding.tvResidenteEstado.text = "-$${abs(usuario.balance)}"
                binding.tvResidenteEstado.setBackgroundResource(R.drawable.bg_badge_red)
                binding.tvResidenteEstado.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.colorError))
            } else {
                binding.tvResidenteAvatar.setBackgroundResource(R.drawable.bg_avatar_blue)
                binding.tvResidenteAvatar.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.colorAvatarBlueText))
                binding.tvResidenteEstado.text = "Al día"
                binding.tvResidenteEstado.setBackgroundResource(R.drawable.bg_badge_green)
                binding.tvResidenteEstado.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.colorSuccess))
            }

            binding.root.setOnClickListener { onClick(usuario) }
        }
        
        private fun abs(n: Double): String = "%.2f".format(kotlin.math.abs(n))
    }
}
