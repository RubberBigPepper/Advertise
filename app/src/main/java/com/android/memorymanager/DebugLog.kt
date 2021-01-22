package com.android.memorymanager

import android.util.Log
import com.yandex.metrica.YandexMetrica

object DebugLog {
    fun e(strTag: String, strMessage: String) {
        Log.e(strTag, strMessage)
        YandexMetrica.reportEvent("$strTag: $strMessage")
    }

    fun printStackTrace(strTag: String, ex: Exception) {
        var strMessage = ex.localizedMessage
        if (strMessage == null) strMessage = ex.message
        if (strMessage == null) strMessage = "null"
        e(strTag, strMessage)
        ex.printStackTrace()
    }
}
