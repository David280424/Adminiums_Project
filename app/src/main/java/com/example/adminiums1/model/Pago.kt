package com.example.adminiums1.model

data class Pago(
    val id: String = "",
    val residenteUid: String = "",
    val residenteNombre: String = "",
    val unidad: String = "",
    val edificioId: String = "",
    val edificioNombre: String = "",
    val monto: Double = 0.0,
    val concepto: String = "Cuota mensual", // "Cuota mensual", "Cuota extraordinaria", "Multa", "Mantenimiento", "Otro"
    val metodoPago: String = "Efectivo",    // "Efectivo", "Transferencia", "Tarjeta", "App"
    val referencia: String = "",
    val fecha: String = "",
    val folio: String = "",
    val estado: String = ""
)
