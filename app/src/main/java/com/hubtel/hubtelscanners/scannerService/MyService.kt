package com.hubtel.hubtelscanners.scannerService

import android.app.Service
import android.content.Intent
import android.os.IBinder

class MyService : Service() {
    private var _deviceSelectedToPairWith  = ""
    private var _hostBluetoothAddress = ""
    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }
}
