package com.wifi.service

import android.app.Application
import com.yandex.metrica.YandexMetrica

import com.yandex.metrica.YandexMetricaConfig

class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        // Creating an extended library configuration.
        val config = YandexMetricaConfig.newConfigBuilder("bcbad8c0-e1e8-4400-b0f7-c2fdab153ee7").build()
        // Initializing the AppMetrica SDK.
        YandexMetrica.activate(applicationContext, config)
        // Automatic tracking of user activity.
        YandexMetrica.enableActivityAutoTracking(this)
    }
}