package com.example.adminiums1.model

data class Incidencia(
    val id: String = "",
    val residenteUid: String = "",
    val residenteNombre: String = "",
    val unidad: String = "",
    val edificioId: String = "",
    val edificioNombre: String = "",
    val categoria: String = "", // "Plomería", "Electricidad", "Ruido", "Seguridad", "Limpieza", "Otro"
    val prioridad: String = "Normal", // "Baja", "Normal", "Alta", "Urgente"
    val titulo: String = "",
    val descripcion: String = "",
    val ubicacion: String = "", // "Unidad propia", "Pasillo", "Estacionamiento", "Jardín", "Elevador", "Otro"
    val fecha: String = "",
    val fechaResolucion: String = "",
    val estado: String = "Pendiente",
    val respuestaAdmin: String = "",
    val fotoUrl: String = ""
)
