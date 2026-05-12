package com.example.adminiums1.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.adminiums1.model.Pago
import com.example.adminiums1.model.Usuario
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfGenerator {

    fun generarEstadoCuentaPDF(context: Context, usuario: Usuario, pagos: List<Pago>, edificioNombre: String = "") {
        try {
            val document = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()

            // Header (Azul oscuro #1A237E)
            paint.color = Color.parseColor("#1A237E")
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 120f, paint)

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
            canvas.drawText("Edificio: ${if (edificioNombre.isNotEmpty()) edificioNombre else usuario.edificioId}", 40f, 200f, paint)

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
            
            // Limit to avoid overflow for now
            val pagosLimitados = pagos.take(20)

            pagosLimitados.forEachIndexed { index, pago ->
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

            val fileName = "EstadoCuenta_${usuario.nombre.replace(" ", "_")}.pdf"
            val file = File(context.cacheDir, fileName)
            document.writeTo(file.outputStream())
            document.close()

            guardarYCompartir(context, file, fileName, "Estado de Cuenta")

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al generar PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun generarReciboPagoPDF(context: Context, pago: Pago) {
        try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(400, 600, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()

            // Fondo cabecera
            paint.color = Color.parseColor("#1A237E")
            canvas.drawRect(0f, 0f, 400f, 100f, paint)

            paint.color = Color.WHITE
            paint.textSize = 20f
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("COMPROBANTE DE PAGO", 40f, 50f, paint)
            
            paint.textSize = 12f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("ADMINIUMS - ${pago.edificioNombre}", 40f, 80f, paint)

            // Cuerpo
            paint.color = Color.BLACK
            paint.textSize = 14f
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("Folio: ${pago.folio}", 40f, 140f, paint)

            paint.typeface = Typeface.DEFAULT
            paint.textSize = 12f
            canvas.drawText("Residente: ${pago.residenteNombre}", 40f, 170f, paint)
            canvas.drawText("Unidad: ${pago.unidad}", 40f, 195f, paint)
            canvas.drawText("Fecha: ${pago.fecha}", 40f, 220f, paint)
            canvas.drawText("Método: ${pago.metodoPago}", 40f, 245f, paint)
            canvas.drawText("Concepto: ${pago.concepto}", 40f, 270f, paint)

            // Línea divisoria
            paint.color = Color.LTGRAY
            canvas.drawLine(40f, 300f, 360f, 300f, paint)

            // Monto
            paint.color = Color.BLACK
            paint.textSize = 16f
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("MONTO TOTAL:", 40f, 340f, paint)
            
            paint.color = Color.parseColor("#1A237E")
            paint.textSize = 24f
            canvas.drawText("$ ${"%.2f".format(pago.monto)}", 200f, 340f, paint)

            paint.color = Color.parseColor("#4CAF50")
            paint.textSize = 14f
            canvas.drawText("ESTADO: ${pago.estado.uppercase()}", 40f, 380f, paint)

            // Footer
            paint.color = Color.GRAY
            paint.textSize = 10f
            canvas.drawText("Gracias por su pago.", 140f, 550f, paint)

            document.finishPage(page)

            val fileName = "Recibo_${pago.folio}.pdf"
            val file = File(context.cacheDir, fileName)
            document.writeTo(file.outputStream())
            document.close()

            guardarYCompartir(context, file, fileName, "Recibo de Pago")

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al generar recibo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun guardarYCompartir(context: Context, cacheFile: File, fileName: String, titulo: String) {
        try {
            // Option 1: Copy to Downloads (for user access)
            val downloadExito = copiarADescargas(context, cacheFile, fileName)

            // Option 2: Share via Intent (for sending to others)
            val authority = "${context.packageName}.provider"
            val uri = FileProvider.getUriForFile(context, authority, cacheFile)
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Compartir $titulo")
            context.startActivity(chooser)

            if (downloadExito) {
                Toast.makeText(context, "Archivo guardado en Descargas", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al procesar archivo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copiarADescargas(context: Context, sourceFile: File, fileName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri).use { output ->
                        sourceFile.inputStream().use { input ->
                            input.copyTo(output!!)
                        }
                    }
                    true
                } else false
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetFile = File(downloadsDir, fileName)
                sourceFile.copyTo(targetFile, overwrite = true)
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
