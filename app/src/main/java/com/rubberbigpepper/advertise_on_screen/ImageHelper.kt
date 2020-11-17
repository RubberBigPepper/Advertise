package com.rubberbigpepper.advertise_on_screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer

class ImageHelper {
    companion object {
        val LOGO_FILENAME = "internal_file"

        fun RemoveInternalFile(context: Context) {
            try {
                context.deleteFile(LOGO_FILENAME)
            } catch (ex: Exception) {
            }
        }

        fun CopyFileToInternalFileDir(cContext: Context, uriImage: Uri?) { //копируем файл с Uri во внутреннее хранилище
            try {
                val stream = cContext.contentResolver.openInputStream(uriImage!!)
                val fos = cContext.openFileOutput(LOGO_FILENAME, Context.MODE_PRIVATE)
                val byArBuffer = ByteArray(65536)
                while (true) {
                    val nRead = stream!!.read(byArBuffer)
                    if (nRead <= 0) break
                    fos.write(byArBuffer, 0, nRead)
                }
                stream.close()
                fos.close()
            } catch (ex: Exception) {
            }
        }

        fun applyImage(context: Context, imageView: ImageView, uriImage: Uri?) { //Файл извне
            RemoveInternalFile(context)
            CopyFileToInternalFileDir(context,uriImage)
            applyImage(context,imageView)
        }

        fun applyImage(context: Context, imageView: ImageView) { //Файл из внутреннего хранилища
            try {
                val stream: InputStream = context.openFileInput(LOGO_FILENAME)
                val buffer = ByteArray(65536)
                val imageBuffer = ByteArrayOutputStream()
                while (true) {
                    val nRead = stream!!.read(buffer)
                    if (nRead <= 0) break
                    imageBuffer.write(buffer,0,nRead)
                }
                stream.close()
                Glide
                        .with(context)
                        .asGif()
                        .load(imageBuffer.toByteArray())
                        .into(imageView)
            } catch (ex: OutOfMemoryError) {
                ex.printStackTrace()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}