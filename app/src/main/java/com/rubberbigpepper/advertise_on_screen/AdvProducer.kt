package com.rubberbigpepper.advertise_on_screen

import android.content.Context
import android.graphics.Color
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.net.URL
import java.nio.charset.Charset
import java.util.regex.Pattern

class AdvProducer (context: Context) {//класс будет выдавать следущий элемент, возможно внутри будет хранить и обновлять элементы
    private var index: Int = 0
    private var entries: MutableList<AdvData> = mutableListOf()
    private val descriptionFileName="description.txt"
    private var loop=false
    private var mContext = context
    private var foundNewSource=false

    companion object {
        @JvmStatic
        val textKey = "text"
        @JvmStatic
        val imagePathKey = "image"
        @JvmStatic
        val showCountKey = "showCount"
        @JvmStatic
        val textSizeKey = "textSize"
        @JvmStatic
        val textColorKey = "textColor"
        @JvmStatic
        val textBackgroundKey = "textBackground"
        @JvmStatic
        val shiftYKey = "shiftY"
        @JvmStatic
        val newSource = "source"
    }

    fun next(): AdvData? {
        entries.size?.let {
            if (index>=it&&loop)
                index=0;
            if (index>=0&&index<it)
                return entries.get(index++)
        }
        return null
    }

    private fun FolderPath(context: Context): String {
        var fileName = context.filesDir.absolutePath
        if (fileName.length > 0 && fileName[fileName.length-1] != '/')
            fileName += "/"
        return fileName

    }

    private fun clearFolder(folderFile: File){//удаляем все файлы и папки в указанной
        val entries=folderFile.listFiles()
        entries?.let {
            for (entry in it){
                if (entry.isDirectory){
                    clearFolder(entry)//нашли подпапку, удаляем рекурсивно
                }
                entry.delete()
            }
        }
    }

    fun clearFolder(context: Context){//чистка папки
        val rootFolder=context.filesDir
        clearFolder(rootFolder)
    }

    private fun readInternalFolder(context: Context){//чтение уже внутренней папки
        //readFolder(context.filesDir)
        var rootFolder=FolderPath(context)
        entries.clear()
        index=0
        foundNewSource=false
        val data=readFromFile(File(rootFolder+descriptionFileName))
        data?.let {
            entries = data
        }
        //readFolder(File(rootFolder))
    }

    private fun readFromFile(file: File): MutableList<AdvData>?{//чтение данных из файла. Простой текстовый файл
        try {
            var advData = AdvData()
            var data: MutableList<AdvData> = mutableListOf()
            file.forEachLine(Charset.forName("windows-1251")) {
                var row=it.trim()
                if (row.length==0) {//новый фрагмент
                    data.add(advData)
                    val newData = AdvData()
                    newData.readFromData(advData)
                    advData=newData
                }
                else {
                    val fields = it.split(Pattern.compile("="), 2)
                    if (fields.size == 2) {
                        val key = fields[0].trim()
                        var value = fields[1].trim()
                        if (key.equals(AdvProducer.newSource, ignoreCase = true)) {//нашли новый адрес сервера
                            val cPrefs = mContext.getSharedPreferences("Common",
                                AppCompatActivity.MODE_PRIVATE
                            ).edit()
                            cPrefs?.putString("server",value)
                            cPrefs.commit()
                            foundNewSource=true
                        }
                        if (key.equals(AdvProducer.textKey, ignoreCase = true)) {
                            advData.text = value
                        }
                        if (key.equals(AdvProducer.imagePathKey, ignoreCase = true)) {
                            advData.imagePath = value
                        }
                        if (key.equals(AdvProducer.showCountKey, ignoreCase = true)) {
                            value.toIntOrNull()?.let { advData.showCount = it }
                        }
                        if (key.equals(AdvProducer.textSizeKey, ignoreCase = true)) {
                            value.toIntOrNull()?.let { advData.textSize = it.toFloat() }
                        }
                        if (key.equals(AdvProducer.textColorKey, ignoreCase = true)) {
                            advData.textColor=makeColorFromString(value)
                        }
                        if (key.equals(AdvProducer.textBackgroundKey, ignoreCase = true)) {
                            advData.textBackground=makeColorFromString(value)
                        }
                        if (key.equals(AdvProducer.shiftYKey, ignoreCase = true)) {
                            value.toIntOrNull()?.let { advData.shiftY = it }
                        }
                    }
                }
            }
            data.add(advData)
            return data
        }
        catch (ex: Exception){
            ex.printStackTrace()
        }
        return null
    }

    fun readNextDataFromServer(context: Context, address: String, callback: (Boolean)->Unit){//чтение данных с сервера
        GlobalScope.launch(Dispatchers.Default, CoroutineStart.DEFAULT, null, {
            entries.clear()
            clearFolder(context)
            try {
                val url=URL(address)
                url.let {
                    val text = it.readText(Charset.forName("windows-1251"))
                    var fileName = FolderPath(context)
                    fileName += descriptionFileName
                    File(fileName).writeText(text, Charset.forName("windows-1251"))
                    readInternalFolder(context)
                }
            }
            catch (ex: java.lang.Exception){
                ex.printStackTrace()
            }
            callback(foundNewSource)
        })
    }

    private fun makeColorFromString(colorStr: String): Int {
        var str=colorStr
        if (str.indexOf("#", ignoreCase = true)!=-1 || str.indexOf("h", ignoreCase = true)!=-1){//16тиричное число
            str = colorStr.replace("#","").replace("h","", ignoreCase = true)
            str.toLongOrNull(radix = 16)?.let {
                var temp = it
                if ((temp and 0xFF000000) == 0L)//не задана альфа, поставим ее в 255
                    temp = temp or 0xFF000000
                return (temp and 0xffffffff).toInt()
            }
        }
        else{
            val argb = str.split(Pattern.compile(","), 4)
            when(argb.size){
                3->{//rgb
                    val red = argb[0].toIntOrNull()
                    val green = argb[1].toIntOrNull()
                    val blue = argb[2].toIntOrNull()
                    if (red!=null && green !=null && blue !=null){
                        return Color.rgb(red!!,green!!, blue!!)
                    }
                }
                4->{
                    val alpha = argb[0].toIntOrNull()
                    val red = argb[1].toIntOrNull()
                    val green = argb[2].toIntOrNull()
                    val blue = argb[3].toIntOrNull()
                    if (alpha!=null && red!=null && green !=null && blue !=null){
                        return Color.argb(alpha!!, red!!,green!!, blue!!)
                    }
                }
                else->str.toIntOrNull()?.let { return it }
            }
        }
        return Color.TRANSPARENT
    }
}