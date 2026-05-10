package com.example.adminiums1.utils

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object PaymentUtils {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun calcularDiasRestantes(fechaVencimiento: String): Int {
        if (fechaVencimiento.isEmpty()) return 0
        return try {
            val fechaFin = dateFormat.parse(fechaVencimiento) ?: return 0
            val hoy = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            
            val diff = fechaFin.time - hoy.time
            TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS).toInt()
        } catch (e: Exception) {
            0
        }
    }

    fun calcularRecargo(montoBase: Double, fechaVencimiento: String, porcentajeMensual: Double = 0.10): Double {
        val diasAtraso = -calcularDiasRestantes(fechaVencimiento)
        if (diasAtraso <= 0) return 0.0
        
        // Ejemplo: 10% de recargo si se pasa de la fecha
        // Podemos hacerlo proporcional o fijo. Hagámoslo proporcional por mes de atraso simplificado
        val mesesAtraso = (diasAtraso / 30.0)
        return montoBase * porcentajeMensual * if (mesesAtraso < 1.0) 1.0 else mesesAtraso
    }
    
    fun obtenerEstadoVencimiento(fechaVencimiento: String): String {
        val dias = calcularDiasRestantes(fechaVencimiento)
        return when {
            dias < 0 -> "Vencido hace ${-dias} días"
            dias == 0 -> "Vence hoy"
            dias <= 5 -> "Vence en $dias días"
            else -> "Vence el $fechaVencimiento"
        }
    }
}
