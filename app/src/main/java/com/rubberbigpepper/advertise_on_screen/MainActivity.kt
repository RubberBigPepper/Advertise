package com.rubberbigpepper.advertise_on_screen

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.File

class MainActivity : AppCompatActivity(), View.OnClickListener{

    private final val PERMISSION_REQUEST_CODE = 5692
    private final val OVERLAY_REQUEST_CODE = 5693

    private var showView = false
    //переменные для элементов управления
    private var serverEditText: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CheckPermissionsOrRun()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            try {
                savePrefs()
            } catch (ex: java.lang.Exception) {
            }
            if (showView) startService(true, false)
        }
    }


    private fun openSettingsWindow(){
        setContentView(R.layout.activity_main)
        serverEditText=findViewById(R.id.editTextServer)
        val cPrefs = getSharedPreferences("Common", MODE_PRIVATE)
        serverEditText?.setText(cPrefs?.getString("server","https://miner.net.ru/reklama"))
        serverEditText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                savePrefs()
                startService(false, false)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })
        findViewById<Button>(R.id.ButtonStart)?.setOnClickListener(this)
        findViewById<Button>(R.id.ButtonStop)?.setOnClickListener(this)
        savePrefs()
        startService(true, true)
       /* if (!intent.getBooleanExtra("show settings",false)){//нет необходимого параметра в интенте - просто выходим
            showView=true
            finish()
        }*/
    }

    private fun CheckPermissionsOrRun() {
        val bOverlay = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.SYSTEM_ALERT_WINDOW
        ) == PackageManager.PERMISSION_GRANTED
        val bUsage = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.PACKAGE_USAGE_STATS
        ) == PackageManager.PERMISSION_GRANTED
        val bInternet = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.INTERNET
        ) == PackageManager.PERMISSION_GRANTED
        val bNetworkState = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_NETWORK_STATE
        ) == PackageManager.PERMISSION_GRANTED
        val bWiFiState = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_WIFI_STATE
        ) == PackageManager.PERMISSION_GRANTED
        val bWriteStorage = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val bReadStorage = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val bReboot = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECEIVE_BOOT_COMPLETED
        ) == PackageManager.PERMISSION_GRANTED
        val strArNeedPermission = ArrayList<String>()
        if (!bOverlay) strArNeedPermission.add(Manifest.permission.SYSTEM_ALERT_WINDOW)
        if (!bUsage) strArNeedPermission.add(Manifest.permission.PACKAGE_USAGE_STATS)
        if (!bInternet) strArNeedPermission.add(Manifest.permission.INTERNET)
        if (!bNetworkState) strArNeedPermission.add(Manifest.permission.ACCESS_NETWORK_STATE)
        if (!bWiFiState) strArNeedPermission.add(Manifest.permission.ACCESS_WIFI_STATE)
        if (!bWriteStorage) strArNeedPermission.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (!bReadStorage) strArNeedPermission.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (!bReboot) strArNeedPermission.add(Manifest.permission.RECEIVE_BOOT_COMPLETED)
        if (strArNeedPermission.size > 0) {
            requestPermission(strArNeedPermission)
        } else {
            CheckOverlayPermissionOrRun()
        }
    }

    private fun requestPermission(strArNeedPermission: ArrayList<String>) {
        val strArList = arrayOfNulls<String>(strArNeedPermission.size)
        strArNeedPermission.toArray(strArList)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this, strArList, PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.size > 0) {
            var bOverlay = ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.SYSTEM_ALERT_WINDOW
            ) == PackageManager.PERMISSION_GRANTED
            var bInternet = ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.INTERNET
            ) == PackageManager.PERMISSION_GRANTED
            var bNetworkState = ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_NETWORK_STATE
            ) == PackageManager.PERMISSION_GRANTED
            var bWiFiState = ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_WIFI_STATE
            ) == PackageManager.PERMISSION_GRANTED
            var bWriteSettings = ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_SETTINGS
            ) == PackageManager.PERMISSION_GRANTED
            var bWriteStorage = ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            var bReadStorage = ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            var bReboot = ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECEIVE_BOOT_COMPLETED
            ) == PackageManager.PERMISSION_GRANTED
            for (n in permissions.indices) {
                if (grantResults[n] == PackageManager.PERMISSION_GRANTED) {
                    if (permissions[n] == Manifest.permission.SYSTEM_ALERT_WINDOW) bOverlay = true
                    if (permissions[n] == Manifest.permission.INTERNET) bInternet = true
                    if (permissions[n] == Manifest.permission.ACCESS_NETWORK_STATE) bNetworkState =
                        true
                    if (permissions[n] == Manifest.permission.ACCESS_WIFI_STATE) bWiFiState = true
                    if (permissions[n] == Manifest.permission.WRITE_SETTINGS) bWriteSettings = true
                    if (permissions[n] == Manifest.permission.WRITE_EXTERNAL_STORAGE) bWriteStorage = true
                    if (permissions[n] == Manifest.permission.READ_EXTERNAL_STORAGE) bReadStorage = true
                    if (permissions[n] == Manifest.permission.RECEIVE_BOOT_COMPLETED) bReboot = true
                }
            }
            CheckOverlayPermissionOrRun()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun CheckOverlayPermissionOrRun() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this.applicationContext)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivityForResult(intent, OVERLAY_REQUEST_CODE)
                return
            }
        }
        openSettingsWindow()
    }

    private fun showNotAllPermissionGranted(){
        val cBuilder: AlertDialog.Builder = android.app.AlertDialog.Builder(this)
        cBuilder.setTitle(R.string.app_name)
        cBuilder.setMessage(R.string.PermissionsNeeded)
        cBuilder.setPositiveButton("OK",
                DialogInterface.OnClickListener { dialog, _ -> finish() })
        cBuilder.setOnCancelListener(DialogInterface.OnCancelListener { finish() })
        cBuilder.create().show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            OVERLAY_REQUEST_CODE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                    openSettingsWindow()
                } else {
                    showNotAllPermissionGranted()
                }
            }
        }
    }

    private fun savePrefs() {
        val cPrefs = getSharedPreferences("Common", MODE_PRIVATE).edit()
        cPrefs.clear()
        cPrefs?.putString("server",serverEditText?.text.toString())
        cPrefs.commit()
    }

    private fun startService(bStart: Boolean, bReboot: Boolean) {
        try {
            val cIntent = Intent(this, AdvertiseService::class.java)
            if (bStart) {
                if (bReboot)
                    cIntent.action = AdvertiseService.ACTION_START_AFTERBOOT
                else
                    cIntent.action = AdvertiseService.ACTION_START
            }
            else
                cIntent.action = AdvertiseService.ACTION_UPDATE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(cIntent)
            } else startService(cIntent)
        } catch (ex: Exception) {
        }
    }

    private fun stopService() {
        try {
            val cIntent = Intent(this, AdvertiseService::class.java)
            cIntent.action = AdvertiseService.ACTION_STOP
            startService(cIntent)
        } catch (ex: Exception) {
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.ButtonStart -> {
                showView = true
                savePrefs()
                startService(true, false)
            }
            R.id.ButtonStop -> {
                showView = false
                stopService()
            }
        }
    }

}
