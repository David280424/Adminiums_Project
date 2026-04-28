package com.example.adminiums1.utils

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.adminiums1.model.Pago
import com.example.adminiums1.model.Usuario
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object PdfGenerator {

    fun generarEstadoCuentaPDF(context: Context, usuario: Usuario, pagos: List<Pago>) {
        try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()

            // Header (Azul oscuro #1A237E)
            paint.color = Color.parseColor("#1A237E")
            canvas.drawRect(0f, 0f, 595f, 120f, paint)

            paint.color = Color.WHITE
            paint.textSize = 26f
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("ADMINIUMS", 40f, 60f, paint)

            paint.textSize = 13f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("Estado de Cuenta", 40f, 90f, paint)

            val fechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            paint.textSize = 10f
            canvas.drawText("Fecha de emisión: $fechaHoy", 450f, 90f, paint)

            // Datos del Residente
            paint.color = Color.BLACK
            paint.textSize = 14f
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("Residente: ${usuario.nombre}", 40f, 150f, paint)
            
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 12f
            canvas.drawText("Unidad: ${usuario.unidad}", 40f, 175f, paint)
            canvas.drawText("Edificio: ${usuario.edificioId}", 40f, 200f, paint)

            // Card de Resumen
            paint.style = Paint.Style.STROKE
            paint.color = Color.LTGRAY
            canvas.drawRect(40f, 240f, 555f, 340f, paint)
            
            paint.style = Paint.Style.FILL
            paint.textSize = 12f
            paint.color = Color.DKGRAY
            canvas.drawText("Balance actual:", 60f, 270f, paint)
            
            val balanceColor = if (usuario.balance >= 0) "#4CAF50" else "#F44336"
            paint.color = Color.parseColor(balanceColor)
            paint.textSize = 24f
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("$ ${"%.2f".format(usuario.balance)}", 60f, 305f, paint)

            // Tabla de pagos
            paint.color = Color.parseColor("#EEEEEE")
            canvas.drawRect(40f, 360f, 555f, 390f, paint)
            
            paint.color = Color.BLACK
            paint.textSize = 11f
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("Folio", 45f, 380f, paint)
            canvas.drawText("Fecha", 130f, 380f, paint)
            canvas.drawText("Método", 230f, 380f, paint)
            canvas.drawText("Monto", 380f, 380f, paint)
            canvas.drawText("Estado", 480f, 380f, paint)

            var y = 410f
            paint.typeface = Typeface.DEFAULT
            pagos.forEachIndexed { index, pago ->
                if (index % 2 != 0) {
                    paint.color = Color.parseColor("#F8F9FA")
                    canvas.drawRect(40f, y - 20f, 555f, y + 10f, paint)
                }
                paint.color = Color.BLACK
                canvas.drawText(pago.folio.takeLast(10), 45f, y, paint)
                canvas.drawText(pago.fecha.split(" ")[0], 130f, y, paint)
                canvas.drawText(pago.metodoPago, 230f, y, paint)
                canvas.drawText("$ ${"%.2f".format(pago.monto)}", 380f, y, paint)
                canvas.drawText(pago.estado, 480f, y, paint)
                y += 30f
            }

            // Footer
            paint.color = Color.GRAY
            paint.textSize = 9f
            canvas.drawText("Generado por Adminiums — Sistema de Administración de Condominios", 150f, 820f, paint)

            document.finishPage(page)

            val file = File(context.getExternalFilesDir(null), "EstadoCuenta_${usuario.nombre.replace(" ", "_")}.pdf")
            document.writeTo(file.outputStream())
            document.close()

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir Estado de Cuenta"))

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
