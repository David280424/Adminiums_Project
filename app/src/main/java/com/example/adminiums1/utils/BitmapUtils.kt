package com.example.adminiums1.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object BitmapUtils {

    fun guardarYCompartirQR(context: Context, bitmap: Bitmap, nombreVisitante: String) {
        try {
            val fileName = "QR_Acceso_${nombreVisitante.replace(" ", "_")}.png"
            val cacheFile = File(context.cacheDir, fileName)
            
            FileOutputStream(cacheFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            // Guardar en Galería/Descargas
            val guardadoExito = guardarEnGaleria(context, bitmap, fileName)

            // Compartir
            val authority = "${context.packageName}.provider"
            val uri = FileProvider.getUriForFile(context, authority, cacheFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Código de acceso para $nombreVisitante")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Compartir código QR"))

            if (guardadoExito) {
                Toast.makeText(context, "QR guardado en la galería", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al compartir QR: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun guardarEnGaleria(context: Context, bitmap: Bitmap, fileName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Adminiums")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri).use { output ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output!!)
                    }
                    true
                } else false
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val adminiumsDir = File(imagesDir, "Adminiums")
                if (!adminiumsDir.exists()) adminiumsDir.mkdirs()
                
                val targetFile = File(adminiumsDir, fileName)
                FileOutputStream(targetFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
