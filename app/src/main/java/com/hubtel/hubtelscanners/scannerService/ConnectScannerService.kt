package com.hubtel.hubtelscanners.scannerService

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.util.Log
import com.hubtel.hubtelscanners.R
import com.hubtel.hubtelscanners.scanners.ScannerSense
import java.lang.reflect.InvocationTargetException

class ConnectScannerService : Service() {
    private var _deviceSelectedToPairWith  = ""
    private var _hostBluetoothAddress = ""
    private var  scannerSense = ScannerSense(this)

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }


    override fun onCreate() {
        super.onCreate()

        scannerSense = ScannerSense(this)
        scannerSense.initScannerProperties()
        scannerSense.increaseViewCount()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {


        setUp()
        return super.onStartCommand(intent, flags, startId)



    }
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceivers()
    }
    fun setUp(){
        initBluetoothPairingProperties()
      connecToScanner()
       // registerToReceiveScannerNotification()
    }
    fun unregisterReceivers(){
        try {

        //    unregisterReceiver(broadcastReveiver)
            //unregisterReceiver(itemScanReceiver)

        } catch (e: IllegalArgumentException) {

            e.printStackTrace()
        }
    }
    fun connecToScanner(){




        val intent = Intent(ScannerSense.START_EZ_PAIR)
        // remove the bluetooth address and keep only the device friendly name
        if (_deviceSelectedToPairWith != null) {
            if (_deviceSelectedToPairWith.length > 18) {
                _deviceSelectedToPairWith =
                    _deviceSelectedToPairWith.substring(0, _deviceSelectedToPairWith?.length - 18)
            }
            intent.putExtra(ScannerSense.EXTRA_EZ_PAIR_DEVICE, _deviceSelectedToPairWith)
            intent.putExtra(ScannerSense.EXTRA_EZ_PAIR_HOST_ADDRESS, _hostBluetoothAddress)
            sendBroadcast(intent)





        } else {




            Log.d("isnull", "")
        }
    }
    private fun initBluetoothPairingProperties() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        try {

            if (bluetoothAdapter != null) {

                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    _hostBluetoothAddress = bluetoothAdapter.address
                } else {
                    //_hostBluetoothAddress = Settings.Secure.getString(context.contentResolver, "bluetooth_address")

                    if (_hostBluetoothAddress == null)
                        _hostBluetoothAddress = getBluetoothMacAddress()
                    Log.d("macaddress", getBluetoothMacAddress())
                }
                _hostBluetoothAddress = _hostBluetoothAddress?.replace(":", "")

                val pairedDevices = bluetoothAdapter.bondedDevices


                if (pairedDevices.size > 0) {
                    for (device in pairedDevices) {
                        if (device.name.contains("Socket") || device.address.contains("Socket")) {
                            _deviceSelectedToPairWith = device.name + "\n" + device.address
                            Log.d(javaClass.toString(), "when looping devices $_deviceSelectedToPairWith")
                        }
                    }

                }
            } else {
                val noDevices = resources.getText(R.string.none_paired).toString()
                _deviceSelectedToPairWith = noDevices

            }
        } catch (e: NullPointerException) {
            e.printStackTrace()


        }


    }
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private fun getBluetoothMacAddress(): String {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        var bluetoothMacAddress = ""
        try {
            val mServiceField = bluetoothAdapter.javaClass.getDeclaredField("mService")
            mServiceField.isAccessible = true

            val btManagerService = mServiceField.get(bluetoothAdapter)

            if (btManagerService != null) {
                bluetoothMacAddress =
                    btManagerService.javaClass.getMethod("getAddress").invoke(btManagerService) as String
            }
        } catch (ignore: NoSuchFieldException) {

        } catch (ignore: NoSuchMethodException) {
        } catch (ignore: IllegalAccessException) {
        } catch (ignore: InvocationTargetException) {
        }

        return bluetoothMacAddress
    }


}
