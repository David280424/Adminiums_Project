package com.example.adminiums1.ui.residente.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.adminiums1.R

class FotosAdjuntasAdapter(private val onRemove: (Int) -> Unit) : RecyclerView.Adapter<FotosAdjuntasAdapter.ViewHolder>() {

    private var items: MutableList<Uri> = mutableListOf()

    fun addFoto(uri: Uri) {
        items.add(uri)
        notifyItemInserted(items.size - 1)
    }

    fun removeFoto(position: Int) {
        if (position in items.indices) {
            items.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, items.size)
        }
    }

    fun getItems(): List<Uri> = items

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_foto_adjunta, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.ivFoto.setImageURI(items[position])
        holder.btnBorrar.setOnClickListener { onRemove(holder.adapterPosition) }
    }

    override fun getItemCount() = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivFoto: ImageView = view.findViewById(R.id.ivFoto)
        val btnBorrar: ImageButton = view.findViewById(R.id.btnBorrar)
    }
}
