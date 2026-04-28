package com.example.adminiums1.model

data class Pago(
    val id: String = "",
    val residenteUid: String = "",
    val residenteNombre: String = "",
    val unidad: String = "",
    val edificioId: String = "",
    val edificioNombre: String = "",
    val monto: Double = 0.0,
    val concepto: String = "Cuota mensual",
    val metodoPago: String = "",       // "Efectivo-OXXO", "Tarjeta", "Transferencia", "Efectivo directo"
    val referencia: String = "",       // número de referencia OXXO o transferencia
    val ultimosCuatroDigitos: String = "", // solo para tarjeta
    val marcaTarjeta: String = "",     // "Visa", "Mastercard", "Amex", solo para tarjeta
    val fecha: String = "",
    val folio: String = "",
    val estado: String = "",           // "Aprobado", "Pendiente verificación", "Rechazado"
    val comprobante: String = ""       // reservado para URL foto futura
)
