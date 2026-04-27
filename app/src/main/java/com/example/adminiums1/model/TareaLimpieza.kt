package com.example.adminiums1.model

data class TareaLimpieza(
    val id: String = "",
    val edificioId: String = "",
    val edificioNombre: String = "",
    val area: String = "", // "Lobby", "Pasillo Piso 1-N", "Estacionamiento", "Jardín", "Elevador", "Azotea", "Cuarto de basura"
    val tipoLimpieza: String = "", // "Barrido", "Trapeado", "Desinfección", "Recolección basura", "Limpieza de vidrios", "General"
    val descripcion: String = "",
    val prioridad: String = "Normal", // "Baja", "Normal", "Alta"
    val asignadaA: String = "", // Nombre del encargado
    val solicitadaPor: String = "", // "Admin" o nombre del residente
    val completada: Boolean = false,
    val fechaAsignada: String = "",
    val fechaLimite: String = "",
    val fechaCompletada: String = "",
    val notas: String = ""
)
