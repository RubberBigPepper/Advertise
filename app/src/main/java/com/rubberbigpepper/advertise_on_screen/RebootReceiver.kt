package com.rubberbigpepper.advertise_on_screen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class RebootReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action==Intent.ACTION_BOOT_COMPLETED){
            try {
                val cIntent = Intent(context, AdvertiseService::class.java)
                cIntent.action = AdvertiseService.ACTION_START_AFTERBOOT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context?.startForegroundService(cIntent)
                } else context?.startService(cIntent)
            } catch (ex: Exception) {
            }
        }
    }
}