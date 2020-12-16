package com.rubberbigpepper.advertise_on_screen

import android.graphics.Color
import android.net.Uri
import java.io.File
import java.util.regex.Pattern

class AdvData {
    //класс данных для показа, строка или картинка

    var text: String? = null //текст для показа
    var imagePath: String? = null //путь к картинке
    var showCount = 1//количество показов, по умолчанию 1 раз
    var textSize = 30.0f //размер текста
    var textColor = Color.GRAY //цвет текста
    var textBackground = Color.TRANSPARENT//цвет фона
    var marginBottom = 0 // отступ снизу по вертикали
    var height = 120 //высота блока
    var pause = 0 //пауза в секундах до показа следующего


    val isGif: Boolean //проверим на gif
        get() {
            imagePath?.let {
                //val path = it.toString()
                return it.indexOf(".gif", 0, true) != -1
            }
            return false
        }

    fun readFromData(advData: AdvData) {
        this.textColor = advData.textColor
        this.text = advData.text
        this.imagePath = advData.imagePath
        this.showCount = advData.showCount
        this.textSize = advData.textSize
        this.textBackground = advData.textBackground
        this.marginBottom = advData.marginBottom
        this.height = height
    }
}
